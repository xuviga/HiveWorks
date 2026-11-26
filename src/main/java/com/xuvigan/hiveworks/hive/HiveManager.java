package com.xuvigan.hiveworks.hive;

import com.xuvigan.hiveworks.HiveWorksPlugin;
import com.xuvigan.hiveworks.api.HiveHarvestEvent;
import com.xuvigan.hiveworks.bee.BeeItemData;
import com.xuvigan.hiveworks.production.FlowerScanner;
import com.xuvigan.hiveworks.production.Quality;
import com.xuvigan.hiveworks.production.QualityCalculator;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Hive registry, limits, bee placement and harvesting.
 */
public final class HiveManager {

    private final HiveWorksPlugin plugin;
    private final Map<String, HiveData> hives = new ConcurrentHashMap<>();
    private final Map<UUID, Integer> ownerCounts = new ConcurrentHashMap<>();
    private final Map<String, Integer> chunkCounts = new ConcurrentHashMap<>();

    public HiveManager(HiveWorksPlugin plugin) {
        this.plugin = plugin;
    }

    public void load() {
        for (HiveData hive : plugin.storage().loadAll()) {
            hives.put(hive.pos().key(), hive);
            index(hive);
        }
        plugin.getLogger().info("Loaded " + hives.size() + " hives.");
    }

    public void saveAll() {
        for (HiveData hive : hives.values()) {
            plugin.storage().save(hive);
        }
    }

    public void save(HiveData hive) {
        plugin.storage().save(hive);
    }

    public Collection<HiveData> all() {
        return Collections.unmodifiableCollection(hives.values());
    }

    public int count() {
        return hives.size();
    }

    public HiveData get(Block block) {
        return hives.get(HivePos.from(block).key());
    }

    public Optional<HiveData> findNearest(Player player, double radius) {
        HiveData best = null;
        double bestDistance = radius * radius;
        for (HiveData hive : hives.values()) {
            if (!hive.pos().world().equals(player.getWorld().getName())) {
                continue;
            }
            double dx = (hive.pos().x() + 0.5) - player.getLocation().getX();
            double dy = (hive.pos().y() + 0.5) - player.getLocation().getY();
            double dz = (hive.pos().z() + 0.5) - player.getLocation().getZ();
            double distance = dx * dx + dy * dy + dz * dz;
            if (distance <= bestDistance) {
                best = hive;
                bestDistance = distance;
            }
        }
        return Optional.ofNullable(best);
    }

    public HiveData create(Block block, Player player) {
        HivePos pos = HivePos.from(block);
        HiveData existing = hives.get(pos.key());
        if (existing != null) {
            return existing;
        }
        if (plugin.configManager().isWorldBlacklisted(pos.world())
                && !player.hasPermission("hiveworks.bypass.world")) {
            plugin.messages().send(player, "hive-world-blocked");
            return null;
        }

        int limit = limitFor(player);
        if (ownerCount(player.getUniqueId()) >= limit) {
            plugin.messages().send(player, "hive-limit", "limit", limit);
            return null;
        }
        if (chunkCount(pos.chunkKey()) >= plugin.configManager().hivesPerChunk()) {
            plugin.messages().send(player, "hive-limit", "limit", plugin.configManager().hivesPerChunk());
            return null;
        }

        HiveData hive = new HiveData(pos, player.getUniqueId());
        hives.put(pos.key(), hive);
        index(hive);
        plugin.storage().save(hive);
        plugin.messages().send(player, "hive-created",
                "bees", hive.occupiedBees(),
                "slots", hive.tier().beeSlots());
        return hive;
    }

    public void remove(HiveData hive) {
        hives.remove(hive.pos().key());
        unindex(hive);
        plugin.storage().delete(hive.pos());
    }

