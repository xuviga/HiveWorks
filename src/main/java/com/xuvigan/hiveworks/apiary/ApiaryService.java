package com.xuvigan.hiveworks.apiary;

import com.xuvigan.hiveworks.HiveWorksPlugin;
import com.xuvigan.hiveworks.hive.HiveData;
import com.xuvigan.hiveworks.hive.HivePos;
import com.xuvigan.hiveworks.production.FlowerScanner;
import com.xuvigan.hiveworks.util.SchedulerAdapter;
import org.bukkit.Tag;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Named apiaries: 3+ hives, 3+ flower species and a fence ring grant shared bonuses.
 */
public final class ApiaryService {

    public enum CreateResult {
        CREATED,
        INVALID_NAME,
        NEED_HIVES,
        NEED_FLOWERS,
        NEED_FENCE,
        ALREADY
    }

    private final HiveWorksPlugin plugin;
    private final Map<UUID, Apiary> apiaries = new LinkedHashMap<>();
    private final Map<UUID, Integer> memberCounts = new HashMap<>();
    private final Map<String, Optional<Apiary>> hiveApiary = new HashMap<>();

    public ApiaryService(HiveWorksPlugin plugin) {
        this.plugin = plugin;
    }

    public void load() {
        for (Apiary apiary : plugin.storage().loadApiaries()) {
            apiaries.put(apiary.id(), apiary);
        }
        invalidate();
        plugin.getLogger().info("Loaded " + apiaries.size() + " apiaries.");
    }

    /** Called whenever a hive or apiary changes. */
    public void invalidate() {
        memberCounts.clear();
        hiveApiary.clear();
    }

    public int count() {
        return apiaries.size();
    }

    public CreateResult create(Player player, String rawName) {
        String name = sanitizeName(rawName);
        if (!isValidName(name)) {
            return CreateResult.INVALID_NAME;
        }
        HivePos center = HivePos.from(player.getLocation().getBlock());
        if (findNear(player.getUniqueId(), center).isPresent()) {
            return CreateResult.ALREADY;
        }
        if (ownedHives(player, center).size() < plugin.configManager().apiaryMinHives()) {
            return CreateResult.NEED_HIVES;
        }
        Block block = player.getLocation().getBlock();
        if (FlowerScanner.scan(block, 8).species() < plugin.configManager().apiaryMinFlowers()) {
            return CreateResult.NEED_FLOWERS;
        }
        if (countFences(player.getWorld(), center, 6) < plugin.configManager().apiaryMinFences()) {
            return CreateResult.NEED_FENCE;
        }

        Apiary apiary = new Apiary(
                UUID.randomUUID(),
                player.getUniqueId(),
                name,
                center,
                System.currentTimeMillis()
        );
        apiaries.put(apiary.id(), apiary);
        plugin.storage().saveApiary(apiary);
        invalidate();
        return CreateResult.CREATED;
    }

    public boolean disband(Player player) {
        Optional<Apiary> found = findFor(player);
        if (found.isEmpty()) {
            return false;
        }
        Apiary apiary = found.get();
        apiaries.remove(apiary.id());
        plugin.storage().deleteApiary(apiary.id());
        invalidate();
        return true;
    }

    public Optional<Apiary> findFor(Player player) {
        return findNear(player.getUniqueId(), HivePos.from(player.getLocation().getBlock()));
    }

    public Optional<Apiary> apiaryFor(HiveData hive) {
        String key = hive.pos().key();
        Optional<Apiary> cached = hiveApiary.get(key);
        if (cached != null) {
            return cached;
        }
        Apiary best = null;
        double bestDistance = Double.MAX_VALUE;
        double radius = plugin.configManager().apiaryRadius();
        for (Apiary apiary : apiaries.values()) {
            if (!apiary.owner().equals(hive.owner())) {
                continue;
            }
            double distance = distanceSquared(apiary.center(), hive.pos());
            if (distance > radius * radius) {
                continue;
            }
            if (memberCount(apiary) < plugin.configManager().apiaryMinHives()) {
                continue;
            }
            if (distance < bestDistance) {
                best = apiary;
                bestDistance = distance;
            }
        }
        Optional<Apiary> result = Optional.ofNullable(best);
        hiveApiary.put(key, result);
        return result;
    }

    public int memberCount(Apiary apiary) {
        Integer cached = memberCounts.get(apiary.id());
        if (cached != null) {
            return cached;
        }
        int count = 0;
        double radius = plugin.configManager().apiaryRadius();
        for (HiveData hive : plugin.hives().all()) {
            if (!hive.owner().equals(apiary.owner())) {
                continue;
            }
            if (distanceSquared(apiary.center(), hive.pos()) <= radius * radius) {
                count++;
            }
        }
        memberCounts.put(apiary.id(), count);
        return count;
    }

    public double speedBonus(HiveData hive) {
        return apiaryFor(hive).isPresent() ? plugin.configManager().apiarySpeedBonus() : 1.0;
    }

    public double qualityBonus(HiveData hive) {
        return apiaryFor(hive).isPresent() ? plugin.configManager().apiaryQualityBonus() : 0.0;
    }

    public static String sanitizeName(String raw) {
        if (raw == null) {
            return "";
        }
        String name = raw.replace('<', ' ').replace('>', ' ').trim();
        return name.length() > 24 ? name.substring(0, 24) : name;
    }

    public static boolean isValidName(String name) {
        return name != null && name.length() >= 3 && name.length() <= 24;
    }

    public static double distanceSquared(HivePos first, HivePos second) {
        if (!first.world().equals(second.world())) {
            return Double.MAX_VALUE;
        }
        double dx = first.x() - second.x();
        double dy = first.y() - second.y();
        double dz = first.z() - second.z();
        return dx * dx + dy * dy + dz * dz;
    }

    private Optional<Apiary> findNear(UUID owner, HivePos center) {
        double radius = plugin.configManager().apiaryRadius();
        Apiary best = null;
        double bestDistance = Double.MAX_VALUE;
        for (Apiary apiary : apiaries.values()) {
            if (!apiary.owner().equals(owner)) {
                continue;
            }
            double distance = distanceSquared(apiary.center(), center);
            if (distance <= radius * radius && distance < bestDistance) {
                best = apiary;
                bestDistance = distance;
            }
        }
        return Optional.ofNullable(best);
    }

    private List<HiveData> ownedHives(Player player, HivePos center) {
        List<HiveData> hives = new ArrayList<>();
        double radius = plugin.configManager().apiaryRadius();
        for (HiveData hive : plugin.hives().all()) {
            if (!hive.owner().equals(player.getUniqueId())) {
                continue;
            }
            if (distanceSquared(center, hive.pos()) <= radius * radius) {
                hives.add(hive);
            }
        }
        return hives;
    }

    private int countFences(World world, HivePos center, int radius) {
        int count = 0;
        for (int x = -radius; x <= radius; x++) {
            for (int z = -radius; z <= radius; z++) {
                int blockX = center.x() + x;
                int blockZ = center.z() + z;
                if (!SchedulerAdapter.ownedChunkNow(world, blockX >> 4, blockZ >> 4)) {
                    continue;
                }
                for (int y = -3; y <= 3; y++) {
                    Block block = world.getBlockAt(blockX, center.y() + y, blockZ);
                    if (Tag.FENCES.isTagged(block.getType())) {
                        count++;
                    }
                }
            }
        }
        return count;
    }
}
