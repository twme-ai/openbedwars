package io.github.twmeai.openbedwars.special;

import io.github.twmeai.openbedwars.game.TeamColor;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DefenderCombatPolicyTest {
    @Test
    void allowsActiveEnemiesInTheSameArena() {
        assertTrue(DefenderCombatPolicy.isEnemy(TeamColor.RED, TeamColor.BLUE, true, true));
    }

    @Test
    void rejectsAlliesOutsideArenasAndInactiveTargets() {
        assertFalse(DefenderCombatPolicy.isEnemy(TeamColor.RED, TeamColor.RED, true, true));
        assertFalse(DefenderCombatPolicy.isEnemy(TeamColor.RED, TeamColor.BLUE, false, true));
        assertFalse(DefenderCombatPolicy.isEnemy(TeamColor.RED, TeamColor.BLUE, true, false));
    }

    @Test
    void rejectsMissingTeamOwnership() {
        assertThrows(NullPointerException.class,
                () -> DefenderCombatPolicy.isEnemy(null, TeamColor.BLUE, true, true));
        assertThrows(NullPointerException.class,
                () -> DefenderCombatPolicy.isEnemy(TeamColor.RED, null, true, true));
    }
}
