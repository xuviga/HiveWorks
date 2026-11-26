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
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.inventory.meta.ItemMeta;

/**
 * Registered hives: empty hand opens the GUI, bottle/shears run HiveWorks harvesting.
 * Unregistered hives keep vanilla behaviour.
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
        Player player = event.getPlayer();
        HiveData existing = plugin.hives().get(block);
        if (hand != null && hand.getType() != Material.AIR) {
            // Registered hives use HiveWorks harvesting instead of vanilla bottles/shears.
            if (existing != null && (hand.getType() == Material.GLASS_BOTTLE || hand.getType() == Material.SHEARS)) {
                if (!player.hasPermission("hiveworks.use")) {
                    plugin.messages().send(player, "no-permission");
                    return;
                }
                event.setCancelled(true);
                boolean bottle = hand.getType() == Material.GLASS_BOTTLE;
                boolean harvested = plugin.hives().harvest(player, existing) != null;
                if (!harvested) {
                    return;
                }
                if (bottle) {
                    hand.setAmount(Math.max(0, hand.getAmount() - 1));
                } else {
                    damageShears(player, hand);
                }
            }
            return;
        }
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

    private void damageShears(Player player, ItemStack shears) {
        ItemMeta meta = shears.getItemMeta();
        if (!(meta instanceof Damageable damageable)) {
            return;
        }
        int next = damageable.getDamage() + 1;
        if (next >= shears.getType().getMaxDurability()) {
            shears.setAmount(0);
            return;
        }
        damageable.setDamage(next);
        shears.setItemMeta(meta);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBreak(BlockBreakEvent event) {
        if (!HiveManager.isHiveBlock(event.getBlock().getType())) {
            return;
        }
        HiveData hive = plugin.hives().get(event.getBlock());
        if (hive != null) {
            plugin.displays().remove(hive);
            plugin.hives().remove(hive);
        }
    }
}
