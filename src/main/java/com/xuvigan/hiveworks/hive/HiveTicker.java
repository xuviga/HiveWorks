package com.xuvigan.hiveworks.hive;

import com.xuvigan.hiveworks.HiveWorksPlugin;
import com.xuvigan.hiveworks.production.FlowerScanner;
import com.xuvigan.hiveworks.production.HoneyService;
import com.xuvigan.hiveworks.util.SchedulerAdapter;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.scheduler.BukkitTask;

/**
 * Batched production tick. Never loads chunks.
 */
public final class HiveTicker {

    private final HiveWorksPlugin plugin;
    private final HoneyService honeyService;
    private final SchedulerAdapter scheduler;
    private BukkitTask task;
    private long lastSave = System.currentTimeMillis();

    public HiveTicker(HiveWorksPlugin plugin) {
        this.plugin = plugin;
        this.honeyService = new HoneyService(plugin.configManager());
        this.scheduler = new SchedulerAdapter(plugin);
    }

    public void start() {
        long period = plugin.configManager().tickIntervalSeconds() * 20L;
        this.task = scheduler.runTimer(this::tick, 100L, period);
    }

    public void stop() {
        if (task != null) {
            task.cancel();
            task = null;
        }
    }

    private void tick() {
        long now = System.currentTimeMillis();
        long capMillis = plugin.configManager().offlineCapHours() * 3_600_000L;

        for (HiveData hive : plugin.hives().all()) {
            World world = Bukkit.getWorld(hive.pos().world());
            if (world == null) {
                continue;
            }
            if (!world.isChunkLoaded(hive.pos().x() >> 4, hive.pos().z() >> 4)) {
                continue;
            }
            Block block = world.getBlockAt(hive.pos().x(), hive.pos().y(), hive.pos().z());
            if (!HiveManager.isHiveBlock(block.getType())) {
                continue;
            }

            long elapsed = Math.min(capMillis, Math.max(0L, now - hive.lastTick()));
            if (elapsed <= 0L) {
                hive.setLastTick(now);
                continue;
            }

            FlowerScanner.Result flowers;
            if (now - hive.flowerScannedAt() < 300_000L) {
                flowers = new FlowerScanner.Result(hive.cachedFlowerSpecies(), hive.cachedFlowerRare(), java.util.Collections.emptySet());
            } else {
                flowers = FlowerScanner.scan(block, 8);
                hive.setFlowerCache(flowers.species(), flowers.rare(), now);
            }
            boolean nightOrRain = world.hasStorm() || !isDay(world);
            boolean snowy = isSnowy(world.getBiome(hive.pos().x(), hive.pos().y(), hive.pos().z()).name());

            double speed = honeyService.speed(hive, flowers, nightOrRain, snowy);
            double minutes = elapsed / 60_000.0;
            double gain = minutes / plugin.configManager().baseMinutesPerLevel() * speed;

            hive.setHoney(Math.min(hive.honeyMax(), hive.honey() + gain));
            hive.setLastTick(now);
            syncVanillaLevel(block, hive);
            plugin.displays().ensure(hive);
        }

        long saveInterval = plugin.configManager().saveIntervalSeconds() * 1000L;
        if (now - lastSave >= saveInterval) {
            plugin.hives().saveAll();
            lastSave = now;
        }
    }

    private boolean isDay(World world) {
        long time = world.getTime() % 24_000L;
        return time < 12_300L || time > 23_850L;
    }

    private boolean isSnowy(String biomeName) {
        return biomeName.contains("SNOW") || biomeName.contains("FROZEN") || biomeName.contains("ICE");
    }

    private void syncVanillaLevel(Block block, HiveData hive) {
        if (!(block.getBlockData() instanceof org.bukkit.block.data.type.Beehive beehive)) {
            return;
        }
        int visual = Math.max(0, Math.min(5, (int) Math.floor(hive.honey())));
        if (beehive.getHoneyLevel() != visual) {
            beehive.setHoneyLevel(visual);
            block.setBlockData(beehive, false);
        }
    }
}
