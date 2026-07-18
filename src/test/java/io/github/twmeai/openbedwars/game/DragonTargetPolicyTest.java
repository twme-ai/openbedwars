package io.github.twmeai.openbedwars.game;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DragonTargetPolicyTest {
    @Test
    void choosesNearestActiveEnemyAndIgnoresTeammates() {
        UUID teammate = UUID.randomUUID();
        UUID nearbyEnemy = UUID.randomUUID();
        UUID distantEnemy = UUID.randomUUID();

        assertEquals(nearbyEnemy, DragonTargetPolicy.nearestEnemy(TeamColor.RED, List.of(
                candidate(teammate, TeamColor.RED, true, 1),
                candidate(distantEnemy, TeamColor.BLUE, true, 25),
                candidate(nearbyEnemy, TeamColor.GREEN, true, 4)
        )).orElseThrow());
    }

    @Test
    void ignoresEliminatedRespawningOrDisconnectedCandidates() {
        UUID activeEnemy = UUID.randomUUID();

        assertEquals(activeEnemy, DragonTargetPolicy.nearestEnemy(TeamColor.RED, List.of(
                candidate(UUID.randomUUID(), TeamColor.BLUE, false, 1),
                candidate(activeEnemy, TeamColor.BLUE, true, 16)
        )).orElseThrow());
        assertTrue(DragonTargetPolicy.nearestEnemy(TeamColor.RED, List.of(
                candidate(UUID.randomUUID(), TeamColor.RED, true, 1),
                candidate(UUID.randomUUID(), TeamColor.BLUE, false, 2)
        )).isEmpty());
    }

    @Test
    void usesPlayerIdAsAStableTieBreaker() {
        UUID first = new UUID(0, 1);
        UUID second = new UUID(0, 2);

        assertEquals(first, DragonTargetPolicy.nearestEnemy(TeamColor.RED, List.of(
                candidate(second, TeamColor.BLUE, true, 4),
                candidate(first, TeamColor.BLUE, true, 4)
        )).orElseThrow());
    }

    @Test
    void identifiesFriendlyTeamsAndRejectsInvalidDistances() {
        assertTrue(DragonTargetPolicy.isFriendly(TeamColor.RED, TeamColor.RED));
        assertFalse(DragonTargetPolicy.isFriendly(TeamColor.RED, TeamColor.BLUE));
        assertThrows(IllegalArgumentException.class,
                () -> candidate(UUID.randomUUID(), TeamColor.BLUE, true, Double.NaN));
        assertThrows(IllegalArgumentException.class,
                () -> candidate(UUID.randomUUID(), TeamColor.BLUE, true, -1));
    }

    private DragonTargetPolicy.Candidate candidate(
            UUID playerId,
            TeamColor team,
            boolean active,
            double distanceSquared
    ) {
        return new DragonTargetPolicy.Candidate(playerId, team, active, distanceSquared);
    }
}
