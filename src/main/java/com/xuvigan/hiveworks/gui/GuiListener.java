package com.xuvigan.hiveworks.gui;

import com.xuvigan.hiveworks.HiveWorksPlugin;
import com.xuvigan.hiveworks.bee.BeeItemData;
import com.xuvigan.hiveworks.bee.QueenItemData;
import com.xuvigan.hiveworks.bee.Species;
import com.xuvigan.hiveworks.hive.BeeSlot;
import com.xuvigan.hiveworks.hive.FrameSlot;
import com.xuvigan.hiveworks.hive.HiveData;
import com.xuvigan.hiveworks.hive.HiveManager;
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
import java.util.Set;

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
        int slot = event.getRawSlot();
        boolean topClick = slot >= 0 && slot < top.getSize();
        if (top.getHolder() instanceof AtlasHolder) {
            if (!topClick) {
                return;
            }
            event.setCancelled(true);
            if (!(event.getWhoClicked() instanceof Player player)) {
                return;
            }
            if (slot == AtlasGui.SLOT_CLOSE) {
                player.closeInventory();
                return;
            }
            for (int index = 0; index < AtlasGui.REWARD_SLOTS.length; index++) {
                if (slot != AtlasGui.REWARD_SLOTS[index]) {
                    continue;
                }
                plugin.atlas().rewardAt(index).ifPresent(reward -> {
                    switch (plugin.atlas().claim(player, reward.threshold())) {
                        case CLAIMED -> plugin.schedulers().executeFor(player,
                                () -> new AtlasGui(plugin, player).open());
                        case ALREADY -> plugin.messages().send(player, "atlas-reward-already");
                        case NOT_READY -> plugin.messages().send(player, "atlas-reward-not-ready");
                        case UNKNOWN -> {
                        }
                    }
                });
                return;
            }
            return;
        }
        if (!(top.getHolder() instanceof GuiHolder holder)) {
            return;
        }
        if (!topClick) {
            return;
        }
        event.setCancelled(true);
        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }

        HiveData hive = holder.hive();
        if (!plugin.hives().canManage(player, hive)) {
            plugin.messages().send(player, "hive-not-owner");
            player.closeInventory();
            return;
        }
        if (slot == HiveGui.SLOT_CLOSE) {
            player.closeInventory();
            return;
        }
        if (slot == HiveGui.SLOT_UPGRADE) {
            handleUpgrade(player, hive);
            return;
        }
        if (slot == HiveGui.SLOT_BREEDING) {
            handleBreeding(player, hive);
            return;
        }
        if (slot == HiveGui.SLOT_RITUAL) {
            handleRitual(player, hive);
            return;
        }
        if (slot == HiveGui.SLOT_HARVEST) {
            plugin.hives().harvest(player, hive);
            reopen(player, hive);
            return;
        }
        if (slot >= HiveGui.SLOT_MODE_START && slot < HiveGui.SLOT_MODE_START + 4) {
            switchMode(player, hive, slot - HiveGui.SLOT_MODE_START);
            return;
        }
        if (slot == HiveGui.SLOT_QUEEN) {
            handleQueenSlot(player, hive, event.getCursor());
            return;
        }
        if (slot == HiveGui.SLOT_APIARY) {
            plugin.messages().send(player, "apiary-hint");
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
        Inventory top = event.getView().getTopInventory();
        boolean touchesTop = false;
        for (int raw : event.getRawSlots()) {
            if (raw >= 0 && raw < top.getSize()) {
                touchesTop = true;
                break;
            }
        }
        if (!touchesTop) {
            return;
        }
        if (top.getHolder() instanceof AtlasHolder) {
            event.setCancelled(true);
            return;
        }
        if (!(top.getHolder() instanceof GuiHolder holder)) {
            return;
        }
        event.setCancelled(true);
        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }
        Set<Integer> slots = event.getRawSlots();
        if (slots.size() != 1) {
            return;
        }
        int slot = slots.iterator().next();
        if (slot < 0 || slot >= top.getSize()) {
            return;
        }
        ItemStack cursor = event.getOldCursor();
        HiveData hive = holder.hive();
        if (!plugin.hives().canManage(player, hive)) {
            plugin.messages().send(player, "hive-not-owner");
            player.closeInventory();
            return;
        }
        if (slot >= HiveGui.SLOT_BEE_START && slot < HiveGui.SLOT_BEE_START + HiveData.MAX_BEES) {
            handleBeeSlot(player, hive, slot - HiveGui.SLOT_BEE_START, cursor);
            player.setItemOnCursor(cursor == null || cursor.getAmount() <= 0 ? new ItemStack(Material.AIR) : cursor);
        } else if (slot >= HiveGui.SLOT_FRAME_START && slot < HiveGui.SLOT_FRAME_START + HiveData.MAX_FRAMES) {
            handleFrameSlot(player, hive, slot - HiveGui.SLOT_FRAME_START, cursor);
            player.setItemOnCursor(cursor == null || cursor.getAmount() <= 0 ? new ItemStack(Material.AIR) : cursor);
        } else if (slot == HiveGui.SLOT_QUEEN) {
            handleQueenSlot(player, hive, cursor);
            player.setItemOnCursor(cursor == null || cursor.getAmount() <= 0 ? new ItemStack(Material.AIR) : cursor);
        }
    }

    private void switchMode(Player player, HiveData hive, int index) {
        HiveMode mode = switch (index) {
            case 0 -> HiveMode.APIARY;
            case 1 -> HiveMode.POLLINATION;
            case 2 -> HiveMode.COLLECTION;
            case 3 -> HiveMode.GUARD;
            default -> HiveMode.APIARY;
        };
        if (mode == HiveMode.POLLINATION && !plugin.configManager().pollination()) {
            return;
        }
        if (mode == HiveMode.GUARD && !plugin.configManager().guardEnabled()) {
            return;
        }
        hive.setMode(mode);
        plugin.hives().save(hive);
        if (mode == HiveMode.GUARD) {
            plugin.guardService().activate(hive);
        } else {
            plugin.guardService().clearHive(hive.pos().key());
        }
        plugin.messages().send(player, "hive-mode-changed", "mode", plugin.messages().raw(mode.messageKey()));
        reopen(player, hive);
    }

    private void handleBeeSlot(Player player, HiveData hive, int slot, ItemStack cursor) {
        if (slot < 0 || slot >= hive.tier().beeSlots()) {
            return;
        }
        BeeSlot existing = hive.bee(slot);
        if (existing != null) {
            if (slot == hive.queenSlot() && tryFeedQueen(player, hive, cursor)) {
                return;
            }
            if (cursor != null && cursor.getType() != Material.AIR) {
                return;
            }
            boolean wasQueen = hive.queenSlot() == slot;
            BeeSlot removed = plugin.hives().removeBee(hive, slot);
            if (removed != null) {
                Optional<Species> species = plugin.speciesRegistry().byId(removed.speciesId());
                species.ifPresent(value -> player.setItemOnCursor(wasQueen
                        ? plugin.itemFactory().queenItem(removed.beeId(), value, removed.genes(), removed.purity())
                        : plugin.itemFactory().beeJar(removed.beeId(), value, removed.genes(), removed.purity())
                ));
                plugin.messages().send(player, wasQueen ? "queen-removed" : "hive-bee-removed");
                reopen(player, hive);
            }
            return;
        }

        Optional<BeeItemData> data = plugin.beeItem().read(cursor);
        if (data.isEmpty()) {
            plugin.messages().send(player, "hive-wrong-item");
            return;
        }
        Optional<Species> species = plugin.speciesRegistry().byId(data.get().speciesId());
        if (species.isEmpty()) {
            plugin.messages().send(player, "unknown-species", "species", data.get().speciesId());
            return;
        }
        boolean hadQueen = hive.hasQueen();
        HiveManager.BeeAddResult added = plugin.hives().addBee(hive, slot, data.get());
        if (added != HiveManager.BeeAddResult.ADDED) {
            plugin.messages().send(player, added == HiveManager.BeeAddResult.DUPLICATE
                    ? "hive-bee-duplicate" : "hive-bees-full");
            return;
        }
        plugin.atlas().discover(player, data.get().speciesId());
        int remaining = cursor.getAmount() - 1;
        cursor.setAmount(Math.max(0, remaining));
        if (hadQueen) {
            plugin.messages().send(player, "hive-bee-added");
        } else {
            plugin.messages().send(player, "queen-appointed",
                    "species", plugin.messages().speciesName(species.get()));
        }
        reopen(player, hive);
    }

    private void handleQueenSlot(Player player, HiveData hive, ItemStack cursor) {
        boolean hasCursor = cursor != null && cursor.getType() != Material.AIR;
        Optional<QueenItemData> queenData = plugin.queenItem().read(cursor);
        if (queenData.isPresent()) {
            Optional<Species> queenSpecies = plugin.speciesRegistry().byId(queenData.get().speciesId());
            if (queenSpecies.isEmpty()) {
                plugin.messages().send(player, "unknown-species", "species", queenData.get().speciesId());
                return;
            }
            HiveManager.QueenInstallResult result = plugin.hives().installQueen(hive, queenData.get());
            switch (result.status()) {
                case INSTALLED, REPLACED -> {
                    cursor.setAmount(Math.max(0, cursor.getAmount() - 1));
                    plugin.atlas().discover(player, queenData.get().speciesId());
                    if (result.replaced() != null) {
                        plugin.speciesRegistry().byId(result.replaced().speciesId()).ifPresent(oldSpecies ->
                                giveOrDrop(player, plugin.itemFactory().queenItem(
                                        result.replaced().beeId(), oldSpecies,
                                        result.replaced().genes(), result.replaced().purity())));
                        plugin.messages().send(player, "queen-replaced",
                                "species", plugin.messages().speciesName(queenSpecies.get()));
                    } else {
                        plugin.messages().send(player, "queen-installed",
                                "species", plugin.messages().speciesName(queenSpecies.get()));
                    }
                    reopen(player, hive);
                }
                case NO_SPACE -> plugin.messages().send(player, "hive-bees-full");
                case DUPLICATE -> plugin.messages().send(player, "hive-bee-duplicate");
            }
            return;
        }

        if (hive.hasQueen()) {
            if (tryFeedQueen(player, hive, cursor)) {
                return;
            }
            if (!hasCursor) {
                BeeSlot removed = plugin.hives().removeBee(hive, hive.queenSlot());
                if (removed != null) {
                    plugin.speciesRegistry().byId(removed.speciesId()).ifPresent(species ->
                            player.setItemOnCursor(plugin.itemFactory().queenItem(
                                    removed.beeId(), species, removed.genes(), removed.purity())));
                    plugin.messages().send(player, "queen-removed");
                    reopen(player, hive);
                }
                return;
            }
            plugin.messages().send(player, "queen-needs-jelly");
            return;
        }

        Optional<BeeItemData> data = plugin.beeItem().read(cursor);
        if (data.isEmpty()) {
            plugin.messages().send(player, "hive-wrong-item");
            return;
        }
        Optional<Species> species = plugin.speciesRegistry().byId(data.get().speciesId());
        if (species.isEmpty()) {
            plugin.messages().send(player, "unknown-species", "species", data.get().speciesId());
            return;
        }
        int free = firstFreeBeeSlot(hive);
        if (free < 0) {
            plugin.messages().send(player, "hive-bees-full");
            return;
        }
        HiveManager.BeeAddResult added = plugin.hives().addBee(hive, free, data.get());
        if (added != HiveManager.BeeAddResult.ADDED) {
            plugin.messages().send(player, added == HiveManager.BeeAddResult.DUPLICATE
                    ? "hive-bee-duplicate" : "hive-bees-full");
            return;
        }
        plugin.atlas().discover(player, data.get().speciesId());
        cursor.setAmount(Math.max(0, cursor.getAmount() - 1));
        plugin.messages().send(player, "queen-appointed",
                "species", plugin.messages().speciesName(species.get()));
        reopen(player, hive);
    }

    private void giveOrDrop(Player player, ItemStack item) {
        player.getInventory().addItem(item).values()
                .forEach(drop -> player.getWorld().dropItemNaturally(player.getLocation(), drop));
    }

    private int firstFreeBeeSlot(HiveData hive) {
        for (int slot = 0; slot < hive.tier().beeSlots() && slot < HiveData.MAX_BEES; slot++) {
            if (hive.bee(slot) == null) {
                return slot;
            }
        }
        return -1;
    }

    private boolean tryFeedQueen(Player player, HiveData hive, ItemStack cursor) {
        if (!plugin.productItem().isRoyalJelly(cursor)) {
            return false;
        }
        int max = plugin.configManager().queenMaxCyclesCap();
        if (hive.queenCycles() >= max) {
            plugin.messages().send(player, "queen-full");
            return true;
        }
        int next = plugin.hives().feedQueen(hive);
        if (next < 0) {
            plugin.messages().send(player, "queen-no-queen");
            return true;
        }
        cursor.setAmount(Math.max(0, cursor.getAmount() - 1));
        plugin.messages().send(player, "queen-fed", "cycles", next, "max", max);
        reopen(player, hive);
        return true;
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
        plugin.messages().send(player, "frame-added", "frame", plugin.messages().frameName(frame.type()));
        reopen(player, hive);
    }

    private void handleUpgrade(Player player, HiveData hive) {
        switch (plugin.hives().upgrade(player, hive)) {
            case UPGRADED -> reopen(player, hive);
            case MAX_TIER -> plugin.messages().send(player, "hive-upgrade-max");
            case DISABLED -> plugin.messages().send(player, "hive-upgrade-disabled");
            case MISSING_ITEMS -> plugin.messages().send(player, "hive-upgrade-missing");
        }
    }

    private void handleBreeding(Player player, HiveData hive) {
        switch (plugin.hives().startBreeding(player, hive)) {
            case STARTED -> reopen(player, hive);
            case DISABLED -> plugin.messages().send(player, "breeding-disabled");
            case ALREADY -> plugin.messages().send(player, "breeding-already");
            case NEED_BEES -> plugin.messages().send(player, "breeding-need-bees");
            case NO_SPACE -> plugin.messages().send(player, "breeding-no-space");
            case NEED_CELL -> plugin.messages().send(player, "breeding-need-cell",
                    "amount", plugin.configManager().breedingRoyalCellCost());
        }
    }

    private void handleRitual(Player player, HiveData hive) {
        switch (plugin.queenBoss().startRitual(player, hive)) {
            case STARTED -> reopen(player, hive);
            case DISABLED -> plugin.messages().send(player, "ritual-disabled");
            case NEED_TIER -> plugin.messages().send(player, "ritual-need-tier");
            case NEED_QUEEN -> plugin.messages().send(player, "ritual-need-queen");
            case NEED_NIGHT -> plugin.messages().send(player, "ritual-need-night");
            case NEED_CANDLES -> plugin.messages().send(player, "ritual-need-candles",
                    "have", plugin.queenBoss().info(player, hive).candles(),
                    "need", plugin.configManager().ritualCandles());
            case NEED_ITEMS -> plugin.messages().send(player, "ritual-need-items",
                    "jelly", plugin.configManager().ritualRoyalJelly(),
                    "cells", plugin.configManager().ritualRoyalCells());
            case COOLDOWN -> plugin.messages().send(player, "ritual-cooldown");
            case ACTIVE -> plugin.messages().send(player, "ritual-active");
            case NOT_OWNER -> plugin.messages().send(player, "hive-not-owner");
        }
    }

    @EventHandler
    public void onClose(org.bukkit.event.inventory.InventoryCloseEvent event) {
        if (event.getPlayer() instanceof Player player) {
            HiveGui.cancel(player);
        }
    }

    private void reopen(Player player, HiveData hive) {
        plugin.schedulers().executeFor(player, () -> new HiveGui(plugin, hive).open(player));
    }
}
