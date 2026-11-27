package com.xuvigan.hiveworks.production;

import com.xuvigan.hiveworks.HiveWorksPlugin;
import com.xuvigan.hiveworks.hive.HiveData;
import com.xuvigan.hiveworks.hive.HiveMode;
import com.xuvigan.hiveworks.util.SchedulerAdapter;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.data.Ageable;

import java.util.Random;
import java.util.Set;

/**
 * POLLINATION mode: gently advances nearby crop growth without replacing bone meal.
 */
public final class PollinationService {

    private static final Set<Material> CROPS = Set.of(
            Material.WHEAT,
            Material.CARROTS,
            Material.POTATOES,
            Material.BEETROOTS,
            Material.NETHER_WART,
            Material.SWEET_BERRY_BUSH,
            Material.COCOA
    );

    private final HiveWorksPlugin plugin;

    public PollinationService(HiveWorksPlugin plugin) {
        this.plugin = plugin;
    }

    public void tick(HiveData hive, World world, Random random) {
        if (hive.mode() != HiveMode.POLLINATION || !plugin.configManager().pollination()) {
            return;
        }
        int bees = hive.occupiedBees();
        if (bees == 0) {
            return;
        }
        double chance = Math.min(plugin.configManager().pollinationMaxChance(),
                plugin.configManager().pollinationChancePerBee() * bees);
        chance += candleBonus(world, hive.pos().x(), hive.pos().y(), hive.pos().z());
        if (chance <= 0.0) {
            return;
        }
        int radius = plugin.configManager().pollinationRadius();
        int centerX = hive.pos().x();
        int centerY = hive.pos().y();
        int centerZ = hive.pos().z();
        for (int x = -radius; x <= radius; x++) {
            for (int z = -radius; z <= radius; z++) {
                int blockX = centerX + x;
                int blockZ = centerZ + z;
                if (!SchedulerAdapter.ownedChunkNow(world, blockX >> 4, blockZ >> 4)) {
                    continue;
                }
                for (int y = -1; y <= 1; y++) {
                    Block block = world.getBlockAt(blockX, centerY + y, blockZ);
                    if (!CROPS.contains(block.getType())) {
                        continue;
                    }
                    if (!(block.getBlockData() instanceof Ageable ageable)) {
                        continue;
                    }
                    if (ageable.getAge() >= ageable.getMaximumAge()) {
                        continue;
                    }
                    if (random.nextDouble() < chance) {
                        ageable.setAge(ageable.getAge() + 1);
                        block.setBlockData(ageable, false);
                    }
                }
            }
        }
    }

    /** Up to two candles within 4 blocks add +5% growth chance each. */
    private double candleBonus(World world, int centerX, int centerY, int centerZ) {
        int candles = 0;
        for (int x = -4; x <= 4 && candles < 2; x++) {
            for (int z = -4; z <= 4 && candles < 2; z++) {
                int blockX = centerX + x;
                int blockZ = centerZ + z;
                if (!SchedulerAdapter.ownedChunkNow(world, blockX >> 4, blockZ >> 4)) {
                    continue;
                }
                for (int y = -2; y <= 2; y++) {
                    String name = world.getBlockAt(blockX, centerY + y, blockZ).getType().name();
                    if (name.equals("CANDLE") || name.endsWith("_CANDLE")) {
                        candles++;
                        break;
                    }
                }
            }
        }
        return candles * 0.05;
    }
}
