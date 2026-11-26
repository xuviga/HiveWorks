package com.xuvigan.hiveworks.command;

import com.xuvigan.hiveworks.HiveWorksPlugin;
import com.xuvigan.hiveworks.bee.Species;
import com.xuvigan.hiveworks.gui.HiveGui;
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
            case "atlas" -> plugin.messages().send(sender, "atlas-soon");
            case "admin" -> handleAdmin(sender, args);
            default -> plugin.messages().send(sender, "unknown-command");
        }
        return true;
    }

    private void sendHelp(CommandSender sender) {
        for (String line : plugin.messages().list("help")) {
            sender.sendMessage(plugin.messages().parse(line));
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
            case "give" -> giveBee(sender, args);
            default -> plugin.messages().send(sender, "admin-usage");
        }
    }

    private void giveBee(CommandSender sender, String[] args) {
        if (args.length < 3) {
            plugin.messages().send(sender, "admin-give-usage");
            return;
        }
        if (!(sender instanceof Player player)) {
            plugin.messages().send(sender, "player-only");
            return;
        }
        Optional<Species> species = plugin.speciesRegistry().byId(args[2].toLowerCase(Locale.ROOT));
        if (species.isEmpty()) {
            plugin.messages().send(sender, "unknown-species", "species", args[2]);
            return;
        }
        int amount = 1;
        if (args.length >= 4) {
            try {
                amount = Math.max(1, Math.min(64, Integer.parseInt(args[3])));
            } catch (NumberFormatException ignored) {
                amount = 1;
            }
        }
        ItemStack bee = plugin.itemFactory().beeJar(
                species.get(),
                species.get().baseGenes(),
                species.get().basePurity()
        );
        bee.setAmount(amount);
        player.getInventory().addItem(bee).values()
                .forEach(drop -> player.getWorld().dropItemNaturally(player.getLocation(), drop));
        plugin.messages().send(sender, "admin-gave",
                "amount", amount,
                "species", species.get().displayName());
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
            if (sender.hasPermission("hiveworks.admin")) {
                options.add("admin");
            }
        } else if (args.length == 2 && args[0].equalsIgnoreCase("admin")) {
            options.addAll(List.of("reload", "save", "stats", "give"));
        } else if (args.length == 3 && args[0].equalsIgnoreCase("admin") && args[1].equalsIgnoreCase("give")) {
            for (Species species : plugin.speciesRegistry().all()) {
                options.add(species.id());
            }
        } else if (args.length == 4 && args[0].equalsIgnoreCase("admin") && args[1].equalsIgnoreCase("give")) {
            options.addAll(List.of("1", "8", "16", "32"));
        }

        String current = args.length == 0 ? "" : args[args.length - 1].toLowerCase(Locale.ROOT);
        return options.stream()
                .filter(option -> option.toLowerCase(Locale.ROOT).startsWith(current))
                .sorted()
                .toList();
    }
}
