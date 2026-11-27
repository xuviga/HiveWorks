package com.xuvigan.hiveworks.command;

import com.xuvigan.hiveworks.HiveWorksPlugin;
import com.xuvigan.hiveworks.apiary.Apiary;
import com.xuvigan.hiveworks.apiary.ApiaryService;
import com.xuvigan.hiveworks.bee.Species;
import com.xuvigan.hiveworks.gui.AtlasGui;
import com.xuvigan.hiveworks.gui.HiveGui;
import com.xuvigan.hiveworks.hive.FrameType;
import com.xuvigan.hiveworks.hive.HiveData;
import com.xuvigan.hiveworks.production.FlowerScanner;
import com.xuvigan.hiveworks.production.HoneyService;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

/**
 * /hive, /hw, /пасека.
 */
public final class HiveCommand implements CommandExecutor, TabCompleter {

    private final HiveWorksPlugin plugin;

    public HiveCommand(HiveWorksPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender,
                             @NotNull Command command,
                             @NotNull String label,
                             @NotNull String[] args) {
        if (args.length == 0) {
            if (sender instanceof Player player) {
                Optional<HiveData> nearest = plugin.hives().findNearest(player, 6.0);
                if (nearest.isPresent()) {
                    if (!plugin.hives().canManage(player, nearest.get())) {
                        plugin.messages().send(player, "hive-not-owner");
                    } else {
                        new HiveGui(plugin, nearest.get()).open(player);
                    }
                } else {
                    plugin.messages().send(player, "hive-not-found");
                    sendHelp(player);
                }
            } else {
                sendHelp(sender);
            }
            return true;
        }

        switch (args[0].toLowerCase(Locale.ROOT)) {
            case "guide", "help" -> sendHelp(sender);
            case "top" -> handleTop(sender);
            case "ritual" -> {
                if (sender instanceof Player player) {
                    plugin.schedulers().executeFor(player, () -> handleRitual(player));
                } else {
                    plugin.messages().send(sender, "player-only");
                }
            }
            case "atlas" -> {
                if (sender instanceof Player player) {
                    new AtlasGui(plugin, player).open();
                } else {
                    plugin.messages().send(sender, "player-only");
                }
            }
            case "pack" -> {
                if (sender instanceof Player player) {
                    if (plugin.packServer().send(player)) {
                        plugin.messages().send(player, "pack-sent");
                    } else {
                        plugin.messages().send(player, "pack-unavailable");
                    }
                } else {
                    plugin.messages().send(sender, "player-only");
                }
            }
            case "admin" -> handleAdmin(sender, args);
            case "apiary" -> {
                if (sender instanceof Player player) {
                    plugin.schedulers().executeFor(player, () -> handleApiary(player, args));
                } else {
                    plugin.messages().send(sender, "player-only");
                }
            }
            default -> plugin.messages().send(sender, "unknown-command");
        }
        return true;
    }

    private void sendHelp(CommandSender sender) {
        for (String line : plugin.messages().list("help")) {
            sender.sendMessage(plugin.messages().parse(line));
        }
    }

    private void handleTop(CommandSender sender) {
        List<com.xuvigan.hiveworks.storage.Storage.TopEntry> top = plugin.storage().topHarvesters(10);
        plugin.messages().send(sender, "top-header");
        if (top.isEmpty()) {
            plugin.messages().send(sender, "top-empty");
            return;
        }
        int rank = 1;
        for (com.xuvigan.hiveworks.storage.Storage.TopEntry entry : top) {
            plugin.messages().send(sender, "top-line",
                    "rank", rank++,
                    "player", entry.name(),
                    "honey", entry.honey(),
                    "harvests", entry.harvests(),
                    "legendary", entry.legendary());
        }
    }

