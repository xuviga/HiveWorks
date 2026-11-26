package com.xuvigan.hiveworks.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class VersionTest {

    @Test
    void comparesMinecraftVersions() {
        assertTrue(Version.isAtLeast("1.21.4", "1.21.4"));
        assertTrue(Version.isAtLeast("1.21.5", "1.21.4"));
        assertTrue(Version.isAtLeast("26.3", "1.21.4"));
        assertFalse(Version.isAtLeast("1.21.1", "1.21.4"));
        assertFalse(Version.isAtLeast("1.20.6", "1.21"));
    }

    @Test
    void blankRequirementAlwaysMatches() {
        assertTrue(Version.isAtLeast("1.21.1", null));
        assertTrue(Version.isAtLeast("1.21.1", " "));
    }
}
