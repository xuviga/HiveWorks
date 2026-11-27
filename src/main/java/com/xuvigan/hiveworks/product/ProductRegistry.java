package com.xuvigan.hiveworks.product;

import com.xuvigan.hiveworks.HiveWorksPlugin;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.util.Locale;

/**
 * Applies products.yml names and custom model data to ProductType.
 */
public final class ProductRegistry {

    private final HiveWorksPlugin plugin;

    public ProductRegistry(HiveWorksPlugin plugin) {
        this.plugin = plugin;
        reload();
    }

    public void reload() {
        plugin.saveResource("products.yml", false);
        YamlConfiguration config = YamlConfiguration.loadConfiguration(
                new File(plugin.getDataFolder(), "products.yml"));
        ConfigurationSection root = config.getConfigurationSection("products");
        if (root == null) {
            plugin.getLogger().warning("products.yml has no 'products' section.");
            return;
        }
        for (ProductType type : ProductType.values()) {
            ConfigurationSection section = root.getConfigurationSection(
                    type.name().toLowerCase(Locale.ROOT));
            if (section == null) {
                continue;
            }
            type.applyConfig(
                    section.getString("name", type.displayName()),
                    section.getInt("cmd", type.customModelData())
            );
        }
        plugin.getLogger().info("Loaded " + ProductType.values().length + " product definitions.");
    }
}
