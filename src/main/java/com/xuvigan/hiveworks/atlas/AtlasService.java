package com.xuvigan.hiveworks.atlas;

import com.xuvigan.hiveworks.HiveWorksPlugin;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Bee atlas: discovered species per player, persisted by Storage.
 */
public final class AtlasService {

    private final HiveWorksPlugin plugin;
    private final Map<UUID, Set<String>> cache = new ConcurrentHashMap<>();

    public AtlasService(HiveWorksPlugin plugin) {
        this.plugin = plugin;
    }

    public Set<String> discovered(UUID uuid) {
        return cache.computeIfAbsent(uuid, id -> new LinkedHashSet<>(plugin.storage().discoveredSpecies(id)));
    }

    public int count(UUID uuid) {
        return discovered(uuid).size();
    }

    public boolean isDiscovered(UUID uuid, String speciesId) {
        return discovered(uuid).contains(speciesId);
    }

    public boolean discover(Player player, String speciesId) {
        if (!discover(player.getUniqueId(), player.getName(), speciesId)) {
            return false;
        }
        plugin.speciesRegistry().byId(speciesId).ifPresent(species ->
                plugin.messages().send(player, "atlas-new", "species", plugin.messages().speciesName(species)));
        return true;
    }

    /** Offline-safe discovery used by breeding and admin tools. */
    public boolean discover(UUID uuid, String playerName, String speciesId) {
        Set<String> discovered = discovered(uuid);
        if (!discovered.add(speciesId)) {
            return false;
        }
        plugin.storage().discoverSpecies(uuid, playerName == null ? "" : playerName, speciesId);
        return true;
    }

    public enum ClaimResult {
        CLAIMED,
        ALREADY,
        NOT_READY,
        UNKNOWN
    }

    public ClaimResult claim(Player player, int threshold) {
        Optional<AtlasReward> reward = plugin.atlasRewards().byThreshold(threshold);
        if (reward.isEmpty()) {
            return ClaimResult.UNKNOWN;
        }
        if (plugin.storage().claimedAtlasRewards(player.getUniqueId()).contains(threshold)) {
            return ClaimResult.ALREADY;
        }
        if (count(player.getUniqueId()) < threshold) {
            return ClaimResult.NOT_READY;
        }
        ItemStack item = reward.get().create(plugin.itemFactory());
        player.getInventory().addItem(item).values()
                .forEach(drop -> player.getWorld().dropItemNaturally(player.getLocation(), drop));
        plugin.storage().claimAtlasReward(player.getUniqueId(), threshold);
        plugin.messages().send(player, "atlas-reward-got",
                "reward", plugin.messages().formatted("atlas-reward-name", "threshold", threshold));
        return ClaimResult.CLAIMED;
    }

    public Optional<AtlasReward> rewardAt(int index) {
        return plugin.atlasRewards().at(index);
    }

    public Set<Integer> claimed(UUID uuid) {
        return plugin.storage().claimedAtlasRewards(uuid);
    }

    public void reload() {
        cache.clear();
    }
}
