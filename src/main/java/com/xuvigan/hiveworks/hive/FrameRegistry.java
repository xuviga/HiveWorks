package com.xuvigan.hiveworks.hive;

import com.xuvigan.hiveworks.HiveWorksPlugin;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.util.Locale;

/**
 * Applies frames.yml values to the FrameType enum. Enum constants stay the stable
 * identity (PDC/database), while numbers and the fallback name are configurable.
 */
public final class FrameRegistry {

    private final HiveWorksPlugin plugin;

    public FrameRegistry(HiveWorksPlugin plugin) {
        this.plugin = plugin;
        reload();
    }

    public void reload() {
        plugin.saveResource("frames.yml", false);
        YamlConfiguration config = YamlConfiguration.loadConfiguration(
                new File(plugin.getDataFolder(), "frames.yml"));
        ConfigurationSection root = config.getConfigurationSection("frames");
        if (root == null) {
            plugin.getLogger().warning("frames.yml has no 'frames' section.");
            return;
        }
        for (FrameType type : FrameType.values()) {
            ConfigurationSection section = root.getConfigurationSection(
                    type.name().toLowerCase(Locale.ROOT));
            if (section == null) {
                continue;
            }
            type.applyConfig(
                    section.getString("name", type.displayName()),
                    section.getInt("cmd", type.customModelData()),
                    section.getInt("max-uses", type.maxUses()),
                    section.getInt("max-honey", type.maxHoneyBonus()),
                    section.getDouble("speed", type.speedBonus()),
                    section.getDouble("quality-weight", type.qualityWeight()),
                    section.getDouble("quality-bonus", type.qualityBonus()),
                    section.getDouble("sting-reduction", type.stingReduction())
            );
        }
        plugin.getLogger().info("Loaded " + FrameType.values().length + " frame definitions.");
    }
}
