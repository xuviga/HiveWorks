package com.xuvigan.hiveworks.atlas;

import com.xuvigan.hiveworks.HiveWorksPlugin;
import com.xuvigan.hiveworks.product.ProductType;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Loads atlas.yml rewards. Missing or malformed entries are skipped, never fatal.
 */
public final class AtlasRewardRegistry {

    private static final Pattern ITEM_PATTERN = Pattern.compile("^\\s*([A-Za-z_]+)(?:\\s*[xX]\\s*(\\d+))?");

    private final HiveWorksPlugin plugin;
    private final TreeMap<Integer, AtlasReward> rewards = new TreeMap<>();

    public AtlasRewardRegistry(HiveWorksPlugin plugin) {
        this.plugin = plugin;
        reload();
    }

    public void reload() {
        rewards.clear();
        plugin.saveResource("atlas.yml", false);
        File file = new File(plugin.getDataFolder(), "atlas.yml");
        YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
        ConfigurationSection root = config.getConfigurationSection("atlas.rewards");
        if (root == null) {
            plugin.getLogger().warning("atlas.yml has no 'atlas.rewards' section.");
            return;
        }
        for (String key : root.getKeys(false)) {
            int threshold;
            try {
                threshold = Integer.parseInt(key);
            } catch (NumberFormatException exception) {
                continue;
            }
            ConfigurationSection section = root.getConfigurationSection(key);
            if (section == null) {
                continue;
            }
            AtlasReward reward = parse(threshold, section.getString("name", "Reward"),
                    section.getString("item", ""));
            if (reward != null) {
                rewards.put(threshold, reward);
            }
        }
        plugin.getLogger().info("Loaded " + rewards.size() + " atlas rewards.");
    }

    private AtlasReward parse(int threshold, String name, String rawItem) {
        Matcher matcher = ITEM_PATTERN.matcher(rawItem == null ? "" : rawItem);
        if (!matcher.find()) {
            return null;
        }
        String materialName = matcher.group(1).toUpperCase(java.util.Locale.ROOT);
        int amount = matcher.group(2) == null ? 1 : Integer.parseInt(matcher.group(2));
        ProductType product = switch (materialName) {
            case "WAX" -> ProductType.WAX;
            case "ROYAL_JELLY" -> ProductType.ROYAL_JELLY;
            default -> null;
        };
        if (product != null) {
            return new AtlasReward(threshold, name, product.material(), amount, product);
        }
        Material material = Material.matchMaterial(materialName);
        if (material == null) {
            plugin.getLogger().warning("Unknown atlas reward material: " + materialName);
            return null;
        }
        return new AtlasReward(threshold, name, material, amount, null);
    }

    public List<AtlasReward> rewards() {
        return Collections.unmodifiableList(new ArrayList<>(rewards.values()));
    }

    public Optional<AtlasReward> byThreshold(int threshold) {
        return Optional.ofNullable(rewards.get(threshold));
    }

    public Optional<AtlasReward> at(int index) {
        if (index < 0 || index >= rewards.size()) {
            return Optional.empty();
        }
        return Optional.of(new ArrayList<>(rewards.values()).get(index));
    }

    public int size() {
        return rewards.size();
    }
}
