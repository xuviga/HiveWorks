package com.xuvigan.hiveworks.product;

import com.xuvigan.hiveworks.HiveWorksPlugin;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerItemConsumeEvent;

/**
 * Applies mead buffs when a player drinks a HiveWorks mead bottle.
 */
public final class MeadListener implements Listener {

    private final HiveWorksPlugin plugin;

    public MeadListener(HiveWorksPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(ignoreCancelled = true)
    public void onConsume(PlayerItemConsumeEvent event) {
        if (!plugin.configManager().meadBuffs()) {
            return;
        }
        plugin.meadItem().read(event.getItem()).ifPresent(type -> {
            Player player = event.getPlayer();
            type.apply(player);
        });
    }
}
