package io.github.twmeai.openbedwars.game;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EnemyBaseEntryTrackerTest {
    @Test
    void reportsOnlyTheFirstEntryUntilThePlayerLeaves() {
        EnemyBaseEntryTracker tracker = new EnemyBaseEntryTracker();
        UUID playerId = UUID.randomUUID();

        assertTrue(tracker.enter(playerId, TeamColor.RED));
        assertFalse(tracker.enter(playerId, TeamColor.RED));
        tracker.leave(playerId, TeamColor.RED);
        assertTrue(tracker.enter(playerId, TeamColor.RED));
    }

    @Test
    void tracksDifferentPlayersAndBasesIndependently() {
        EnemyBaseEntryTracker tracker = new EnemyBaseEntryTracker();
        UUID first = UUID.randomUUID();
        UUID second = UUID.randomUUID();

        assertTrue(tracker.enter(first, TeamColor.RED));
        assertTrue(tracker.enter(first, TeamColor.BLUE));
        assertTrue(tracker.enter(second, TeamColor.RED));
        assertFalse(tracker.enter(first, TeamColor.RED));
        assertFalse(tracker.enter(first, TeamColor.BLUE));
    }

    @Test
    void playerAndMatchLifecycleClearsPermitFreshEntries() {
        EnemyBaseEntryTracker tracker = new EnemyBaseEntryTracker();
        UUID first = UUID.randomUUID();
        UUID second = UUID.randomUUID();
        tracker.enter(first, TeamColor.RED);
        tracker.enter(second, TeamColor.BLUE);

        tracker.remove(first);
        assertTrue(tracker.enter(first, TeamColor.RED));
        tracker.clear();
        assertTrue(tracker.enter(first, TeamColor.RED));
        assertTrue(tracker.enter(second, TeamColor.BLUE));
    }
}
