package com.xuvigan.hiveworks.bee;

import java.util.Set;

/**
 * Immutable bee species definition, loaded from species.yml.
 */
public record Species(
        String id,
        String displayName,
        Set<String> biomes,
        Rarity rarity,
        int customModelData,
        int basePurity,
        Genes baseGenes,
        String minVersion
) {

    public boolean rare() {
        return rarity == Rarity.RARE;
    }

    public boolean matchesBiome(String biomeName) {
        return biomes.contains(biomeName.toLowerCase());
    }
}
