package com.xuvigan.hiveworks.hive;

import com.xuvigan.hiveworks.bee.Genes;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class QueenTest {

    @Test
    void queenIsOneOfTheBees() {
        HiveData hive = new HiveData(new HivePos("world", 0, 64, 0), UUID.randomUUID());
        assertFalse(hive.hasQueen());

        BeeSlot bee = new BeeSlot(UUID.randomUUID(), "forest", new Genes(60, 55, 40, 20), 70);
        hive.setBee(1, bee);
        hive.setQueenSlot(1);
        hive.setQueenCycles(40);

        assertTrue(hive.hasQueen());
        assertEquals(bee, hive.queen());
        assertEquals(1, hive.occupiedBees());
        assertEquals(40, hive.queenCycles());
    }

    @Test
    void queenSlotAndCyclesAreClamped() {
        HiveData hive = new HiveData(new HivePos("world", 0, 64, 0), UUID.randomUUID());
        hive.setQueenCycles(-10);
        assertEquals(0, hive.queenCycles());

        hive.setQueenSlot(999);
        assertEquals(HiveData.MAX_BEES - 1, hive.queenSlot());
        hive.setQueenSlot(-5);
        assertEquals(-1, hive.queenSlot());
    }
}
