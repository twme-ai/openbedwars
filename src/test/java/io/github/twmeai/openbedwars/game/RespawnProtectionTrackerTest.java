package io.github.twmeai.openbedwars.game;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RespawnProtectionTrackerTest {
    private final UUID playerId = UUID.randomUUID();

    @Test
    void protectsUntilTheExclusiveExpiryBoundary() {
        RespawnProtectionTracker tracker = new RespawnProtectionTracker();

        tracker.grant(playerId, 1_000L, 3);

        assertTrue(tracker.isProtected(playerId, 1_000L));
        assertTrue(tracker.isProtected(playerId, 3_000_000_999L));
        assertFalse(tracker.isProtected(playerId, 3_000_001_000L));
        assertFalse(tracker.isProtected(playerId, 3_000_001_001L));
    }

    @Test
    void attacksDisableProtectionAndZeroSecondsNeverProtects() {
        RespawnProtectionTracker tracker = new RespawnProtectionTracker();
        tracker.grant(playerId, 1_000L, 3);

        tracker.remove(playerId);
        assertFalse(tracker.isProtected(playerId, 1_001L));

        tracker.grant(playerId, 2_000L, 0);
        assertFalse(tracker.isProtected(playerId, 2_000L));
    }

    @Test
    void clearRemovesEveryProtectionWindow() {
        RespawnProtectionTracker tracker = new RespawnProtectionTracker();
        UUID teammate = UUID.randomUUID();
        tracker.grant(playerId, 1_000L, 3);
        tracker.grant(teammate, 1_000L, 3);

        tracker.clear();

        assertFalse(tracker.isProtected(playerId, 1_001L));
        assertFalse(tracker.isProtected(teammate, 1_001L));
    }
}
