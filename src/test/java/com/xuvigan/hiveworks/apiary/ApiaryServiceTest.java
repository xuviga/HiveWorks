package com.xuvigan.hiveworks.apiary;

import com.xuvigan.hiveworks.hive.HivePos;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ApiaryServiceTest {

    @Test
    void namesAreSanitizedAndValidated() {
        assertEquals("hack", ApiaryService.sanitizeName("<hack>"));
        assertEquals("Медовый двор", ApiaryService.sanitizeName("  Медовый двор  "));
        assertFalse(ApiaryService.isValidName("ab"));
        assertTrue(ApiaryService.isValidName("abc"));
        assertFalse(ApiaryService.isValidName("x".repeat(25)));
    }

    @Test
    void distanceIsPerWorld() {
        HivePos a = new HivePos("world", 0, 64, 0);
        HivePos b = new HivePos("world", 3, 68, 0);
        assertEquals(25.0, ApiaryService.distanceSquared(a, b), 0.0001);
        assertEquals(Double.MAX_VALUE, ApiaryService.distanceSquared(a, new HivePos("nether", 0, 64, 0)));
    }
}
