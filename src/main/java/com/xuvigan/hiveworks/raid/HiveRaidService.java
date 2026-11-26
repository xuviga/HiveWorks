package com.xuvigan.hiveworks.raid;

import com.xuvigan.hiveworks.HiveWorksPlugin;
import com.xuvigan.hiveworks.hive.FrameSlot;
import com.xuvigan.hiveworks.hive.HiveData;
import com.xuvigan.hiveworks.item.PdcKeys;
import com.xuvigan.hiveworks.util.SchedulerAdapter;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Fox;
import org.bukkit.entity.Mob;
import org.bukkit.entity.PolarBear;
import org.bukkit.entity.Player;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Night raids: a fox steals a little honey, a bear steals more and may break a frame.
 */
public final class HiveRaidService {

    private enum RaidType {
        FOX,
        BEAR
    }

    private static final class Raid {
        private final UUID entityId;
        private final String hiveKey;
        private final RaidType type;
        private long expiresAt;
        private boolean stolen;

        private Raid(UUID entityId, String hiveKey, RaidType type, long expiresAt) {
            this.entityId = entityId;
            this.hiveKey = hiveKey;
            this.type = type;
            this.expiresAt = expiresAt;
        }
    }

    private final HiveWorksPlugin plugin;
    private final SchedulerAdapter scheduler;
    private final Map<UUID, Raid> raids = new ConcurrentHashMap<>();
    private final Map<String, Long> cooldowns = new ConcurrentHashMap<>();
    private final Random random = new Random();
    private long nextAttemptAt;
    private BukkitTask task;

    public HiveRaidService(HiveWorksPlugin plugin) {
        this.plugin = plugin;
        this.scheduler = new SchedulerAdapter(plugin);
    }

    public void start() {
        if (!plugin.configManager().foxRaider()) {
            plugin.getLogger().info("Night raids disabled by config.");
            return;
        }
        this.task = scheduler.runTimer(this::tick, 40L, 20L);
    }

    public void stop() {
        if (task != null) {
            task.cancel();
            task = null;
        }
        removeAll();
    }

    public static boolean isRaider(Entity entity) {
        Byte value = entity.getPersistentDataContainer().get(PdcKeys.RAIDER, PersistentDataType.BYTE);
        return value != null && value == 1;
    }

    public static int stealAmount(double honey, int configured) {
        int levels = (int) Math.floor(honey);
        return Math.max(0, Math.min(configured, levels));
    }

