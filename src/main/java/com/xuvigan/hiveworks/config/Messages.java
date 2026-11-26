package com.xuvigan.hiveworks.config;

import com.xuvigan.hiveworks.HiveWorksPlugin;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.util.Collections;
import java.util.List;

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
        plugin.saveResource(fileName, false);
        return YamlConfiguration.loadConfiguration(new File(plugin.getDataFolder(), fileName));
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
