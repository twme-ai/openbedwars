package io.github.twmeai.openbedwars.game;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MatchStartPolicyTest {
    @Test
    void normalStartRequiresTheConfiguredMinimumAndTwoOccupiedTeams() {
        assertTrue(MatchStartPolicy.canStartNormally(2, 2, 2));
        assertTrue(MatchStartPolicy.canStartNormally(4, 4, 2));
        assertFalse(MatchStartPolicy.canStartNormally(3, 4, 2));
        assertFalse(MatchStartPolicy.canStartNormally(4, 4, 1));
    }

    @Test
    void forceStartBypassesTheConfiguredMinimumButNotCompetition() {
        assertTrue(MatchStartPolicy.canForceStart(2, 2));
        assertFalse(MatchStartPolicy.canForceStart(2, 1));
        assertFalse(MatchStartPolicy.canForceStart(1, 1));
    }

    @Test
    void rejectsImpossibleCountsAndInvalidMinimums() {
        assertThrows(IllegalArgumentException.class, () -> MatchStartPolicy.canStartNormally(-1, 2, 0));
        assertThrows(IllegalArgumentException.class, () -> MatchStartPolicy.canStartNormally(1, 2, 2));
        assertThrows(IllegalArgumentException.class, () -> MatchStartPolicy.canStartNormally(2, 1, 2));
        assertThrows(IllegalArgumentException.class, () -> MatchStartPolicy.canForceStart(2, -1));
    }
}
