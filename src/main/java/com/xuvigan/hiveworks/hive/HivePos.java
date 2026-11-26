package com.xuvigan.hiveworks.hive;

import org.bukkit.block.Block;

/**
 * World-independent hive position.
 */
public record HivePos(String world, int x, int y, int z) {

    public static HivePos from(Block block) {
        return new HivePos(block.getWorld().getName(), block.getX(), block.getY(), block.getZ());
    }

    public String key() {
        return world + ":" + x + "," + y + "," + z;
    }

    public String chunkKey() {
        return world + ":" + (x >> 4) + "," + (z >> 4);
    }
}
