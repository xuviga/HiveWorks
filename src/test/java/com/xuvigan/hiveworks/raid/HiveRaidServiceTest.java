package com.xuvigan.hiveworks.raid;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class HiveRaidServiceTest {

    @Test
    void stealAmountRespectsHoneyAndLimit() {
        assertEquals(0, HiveRaidService.stealAmount(0.9, 1));
        assertEquals(1, HiveRaidService.stealAmount(1.0, 1));
        assertEquals(1, HiveRaidService.stealAmount(2.9, 1));
        assertEquals(3, HiveRaidService.stealAmount(4.9, 3));
        assertEquals(2, HiveRaidService.stealAmount(2.2, 5));
    }
}