    public boolean addBee(HiveData hive, int slot, BeeItemData data) {
        if (slot < 0 || slot >= hive.tier().beeSlots() || slot >= HiveData.MAX_BEES) {
            return false;
        }
        if (hive.bee(slot) != null) {
            return false;
        }
        hive.setBee(slot, new BeeSlot(data.id(), data.speciesId(), data.genes(), data.purity()));
        plugin.storage().save(hive);
        return true;
    }

    public BeeSlot removeBee(HiveData hive, int slot) {
        BeeSlot bee = hive.bee(slot);
        if (bee == null) {
            return null;
        }
        hive.setBee(slot, null);
        plugin.storage().save(hive);
        return bee;
    }

    public Quality harvest(Player player, HiveData hive) {
        int levels = (int) Math.floor(hive.honey());
        if (levels <= 0) {
            plugin.messages().send(player, "hive-empty");
            return null;
        }
        World world = Bukkit.getWorld(hive.pos().world());
        if (world == null) {
            return null;
        }
        Block block = world.getBlockAt(hive.pos().x(), hive.pos().y(), hive.pos().z());
        FlowerScanner.Result flowers = FlowerScanner.scan(block, 8);

        QualityCalculator calculator = new QualityCalculator(
                plugin.configManager().weightFlowers(),
                plugin.configManager().weightFrames(),
                plugin.configManager().weightPurity(),
                plugin.configManager().goodThreshold(),
                plugin.configManager().eliteThreshold(),
                plugin.configManager().legendaryThreshold()
        );
        double score = calculator.score(
                flowers.species(),
                flowers.rare(),
                hive.frameQualityWeight(),
                hive.averagePurity(),
                hive.frameQualityBonus() + hive.tier().qualityBonus()
        );
        Quality quality = calculator.qualityOf(score);

        HiveHarvestEvent event = new HiveHarvestEvent(hive, player, levels, quality);
        Bukkit.getPluginManager().callEvent(event);
        if (event.isCancelled()) {
            return null;
        }

        hive.setHoney(Math.max(0.0, hive.honey() - levels));
        hive.setLastTick(System.currentTimeMillis());
        wearFrames(hive);

        int remaining = levels;
        while (remaining > 0) {
            int amount = Math.min(64, remaining);
            ItemStack honey = plugin.itemFactory().honey(quality, amount);
            player.getInventory().addItem(honey).values()
                    .forEach(drop -> player.getWorld().dropItemNaturally(player.getLocation(), drop));
            remaining -= amount;
        }

        plugin.messages().send(player, "hive-harvested",
                "amount", levels,
                "quality", quality.displayName());
        plugin.storage().save(hive);
        return quality;
    }

    private void wearFrames(HiveData hive) {
        for (int slot = 0; slot < HiveData.MAX_FRAMES; slot++) {
            FrameSlot frame = hive.frame(slot);
            if (frame != null) {
                hive.setFrame(slot, frame.wear());
            }
        }
    }

    private int limitFor(Player player) {
        int limit = plugin.configManager().hivesPerPlayer();
        for (int value = limit + 1; value <= 512; value++) {
            if (player.hasPermission("hiveworks.limit." + value)) {
                limit = value;
            }
        }
        return limit;
    }

    private int ownerCount(UUID owner) {
        return ownerCounts.getOrDefault(owner, 0);
    }

    private int chunkCount(String chunkKey) {
        return chunkCounts.getOrDefault(chunkKey, 0);
    }

    private void index(HiveData hive) {
        ownerCounts.merge(hive.owner(), 1, Integer::sum);
        chunkCounts.merge(hive.pos().chunkKey(), 1, Integer::sum);
    }

    private void unindex(HiveData hive) {
        ownerCounts.computeIfPresent(hive.owner(), (key, value) -> value <= 1 ? null : value - 1);
        chunkCounts.computeIfPresent(hive.pos().chunkKey(), (key, value) -> value <= 1 ? null : value - 1);
    }

    public static boolean isHiveBlock(Material material) {
        return material == Material.BEEHIVE || material == Material.BEE_NEST;
    }
}
