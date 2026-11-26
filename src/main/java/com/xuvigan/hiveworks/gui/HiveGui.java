package com.xuvigan.hiveworks.gui;

import com.xuvigan.hiveworks.HiveWorksPlugin;
import com.xuvigan.hiveworks.bee.Species;
import com.xuvigan.hiveworks.hive.BeeSlot;
import com.xuvigan.hiveworks.hive.FrameSlot;
import com.xuvigan.hiveworks.hive.FrameType;
import com.xuvigan.hiveworks.hive.HiveData;
import com.xuvigan.hiveworks.hive.HiveMode;
import com.xuvigan.hiveworks.production.FlowerScanner;
import com.xuvigan.hiveworks.item.ItemFactory;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.List;
import java.util.Optional;

/**
 * 9x5 hive panel.
 *
 * <pre>
 * row 1: [info] [honey] [honey] [honey] [honey] [honey] [honey] [mode] [harvest]
 * row 2: [bee] [bee] [bee] [bee] [bee] [bee]
 * row 3: [frame] [frame] [frame] [frame]
 * row 4: [apiary] [pollination] [collection]
 * row 5: ... [close]
 * </pre>
 */
public final class HiveGui {

    public static final int SLOT_INFO = 0;
    public static final int SLOT_HONEY_START = 1;
    public static final int SLOT_MODE_INFO = 7;
    public static final int SLOT_HARVEST = 8;
    public static final int SLOT_BEE_START = 9;
    public static final int SLOT_FRAME_START = 18;
    public static final int SLOT_MODE_START = 27;
    public static final int SLOT_CLOSE = 44;
    public static final int SIZE = 45;

    private final HiveWorksPlugin plugin;
    private final HiveData hive;

    public HiveGui(HiveWorksPlugin plugin, HiveData hive) {
        this.plugin = plugin;
        this.hive = hive;
    }

    public void open(Player player) {
        GuiHolder holder = new GuiHolder(hive);
        Component title = plugin.messages().component("hive-gui-title", "tier", hive.tier().display());
        Inventory inventory = Bukkit.createInventory(holder, SIZE, title);
        holder.setInventory(inventory);

        ItemFactory items = plugin.itemFactory();
        ItemStack pane = items.pane(Material.GRAY_STAINED_GLASS_PANE);
        for (int slot = 0; slot < SIZE; slot++) {
            inventory.setItem(slot, pane);
        }

        inventory.setItem(SLOT_INFO, infoItem());
        fillHoney(inventory, items);
        inventory.setItem(SLOT_MODE_INFO, modeItem());
        inventory.setItem(SLOT_HARVEST, items.icon(
                Material.HONEYCOMB,
                plugin.messages().formatted("gui-harvest-name"),
                plugin.messages().formattedList("gui-harvest-lore"),
                0
        ));
        fillBees(inventory, items);
        fillFrames(inventory, items);
        fillModes(inventory, items);
        inventory.setItem(SLOT_CLOSE, items.icon(
                Material.BARRIER,
                plugin.messages().formatted("gui-close-name"),
                List.of(),
                0
        ));

        player.openInventory(inventory);
    }

    private ItemStack infoItem() {
        int flowers = 0;
        World world = Bukkit.getWorld(hive.pos().world());
        if (world != null) {
            Block block = world.getBlockAt(hive.pos().x(), hive.pos().y(), hive.pos().z());
            flowers = FlowerScanner.scan(block, 8).species();
        }
        String owner = Optional.ofNullable(Bukkit.getOfflinePlayer(hive.owner()).getName())
                .orElse(hive.owner().toString().substring(0, 8));
        List<String> lore = plugin.messages().formattedList("gui-info-lore",
                "owner", owner,
                "bees", hive.occupiedBees(),
                "slots", hive.tier().beeSlots(),
                "frames", hive.framesUsed(),
                "frame_slots", hive.tier().frameSlots(),
                "flowers", flowers,
                "honey", String.format("%.1f", hive.honey()),
                "max", hive.honeyMax()
        );
        return plugin.itemFactory().icon(
                Material.BEEHIVE,
                plugin.messages().formatted("gui-info-name", "tier", hive.tier().display()),
                lore,
                0
        );
    }

    private void fillHoney(Inventory inventory, ItemFactory items) {
        int max = Math.min(6, hive.honeyMax());
        for (int level = 1; level <= max; level++) {
            boolean filled = hive.honey() >= level;
            Material material = filled ? Material.HONEY_BOTTLE : Material.GLASS_BOTTLE;
            String name = plugin.messages().formatted("gui-honey-name", "level", level, "max", hive.honeyMax());
            inventory.setItem(SLOT_HONEY_START + level - 1, items.icon(material, name, List.of(), 0));
        }
    }

    private ItemStack modeItem() {
        HiveMode mode = hive.mode();
        String modeName = plugin.messages().raw(mode.messageKey());
        return plugin.itemFactory().icon(
                mode.icon(),
                plugin.messages().formatted("gui-mode-name", "mode", modeName),
                plugin.messages().formattedList("gui-mode-lore"),
                0
        );
    }

    private void fillBees(Inventory inventory, ItemFactory items) {
        for (int slot = 0; slot < hive.tier().beeSlots(); slot++) {
            BeeSlot bee = hive.bee(slot);
            int guiSlot = SLOT_BEE_START + slot;
            if (bee == null) {
                inventory.setItem(guiSlot, items.icon(
                        Material.GLASS_BOTTLE,
                        plugin.messages().formatted("gui-bee-empty"),
                        plugin.messages().formattedList("gui-bee-empty-lore"),
                        0
                ));
                continue;
            }
            Optional<Species> species = plugin.speciesRegistry().byId(bee.speciesId());
            if (species.isPresent()) {
                inventory.setItem(guiSlot, items.beeJar(bee.beeId(), species.get(), bee.genes(), bee.purity()));
            }
        }
    }

    private void fillFrames(Inventory inventory, ItemFactory items) {
        for (int slot = 0; slot < hive.tier().frameSlots(); slot++) {
            FrameSlot frame = hive.frame(slot);
            int guiSlot = SLOT_FRAME_START + slot;
            if (frame == null) {
                inventory.setItem(guiSlot, items.icon(
                        Material.STICK,
                        plugin.messages().formatted("gui-frame-empty"),
                        plugin.messages().formattedList("gui-frame-empty-lore"),
                        0
                ));
                continue;
            }
            FrameType type = frame.type();
            ItemStack item = items.icon(
                    Material.HONEYCOMB,
                    "<gold>" + type.displayName() + "</gold>",
                    List.of("<gray>Осталось сборов: <white>" + frame.usesLeft() + "</white>",
                            "<gray>Эффект: <white>" + type.effectDescription() + "</white>"),
                    0
            );
            inventory.setItem(guiSlot, item);
        }
    }

    private void fillModes(Inventory inventory, ItemFactory items) {
        List<HiveMode> modes = List.of(HiveMode.APIARY, HiveMode.POLLINATION, HiveMode.COLLECTION);
        for (int index = 0; index < modes.size(); index++) {
            HiveMode mode = modes.get(index);
            boolean selected = hive.mode() == mode;
            String name = (selected ? "<green>▶ </green>" : "") + plugin.messages().raw(mode.messageKey());
            inventory.setItem(SLOT_MODE_START + index, items.icon(
                    mode.icon(),
                    name,
                    plugin.messages().formattedList("gui-mode-lore"),
                    0
            ));
        }
    }
}
