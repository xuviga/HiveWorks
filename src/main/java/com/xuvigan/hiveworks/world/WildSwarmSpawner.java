package com.xuvigan.hiveworks.world;

import com.xuvigan.hiveworks.HiveWorksPlugin;
import com.xuvigan.hiveworks.item.PdcKeys;
import com.xuvigan.hiveworks.production.FlowerScanner;
import com.xuvigan.hiveworks.util.SchedulerAdapter;
import io.papermc.paper.threadedregions.scheduler.ScheduledTask;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Bee;
import org.bukkit.entity.Player;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import java.util.Locale;
import java.util.Map;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Spawns catchable wild swarms near players during the day (and in the Nether).
 *
 * <p>The global timer only iterates online players; every world read, spawn and
 * despawn happens on the region that owns the player or the bee. Candidate spots
 * in chunks owned by another region are skipped instead of forcing a chunk load.</p>
 */
public final class WildSwarmSpawner {

    private final HiveWorksPlugin plugin;
    private final SchedulerAdapter scheduler;
    private final Random random = new Random();
    private final Map<UUID, Long> cooldowns = new ConcurrentHashMap<>();
    private ScheduledTask task;

    public WildSwarmSpawner(HiveWorksPlugin plugin) {
        this.plugin = plugin;
        this.scheduler = plugin.schedulers();
    }

    public void start() {
        if (!plugin.configManager().wildSwarm()) {
            plugin.getLogger().info("Wild swarm spawner disabled by config.");
            return;
        }
        long period = plugin.configManager().wildSwarmCheckSeconds() * 20L;
        this.task = scheduler.runGlobalTimer(this::tick, 200L, period);
    }

    public void stop() {
        if (task != null) {
            task.cancel();
            task = null;
        }
    }

