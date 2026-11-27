package com.xuvigan.hiveworks.util;

import com.xuvigan.hiveworks.hive.HivePos;
import io.papermc.paper.threadedregions.scheduler.ScheduledTask;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.plugin.Plugin;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import java.util.logging.Level;

/**
 * Folia-aware scheduling seam.
 *
 * <p>Uses the regionised scheduler API ({@link io.papermc.paper.threadedregions.scheduler.RegionScheduler},
 * {@link io.papermc.paper.threadedregions.scheduler.EntityScheduler},
 * {@link io.papermc.paper.threadedregions.scheduler.GlobalRegionScheduler},
 * {@link io.papermc.paper.threadedregions.scheduler.AsyncScheduler}). Paper implements the same
 * API on its main thread, so there is no BukkitScheduler fallback to maintain.</p>
 *
 * <p>Rules of thumb used across HiveWorks:</p>
 * <ul>
 *   <li>Global region: bookkeeping, timers that only touch in-memory state.</li>
 *   <li>Region of a block/chunk: every block, chunk, entity spawn or world mutation.</li>
 *   <li>Entity scheduler: every player/entity action (inventories, teleports, mob targets).</li>
 *   <li>Async: database flushes and IO only, never world access.</li>
 * </ul>
 */
public final class SchedulerAdapter {

    private final Plugin plugin;
    private final Set<ScheduledTask> tracked = ConcurrentHashMap.newKeySet();

    public SchedulerAdapter(Plugin plugin) {
        this.plugin = plugin;
    }

    public Plugin plugin() {
        return plugin;
    }

    // ------------------------------------------------------------------ global

    public ScheduledTask runGlobalTimer(Runnable action, long delayTicks, long periodTicks) {
        return schedule("schedule global timer", () -> Bukkit.getGlobalRegionScheduler().runAtFixedRate(plugin,
                consumer(action),
                Math.max(1L, delayTicks),
                Math.max(1L, periodTicks)));
    }

    public ScheduledTask runGlobalLater(Runnable action, long delayTicks) {
        return schedule("schedule global delayed task", () -> Bukkit.getGlobalRegionScheduler().runDelayed(plugin,
                consumer(action), Math.max(1L, delayTicks)));
    }

    public void runGlobal(Runnable action) {
        dispatch("run global task", () -> Bukkit.getGlobalRegionScheduler().execute(plugin, guarded(action)));
    }

    // ----------------------------------------------------------------- async

    public void runAsync(Runnable action) {
        dispatch("run async task", () -> Bukkit.getAsyncScheduler().runNow(plugin, consumer(action)));
    }

    // ---------------------------------------------------------------- regions

    /** Runs on the region owning the location, as soon as that region ticks. */
    public void executeAt(Location location, Runnable action) {
        if (location == null || location.getWorld() == null) {
            return;
        }
        World world = location.getWorld();
        int chunkX = location.getBlockX() >> 4;
        int chunkZ = location.getBlockZ() >> 4;
        dispatch("run region task", () -> Bukkit.getRegionScheduler()
                .execute(plugin, world, chunkX, chunkZ, guarded(action)));
    }

    public void executeAt(World world, int blockX, int blockZ, Runnable action) {
        if (world == null) {
            return;
        }
        dispatch("run region task", () -> Bukkit.getRegionScheduler()
                .execute(plugin, world, blockX >> 4, blockZ >> 4, guarded(action)));
    }

    public ScheduledTask runAtFixedRate(Location location, Runnable action, long delayTicks, long periodTicks) {
        if (location == null || location.getWorld() == null) {
            return null;
        }
        World world = location.getWorld();
        int chunkX = location.getBlockX() >> 4;
        int chunkZ = location.getBlockZ() >> 4;
        return schedule("schedule region timer", () -> Bukkit.getRegionScheduler().runAtFixedRate(plugin, world,
                chunkX, chunkZ, consumer(action), Math.max(1L, delayTicks), Math.max(1L, periodTicks)));
    }

    /** Runs on the region that owns the hive block. */
    public void executeAtHive(HivePos pos, Runnable action) {
        World world = Bukkit.getWorld(pos.world());
        if (world == null) {
            return;
        }
        dispatch("run hive region task", () -> Bukkit.getRegionScheduler()
                .execute(plugin, world, pos.x() >> 4, pos.z() >> 4, guarded(action)));
    }

