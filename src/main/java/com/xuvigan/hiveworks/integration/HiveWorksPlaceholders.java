package com.xuvigan.hiveworks.integration;

import com.xuvigan.hiveworks.HiveWorksPlugin;
import com.xuvigan.hiveworks.hive.HiveData;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.Locale;
import java.util.Optional;

/**
 * Optional PlaceholderAPI bridge. Registered only when PlaceholderAPI is present.
 */
public final class HiveWorksPlaceholders extends PlaceholderExpansion {

    private final HiveWorksPlugin plugin;

    public HiveWorksPlaceholders(HiveWorksPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public @NotNull String getIdentifier() {
        return "hiveworks";
    }

    @Override
    public @NotNull String getAuthor() {
        return "XuViGaN";
    }

    @Override
    public @NotNull String getVersion() {
        return plugin.getPluginMeta().getVersion();
    }

    @Override
    public boolean persist() {
        return true;
    }

    @Override
    public String onRequest(OfflinePlayer player, @NotNull String params) {
        if (player == null) {
            return null;
        }
        String key = params.toLowerCase(Locale.ROOT);
        switch (key) {
            case "hives", "hive_count" -> {
                return String.valueOf(ownedHives(player).size());
            }
            case "honey", "total_honey" -> {
                double total = ownedHives(player).stream().mapToDouble(HiveData::honey).sum();
                return String.format(Locale.ROOT, "%.1f", total);
            }
            case "atlas", "species_found" -> {
                return String.valueOf(plugin.atlas().count(player.getUniqueId()));
            }
            case "species", "species_total" -> {
                return String.valueOf(plugin.speciesRegistry().size());
            }
            case "queen_cycles", "queen" -> {
                return String.valueOf(nearest(player).map(HiveData::queenCycles).orElse(0));
            }
            case "tier" -> {
                return nearest(player).map(hive -> hive.tier().name()).orElse("");
            }
            case "mode" -> {
                return nearest(player).map(hive -> hive.mode().name().toLowerCase(Locale.ROOT)).orElse("");
            }
            default -> {
                return null;
            }
        }
    }

    private java.util.List<HiveData> ownedHives(OfflinePlayer player) {
        return plugin.hives().all().stream()
                .filter(hive -> hive.owner().equals(player.getUniqueId()))
                .toList();
    }

    private Optional<HiveData> nearest(OfflinePlayer player) {
        Player online = player.getPlayer();
        if (online == null || !online.isOnline()) {
            return Optional.empty();
        }
        return plugin.hives().findNearestOwned(online, 32.0);
    }
}