    public void clearHive(String hiveKey) {
        cooldowns.remove(hiveKey);
        Iterator<Map.Entry<UUID, Raid>> iterator = raids.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<UUID, Raid> entry = iterator.next();
            if (!entry.getValue().hiveKey.equals(hiveKey)) {
                continue;
            }
            Entity entity = Bukkit.getEntity(entry.getKey());
            if (entity != null) {
                entity.remove();
            }
            iterator.remove();
        }
    }

    public void removeAll() {
        for (UUID id : raids.keySet()) {
            Entity entity = Bukkit.getEntity(id);
            if (entity != null) {
                entity.remove();
            }
        }
        raids.clear();
        cooldowns.clear();
    }

    private void tick() {
        long now = System.currentTimeMillis();
        processRaids(now);
        if (now < nextAttemptAt) {
            return;
        }
        nextAttemptAt = now + plugin.configManager().raidIntervalSeconds() * 1000L;
        if (raids.size() >= plugin.configManager().raidMaxActive()) {
            return;
        }
        if (random.nextDouble() > plugin.configManager().raidChance()) {
            return;
        }
        trySpawnRaid(now);
    }

    private void processRaids(long now) {
        Iterator<Map.Entry<UUID, Raid>> iterator = raids.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<UUID, Raid> entry = iterator.next();
            Raid raid = entry.getValue();
            Entity entity = Bukkit.getEntity(entry.getKey());
            HiveData hive = plugin.hives().byKey(raid.hiveKey);
            if (entity == null || !entity.isValid() || entity.isDead() || hive == null || now > raid.expiresAt) {
                if (entity != null) {
                    entity.remove();
                }
                iterator.remove();
                continue;
            }
            if (!(entity instanceof Mob mob)) {
                entity.remove();
                iterator.remove();
                continue;
            }

            World world = entity.getWorld();
            Location center = new Location(world,
                    hive.pos().x() + 0.5,
                    hive.pos().y() + 1.0,
                    hive.pos().z() + 0.5);

            if (!raid.stolen && entity.getLocation().distanceSquared(center) <= 4.0) {
                steal(hive, raid.type, world, center);
                raid.stolen = true;
                raid.expiresAt = Math.min(raid.expiresAt, now + 10_000L);
            }

            if (raid.stolen) {
                Vector away = entity.getLocation().toVector().subtract(center.toVector());
                if (away.lengthSquared() < 0.01) {
                    away = new Vector(1, 0, 0);
                }
                Location flee = entity.getLocation().clone().add(away.normalize().multiply(18.0));
                mob.getPathfinder().moveTo(flee, 1.4);
                if (now > raid.expiresAt) {
                    entity.remove();
                    iterator.remove();
                }
            } else {
                mob.getPathfinder().moveTo(center, 1.2);
            }
        }
    }

    private void trySpawnRaid(long now) {
        List<HiveData> candidates = new ArrayList<>();
        for (HiveData hive : plugin.hives().all()) {
            if (hive.honey() < 1.0) {
                continue;
            }
            if (plugin.configManager().isWorldBlacklisted(hive.pos().world())) {
                continue;
            }
            World world = Bukkit.getWorld(hive.pos().world());
            if (world == null || !world.isChunkLoaded(hive.pos().x() >> 4, hive.pos().z() >> 4)) {
                continue;
            }
            if (!isNight(world)) {
                return;
            }
            if (now < cooldowns.getOrDefault(hive.pos().key(), 0L)) {
                continue;
            }
            if (hasActiveRaid(hive.pos().key())) {
                continue;
            }
            candidates.add(hive);
        }
        if (candidates.isEmpty()) {
            return;
        }

        HiveData hive = candidates.get(random.nextInt(candidates.size()));
        World world = Bukkit.getWorld(hive.pos().world());
        if (world == null) {
            return;
        }
        Location center = new Location(world,
                hive.pos().x() + 0.5,
                hive.pos().y() + 0.5,
                hive.pos().z() + 0.5);
        Location spawn = findSpawn(center);
        if (spawn == null) {
            return;
        }

        RaidType type = random.nextDouble() < plugin.configManager().raidBearChance()
                ? RaidType.BEAR
                : RaidType.FOX;
        long expiresAt = now + plugin.configManager().raidLifetimeSeconds() * 1000L;
        UUID id = type == RaidType.BEAR
                ? spawnBear(world, spawn, hive)
                : spawnFox(world, spawn, hive);
        if (id == null) {
            return;
        }
        raids.put(id, new Raid(id, hive.pos().key(), type, expiresAt));
        cooldowns.put(hive.pos().key(), now + plugin.configManager().raidCooldownMinutes() * 60_000L);
    }

    private UUID spawnFox(World world, Location location, HiveData hive) {
        Fox fox = world.spawn(location, Fox.class, entity -> {
            entity.setPersistent(false);
            entity.setRemoveWhenFarAway(true);
            entity.setSleeping(false);
            entity.setSitting(false);
            entity.customName(Component.text("Лиса-сладкоежка", NamedTextColor.GOLD));
            entity.setCustomNameVisible(false);
            tag(entity, hive, "FOX");
        });
        return fox.getUniqueId();
    }

    private UUID spawnBear(World world, Location location, HiveData hive) {
        PolarBear bear = world.spawn(location, PolarBear.class, entity -> {
            entity.setPersistent(false);
            entity.setRemoveWhenFarAway(true);
            entity.setAdult();
            entity.customName(Component.text("Медведь-разоритель", NamedTextColor.DARK_RED));
            entity.setCustomNameVisible(false);
            tag(entity, hive, "BEAR");
        });
        return bear.getUniqueId();
    }

    private void tag(Entity entity, HiveData hive, String type) {
        entity.getPersistentDataContainer().set(PdcKeys.RAIDER, PersistentDataType.BYTE, (byte) 1);
        entity.getPersistentDataContainer().set(PdcKeys.RAIDER_HIVE, PersistentDataType.STRING, hive.pos().key());
        entity.getPersistentDataContainer().set(PdcKeys.RAIDER_TYPE, PersistentDataType.STRING, type);
    }

    private void steal(HiveData hive, RaidType type, World world, Location center) {
        int configured = type == RaidType.BEAR
                ? plugin.configManager().raidBearSteal()
                : plugin.configManager().raidFoxSteal();
        int amount = stealAmount(hive.honey(), configured);
        if (amount <= 0) {
            return;
        }
        hive.setHoney(Math.max(0.0, hive.honey() - amount));

        if (type == RaidType.BEAR) {
            world.playSound(center, Sound.ENTITY_POLAR_BEAR_WARNING, 1.0f, 0.9f);
            breakFrame(hive);
            notifyOwner(hive, "raid-bear-stole", "amount", amount);
        } else {
            world.playSound(center, Sound.ENTITY_FOX_EAT, 1.0f, 1.0f);
            notifyOwner(hive, "raid-fox-stole", "amount", amount);
        }
        plugin.storage().save(hive);
    }

    private void breakFrame(HiveData hive) {
        if (hive.framesUsed() == 0 || random.nextDouble() > 0.35) {
            return;
        }
        List<Integer> occupied = new ArrayList<>();
        for (int slot = 0; slot < HiveData.MAX_FRAMES; slot++) {
            if (hive.frame(slot) != null) {
                occupied.add(slot);
            }
        }
        if (occupied.isEmpty()) {
            return;
        }
        int slot = occupied.get(random.nextInt(occupied.size()));
        FrameSlot frame = hive.frame(slot);
        hive.setFrame(slot, null);
        notifyOwner(hive, "raid-bear-broke-frame", "frame", frame.type().displayName());
    }

    private void notifyOwner(HiveData hive, String key, Object... placeholders) {
        Player owner = Bukkit.getPlayer(hive.owner());
        if (owner != null && owner.isOnline()) {
            plugin.messages().send(owner, key, placeholders);
        }
    }

    private boolean hasActiveRaid(String hiveKey) {
        for (Raid raid : raids.values()) {
            if (raid.hiveKey.equals(hiveKey)) {
                return true;
            }
        }
        return false;
    }

    private Location findSpawn(Location center) {
        World world = center.getWorld();
        if (world == null) {
            return null;
        }
        double angle = random.nextDouble() * Math.PI * 2.0;
        double distance = plugin.configManager().raidDistance() + random.nextDouble() * 4.0 - 2.0;
        int x = (int) Math.round(center.getX() + Math.cos(angle) * distance);
        int z = (int) Math.round(center.getZ() + Math.sin(angle) * distance);
        if (!world.isChunkLoaded(x >> 4, z >> 4)) {
            return null;
        }
        int y = world.getHighestBlockYAt(x, z);
        if (!world.getBlockAt(x, y, z).getType().isSolid()) {
            return null;
        }
        if (!world.getBlockAt(x, y + 1, z).getType().isAir()) {
            return null;
        }
        return new Location(world, x + 0.5, y + 1.0, z + 0.5);
    }

    private boolean isNight(World world) {
        long time = world.getTime() % 24_000L;
        return time >= 13_000L && time <= 23_000L;
    }
}
