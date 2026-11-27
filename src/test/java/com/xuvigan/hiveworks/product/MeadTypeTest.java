package com.xuvigan.hiveworks.product;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

class MeadTypeTest {

    @Test
    void parseAcceptsIdAndName() {
        assertEquals(MeadType.MEADOW, MeadType.parse("meadow"));
        assertEquals(MeadType.FOREST, MeadType.parse("FOREST"));
        assertNull(MeadType.parse("unknown"));
        assertNull(MeadType.parse(null));
    }

    @Test
    void everyMeadHasEffectAndModel() {
        for (MeadType type : MeadType.values()) {
            assertNotNull(type.effectName());
            assertNotNull(type.id());
            assertTruePositive(type.durationTicks());
            assertTruePositive(type.customModelData());
        }
    }

    private void assertTruePositive(int value) {
        if (value <= 0) {
            throw new AssertionError("expected positive, got " + value);
        }
    }
}
