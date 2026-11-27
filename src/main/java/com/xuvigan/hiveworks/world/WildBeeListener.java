package com.xuvigan.hiveworks.world;

import com.xuvigan.hiveworks.HiveWorksPlugin;
import com.xuvigan.hiveworks.bee.Genes;
import com.xuvigan.hiveworks.bee.Species;
import org.bukkit.entity.Bee;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.Random;

/**
 * Catches wild bees with the net and turns them into "bee in a jar" items.
 */
public final class WildBeeListener implements Listener {

    private final HiveWorksPlugin plugin;
    private final Random random = new Random();

    public WildBeeListener(HiveWorksPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(ignoreCancelled = true)
    public void onCatch(PlayerInteractEntityEvent event) {
        if (event.getHand() != EquipmentSlot.HAND) {
            return;
        }
        if (!(event.getRightClicked() instanceof Bee bee) || !WildSwarmSpawner.isWild(bee)) {
            return;
        }
        Player player = event.getPlayer();
        ItemStack net = player.getInventory().getItemInMainHand();
        if (!plugin.netItem().isNet(net)) {
            return;
        }
        event.setCancelled(true);
        if (!player.hasPermission("hiveworks.use")) {
            plugin.messages().send(player, "no-permission");
            return;
        }

        String biome = bee.getLocation().getBlock().getBiome().name();
        Species species = plugin.speciesRegistry().byBiome(biome)
                .orElseGet(() -> plugin.speciesRegistry().random(random));
        Genes genes = jitter(species.baseGenes());
        int purity = Math.max(0, Math.min(100, species.basePurity() + random.nextInt(11) - 5));

        ItemStack jar = plugin.itemFactory().beeJar(species, genes, purity);
        player.getInventory().addItem(jar).values()
                .forEach(drop -> player.getWorld().dropItemNaturally(player.getLocation(), drop));
        bee.remove();
        plugin.atlas().discover(player, species.id());
        damageNet(player, net);
        plugin.messages().send(player, "bee-caught", "species", plugin.messages().speciesName(species));
    }

    private Genes jitter(Genes base) {
        return new Genes(
                clamp(base.productivity() + random.nextInt(21) - 10),
                clamp(base.speed() + random.nextInt(21) - 10),
                clamp(base.cold() + random.nextInt(21) - 10),
                clamp(base.temper() + random.nextInt(21) - 10)
        );
    }

    private int clamp(int value) {
        return Math.max(0, Math.min(100, value));
    }

    private void damageNet(Player player, ItemStack net) {
        ItemMeta meta = net.getItemMeta();
        if (!(meta instanceof Damageable damageable)) {
            return;
        }
        int next = damageable.getDamage() + 1;
        if (next >= net.getType().getMaxDurability()) {
            net.setAmount(0);
            plugin.messages().send(player, "net-broken");
            return;
        }
        damageable.setDamage(next);
        net.setItemMeta(meta);
    }
}
