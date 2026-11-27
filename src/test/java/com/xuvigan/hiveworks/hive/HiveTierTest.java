package com.xuvigan.hiveworks.hive;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class HiveTierTest {

    @Test
    void tiersFormAChain() {
        assertEquals(HiveTier.II, HiveTier.I.next());
        assertEquals(HiveTier.III, HiveTier.II.next());
        assertEquals(HiveTier.IV, HiveTier.III.next());
        assertNull(HiveTier.IV.next());
    }

    @Test
    void tiersGrowCapacityAndBonuses() {
        assertTrue(HiveTier.IV.beeSlots() > HiveTier.I.beeSlots());
        assertTrue(HiveTier.IV.frameSlots() > HiveTier.I.frameSlots());
        assertTrue(HiveTier.IV.rareChance() > 0.0);
    }
}
