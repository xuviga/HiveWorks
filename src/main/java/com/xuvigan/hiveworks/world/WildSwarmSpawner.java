package com.xuvigan.hiveworks.world;

import com.xuvigan.hiveworks.HiveWorksPlugin;
import com.xuvigan.hiveworks.item.PdcKeys;
import com.xuvigan.hiveworks.production.FlowerScanner;
import com.xuvigan.hiveworks.util.SchedulerAdapter;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Bee;
import org.bukkit.entity.Player;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.scheduler.BukkitTask;

import java.util.Locale;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Spawns catchable wild swarms near players at dawn.
 */
public final class WildSwarmSpawner {

    private static final Set<String> BIOMES = Set.of(
            "FOREST", "FLOWER_FOREST", "BIRCH_FOREST", "OLD_GROWTH_BIRCH_FOREST",
            "MEADOW", "PLAINS", "SUNFLOWER_PLAINS", "CHERRY_GROVE",
            "JUNGLE", "SPARSE_JUNGLE", "BAMBOO_JUNGLE"
    );

    private final HiveWorksPlugin plugin;
    private final Random random = new Random();
    private final Map<UUID, Long> cooldowns = new ConcurrentHashMap<>();
    private BukkitTask task;

    public WildSwarmSpawner(HiveWorksPlugin plugin) {
        this.plugin = plugin;
    }

    public void start() {
        if (!plugin.configManager().wildSwarm()) {
            plugin.getLogger().info("Wild swarm spawner disabled by config.");
            return;
        }
        long period = plugin.configManager().wildSwarmCheckSeconds() * 20L;
        this.task = new SchedulerAdapter(plugin).runTimer(this::tick, 200L, period);
    }

    public void stop() {
        if (task != null) {
            task.cancel();
            task = null;
        }
    }

    private void tick() {
        long now = System.currentTimeMillis();
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (player.getWorld().getEnvironment() != World.Environment.NORMAL) {
                continue;
            }
            if (plugin.configManager().isWorldBlacklisted(player.getWorld().getName())) {
                continue;
            }
            if (!isDay(player.getWorld())) {
                continue;
            }
            long cooldown = plugin.configManager().wildSwarmCooldownMinutes() * 60_000L;
            long last = cooldowns.getOrDefault(player.getUniqueId(), 0L);
            if (now - last < cooldown) {
                continue;
            }
            if (random.nextDouble() > 0.35) {
                continue;
            }
            Location location = findLocation(player);
            if (location == null) {
                continue;
            }
            spawnSwarm(location);
            cooldowns.put(player.getUniqueId(), now);
        }
        cleanup(now);
    }

    private Location findLocation(Player player) {
        World world = player.getWorld();
        for (int attempt = 0; attempt < 12; attempt++) {
            int dx = random.nextInt(49) - 24;
            int dz = random.nextInt(49) - 24;
            if (Math.abs(dx) < 8 && Math.abs(dz) < 8) {
                continue;
            }
            int x = player.getLocation().getBlockX() + dx;
            int z = player.getLocation().getBlockZ() + dz;
            if (!world.isChunkLoaded(x >> 4, z >> 4)) {
                continue;
            }
            int y = world.getHighestBlockYAt(x, z);
            Block ground = world.getBlockAt(x, y, z);
            Block above = world.getBlockAt(x, y + 1, z);
            if (!ground.getType().isSolid() || !above.getType().isAir()) {
                continue;
            }
            String biome = world.getBiome(x, y, z).name().toUpperCase(Locale.ROOT);
            if (!BIOMES.contains(biome)) {
                continue;
            }
            if (FlowerScanner.scan(above, 3).species() == 0) {
                continue;
            }
            return above.getLocation().add(0.5, 0.0, 0.5);
        }
        return null;
    }

    private void spawnSwarm(Location center) {
        World world = center.getWorld();
        if (world == null) {
            return;
        }
        int max = plugin.configManager().wildSwarmMaxBees();
        int count = 3 + random.nextInt(Math.max(1, max - 2));
        for (int index = 0; index < count; index++) {
            Location spot = center.clone().add(
                    random.nextDouble() * 4 - 2,
                    random.nextDouble() * 2,
                    random.nextDouble() * 4 - 2
            );
            world.spawn(spot, Bee.class, bee -> {
                bee.setPersistent(true);
                bee.setRemoveWhenFarAway(true);
                PersistentDataContainer pdc = bee.getPersistentDataContainer();
                pdc.set(PdcKeys.WILD, PersistentDataType.BYTE, (byte) 1);
                pdc.set(PdcKeys.SPAWNED_AT, PersistentDataType.LONG, System.currentTimeMillis());
            });
        }
    }

    private void cleanup(long now) {
        long lifetime = plugin.configManager().wildSwarmLifetimeMinutes() * 60_000L;
        for (World world : Bukkit.getWorlds()) {
            boolean day = isDay(world);
            for (Bee bee : world.getEntitiesByClass(Bee.class)) {
                if (!isWild(bee)) {
                    continue;
                }
                Long spawnedAt = bee.getPersistentDataContainer().get(PdcKeys.SPAWNED_AT, PersistentDataType.LONG);
                if (!day || spawnedAt == null || now - spawnedAt > lifetime) {
                    bee.remove();
                }
            }
        }
    }

    public static boolean isWild(Bee bee) {
        Byte value = bee.getPersistentDataContainer().get(PdcKeys.WILD, PersistentDataType.BYTE);
        return value != null && value == 1;
    }

    private boolean isDay(World world) {
        long time = world.getTime() % 24_000L;
        return time < 12_300L || time > 23_850L;
    }
}
