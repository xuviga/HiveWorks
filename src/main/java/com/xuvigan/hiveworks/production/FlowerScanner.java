package com.xuvigan.hiveworks.production;

import org.bukkit.Material;
import org.bukkit.Tag;
import org.bukkit.World;
import org.bukkit.block.Block;

import java.util.Collections;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

/**
 * Counts flower species around a hive. Runs on harvest / cached tick only.
 */
public final class FlowerScanner {

    private static final Set<String> RARE_FLOWERS = Set.of(
            "TORCHFLOWER",
            "LILY_OF_THE_VALLEY",
            "CACTUS_FLOWER",
            "WILDFLOWERS",
            "PITCHER_PLANT"
    );

    private FlowerScanner() {
    }

    public static Result scan(Block center, int radius) {
        World world = center.getWorld();
        if (world == null) {
            return Result.empty();
        }
        Set<String> names = new HashSet<>();
        boolean rare = false;
        int centerX = center.getX();
        int centerY = center.getY();
        int centerZ = center.getZ();

        for (int x = -radius; x <= radius; x++) {
            for (int y = -2; y <= 3; y++) {
                for (int z = -radius; z <= radius; z++) {
                    Block block = world.getBlockAt(centerX + x, centerY + y, centerZ + z);
                    Material material = block.getType();
                    if (!Tag.FLOWERS.isTagged(material)) {
                        continue;
                    }
                    names.add(material.name().toUpperCase(Locale.ROOT));
                    if (RARE_FLOWERS.contains(material.name())) {
                        rare = true;
                    }
                }
            }
        }
        return new Result(names.size(), rare, Collections.unmodifiableSet(names));
    }

    public record Result(int species, boolean rare, Set<String> names) {
        public static Result empty() {
            return new Result(0, false, Collections.emptySet());
        }
    }
}