    private void tick() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            scheduler.executeFor(player, () -> trySpawn(player));
        }
    }

    /** Runs on the player's region. */
    private void trySpawn(Player player) {
        long now = System.currentTimeMillis();
        World world = player.getWorld();
        World.Environment environment = world.getEnvironment();
        if (environment != World.Environment.NORMAL && environment != World.Environment.NETHER) {
            return;
        }
        if (plugin.configManager().isWorldBlacklisted(world.getName())) {
            return;
        }
        if (environment == World.Environment.NORMAL && !isDay(world)) {
            return;
        }
        long cooldown = plugin.configManager().wildSwarmCooldownMinutes() * 60_000L;
        long last = cooldowns.getOrDefault(player.getUniqueId(), 0L);
        if (now - last < cooldown) {
            return;
        }
        if (random.nextDouble() > 0.35) {
            return;
        }
        Location location = environment == World.Environment.NETHER
                ? findNetherLocation(player)
                : findOverworldLocation(player);
        if (location == null) {
            return;
        }
        spawnSwarm(location, world, now);
        cooldowns.put(player.getUniqueId(), now);
    }

    private Location findOverworldLocation(Player player) {
        World world = player.getWorld();
        for (int attempt = 0; attempt < 24; attempt++) {
            int dx = random.nextInt(49) - 24;
            int dz = random.nextInt(49) - 24;
            if (Math.abs(dx) < 8 && Math.abs(dz) < 8) {
                continue;
            }
            int x = player.getLocation().getBlockX() + dx;
            int z = player.getLocation().getBlockZ() + dz;
            if (!SchedulerAdapter.ownedNow(world, x, z) || !world.isChunkLoaded(x >> 4, z >> 4)) {
                continue;
            }
            int y = world.getHighestBlockYAt(x, z);
            Block ground = world.getBlockAt(x, y, z);
            Block above = world.getBlockAt(x, y + 1, z);
            if (!ground.getType().isSolid() || !above.getType().isAir()
                    || !world.getBlockAt(x, y + 2, z).getType().isAir()) {
                continue;
            }
            String biome = world.getBiome(x, y, z).name().toUpperCase(Locale.ROOT);
            if (plugin.speciesRegistry().byBiome(biome).isEmpty()) {
                Location cave = findCaveLocation(world, x, z);
                if (cave != null) {
                    return cave;
                }
                continue;
            }
            // Prefer flower patches, but after a dozen attempts allow flowerless
            // biomes (desert, snowy, mushroom, taiga, swamp, ...).
            boolean flowers = FlowerScanner.scan(above, 3).species() > 0;
            if (flowers || attempt >= 12) {
                return above.getLocation().add(0.5, 0.0, 0.5);
            }
        }
        return null;
    }

    private Location findCaveLocation(World world, int x, int z) {
        for (int attempt = 0; attempt < 8; attempt++) {
            int y = random.nextInt(90) - 40;
            Block ground = world.getBlockAt(x, y, z);
            Block above = world.getBlockAt(x, y + 1, z);
            Block above2 = world.getBlockAt(x, y + 2, z);
            if (!ground.getType().isSolid() || !above.getType().isAir() || !above2.getType().isAir()) {
                continue;
            }
            String biome = world.getBiome(x, y, z).name().toUpperCase(Locale.ROOT);
            if (plugin.speciesRegistry().byBiome(biome).isPresent()) {
                return above.getLocation().add(0.5, 0.0, 0.5);
            }
        }
        return null;
    }

    private Location findNetherLocation(Player player) {
        World world = player.getWorld();
        for (int attempt = 0; attempt < 24; attempt++) {
            int dx = random.nextInt(49) - 24;
            int dz = random.nextInt(49) - 24;
            int x = player.getLocation().getBlockX() + dx;
            int z = player.getLocation().getBlockZ() + dz;
            if (!SchedulerAdapter.ownedNow(world, x, z) || !world.isChunkLoaded(x >> 4, z >> 4)) {
                continue;
            }
            int y = 32 + random.nextInt(90);
            Block ground = world.getBlockAt(x, y, z);
            Block above = world.getBlockAt(x, y + 1, z);
            Block above2 = world.getBlockAt(x, y + 2, z);
            if (!ground.getType().isSolid() || !above.getType().isAir() || !above2.getType().isAir()) {
                continue;
            }
            String biome = world.getBiome(x, y, z).name().toUpperCase(Locale.ROOT);
            if (plugin.speciesRegistry().byBiome(biome).isPresent()) {
                return above.getLocation().add(0.5, 0.0, 0.5);
            }
        }
        return null;
    }

    private void spawnSwarm(Location center, World world, long now) {
        int max = plugin.configManager().wildSwarmMaxBees();
        int count = Math.min(max, 3 + random.nextInt(Math.max(1, max - 2)));
        long lifetimeTicks = despawnTicks(world);
        for (int index = 0; index < count; index++) {
            Location spot = center.clone().add(
                    random.nextDouble() * 4 - 2,
                    random.nextDouble() * 2,
                    random.nextDouble() * 4 - 2
            );
            if (!SchedulerAdapter.ownedNow(spot)) {
                continue;
            }
            Bee bee = world.spawn(spot, Bee.class, entity -> {
                entity.setPersistent(true);
                entity.setRemoveWhenFarAway(true);
                PersistentDataContainer pdc = entity.getPersistentDataContainer();
                pdc.set(PdcKeys.WILD, PersistentDataType.BYTE, (byte) 1);
                pdc.set(PdcKeys.SPAWNED_AT, PersistentDataType.LONG, now);
            });
            // EntityScheduler stays with the bee's region and is dropped when the
            // bee dies or unloads, so no global entity sweep is required.
            scheduler.runForLater(bee, () -> despawnIfWild(bee), lifetimeTicks);
        }
    }

    private void despawnIfWild(Bee bee) {
        if (bee.isValid() && isWild(bee)) {
            bee.remove();
        }
    }

    /** Ticks until the game turns to night, capped by the configured lifetime. */
    private long despawnTicks(World world) {
        long lifetimeTicks = plugin.configManager().wildSwarmLifetimeMinutes() * 60L * 20L;
        if (world.getEnvironment() == World.Environment.NETHER) {
            return Math.max(20L, lifetimeTicks);
        }
        long time = world.getTime() % 24_000L;
        long untilNight = time < 12_300L ? 12_300L - time : 24_000L - time + 12_300L;
        return Math.max(20L, Math.min(lifetimeTicks, untilNight));
    }

    public static boolean isWild(Bee bee) {
        Byte value = bee.getPersistentDataContainer().get(PdcKeys.WILD, PersistentDataType.BYTE);
        return value != null && value == 1;
    }

    private boolean isDay(World world) {
        if (world.getEnvironment() == World.Environment.NETHER) {
            return true;
        }
        long time = world.getTime() % 24_000L;
        return time < 12_300L || time > 23_850L;
    }
}
