package com.xuvigan.hiveworks.production;

import com.xuvigan.hiveworks.util.SchedulerAdapter;
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

    private static final Set<String> TALL_FLOWERS = Set.of(
            "SUNFLOWER",
            "LILAC",
            "ROSE_BUSH",
            "PEONY"
    );

    private static final java.util.Map<String, String> RU_NAMES = java.util.Map.ofEntries(
            java.util.Map.entry("DANDELION", "одуванчик"),
            java.util.Map.entry("POPPY", "мак"),
            java.util.Map.entry("BLUE_ORCHID", "синяя орхидея"),
            java.util.Map.entry("ALLIUM", "декоративный лук"),
            java.util.Map.entry("AZURE_BLUET", "хатьма"),
            java.util.Map.entry("RED_TULIP", "красный тюльпан"),
            java.util.Map.entry("ORANGE_TULIP", "оранжевый тюльпан"),
            java.util.Map.entry("WHITE_TULIP", "белый тюльпан"),
            java.util.Map.entry("PINK_TULIP", "розовый тюльпан"),
            java.util.Map.entry("OXEYE_DAISY", "ромашка"),
            java.util.Map.entry("CORNFLOWER", "василёк"),
            java.util.Map.entry("LILY_OF_THE_VALLEY", "ландыш"),
            java.util.Map.entry("TORCHFLOWER", "факельник"),
            java.util.Map.entry("CACTUS_FLOWER", "кактусовый цветок"),
            java.util.Map.entry("WILDFLOWERS", "полевые цветы"),
            java.util.Map.entry("PITCHER_PLANT", "непентес"),
            java.util.Map.entry("SUNFLOWER", "подсолнух"),
            java.util.Map.entry("LILAC", "сирень"),
            java.util.Map.entry("ROSE_BUSH", "розовый куст"),
            java.util.Map.entry("PEONY", "пион")
    );

    public static String displayName(String materialName, boolean russian) {
        if (!russian) {
            return materialName;
        }
        return RU_NAMES.getOrDefault(materialName, materialName);
    }

    private FlowerScanner() {
    }

    public static Result scan(Block center, int radius) {
        World world = center.getWorld();
        if (world == null) {
            return Result.empty();
        }
        Set<String> names = new HashSet<>();
        boolean rare = false;
        int count = 0;
        int centerX = center.getX();
        int centerY = center.getY();
        int centerZ = center.getZ();
        int lastChunkX = Integer.MIN_VALUE;
        int lastChunkZ = Integer.MIN_VALUE;
        boolean owned = false;

        for (int x = -radius; x <= radius; x++) {
            for (int z = -radius; z <= radius; z++) {
                int blockX = centerX + x;
                int blockZ = centerZ + z;
                int chunkX = blockX >> 4;
                int chunkZ = blockZ >> 4;
                if (chunkX != lastChunkX || chunkZ != lastChunkZ) {
                    lastChunkX = chunkX;
                    lastChunkZ = chunkZ;
                    owned = SchedulerAdapter.ownedChunkNow(world, chunkX, chunkZ);
                }
                if (!owned) {
                    // Never touch a chunk owned by another region.
                    continue;
                }
                for (int y = -2; y <= 3; y++) {
                    Block block = world.getBlockAt(blockX, centerY + y, blockZ);
                    Material material = block.getType();
                    String name = material.name();
                    if (!Tag.FLOWERS.isTagged(material) && !TALL_FLOWERS.contains(name)) {
                        continue;
                    }
                    // Tall flowers occupy two blocks: count only the lower half.
                    if (TALL_FLOWERS.contains(name)
                            && world.getBlockAt(blockX, centerY + y - 1, blockZ).getType() == material) {
                        continue;
                    }
                    count++;
                    names.add(material.name().toUpperCase(Locale.ROOT));
                    if (RARE_FLOWERS.contains(material.name())) {
                        rare = true;
                    }
                }
            }
        }
        return new Result(names.size(), count, rare, Collections.unmodifiableSet(names));
    }

    public record Result(int species, int count, boolean rare, Set<String> names) {
        public static Result empty() {
            return new Result(0, 0, false, Collections.emptySet());
        }
    }
}
