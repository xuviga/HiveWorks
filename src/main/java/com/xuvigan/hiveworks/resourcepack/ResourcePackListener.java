package com.xuvigan.hiveworks.resourcepack;

import com.xuvigan.hiveworks.HiveWorksPlugin;
import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

/**
 * Sends the pack one second after join, when the client is ready.
 */
public final class ResourcePackListener implements Listener {

    private final HiveWorksPlugin plugin;

    public ResourcePackListener(HiveWorksPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        if (!plugin.packServer().active()) {
            return;
        }
        Bukkit.getScheduler().runTaskLater(plugin,
                () -> plugin.packServer().send(event.getPlayer()), 20L);
    }
}
