package com.xuvigan.hiveworks.bee;

import com.xuvigan.hiveworks.hive.BeeSlot;
import com.xuvigan.hiveworks.hive.HiveData;
import com.xuvigan.hiveworks.hive.HivePos;
import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.Random;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BreedingServiceTest {

    @Test
    void childInheritsSpeciesAndAveragePurity() {
        HiveData hive = new HiveData(new HivePos("world", 0, 64, 0), UUID.randomUUID());
        hive.setBee(0, new BeeSlot(UUID.randomUUID(), "forest", new Genes(60, 60, 40, 20), 80));
        hive.setBee(1, new BeeSlot(UUID.randomUUID(), "forest", new Genes(40, 40, 60, 40), 40));

        Optional<BeeSlot> child = new BreedingService().breed(hive, 0, 1, new Random(7));
        assertTrue(child.isPresent());
        assertEquals("forest", child.get().speciesId());
        assertEquals(60, child.get().purity());
        assertTrue(child.get().genes().productivity() >= 0 && child.get().genes().productivity() <= 100);
    }

    @Test
    void missingParentProducesNothing() {
        HiveData hive = new HiveData(new HivePos("world", 0, 64, 0), UUID.randomUUID());
        hive.setBee(0, new BeeSlot(UUID.randomUUID(), "forest", Genes.DEFAULT, 50));
        assertTrue(new BreedingService().breed(hive, 0, 1, new Random(1)).isEmpty());
    }
}
