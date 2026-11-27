package com.xuvigan.hiveworks.config;

import com.xuvigan.hiveworks.HiveWorksPlugin;
import com.xuvigan.hiveworks.bee.Species;
import com.xuvigan.hiveworks.hive.FrameType;
import com.xuvigan.hiveworks.hive.HiveTier;
import com.xuvigan.hiveworks.product.ProductType;
import com.xuvigan.hiveworks.production.Quality;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

/**
 * MiniMessage-based localization with an English fallback.
 */
public final class Messages {

    private final HiveWorksPlugin plugin;
    private final MiniMessage mini = MiniMessage.miniMessage();
    private YamlConfiguration primary;
    private YamlConfiguration fallback;

    public Messages(HiveWorksPlugin plugin) {
        this.plugin = plugin;
        reload();
    }

    public void reload() {
        String locale = plugin.getConfig().getString("locale", "ru");
        this.primary = load("messages_" + locale + ".yml");
        this.fallback = load("messages_en.yml");
    }

    private YamlConfiguration load(String fileName) {
        File file = new File(plugin.getDataFolder(), fileName);
        if (!file.exists()) {
            if (plugin.getResource(fileName) == null) {
                return new YamlConfiguration();
            }
            plugin.saveResource(fileName, false);
        }
        YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
        mergeDefaults(file, fileName, config);
        return config;
    }

    /**
     * Adds keys that exist in the bundled file but not on disk yet, so plugin
     * updates ship new lore/descriptions without wiping player edits.
     */
    private void mergeDefaults(File file, String fileName, YamlConfiguration config) {
        InputStream stream = plugin.getResource(fileName);
        if (stream == null) {
            return;
        }
        try (InputStream in = stream) {
            YamlConfiguration defaults = YamlConfiguration.loadConfiguration(
                    new InputStreamReader(in, StandardCharsets.UTF_8));
            boolean changed = false;
            for (String key : defaults.getKeys(true)) {
                if (defaults.isConfigurationSection(key) || config.contains(key)) {
                    continue;
                }
                config.set(key, defaults.get(key));
                changed = true;
            }
            if (changed) {
                config.save(file);
            }
        } catch (IOException | IllegalArgumentException ignored) {
            // A broken or unreadable user file should never stop the plugin.
        }
    }

    public String raw(String key) {
        String value = primary.getString(key);
        if (value == null) {
            value = fallback.getString(key);
        }
        return value == null ? "<red>Missing message: " + key + "</red>" : value;
    }

    public Component component(String key, Object... placeholders) {
        return mini.deserialize(fill(raw(key), placeholders));
    }

    /** Raw MiniMessage string with placeholders applied. */
    public String formatted(String key, Object... placeholders) {
        return fill(raw(key), placeholders);
    }

    /** Lore lines with placeholders applied. */
    public List<String> formattedList(String key, Object... placeholders) {
        return list(key).stream().map(line -> fill(line, placeholders)).toList();
    }

    public List<String> list(String key) {
        List<String> lines = primary.getStringList(key);
        if (lines.isEmpty()) {
            lines = fallback.getStringList(key);
        }
        return lines.isEmpty() ? Collections.emptyList() : lines;
    }

    public String speciesName(Species species) {
        String locale = plugin.getConfig().getString("locale", "ru");
        if ("en".equalsIgnoreCase(locale) && species.displayNameEn() != null
                && !species.displayNameEn().isBlank()) {
            return species.displayNameEn();
        }
        return species.displayName();
    }

    public String tierName(HiveTier tier) {
        return raw("tier." + tier.name());
    }

    public String frameName(FrameType type) {
        return raw("frame." + type.name().toLowerCase(Locale.ROOT) + ".name");
    }

    public String frameEffect(FrameType type) {
        return raw("frame." + type.name().toLowerCase(Locale.ROOT) + ".effect");
    }

    public String productName(ProductType type) {
        return raw("product." + type.name().toLowerCase(Locale.ROOT) + ".name");
    }

    public String qualityName(Quality quality) {
        return raw("quality." + quality.name().toLowerCase(Locale.ROOT) + ".name");
    }

    public void send(CommandSender sender, String key, Object... placeholders) {
        sender.sendMessage(component(key, placeholders));
    }

    public Component parse(String miniMessage) {
        return mini.deserialize(miniMessage);
    }

    private String fill(String message, Object... placeholders) {
        String result = message;
        for (int index = 0; index + 1 < placeholders.length; index += 2) {
            result = result.replace("{" + placeholders[index] + "}", String.valueOf(placeholders[index + 1]));
        }
        return result;
    }
}
