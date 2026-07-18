package io.github.twmeai.openbedwars.game;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class InvisibilityPolicyTest {
    @Test
    void hidesArmorOnlyFromActiveEnemies() {
        assertTrue(InvisibilityPolicy.hidesArmor(TeamColor.RED, TeamColor.BLUE, false, false));
        assertFalse(InvisibilityPolicy.hidesArmor(TeamColor.RED, TeamColor.RED, false, false));
        assertFalse(InvisibilityPolicy.hidesArmor(TeamColor.RED, TeamColor.BLUE, true, false));
        assertFalse(InvisibilityPolicy.hidesArmor(TeamColor.RED, TeamColor.BLUE, false, true));
    }
}
