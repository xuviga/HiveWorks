package com.xuvigan.hiveworks.util;

import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitTask;

/**
 * Scheduler seam. Folia support (region schedulers) is planned after 1.0.
 */
public final class SchedulerAdapter {

    private final Plugin plugin;

    public SchedulerAdapter(Plugin plugin) {
        this.plugin = plugin;
    }

    public BukkitTask runTimer(Runnable task, long delayTicks, long periodTicks) {
        return Bukkit.getScheduler().runTaskTimer(plugin, task, delayTicks, periodTicks);
    }

    public void cancelTasks() {
        Bukkit.getScheduler().cancelTasks(plugin);
    }
}
