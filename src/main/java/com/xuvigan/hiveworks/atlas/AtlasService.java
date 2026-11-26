package com.xuvigan.hiveworks.atlas;

import com.xuvigan.hiveworks.HiveWorksPlugin;
import org.bukkit.entity.Player;

import java.util.LinkedHashSet;
import java.util.Map;
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
        Set<String> discovered = discovered(player.getUniqueId());
        if (!discovered.add(speciesId)) {
            return false;
        }
        plugin.storage().discoverSpecies(player.getUniqueId(), player.getName(), speciesId);
        plugin.speciesRegistry().byId(speciesId).ifPresent(species ->
                plugin.messages().send(player, "atlas-new", "species", species.displayName()));
        return true;
    }

    public void reload() {
        cache.clear();
    }
}
