package io.github.twmeai.openbedwars.config;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ArenaProtectionTest {
    private final Position center = new Position(10.5, 64, -3.5, 0, 0);

    @Test
    void protectsConfiguredHorizontalAndVerticalRanges() {
        ArenaProtection protection = new ArenaProtection(5, 1, 1, 1);

        assertTrue(protection.protectsSpawn(center, 15, 69, -3));
        assertFalse(protection.protectsSpawn(center, 16, 64, -3));
        assertTrue(protection.protectsItemShop(center, 11, 68, -3));
        assertFalse(protection.protectsItemShop(center, 11, 69, -3));
        assertTrue(protection.protectsGenerator(center, 10, 69, -4));
        assertFalse(protection.protectsGenerator(center, 10, 70, -4));
    }

    @Test
    void zeroDisablesAProtectionTypeAndNegativeRadiiAreRejected() {
        ArenaProtection disabled = new ArenaProtection(0, 0, 0, 0);

        assertFalse(disabled.protectsSpawn(center, 10, 64, -4));
        assertThrows(IllegalArgumentException.class, () -> new ArenaProtection(-1, 1, 1, 1));
    }
}
