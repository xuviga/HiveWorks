package com.xuvigan.hiveworks.gui;

import com.xuvigan.hiveworks.HiveWorksPlugin;
import com.xuvigan.hiveworks.atlas.AtlasReward;
import com.xuvigan.hiveworks.bee.Species;
import com.xuvigan.hiveworks.item.ItemFactory;
import com.xuvigan.hiveworks.util.SchedulerAdapter;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * 6x6 atlas: discovered species, progress and a close button.
 */
public final class AtlasGui {

    public static final int SIZE = 54;
    public static final int SLOT_PROGRESS = 49;
    public static final int SLOT_CLOSE = 53;
    public static final int[] REWARD_SLOTS = {45, 46, 47, 48};
    private static final int[] SLOTS = {
            10, 11, 12, 13, 14, 15, 16,
            19, 20, 21, 22, 23, 24, 25,
            28, 29, 30, 31, 32, 33, 34
    };

    private final HiveWorksPlugin plugin;
    private final Player player;

    public AtlasGui(HiveWorksPlugin plugin, Player player) {
        this.plugin = plugin;
        this.player = player;
    }

    public void open() {
        if (!SchedulerAdapter.ownedNow(player)) {
            plugin.schedulers().executeFor(player, this::open);
            return;
        }
        AtlasHolder holder = new AtlasHolder();
        Inventory inventory = Bukkit.createInventory(holder, SIZE, plugin.messages().component("atlas-title"));
        holder.setInventory(inventory);

        ItemFactory items = plugin.itemFactory();
        ItemStack pane = items.pane(Material.GRAY_STAINED_GLASS_PANE);
        for (int slot = 0; slot < SIZE; slot++) {
            inventory.setItem(slot, pane);
        }

        List<Species> species = new ArrayList<>(plugin.speciesRegistry().all());
        int discovered = plugin.atlas().count(player.getUniqueId());
        for (int index = 0; index < Math.min(species.size(), SLOTS.length); index++) {
            Species entry = species.get(index);
            if (plugin.atlas().isDiscovered(player.getUniqueId(), entry.id())) {
                inventory.setItem(SLOTS[index], items.beeJar(atlasId(entry.id()), entry, entry.baseGenes(), entry.basePurity()));
            } else {
                String biome = entry.biomes().isEmpty() ? "—" : entry.biomes().iterator().next();
                inventory.setItem(SLOTS[index], items.icon(
                        Material.GRAY_STAINED_GLASS_PANE,
                        plugin.messages().formatted("atlas-unknown-name"),
                        plugin.messages().formattedList("atlas-unknown-lore", "biome", biome),
                        0
                ));
            }
        }

        List<AtlasReward> rewards = plugin.atlasRewards().rewards();
        Set<Integer> claimed = plugin.atlas().claimed(player.getUniqueId());
        for (int index = 0; index < Math.min(rewards.size(), REWARD_SLOTS.length); index++) {
            AtlasReward reward = rewards.get(index);
            boolean isClaimed = claimed.contains(reward.threshold());
            boolean available = discovered >= reward.threshold();
            String stateKey = isClaimed ? "atlas-reward-claimed"
                    : (available ? "atlas-reward-available" : "atlas-reward-locked");
            String state = plugin.messages().formatted(stateKey, "threshold", reward.threshold());
            Material material = isClaimed ? Material.LIME_DYE
                    : (available ? Material.CHEST : Material.GRAY_DYE);
            inventory.setItem(REWARD_SLOTS[index], items.icon(
                    material,
                    plugin.messages().formatted("atlas-reward-name", "threshold", reward.threshold()),
                    plugin.messages().formattedList("atlas-reward-lore", "state", state),
                    0
            ));
        }

        inventory.setItem(SLOT_PROGRESS, items.icon(
                Material.BOOK,
                plugin.messages().formatted("atlas-progress",
                        "found", discovered,
                        "total", species.size()),
                List.of(),
                0
        ));
        inventory.setItem(SLOT_CLOSE, items.icon(
                Material.BARRIER,
                plugin.messages().formatted("atlas-close"),
                List.of(),
                0
        ));
        player.openInventory(inventory);
    }

    private static UUID atlasId(String speciesId) {
        return UUID.nameUUIDFromBytes(("hiveworks:atlas:" + speciesId).getBytes(StandardCharsets.UTF_8));
    }
}
