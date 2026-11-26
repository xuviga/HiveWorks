package com.xuvigan.hiveworks.hive;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class HiveModeTest {

    @Test
    void allModesHaveKeysAndIcons() {
        assertEquals(4, HiveMode.values().length);
        for (HiveMode mode : HiveMode.values()) {
            assertNotNull(mode.messageKey());
            assertNotNull(mode.icon());
        }
        assertEquals("mode-guard", HiveMode.GUARD.messageKey());
    }
}