    private void handleRitual(Player player) {
        Optional<HiveData> found = plugin.hives().findNearestOwned(player, 16.0);
        if (found.isEmpty()) {
            plugin.messages().send(player, "hive-not-found");
            return;
        }
        HiveData hive = found.get();
        switch (plugin.queenBoss().startRitual(player, hive)) {
            case STARTED -> {
                // startRitual already sent the countdown message
            }
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

    private void flowers(CommandSender sender) {
        if (!(sender instanceof Player player)) {
            plugin.messages().send(sender, "player-only");
            return;
        }
        Optional<HiveData> found = plugin.hives().findNearest(player, 8.0);
        if (found.isEmpty()) {
            plugin.messages().send(player, "hive-not-found");
            return;
        }
        HiveData hive = found.get();
        World world = Bukkit.getWorld(hive.pos().world());
        if (world == null) {
            return;
        }
        Block block = world.getBlockAt(hive.pos().x(), hive.pos().y(), hive.pos().z());
        FlowerScanner.Result result = FlowerScanner.scan(block, 8);
        boolean russian = "ru".equalsIgnoreCase(plugin.getConfig().getString("locale", "ru"));
        String names = result.names().stream()
                .map(name -> FlowerScanner.displayName(name, russian))
                .sorted()
                .collect(java.util.stream.Collectors.joining(", "));
        plugin.messages().send(player, "admin-flowers",
                "count", result.count(),
                "species", result.species(),
                "names", names);
    }

    private void debug(CommandSender sender) {
        List<HiveData> hives = new ArrayList<>();
        if (sender instanceof Player player) {
            plugin.hives().findNearest(player, 16.0).ifPresent(hives::add);
            if (hives.isEmpty()) {
                plugin.messages().send(player, "hive-not-found");
                return;
            }
        } else {
            hives.addAll(plugin.hives().all());
        }
        if (hives.isEmpty()) {
            sender.sendMessage("[HiveWorks] no hives loaded");
            return;
        }
        for (HiveData hive : hives) {
            World world = Bukkit.getWorld(hive.pos().world());
            boolean loaded = world != null
                    && world.isChunkLoaded(hive.pos().x() >> 4, hive.pos().z() >> 4);
            FlowerScanner.Result scan = FlowerScanner.Result.empty();
            boolean storm = false;
            boolean night = false;
            boolean snowy = false;
            String blockType = "?";
            if (world != null && loaded) {
                Block block = world.getBlockAt(hive.pos().x(), hive.pos().y(), hive.pos().z());
                blockType = block.getType().name();
                scan = FlowerScanner.scan(block, 8);
                storm = world.hasStorm();
                long time = world.getTime() % 24_000L;
                night = !(time < 12_300L || time > 23_850L);
                String biome = world.getBiome(hive.pos().x(), hive.pos().y(), hive.pos().z()).name();
                snowy = biome.contains("SNOW") || biome.contains("FROZEN") || biome.contains("ICE");
            }
            double speed = new HoneyService(plugin).speed(hive, scan, storm || night, snowy);
            double perMinute = speed / plugin.configManager().baseMinutesPerLevel();
            long age = Math.max(0L, (System.currentTimeMillis() - hive.lastTick()) / 1000L);
            sender.sendMessage(String.format(java.util.Locale.ROOT,
                    "[HiveWorks] %s %d,%d,%d | honey=%.3f/%d | bees=%d/%d queen=%d cycles=%d"
                            + " | frames=%d | mode=%s tier=%s | flowers=%d (%d sp.)"
                            + " | day=%s storm=%s snowy=%s | loaded=%s block=%s"
                            + " | speed=%.3f gain/min=%.4f | lastTick=%ds ago",
                    hive.pos().world(), hive.pos().x(), hive.pos().y(), hive.pos().z(),
                    hive.honey(), hive.honeyMax(),
                    hive.occupiedBees(), hive.tier().beeSlots(),
                    hive.queenSlot(), hive.queenCycles(),
                    hive.framesUsed(), hive.mode(), hive.tier(),
                    scan.count(), scan.species(),
                    !night, storm, snowy, loaded, blockType,
                    speed, perMinute, age));
        }
    }

    private void handleApiary(Player player, String[] args) {
        if (args.length < 2) {
            plugin.messages().send(player, "apiary-usage");
            return;
        }
        switch (args[1].toLowerCase(Locale.ROOT)) {
            case "create" -> {
                if (args.length < 3) {
                    plugin.messages().send(player, "apiary-usage");
                    return;
                }
                String name = args[2];
                ApiaryService.CreateResult result = plugin.apiaries().create(player, name);
                switch (result) {
                    case CREATED -> {
                        Optional<Apiary> created = plugin.apiaries().findFor(player);
                        int hives = created.map(apiary -> plugin.apiaries().memberCount(apiary)).orElse(0);
                        plugin.messages().send(player, "apiary-created",
                                "name", ApiaryService.sanitizeName(name),
                                "hives", hives);
                    }
                    case INVALID_NAME -> plugin.messages().send(player, "apiary-invalid-name");
                    case NEED_HIVES -> plugin.messages().send(player, "apiary-need-hives",
                            "min", plugin.configManager().apiaryMinHives(),
                            "radius", Math.round(plugin.configManager().apiaryRadius()));
                    case NEED_FLOWERS -> plugin.messages().send(player, "apiary-need-flowers",
                            "min", plugin.configManager().apiaryMinFlowers());
                    case NEED_FENCE -> plugin.messages().send(player, "apiary-need-fence",
                            "min", plugin.configManager().apiaryMinFences());
                    case ALREADY -> plugin.messages().send(player, "apiary-exists");
                }
            }
            case "info" -> {
                Optional<Apiary> found = plugin.apiaries().findFor(player);
                if (found.isEmpty()) {
                    plugin.messages().send(player, "apiary-not-found");
                    return;
                }
                Apiary apiary = found.get();
                plugin.messages().send(player, "apiary-info",
                        "name", apiary.name(),
                        "hives", plugin.apiaries().memberCount(apiary),
                        "speed", String.format(Locale.ROOT, "%.2f", plugin.configManager().apiarySpeedBonus()),
                        "quality", Math.round(plugin.configManager().apiaryQualityBonus() * 100.0));
            }
            case "disband" -> {
                if (plugin.apiaries().disband(player)) {
                    plugin.messages().send(player, "apiary-disbanded");
                } else {
                    plugin.messages().send(player, "apiary-not-found");
                }
            }
            default -> plugin.messages().send(player, "apiary-usage");
        }
    }

    private void handleAdmin(CommandSender sender, String[] args) {
        if (!sender.hasPermission("hiveworks.admin")) {
            plugin.messages().send(sender, "no-permission");
            return;
        }
        if (args.length < 2) {
            plugin.messages().send(sender, "admin-usage");
            return;
        }
        switch (args[1].toLowerCase(Locale.ROOT)) {
            case "reload" -> {
                plugin.reloadAll();
                plugin.messages().send(sender, "admin-reloaded");
            }
            case "save" -> {
                plugin.hives().saveAll();
                plugin.messages().send(sender, "admin-saved");
            }
            case "stats" -> plugin.messages().send(sender, "admin-stats",
                    "hives", plugin.hives().count(),
                    "species", plugin.speciesRegistry().size());
            case "flowers" -> dispatchWorld(sender, () -> flowers(sender));
            case "debug" -> dispatchWorld(sender, () -> debug(sender));
            case "boss" -> dispatchWorld(sender, () -> startBoss(sender));
            case "give" -> dispatchWorld(sender, () -> give(sender, args));
            default -> plugin.messages().send(sender, "admin-usage");
        }
    }

    /** Block-reading admin actions must run on the sender's region. */
    private void dispatchWorld(CommandSender sender, Runnable action) {
        if (sender instanceof Player player) {
            plugin.schedulers().executeFor(player, action);
        } else {
            plugin.messages().send(sender, "player-only");
        }
    }

    private void startBoss(CommandSender sender) {
        if (!(sender instanceof Player player)) {
            plugin.messages().send(sender, "player-only");
            return;
        }
        Optional<HiveData> nearest = plugin.hives().findNearest(player, 16.0);
        if (nearest.isEmpty()) {
            plugin.messages().send(player, "boss-no-hive");
            return;
        }
        plugin.queenBoss().forceStart(player, nearest.get());
    }

    private void give(CommandSender sender, String[] args) {
        if (args.length < 3) {
            plugin.messages().send(sender, "admin-give-usage");
            return;
        }
        if (!(sender instanceof Player player)) {
            plugin.messages().send(sender, "player-only");
            return;
        }
        String kind = args[2].toLowerCase(Locale.ROOT);
        if (kind.equals("frame")) {
            giveFrame(player, args);
            return;
        }
        if (kind.equals("bee")) {
            if (args.length < 4) {
                plugin.messages().send(sender, "admin-give-usage");
                return;
            }
            giveBee(player, args[3], args.length >= 5 ? args[4] : null);
            return;
        }
        if (kind.equals("queen")) {
            if (args.length < 4) {
                plugin.messages().send(sender, "admin-give-usage");
                return;
            }
            giveQueen(player, args[3], args.length >= 5 ? args[4] : null);
            return;
        }
        giveBee(player, args[2], args.length >= 4 ? args[3] : null);
    }

    private void giveBee(Player player, String speciesId, String amountRaw) {
        Optional<Species> species = plugin.speciesRegistry().byId(speciesId.toLowerCase(Locale.ROOT));
        if (species.isEmpty()) {
            plugin.messages().send(player, "unknown-species", "species", speciesId);
            return;
        }
        int amount = parseAmount(amountRaw);
        for (int index = 0; index < amount; index++) {
            giveItem(player, plugin.itemFactory().beeJar(
                    species.get(),
                    species.get().baseGenes(),
                    species.get().basePurity()
            ));
        }
        plugin.messages().send(player, "admin-gave",
                "amount", amount,
                "species", plugin.messages().speciesName(species.get()));
    }

    private void giveQueen(Player player, String speciesId, String amountRaw) {
        Optional<Species> species = plugin.speciesRegistry().byId(speciesId.toLowerCase(Locale.ROOT));
        if (species.isEmpty()) {
            plugin.messages().send(player, "unknown-species", "species", speciesId);
            return;
        }
        int amount = parseAmount(amountRaw);
        for (int index = 0; index < amount; index++) {
            giveItem(player, plugin.itemFactory().queenItem(
                    species.get(),
                    species.get().baseGenes(),
                    species.get().basePurity()
            ));
        }
        plugin.messages().send(player, "admin-gave-queen",
                "amount", amount,
                "species", plugin.messages().speciesName(species.get()));
    }

    private void giveFrame(Player player, String[] args) {
        if (args.length < 4) {
            plugin.messages().send(player, "admin-frame-usage");
            return;
        }
        FrameType type;
        try {
            type = FrameType.valueOf(args[3].toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException exception) {
            plugin.messages().send(player, "unknown-frame", "frame", args[3]);
            return;
        }
        int amount = parseAmount(args.length >= 5 ? args[4] : null);
        ItemStack frame = plugin.itemFactory().frame(type, type.maxUses());
        frame.setAmount(amount);
        giveItem(player, frame);
        plugin.messages().send(player, "admin-gave-frame",
                "amount", amount,
                "frame", plugin.messages().frameName(type));
    }

    private int parseAmount(String raw) {
        if (raw == null) {
            return 1;
        }
        try {
            return Math.max(1, Math.min(64, Integer.parseInt(raw)));
        } catch (NumberFormatException exception) {
            return 1;
        }
    }

    private void giveItem(Player player, ItemStack item) {
        player.getInventory().addItem(item).values()
                .forEach(drop -> player.getWorld().dropItemNaturally(player.getLocation(), drop));
    }

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender,
                                      @NotNull Command command,
                                      @NotNull String alias,
                                      @NotNull String[] args) {
        List<String> options = new ArrayList<>();
        if (args.length == 1) {
            options.add("guide");
            options.add("atlas");
            options.add("top");
            options.add("ritual");
            options.add("pack");
            options.add("apiary");
            if (sender.hasPermission("hiveworks.admin")) {
                options.add("admin");
            }
        } else if (args.length == 2 && args[0].equalsIgnoreCase("admin")) {
            options.addAll(List.of("reload", "save", "stats", "give", "flowers", "debug", "boss"));
        } else if (args.length == 2 && args[0].equalsIgnoreCase("apiary")) {
            options.addAll(List.of("create", "info", "disband"));
        } else if (args.length == 3 && args[0].equalsIgnoreCase("admin") && args[1].equalsIgnoreCase("give")) {
            options.add("bee");
            options.add("queen");
            options.add("frame");
            for (Species species : plugin.speciesRegistry().all()) {
                options.add(species.id());
            }
        } else if (args.length == 4 && args[0].equalsIgnoreCase("admin") && args[1].equalsIgnoreCase("give")) {
            if (args[2].equalsIgnoreCase("frame")) {
                for (FrameType type : FrameType.values()) {
                    options.add(type.name().toLowerCase(Locale.ROOT));
                }
            } else {
                for (Species species : plugin.speciesRegistry().all()) {
                    options.add(species.id());
                }
            }
        } else if (args.length == 5 && args[0].equalsIgnoreCase("admin") && args[1].equalsIgnoreCase("give")) {
            options.addAll(List.of("1", "4", "8", "16"));
        }

        String current = args.length == 0 ? "" : args[args.length - 1].toLowerCase(Locale.ROOT);
        return options.stream()
                .filter(option -> option.toLowerCase(Locale.ROOT).startsWith(current))
                .sorted()
                .toList();
    }
}
