package com.xuvigan.hiveworks.gui;

import com.xuvigan.hiveworks.HiveWorksPlugin;
import com.xuvigan.hiveworks.bee.BeeItemData;
import com.xuvigan.hiveworks.bee.Species;
import com.xuvigan.hiveworks.hive.BeeSlot;
import com.xuvigan.hiveworks.hive.FrameSlot;
import com.xuvigan.hiveworks.hive.HiveData;
import com.xuvigan.hiveworks.hive.HiveMode;
import com.xuvigan.hiveworks.item.FrameItem;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.Optional;

/**
 * Handles clicks inside the 9x5 hive panel.
 */
public final class GuiListener implements Listener {

    private final HiveWorksPlugin plugin;

    public GuiListener(HiveWorksPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        Inventory top = event.getView().getTopInventory();
        if (top.getHolder() instanceof AtlasHolder) {
            event.setCancelled(true);
            if (event.getWhoClicked() instanceof Player player && event.getRawSlot() == AtlasGui.SLOT_CLOSE) {
                player.closeInventory();
            }
            return;
        }
        if (!(top.getHolder() instanceof GuiHolder holder)) {
            return;
        }
        event.setCancelled(true);
        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }
        int slot = event.getRawSlot();
        if (slot < 0 || slot >= top.getSize()) {
            return;
        }

        HiveData hive = holder.hive();
        if (slot == HiveGui.SLOT_CLOSE) {
            player.closeInventory();
            return;
        }
        if (slot == HiveGui.SLOT_HARVEST) {
            plugin.hives().harvest(player, hive);
            reopen(player, hive);
            return;
        }
        if (slot >= HiveGui.SLOT_MODE_START && slot < HiveGui.SLOT_MODE_START + 3) {
            switchMode(player, hive, slot - HiveGui.SLOT_MODE_START);
            return;
        }
        if (slot >= HiveGui.SLOT_BEE_START && slot < HiveGui.SLOT_BEE_START + HiveData.MAX_BEES) {
            handleBeeSlot(player, hive, slot - HiveGui.SLOT_BEE_START, event.getCursor());
            return;
        }
        if (slot >= HiveGui.SLOT_FRAME_START && slot < HiveGui.SLOT_FRAME_START + HiveData.MAX_FRAMES) {
            handleFrameSlot(player, hive, slot - HiveGui.SLOT_FRAME_START, event.getCursor());
        }
    }

    @EventHandler
    public void onDrag(InventoryDragEvent event) {
        if (event.getView().getTopInventory().getHolder() instanceof GuiHolder
                || event.getView().getTopInventory().getHolder() instanceof AtlasHolder) {
            event.setCancelled(true);
        }
    }

    private void switchMode(Player player, HiveData hive, int index) {
        HiveMode mode = switch (index) {
            case 0 -> HiveMode.APIARY;
            case 1 -> HiveMode.POLLINATION;
            default -> HiveMode.COLLECTION;
        };
        if (mode == HiveMode.POLLINATION && !plugin.configManager().pollination()) {
            return;
        }
        hive.setMode(mode);
        plugin.hives().save(hive);
        plugin.messages().send(player, "hive-mode-changed", "mode", plugin.messages().raw(mode.messageKey()));
        reopen(player, hive);
    }

    private void handleBeeSlot(Player player, HiveData hive, int slot, ItemStack cursor) {
        if (slot < 0 || slot >= hive.tier().beeSlots()) {
            return;
        }
        BeeSlot existing = hive.bee(slot);
        if (existing != null) {
            if (cursor != null && cursor.getType() != Material.AIR) {
                return;
            }
            BeeSlot removed = plugin.hives().removeBee(hive, slot);
            if (removed != null) {
                Optional<Species> species = plugin.speciesRegistry().byId(removed.speciesId());
                species.ifPresent(value -> player.setItemOnCursor(
                        plugin.itemFactory().beeJar(removed.beeId(), value, removed.genes(), removed.purity())
                ));
                plugin.messages().send(player, "hive-bee-removed");
                reopen(player, hive);
            }
            return;
        }

        Optional<BeeItemData> data = plugin.beeItem().read(cursor);
        if (data.isEmpty()) {
            plugin.messages().send(player, "hive-wrong-item");
            return;
        }
        if (plugin.speciesRegistry().byId(data.get().speciesId()).isEmpty()) {
            plugin.messages().send(player, "unknown-species", "species", data.get().speciesId());
            return;
        }
        if (!plugin.hives().addBee(hive, slot, data.get())) {
            plugin.messages().send(player, "hive-bees-full");
            return;
        }
        plugin.atlas().discover(player, data.get().speciesId());
        int remaining = cursor.getAmount() - 1;
        cursor.setAmount(Math.max(0, remaining));
        plugin.messages().send(player, "hive-bee-added");
        reopen(player, hive);
    }

    private void handleFrameSlot(Player player, HiveData hive, int slot, ItemStack cursor) {
        if (slot < 0 || slot >= hive.tier().frameSlots()) {
            return;
        }
        FrameSlot existing = hive.frame(slot);
        if (existing != null) {
            if (cursor != null && cursor.getType() != Material.AIR) {
                return;
            }
            hive.setFrame(slot, null);
            plugin.hives().save(hive);
            player.setItemOnCursor(plugin.itemFactory().frame(existing.type(), existing.usesLeft()));
            plugin.messages().send(player, "frame-removed");
            reopen(player, hive);
            return;
        }

        var data = plugin.frameItem().read(cursor);
        if (data.isEmpty()) {
            plugin.messages().send(player, "hive-wrong-item");
            return;
        }
        FrameItem.FrameItemData frame = data.get();
        hive.setFrame(slot, new FrameSlot(frame.type(), frame.usesLeft()));
        plugin.hives().save(hive);
        if (cursor != null) {
            cursor.setAmount(Math.max(0, cursor.getAmount() - 1));
        }
        plugin.messages().send(player, "frame-added", "frame", frame.type().displayName());
        reopen(player, hive);
    }

    private void reopen(Player player, HiveData hive) {
        Bukkit.getScheduler().runTask(plugin, () -> new HiveGui(plugin, hive).open(player));
    }
}
