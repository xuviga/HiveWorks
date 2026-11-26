package com.xuvigan.hiveworks.craft;

import com.xuvigan.hiveworks.HiveWorksPlugin;

/**
 * Custom recipes arrive in 0.1.1.
 */
public final class RecipeRegistrar {

    private final HiveWorksPlugin plugin;

    public RecipeRegistrar(HiveWorksPlugin plugin) {
        this.plugin = plugin;
    }

    public void registerAll() {
        plugin.getLogger().info("Custom recipes: 0 registered (planned for 0.1.1).");
    }
}
