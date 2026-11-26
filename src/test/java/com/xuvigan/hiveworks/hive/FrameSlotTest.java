package com.xuvigan.hiveworks.hive;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class FrameSlotTest {

    @Test
    void wearDecrementsUses() {
        FrameSlot slot = new FrameSlot(FrameType.WAX, 2);
        FrameSlot next = slot.wear();
        assertEquals(new FrameSlot(FrameType.WAX, 1), next);
    }

    @Test
    void lastUseBreaksFrame() {
        FrameSlot slot = new FrameSlot(FrameType.HONEY, 1);
        assertNull(slot.wear());
    }

    @Test
    void frameTypesExposeSanity() {
        for (FrameType type : FrameType.values()) {
            assertEquals(type.maxUses(), Math.max(1, type.maxUses()));
            assertEquals(type.customModelData(), type.customModelData());
        }
    }
}
