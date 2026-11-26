package com.xuvigan.hiveworks.command;

import com.xuvigan.hiveworks.HiveWorksPlugin;
import com.xuvigan.hiveworks.apiary.Apiary;
import com.xuvigan.hiveworks.apiary.ApiaryService;
import com.xuvigan.hiveworks.bee.Species;
import com.xuvigan.hiveworks.gui.AtlasGui;
import com.xuvigan.hiveworks.gui.HiveGui;
import com.xuvigan.hiveworks.hive.FrameType;
import com.xuvigan.hiveworks.hive.HiveData;
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
                    new HiveGui(plugin, nearest.get()).open(player);
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
            case "apiary" -> handleApiary(sender, args);
            default -> plugin.messages().send(sender, "unknown-command");
        }
        return true;
    }

    private void sendHelp(CommandSender sender) {
        for (String line : plugin.messages().list("help")) {
            sender.sendMessage(plugin.messages().parse(line));
        }
    }

    private void handleApiary(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            plugin.messages().send(sender, "player-only");
            return;
        }
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
            case "give" -> give(sender, args);
            default -> plugin.messages().send(sender, "admin-usage");
        }
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
                "species", species.get().displayName());
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
                "frame", type.displayName());
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
            options.add("pack");
            options.add("apiary");
            if (sender.hasPermission("hiveworks.admin")) {
                options.add("admin");
            }
        } else if (args.length == 2 && args[0].equalsIgnoreCase("admin")) {
            options.addAll(List.of("reload", "save", "stats", "give"));
        } else if (args.length == 2 && args[0].equalsIgnoreCase("apiary")) {
            options.addAll(List.of("create", "info", "disband"));
        } else if (args.length == 3 && args[0].equalsIgnoreCase("admin") && args[1].equalsIgnoreCase("give")) {
            options.add("bee");
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
