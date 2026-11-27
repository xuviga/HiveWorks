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
            if (existing != null) {
                var queenData = plugin.queenItem().read(hand);
                if (queenData.isPresent()) {
                    if (!player.hasPermission("hiveworks.use")) {
                        plugin.messages().send(player, "no-permission");
                        return;
                    }
                    if (!plugin.hives().canManage(player, existing)) {
                        event.setCancelled(true);
                        plugin.messages().send(player, "hive-not-owner");
                        return;
                    }
                    var species = plugin.speciesRegistry().byId(queenData.get().speciesId());
                    if (species.isEmpty()) {
                        plugin.messages().send(player, "unknown-species", "species", queenData.get().speciesId());
                        return;
                    }
                    event.setCancelled(true);
                    HiveManager.QueenInstallResult result = plugin.hives().installQueen(existing, queenData.get());
                    switch (result.status()) {
                        case INSTALLED, REPLACED -> {
                            hand.setAmount(Math.max(0, hand.getAmount() - 1));
                            plugin.atlas().discover(player, queenData.get().speciesId());
                            if (result.replaced() != null) {
                                plugin.speciesRegistry().byId(result.replaced().speciesId()).ifPresent(oldSpecies ->
                                        giveOrDrop(player, plugin.itemFactory().queenItem(
                                                result.replaced().beeId(), oldSpecies,
                                                result.replaced().genes(), result.replaced().purity())));
                                plugin.messages().send(player, "queen-replaced",
                                        "species", plugin.messages().speciesName(species.get()));
                            } else {
                                plugin.messages().send(player, "queen-installed",
                                        "species", plugin.messages().speciesName(species.get()));
                            }
                        }
                        case NO_SPACE -> plugin.messages().send(player, "hive-bees-full");
                        case DUPLICATE -> plugin.messages().send(player, "hive-bee-duplicate");
                    }
                    return;
                }
            }
            if (existing != null && plugin.productItem().isPollen(hand)) {
                if (!player.hasPermission("hiveworks.use")) {
                    plugin.messages().send(player, "no-permission");
                    return;
                }
                if (!plugin.hives().canManage(player, existing)) {
                    event.setCancelled(true);
                    plugin.messages().send(player, "hive-not-owner");
                    return;
                }
                event.setCancelled(true);
                existing.setPollenBonus(plugin.configManager().pollenBonus());
                plugin.hives().save(existing);
                hand.setAmount(Math.max(0, hand.getAmount() - 1));
                plugin.messages().send(player, "hive-pollen-applied",
                        "bonus", String.format(java.util.Locale.ROOT, "%.2f", plugin.configManager().pollenBonus()));
                return;
            }
            // Registered hives use HiveWorks harvesting instead of vanilla bottles/shears.
            if (existing != null && (hand.getType() == Material.GLASS_BOTTLE || hand.getType() == Material.SHEARS)) {
                if (!player.hasPermission("hiveworks.use")) {
                    plugin.messages().send(player, "no-permission");
                    return;
                }
                if (!plugin.hives().canManage(player, existing)) {
                    event.setCancelled(true);
                    plugin.messages().send(player, "hive-not-owner");
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
        if (existing != null) {
            if (!plugin.hives().canManage(player, existing)) {
                plugin.messages().send(player, "hive-not-owner");
                return;
            }
            plugin.schedulers().executeFor(player, () -> new HiveGui(plugin, existing).open(player));
            return;
        }
        HiveData hive = plugin.hives().create(block, player);
        if (hive != null) {
            plugin.schedulers().executeFor(player, () -> new HiveGui(plugin, hive).open(player));
        }
    }

    private void giveOrDrop(Player player, ItemStack item) {
        player.getInventory().addItem(item).values()
                .forEach(drop -> player.getWorld().dropItemNaturally(player.getLocation(), drop));
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

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBreakProtect(BlockBreakEvent event) {
        if (!HiveManager.isHiveBlock(event.getBlock().getType())) {
            return;
        }
        HiveData hive = plugin.hives().get(event.getBlock());
        if (hive != null && !plugin.hives().canManage(event.getPlayer(), hive)) {
            event.setCancelled(true);
            plugin.messages().send(event.getPlayer(), "hive-not-owner");
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBreak(BlockBreakEvent event) {
        if (!HiveManager.isHiveBlock(event.getBlock().getType())) {
            return;
        }
        HiveData hive = plugin.hives().get(event.getBlock());
        if (hive != null) {
            plugin.raids().clearHive(hive.pos().key());
            plugin.guardService().clearHive(hive.pos().key());
            plugin.displays().remove(hive);
            if (plugin.configManager().dropContentsOnBreak()) {
                plugin.hives().dropContents(hive,
                        event.getBlock().getLocation().add(0.5, 0.5, 0.5));
            }
            plugin.hives().remove(hive);
        }
    }
}
