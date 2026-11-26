package com.xuvigan.hiveworks.world;

import com.xuvigan.hiveworks.HiveWorksPlugin;

/**
 * Fox that steals one honey level at night. Planned for 0.1.1.
 */
public final class FoxRaider {

    private final HiveWorksPlugin plugin;

    public FoxRaider(HiveWorksPlugin plugin) {
        this.plugin = plugin;
    }

    public void start() {
        plugin.getLogger().info("Fox raider: disabled in 0.1.0 skeleton.");
    }

    public void stop() {
        // nothing to cancel yet
    }
}
