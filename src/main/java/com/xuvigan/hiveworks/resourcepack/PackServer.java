package com.xuvigan.hiveworks.resourcepack;

import com.xuvigan.hiveworks.HiveWorksPlugin;
import com.xuvigan.hiveworks.config.ConfigManager;

/**
 * Resource pack delivery: OFF, URL or an embedded HTTP server (JDK only).
 * The pack itself ships in 0.1.1.
 */
public final class PackServer {

    private final HiveWorksPlugin plugin;
    private final ConfigManager config;

    public PackServer(HiveWorksPlugin plugin, ConfigManager config) {
        this.plugin = plugin;
        this.config = config;
    }

    public void start() {
        plugin.getLogger().info("Resource pack mode: " + config.resourcePackMode() + " (pack arrives in 0.1.1).");
    }

    public void stop() {
        // embedded HTTP server will be stopped here
    }
}
