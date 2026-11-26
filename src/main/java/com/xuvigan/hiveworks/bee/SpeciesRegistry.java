package com.xuvigan.hiveworks.bee;

import com.xuvigan.hiveworks.HiveWorksPlugin;
import com.xuvigan.hiveworks.util.Version;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Random;
import java.util.Set;

/**
 * Loads species.yml. Version-gates entries such as pale_garden (1.21.4+).
 */
public final class SpeciesRegistry {

    private final HiveWorksPlugin plugin;
    private final Map<String, Species> byId = new LinkedHashMap<>();

    public SpeciesRegistry(HiveWorksPlugin plugin) {
        this.plugin = plugin;
        reload();
    }

    public void reload() {
        byId.clear();
        plugin.saveResource("species.yml", false);
        YamlConfiguration config = YamlConfiguration.loadConfiguration(new File(plugin.getDataFolder(), "species.yml"));
        ConfigurationSection root = config.getConfigurationSection("species");
        if (root == null) {
            plugin.getLogger().warning("species.yml has no 'species' section.");
            return;
        }

        String serverVersion = Version.minecraftVersion();
        for (String id : root.getKeys(false)) {
            ConfigurationSection section = root.getConfigurationSection(id);
            if (section == null) {
                continue;
            }
            String minVersion = section.getString("min-version");
            if (!Version.isAtLeast(serverVersion, minVersion)) {
                plugin.getLogger().info("Skipping species '" + id + "' (requires " + minVersion + ").");
                continue;
            }
            ConfigurationSection genes = section.getConfigurationSection("genes");
            Genes baseGenes = genes == null
                    ? Genes.DEFAULT
                    : new Genes(
                            genes.getInt("productivity", 50),
                            genes.getInt("speed", 50),
                            genes.getInt("cold", 50),
                            genes.getInt("temper", 50)
                    );
            Set<String> biomes = new LinkedHashSet<>();
            for (String biome : section.getStringList("biomes")) {
                biomes.add(biome.toLowerCase(Locale.ROOT));
            }
            Rarity rarity;
            try {
                rarity = Rarity.valueOf(section.getString("rarity", "COMMON").toUpperCase(Locale.ROOT));
            } catch (IllegalArgumentException exception) {
                rarity = Rarity.COMMON;
            }
            byId.put(id, new Species(
                    id,
                    section.getString("name", id),
                    Collections.unmodifiableSet(biomes),
                    rarity,
                    section.getInt("cmd", 1000),
                    Math.max(0, Math.min(100, section.getInt("purity", 50))),
                    baseGenes,
                    minVersion
            ));
        }
        plugin.getLogger().info("Loaded " + byId.size() + " bee species for " + serverVersion + ".");
    }

    public Optional<Species> byId(String id) {
        return Optional.ofNullable(byId.get(id));
    }

    public Optional<Species> byBiome(String biomeName) {
        if (biomeName == null) {
            return Optional.empty();
        }
        String normalized = biomeName.toLowerCase(Locale.ROOT);
        for (Species species : byId.values()) {
            if (species.biomes().contains(normalized)) {
                return Optional.of(species);
            }
        }
        return Optional.empty();
    }

    public Species random(Random random) {
        List<Species> all = new ArrayList<>(byId.values());
        if (all.isEmpty()) {
            throw new IllegalStateException("No species loaded from species.yml");
        }
        return all.get(random.nextInt(all.size()));
    }

    public Collection<Species> all() {
        return Collections.unmodifiableCollection(byId.values());
    }

    public int size() {
        return byId.size();
    }
}
