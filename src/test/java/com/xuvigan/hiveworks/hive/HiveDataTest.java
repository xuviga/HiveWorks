package com.xuvigan.hiveworks.hive;

import com.xuvigan.hiveworks.bee.Genes;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class HiveDataTest {

    private HiveData hive() {
        return new HiveData(new HivePos("world", 0, 64, 0), UUID.randomUUID());
    }

    @Test
    void honeyMaxIsCappedAtEight() {
        HiveData hive = hive();
        hive.setTier(HiveTier.IV);
        for (int slot = 0; slot < HiveData.MAX_FRAMES; slot++) {
            hive.setFrame(slot, new FrameSlot(FrameType.WAX, 32));
        }
        assertEquals(8, hive.honeyMax());
    }

    @Test
    void breedingLifecycle() {
        HiveData hive = hive();
        long now = 1_000L;
        hive.startBreeding(0, 1, now + 5_000L);
        assertTrue(hive.breedingActive(now));
        assertFalse(hive.breedingReady(now));
        assertTrue(hive.breedingReady(now + 5_000L));
        hive.clearBreeding();
        assertFalse(hive.breedingActive(now));
    }

    @Test
    void pollenBonusIsClamped() {
        HiveData hive = hive();
        hive.setPollenBonus(5.0);
        assertEquals(1.0, hive.pollenBonus());
        hive.setPollenBonus(-2.0);
        assertEquals(0.0, hive.pollenBonus());
    }

    @Test
    void averageTemperUsesOccupiedSlots() {
        HiveData hive = hive();
        hive.setBee(0, new BeeSlot(UUID.randomUUID(), "forest", new Genes(50, 50, 50, 20), 50));
        hive.setBee(1, new BeeSlot(UUID.randomUUID(), "forest", new Genes(50, 50, 50, 80), 50));
        assertEquals(50, hive.averageTemper());
    }
}
