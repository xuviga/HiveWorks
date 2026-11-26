package com.xuvigan.hiveworks.world;

import com.xuvigan.hiveworks.HiveWorksPlugin;

/**
 * Wild swarm spawner. Planned for 0.1.1 (dawn, flower biomes, anti-farm radius).
 */
public final class WildSwarmSpawner {

    private final HiveWorksPlugin plugin;

    public WildSwarmSpawner(HiveWorksPlugin plugin) {
        this.plugin = plugin;
    }

    public void start() {
        plugin.getLogger().info("Wild swarm spawner: disabled in 0.1.0 skeleton.");
    }

    public void stop() {
        // nothing to cancel yet
    }
}
