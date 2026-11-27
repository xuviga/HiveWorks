package com.xuvigan.hiveworks.gui;

import com.xuvigan.hiveworks.HiveWorksPlugin;
import com.xuvigan.hiveworks.apiary.Apiary;
import com.xuvigan.hiveworks.boss.QueenBossService;
import com.xuvigan.hiveworks.bee.Species;
import com.xuvigan.hiveworks.hive.BeeSlot;
import com.xuvigan.hiveworks.hive.FrameSlot;
import com.xuvigan.hiveworks.hive.FrameType;
import com.xuvigan.hiveworks.hive.HiveData;
import com.xuvigan.hiveworks.hive.HiveManager;
import com.xuvigan.hiveworks.hive.HiveMode;
import com.xuvigan.hiveworks.hive.HiveTier;
import com.xuvigan.hiveworks.production.FlowerScanner;
import com.xuvigan.hiveworks.production.HoneyService;
import com.xuvigan.hiveworks.item.ItemFactory;
import com.xuvigan.hiveworks.util.SchedulerAdapter;
import io.papermc.paper.threadedregions.scheduler.ScheduledTask;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

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
    public static final int SLOT_BEE_START = 9;
    public static final int SLOT_QUEEN = 15;
    public static final int SLOT_APIARY = 16;
    public static final int SLOT_HARVEST = 17;
    public static final int SLOT_FRAME_START = 18;
    public static final int SLOT_UPGRADE = 22;
    public static final int SLOT_BREEDING = 23;
    public static final int SLOT_RITUAL = 24;
    public static final int SLOT_MODE_START = 27;
    public static final int SLOT_CLOSE = 44;
    public static final int SIZE = 45;

    private static final Map<UUID, ScheduledTask> TASKS = new ConcurrentHashMap<>();

    private final HiveWorksPlugin plugin;
    private final HiveData hive;
    private FlowerScanner.Result cachedScan = FlowerScanner.Result.empty();
    private long cachedScanAt;
    private QueenBossService.RitualInfo cachedRitualInfo;
    private long cachedRitualAt;

    public HiveGui(HiveWorksPlugin plugin, HiveData hive) {
        this.plugin = plugin;
        this.hive = hive;
    }

    public void open(Player player) {
        if (!SchedulerAdapter.ownedNow(player)) {
            plugin.schedulers().executeFor(player, () -> open(player));
            return;
        }
        cancel(player);
        GuiHolder holder = new GuiHolder(hive);
        Component title = plugin.messages().component("hive-gui-title",
                "tier", plugin.messages().tierName(hive.tier()));
        Inventory inventory = Bukkit.createInventory(holder, SIZE, title);
        holder.setInventory(inventory);

        ItemStack pane = plugin.itemFactory().pane(Material.GRAY_STAINED_GLASS_PANE);
        for (int slot = 0; slot < SIZE; slot++) {
            inventory.setItem(slot, pane);
        }
        render(player, inventory);
        player.openInventory(inventory);

        ScheduledTask task = plugin.schedulers().runForTimer(player, () -> {
            if (!player.isOnline()
                    || !(player.getOpenInventory().getTopInventory().getHolder() instanceof GuiHolder current)
                    || current.hive() != hive) {
                cancel(player);
                return;
            }
            render(player, inventory);
        }, 20L, 20L);
        if (task != null) {
            TASKS.put(player.getUniqueId(), task);
        }
    }

    /** Stops the 1-second refresh task for a player. */
    public static void cancel(Player player) {
        ScheduledTask task = TASKS.remove(player.getUniqueId());
        if (task != null) {
            task.cancel();
        }
    }

    private void render(Player player, Inventory inventory) {
        ItemFactory items = plugin.itemFactory();
        inventory.setItem(SLOT_INFO, infoItem());
        fillHoney(inventory, items);
        inventory.setItem(SLOT_HARVEST, items.icon(
                Material.HONEYCOMB,
                plugin.messages().formatted("gui-harvest-name"),
                plugin.messages().formattedList("gui-harvest-lore"),
                0
        ));
        fillBees(inventory, items);
        fillQueen(inventory, items);
        fillApiary(inventory, items);
        fillFrames(inventory, items);
        fillUpgrade(inventory, items);
        fillBreeding(inventory, items);
        fillRitual(player, inventory, items);
        fillModes(inventory, items);
        inventory.setItem(SLOT_CLOSE, items.icon(
                Material.BARRIER,
                plugin.messages().formatted("gui-close-name"),
                List.of(),
                0
        ));
    }

    private ItemStack infoItem() {
        FlowerScanner.Result scan;
        boolean storm;
        boolean night;
        boolean snowy;
        World world = Bukkit.getWorld(hive.pos().world());
        long now = System.currentTimeMillis();
        if (world != null && SchedulerAdapter.ownedNow(world, hive.pos().x(), hive.pos().z())) {
            Block block = world.getBlockAt(hive.pos().x(), hive.pos().y(), hive.pos().z());
            if (now - cachedScanAt > 30_000L) {
                cachedScan = FlowerScanner.scan(block, 8);
                cachedScanAt = now;
            }
            scan = cachedScan;
            storm = world.hasStorm();
            night = !isDay(world);
            snowy = isSnowy(world.getBiome(hive.pos().x(), hive.pos().y(), hive.pos().z()).name());
            hive.setEnvironment(storm, night, snowy, now);
        } else {
            scan = new FlowerScanner.Result(hive.cachedFlowerSpecies(), 0, hive.cachedFlowerRare(),
                    java.util.Collections.emptySet());
            storm = hive.cachedStorm();
            night = hive.cachedNight();
            snowy = hive.cachedSnowy();
        }

        double speed = new HoneyService(plugin).speed(hive, scan, storm || night, snowy);
        double perMinute = speed / plugin.configManager().baseMinutesPerLevel();
        int max = hive.honeyMax();
        double honey = hive.honey();
        double nextLevel = Math.min(max, Math.floor(honey) + 1.0);
        double remaining = Math.max(0.0, nextLevel - honey);
        String eta = honey >= max ? plugin.messages().raw("gui-eta-full")
                : (perMinute <= 0.0 ? plugin.messages().raw("gui-eta-stopped")
                : plugin.messages().formatted("gui-eta-next", "time", formatDuration(remaining / perMinute)));
        String phaseKey = storm ? "phase-rain" : (night ? "phase-night" : "phase-day");
        String phase = plugin.messages().raw(phaseKey);

        String owner = Optional.ofNullable(Bukkit.getOfflinePlayer(hive.owner()).getName())
                .orElse(hive.owner().toString().substring(0, 8));
        List<String> lore = plugin.messages().formattedList("gui-info-lore",
                "owner", owner,
                "bees", hive.occupiedBees(),
                "slots", hive.tier().beeSlots(),
                "frames", hive.framesUsed(),
                "frame_slots", hive.tier().frameSlots(),
                "flowers", scan.count(),
                "flower_species", scan.species(),
                "honey", String.format(Locale.ROOT, "%.2f", honey),
                "max", max,
                "percent", Math.round(honey / max * 100),
                "speed", String.format(Locale.ROOT, "%.2f", speed),
                "eta", eta,
                "phase", phase,
                "mode", plugin.messages().raw(hive.mode().messageKey()),
                "night", String.format(Locale.ROOT, "%.2f", plugin.configManager().nightRainMultiplier())
        );
        return plugin.itemFactory().icon(
                Material.BOOK,
                plugin.messages().formatted("gui-info-name",
                        "tier", plugin.messages().tierName(hive.tier())),
                lore,
                0
        );
    }

    private boolean isDay(World world) {
        long time = world.getTime() % 24_000L;
        return time < 12_300L || time > 23_850L;
    }

    private boolean isSnowy(String biomeName) {
        return biomeName.contains("SNOW") || biomeName.contains("FROZEN") || biomeName.contains("ICE");
    }

    private String formatDuration(double minutes) {
        if (minutes < 1.0) {
            return plugin.messages().raw("gui-eta-soon");
        }
        int totalMinutes = Math.max(1, (int) Math.ceil(minutes));
        if (totalMinutes < 60) {
            return totalMinutes + " " + plugin.messages().raw("unit-minutes");
        }
        int hours = totalMinutes / 60;
        int rest = totalMinutes % 60;
        if (rest == 0) {
            return hours + " " + plugin.messages().raw("unit-hours");
        }
        return hours + " " + plugin.messages().raw("unit-hours") + " " + rest + " " + plugin.messages().raw("unit-minutes");
    }

    private void fillHoney(Inventory inventory, ItemFactory items) {
        int max = Math.min(8, hive.honeyMax());
        double honey = hive.honey();
        for (int level = 1; level <= max; level++) {
            boolean filled = honey >= level;
            Material material = filled ? Material.HONEY_BOTTLE : Material.GLASS_BOTTLE;
            String name = plugin.messages().formatted("gui-honey-name", "level", level, "max", hive.honeyMax());
            String state;
            if (filled) {
                state = plugin.messages().formatted("gui-honey-filled");
            } else if (level == (int) Math.floor(honey) + 1) {
                int progress = (int) Math.round((honey - (level - 1)) * 100.0);
                state = plugin.messages().formatted("gui-honey-progress", "percent", Math.max(0, Math.min(100, progress)));
            } else {
                state = plugin.messages().formatted("gui-honey-empty");
            }
            inventory.setItem(SLOT_HONEY_START + level - 1, items.icon(material, name, List.of(state), 0));
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
                        Material.HONEYCOMB,
                        plugin.messages().formatted("gui-bee-empty"),
                        plugin.messages().formattedList("gui-bee-empty-lore"),
                        0
                ));
                continue;
            }
            Optional<Species> species = plugin.speciesRegistry().byId(bee.speciesId());
            if (species.isPresent()) {
                inventory.setItem(guiSlot, items.beeJar(
                        bee.beeId(),
                        species.get(),
                        bee.genes(),
                        bee.purity(),
                        slot == hive.queenSlot()
                ));
            }
        }
    }

    private void fillQueen(Inventory inventory, ItemFactory items) {
        BeeSlot queen = hive.queen();
        if (queen == null) {
            inventory.setItem(SLOT_QUEEN, items.icon(
                    Material.HONEYCOMB,
                    plugin.messages().formatted("queen-empty-name"),
                    plugin.messages().formattedList("queen-empty-lore"),
                    0
            ));
            return;
        }
        Optional<Species> species = plugin.speciesRegistry().byId(queen.speciesId());
        if (species.isEmpty()) {
            return;
        }
        inventory.setItem(SLOT_QUEEN, items.icon(
                Material.HONEYCOMB,
                plugin.messages().formatted("queen-name"),
                plugin.messages().formattedList("queen-lore",
                        "species", plugin.messages().speciesName(species.get()),
                        "cycles", hive.queenCycles(),
                        "max", plugin.configManager().queenMaxCyclesCap(),
                        "per_feed", plugin.configManager().queenRoyalJellyCycles()),
                0
        ));
    }

    private void fillApiary(Inventory inventory, ItemFactory items) {
        Optional<Apiary> found = plugin.apiaries().apiaryFor(hive);
        if (found.isEmpty()) {
            inventory.setItem(SLOT_APIARY, items.icon(
                    Material.OAK_FENCE,
                    plugin.messages().formatted("apiary-empty-name"),
                    plugin.messages().formattedList("apiary-empty-lore"),
                    0
            ));
            return;
        }
        Apiary apiary = found.get();
        int hives = plugin.apiaries().memberCount(apiary);
        String speed = String.format(Locale.ROOT, "%.2f", plugin.configManager().apiarySpeedBonus());
        int quality = (int) Math.round(plugin.configManager().apiaryQualityBonus() * 100.0);
        inventory.setItem(SLOT_APIARY, items.icon(
                Material.OAK_FENCE,
                plugin.messages().formatted("apiary-name", "name", apiary.name()),
                plugin.messages().formattedList("apiary-lore",
                        "hives", hives,
                        "speed", speed,
                        "quality", quality),
                0
        ));
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
                    plugin.messages().frameName(type),
                    plugin.messages().formattedList("item-frame-lore",
                            "uses", frame.usesLeft(),
                            "max", type.maxUses(),
                            "effect", plugin.messages().frameEffect(type)),
                    0
            );
            inventory.setItem(guiSlot, item);
        }
    }

    private void fillUpgrade(Inventory inventory, ItemFactory items) {
        if (!plugin.configManager().upgradeEnabled()) {
            inventory.setItem(SLOT_UPGRADE, items.icon(
                    Material.BARRIER,
                    plugin.messages().formatted("hive-upgrade-max-name"),
                    List.of(plugin.messages().formatted("hive-upgrade-disabled")),
                    0
            ));
            return;
        }
        HiveTier next = hive.tier().next();
        HiveManager.UpgradeCost cost = plugin.hives().upgradeCost(hive.tier());
        if (next == null || cost == null) {
            inventory.setItem(SLOT_UPGRADE, items.icon(
                    Material.NETHERITE_INGOT,
                    plugin.messages().formatted("hive-upgrade-max-name"),
                    plugin.messages().formattedList("hive-upgrade-max-lore"),
                    0
            ));
            return;
        }
        String material = cost.material().name().toLowerCase(Locale.ROOT).replace('_', ' ');
        inventory.setItem(SLOT_UPGRADE, items.icon(
                Material.ANVIL,
                plugin.messages().formatted("hive-upgrade-name"),
                plugin.messages().formattedList("hive-upgrade-lore",
                        "tier", plugin.messages().tierName(next),
                        "product", plugin.messages().productName(cost.product()),
                        "product_amount", cost.productAmount(),
                        "material", material,
                        "material_amount", cost.materialAmount()),
                0
        ));
    }

    private void fillBreeding(Inventory inventory, ItemFactory items) {
        if (!plugin.configManager().breedingEnabled()) {
            inventory.setItem(SLOT_BREEDING, items.icon(
                    Material.BARRIER,
                    plugin.messages().formatted("breeding-name"),
                    List.of(plugin.messages().formatted("breeding-disabled")),
                    0
            ));
            return;
        }
        long now = System.currentTimeMillis();
        if (hive.breedingActive(now)) {
            long remaining = Math.max(0L, hive.breedReadyAt() - now);
            inventory.setItem(SLOT_BREEDING, items.icon(
                    Material.TURTLE_EGG,
                    plugin.messages().formatted("breeding-name"),
                    plugin.messages().formattedList("breeding-progress-lore",
                            "first", hive.breedSlotA() + 1,
                            "second", hive.breedSlotB() + 1,
                            "time", formatDuration(remaining / 60_000.0)),
                    0
            ));
            return;
        }
        inventory.setItem(SLOT_BREEDING, items.icon(
                Material.TURTLE_EGG,
                plugin.messages().formatted("breeding-name"),
                plugin.messages().formattedList("breeding-start-lore",
                        "cells", plugin.configManager().breedingRoyalCellCost(),
                        "minutes", plugin.configManager().breedingMinutes()),
                0
        ));
    }

    private void fillRitual(Player player, Inventory inventory, ItemFactory items) {
        long now = System.currentTimeMillis();
        if (cachedRitualInfo == null || now - cachedRitualAt > 5_000L) {
            cachedRitualInfo = plugin.queenBoss().info(player, hive);
            cachedRitualAt = now;
        }
        QueenBossService.RitualInfo info = cachedRitualInfo;
        String name = plugin.messages().formatted("ritual-name");
        List<String> lore;
        Material material;
        if (!info.enabled()) {
            material = Material.BARRIER;
            lore = List.of(plugin.messages().formatted("ritual-disabled"));
        } else if (!info.tierOk()) {
            material = Material.BARRIER;
            lore = plugin.messages().formattedList("ritual-lore-tier");
        } else if (!info.hasQueen()) {
            material = Material.BARRIER;
            lore = plugin.messages().formattedList("ritual-lore-queen");
        } else if (!info.night()) {
            material = Material.CLOCK;
            lore = plugin.messages().formattedList("ritual-lore-night");
        } else if (info.candles() < info.needCandles()) {
            material = Material.CANDLE;
            lore = plugin.messages().formattedList("ritual-lore-candles",
                    "have", info.candles(),
                    "need", info.needCandles());
        } else if (!info.hasItems()) {
            material = Material.HONEYCOMB;
            lore = plugin.messages().formattedList("ritual-lore-items",
                    "jelly", plugin.configManager().ritualRoyalJelly(),
                    "cells", plugin.configManager().ritualRoyalCells());
        } else if (info.ritualActive() || info.bossActive()) {
            material = Material.CLOCK;
            lore = plugin.messages().formattedList("ritual-lore-active");
        } else if (info.cooldownMs() > 0L) {
            material = Material.CLOCK;
            lore = plugin.messages().formattedList("ritual-lore-cooldown");
        } else {
            material = Material.NETHER_STAR;
            lore = plugin.messages().formattedList("ritual-lore-ready",
                    "candles", info.candles(),
                    "jelly", plugin.configManager().ritualRoyalJelly(),
                    "cells", plugin.configManager().ritualRoyalCells());
        }
        inventory.setItem(SLOT_RITUAL, items.icon(material, name, lore, 0));
    }

    private void fillModes(Inventory inventory, ItemFactory items) {
        List<HiveMode> modes = new ArrayList<>(List.of(
                HiveMode.APIARY,
                HiveMode.POLLINATION,
                HiveMode.COLLECTION
        ));
        if (plugin.configManager().guardEnabled()) {
            modes.add(HiveMode.GUARD);
        }
        for (int index = 0; index < modes.size(); index++) {
            HiveMode mode = modes.get(index);
            boolean selected = hive.mode() == mode;
            String name = (selected ? "<green>▶ </green>" : "") + plugin.messages().raw(mode.messageKey());
            inventory.setItem(SLOT_MODE_START + index, items.icon(
                    mode.icon(),
                    name,
                    plugin.messages().formattedList(mode.messageKey() + "-lore"),
                    0
            ));
        }
    }
}
