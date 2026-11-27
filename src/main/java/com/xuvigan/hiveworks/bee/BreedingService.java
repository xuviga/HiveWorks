package com.xuvigan.hiveworks.bee;

import com.xuvigan.hiveworks.hive.BeeSlot;
import com.xuvigan.hiveworks.hive.HiveData;

import java.util.Optional;
import java.util.Random;

/**
 * Breeding system: two parent bees produce a child with inherited genes.
 * The hive timer and royal jelly cost are handled by HiveManager.
 */
public final class BreedingService {

    public Optional<BeeSlot> breed(HiveData hive, int firstSlot, int secondSlot, Random random) {
        return breed(hive, firstSlot, secondSlot, random, 0.0, null);
    }

    public Optional<BeeSlot> breed(HiveData hive, int firstSlot, int secondSlot, Random random,
                                   double rareChance, SpeciesRegistry registry) {
        BeeSlot first = hive.bee(firstSlot);
        BeeSlot second = hive.bee(secondSlot);
        if (first == null || second == null) {
            return Optional.empty();
        }
        Genes child = first.genes().child(second.genes(), random);
        int purity = (first.purity() + second.purity()) / 2;
        String speciesId = first.speciesId();
        if (registry != null && rareChance > 0.0 && random.nextDouble() < rareChance) {
            Optional<Species> rare = registry.rareRandom(random);
            if (rare.isPresent()) {
                speciesId = rare.get().id();
            }
        }
        return Optional.of(new BeeSlot(java.util.UUID.randomUUID(), speciesId, child, purity));
    }
}
