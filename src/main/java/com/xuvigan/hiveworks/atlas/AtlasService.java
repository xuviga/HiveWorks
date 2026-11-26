package com.xuvigan.hiveworks.atlas;

import com.xuvigan.hiveworks.HiveWorksPlugin;
import com.xuvigan.hiveworks.bee.Species;
import org.bukkit.entity.Player;

/**
 * Bee atlas service. Persistence and GUI arrive in 0.1.1.
 */
public final class AtlasService {

    private final HiveWorksPlugin plugin;

    public AtlasService(HiveWorksPlugin plugin) {
        this.plugin = plugin;
    }

    public void discover(Player player, Species species) {
        // TODO 0.1.1: persist player.discovered and fire rewards.
        plugin.getLogger().fine("Atlas discovery is not implemented yet: "
                + player.getName() + " -> " + species.id());
    }
}
