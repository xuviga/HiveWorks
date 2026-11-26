package com.xuvigan.hiveworks.hive;

import com.xuvigan.hiveworks.HiveWorksPlugin;
import com.xuvigan.hiveworks.gui.HiveGui;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;

/**
 * Right-click with an empty hand opens the hive GUI; bottles and shears keep vanilla behaviour.
 */
public final class HiveListener implements Listener {

    private final HiveWorksPlugin plugin;

    public HiveListener(HiveWorksPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK || event.getClickedBlock() == null) {
            return;
        }
        if (event.getHand() != EquipmentSlot.HAND) {
            return;
        }
        Block block = event.getClickedBlock();
        if (!HiveManager.isHiveBlock(block.getType())) {
            return;
        }
        ItemStack hand = event.getItem();
        if (hand != null && hand.getType() != Material.AIR) {
            // Let vanilla bottle/shears interactions happen.
            return;
        }
        Player player = event.getPlayer();
        if (!player.hasPermission("hiveworks.use")) {
            plugin.messages().send(player, "no-permission");
            return;
        }
        event.setCancelled(true);
        HiveData hive = plugin.hives().create(block, player);
        if (hive != null) {
            new HiveGui(plugin, hive).open(player);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBreak(BlockBreakEvent event) {
        if (!HiveManager.isHiveBlock(event.getBlock().getType())) {
            return;
        }
        HiveData hive = plugin.hives().get(event.getBlock());
        if (hive != null) {
            plugin.hives().remove(hive);
        }
    }
}