    public ScheduledTask runAtHiveLater(HivePos pos, Runnable action, long delayTicks) {
        World world = Bukkit.getWorld(pos.world());
        if (world == null) {
            return null;
        }
        return schedule("schedule hive region task", () -> Bukkit.getRegionScheduler().runDelayed(plugin, world,
                pos.x() >> 4, pos.z() >> 4, consumer(action), Math.max(1L, delayTicks)));
    }

    // --------------------------------------------------------------- entities

    /** Runs immediately when already on the entity's region, otherwise on its next tick. */
    public void executeFor(Entity entity, Runnable action) {
        if (entity == null) {
            return;
        }
        dispatch("run entity task", () -> entity.getScheduler()
                .execute(plugin, guarded(action), null, 1L));
    }

    public ScheduledTask runForLater(Entity entity, Runnable action, long delayTicks) {
        if (entity == null) {
            return null;
        }
        return schedule("schedule entity task", () -> entity.getScheduler()
                .runDelayed(plugin, consumer(action), null, Math.max(1L, delayTicks)));
    }

    public ScheduledTask runForTimer(Entity entity, Runnable action, long delayTicks, long periodTicks) {
        if (entity == null) {
            return null;
        }
        return schedule("schedule entity timer", () -> entity.getScheduler()
                .runAtFixedRate(plugin, consumer(action), null, Math.max(1L, delayTicks), Math.max(1L, periodTicks)));
    }

    // ----------------------------------------------------------------- cancel

    public void cancelTasks() {
        for (ScheduledTask task : tracked) {
            try {
                task.cancel();
            } catch (IllegalStateException ignored) {
                // already finished
            }
        }
        tracked.clear();
    }

    // ------------------------------------------------------------ region help

    /** True when the current thread owns the region of this block. */
    public static boolean ownedNow(Block block) {
        if (block == null) {
            return false;
        }
        try {
            return Bukkit.isOwnedByCurrentRegion(block);
        } catch (Throwable throwable) {
            return false;
        }
    }

    public static boolean ownedNow(Location location) {
        if (location == null || location.getWorld() == null) {
            return false;
        }
        try {
            return Bukkit.isOwnedByCurrentRegion(location);
        } catch (Throwable throwable) {
            return false;
        }
    }

    public static boolean ownedNow(World world, int blockX, int blockZ) {
        return ownedChunkNow(world, blockX >> 4, blockZ >> 4);
    }

    /** Chunk-coordinate variant for multi-chunk scans. */
    public static boolean ownedChunkNow(World world, int chunkX, int chunkZ) {
        if (world == null) {
            return false;
        }
        try {
            return Bukkit.isOwnedByCurrentRegion(world, chunkX, chunkZ);
        } catch (Throwable throwable) {
            return false;
        }
    }

    public static boolean ownedNow(Entity entity) {
        if (entity == null) {
            return false;
        }
        try {
            return Bukkit.isOwnedByCurrentRegion(entity);
        } catch (Throwable throwable) {
            return false;
        }
    }

    // ---------------------------------------------------------------- helpers

    private ScheduledTask schedule(String what, java.util.function.Supplier<ScheduledTask> supplier) {
        try {
            ScheduledTask task = supplier.get();
            if (task != null) {
                track(task);
            }
            return task;
        } catch (Throwable throwable) {
            report(what, throwable);
            return null;
        }
    }

    private void dispatch(String what, Runnable action) {
        try {
            action.run();
        } catch (Throwable throwable) {
            report(what, throwable);
        }
    }

    private void report(String what, Throwable throwable) {
        if (plugin.isEnabled()) {
            plugin.getLogger().log(Level.WARNING, "HiveWorks could not " + what, throwable);
        }
    }

    private void track(ScheduledTask task) {
        if (task == null) {
            return;
        }
        tracked.add(task);
        if (!task.isRepeatingTask()) {
            // One-shot tasks are removed from the set by the wrapper below.
            return;
        }
    }

    private Consumer<ScheduledTask> consumer(Runnable action) {
        return task -> {
            try {
                action.run();
            } catch (Throwable throwable) {
                plugin.getLogger().log(Level.SEVERE, "HiveWorks scheduled task failed", throwable);
            } finally {
                if (task != null && !task.isRepeatingTask()) {
                    tracked.remove(task);
                }
            }
        };
    }

    private Runnable guarded(Runnable action) {
        return () -> {
            try {
                action.run();
            } catch (Throwable throwable) {
                plugin.getLogger().log(Level.SEVERE, "HiveWorks region task failed", throwable);
            }
        };
    }
}
