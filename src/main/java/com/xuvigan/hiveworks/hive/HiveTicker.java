package com.xuvigan.hiveworks.hive;

import com.xuvigan.hiveworks.HiveWorksPlugin;
import com.xuvigan.hiveworks.production.FlowerScanner;
import com.xuvigan.hiveworks.production.HoneyService;
import com.xuvigan.hiveworks.production.PollinationService;
import com.xuvigan.hiveworks.util.SchedulerAdapter;
import io.papermc.paper.threadedregions.scheduler.ScheduledTask;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.block.Block;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Batched production tick.
 *
 * <p>On Folia the ticker itself only runs bookkeeping on the global region and
 * dispatches one task per hive to the region that owns the hive block. That is
 * the only place where the vanilla bee level, flowers, crops and the ItemDisplay
 * overlay are touched, so region ownership is always guaranteed.</p>
 */
public final class HiveTicker {

    private final HiveWorksPlugin plugin;
    private final HoneyService honeyService;
    private final PollinationService pollinationService;
    private final SchedulerAdapter scheduler;
    private final Random random = new Random();
    private ScheduledTask task;
    private long tickCounter;
    private long nextSaveAt = System.currentTimeMillis();
    private List<HiveData> saveCycle = List.of();
    private int saveCursor;

    public HiveTicker(HiveWorksPlugin plugin) {
        this.plugin = plugin;
        this.honeyService = new HoneyService(plugin);
        this.pollinationService = new PollinationService(plugin);
        this.scheduler = plugin.schedulers();
    }

    public void start() {
        if (task != null) {
            return;
        }
        long period = plugin.configManager().tickIntervalSeconds() * 20L;
        this.task = scheduler.runGlobalTimer(this::tick, 100L, period);
    }

    public void stop() {
        if (task != null) {
            task.cancel();
            task = null;
        }
    }

    private void tick() {
        long now = System.currentTimeMillis();
        long cacheMillis = plugin.configManager().flowerCacheSeconds() * 1000L;
        int scans = plugin.configManager().flowerScansPerTick();
        AtomicInteger scanBudget = new AtomicInteger(scans);
        long cycle = tickCounter;

        for (HiveData hive : plugin.hives().all()) {
            scheduler.executeAtHive(hive.pos(), () -> tickHive(hive, now, cacheMillis, scanBudget, cycle));
        }

        plugin.hives().tickBreeding(now);
        saveIncrementally(now);
        tickCounter++;
    }

    /** Runs on the region that owns the hive block. */
    private void tickHive(HiveData hive, long now, long cacheMillis, AtomicInteger scanBudget, long cycle) {
        World world = Bukkit.getWorld(hive.pos().world());
        if (world == null) {
            return;
        }
        if (!world.isChunkLoaded(hive.pos().x() >> 4, hive.pos().z() >> 4)) {
            return;
        }
        Block block = world.getBlockAt(hive.pos().x(), hive.pos().y(), hive.pos().z());
        if (!HiveManager.isHiveBlock(block.getType())) {
            return;
        }

        long capMillis = plugin.configManager().offlineCapHours() * 3_600_000L;
        long elapsed = Math.min(capMillis, Math.max(0L, now - hive.lastTick()));
        if (elapsed <= 0L) {
            hive.setLastTick(now);
            return;
        }

        FlowerScanner.Result flowers;
        if (hive.flowerScannedAt() > 0L && now - hive.flowerScannedAt() < cacheMillis) {
            flowers = cachedFlowers(hive);
        } else if (scanBudget.getAndUpdate(value -> value > 0 ? value - 1 : 0) > 0) {
            flowers = FlowerScanner.scan(block, 8);
            hive.setFlowerCache(flowers.species(), flowers.rare(), now);
        } else {
            flowers = cachedFlowers(hive);
        }

        boolean storm = world.hasStorm();
        boolean night = !isDay(world);
        boolean snowy = isSnowy(world.getBiome(hive.pos().x(), hive.pos().y(), hive.pos().z()).name());
        hive.setEnvironment(storm, night, snowy, now);

        double speed = honeyService.speed(hive, flowers, storm || night, snowy);
        double minutes = elapsed / 60_000.0;
        double gain = minutes / plugin.configManager().baseMinutesPerLevel() * speed;

        hive.setHoney(Math.min(hive.honeyMax(), hive.honey() + gain));
        hive.setLastTick(now);
        syncVanillaLevel(block, hive);
        plugin.displays().ensureNow(hive);

        // Spread the pollination scan over several ticks.
        if (Math.floorMod(cycle + hive.pos().key().hashCode(), 5) == 0) {
            pollinationService.tick(hive, world, random);
        }
    }

    private FlowerScanner.Result cachedFlowers(HiveData hive) {
        return new FlowerScanner.Result(hive.cachedFlowerSpecies(), 0, hive.cachedFlowerRare(),
                Collections.emptySet());
    }

    private void saveIncrementally(long now) {
        if (now >= nextSaveAt) {
            saveCycle = new ArrayList<>(plugin.hives().all());
            saveCursor = 0;
        }
        if (saveCycle.isEmpty()) {
            return;
        }
        int budget = plugin.configManager().saveBatchSize();
        while (budget > 0 && saveCursor < saveCycle.size()) {
            HiveData hive = saveCycle.get(saveCursor++);
            if (plugin.hives().byKey(hive.pos().key()) == hive) {
                plugin.storage().save(hive);
            }
            budget--;
        }
        if (saveCursor >= saveCycle.size()) {
            saveCycle = List.of();
            nextSaveAt = now + plugin.configManager().saveIntervalSeconds() * 1000L;
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
