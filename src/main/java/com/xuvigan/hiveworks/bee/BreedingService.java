package com.xuvigan.hiveworks.bee;

import com.xuvigan.hiveworks.hive.BeeSlot;
import com.xuvigan.hiveworks.hive.HiveData;

import java.util.Optional;
import java.util.Random;

/**
 * Breeding system. Gene inheritance exists in Genes; hive slot and royal jelly flow arrive in 0.1.1.
 */
public final class BreedingService {

    public Optional<BeeSlot> breed(HiveData hive, int firstSlot, int secondSlot, Random random) {
        BeeSlot first = hive.bee(firstSlot);
        BeeSlot second = hive.bee(secondSlot);
        if (first == null || second == null) {
            return Optional.empty();
        }
        // TODO 0.1.1: require a royal cell, run a 30-minute timer, pick a free slot.
        Genes child = first.genes().child(second.genes(), random);
        int purity = (first.purity() + second.purity()) / 2;
        return Optional.of(new BeeSlot(java.util.UUID.randomUUID(), first.speciesId(), child, purity));
    }
}
