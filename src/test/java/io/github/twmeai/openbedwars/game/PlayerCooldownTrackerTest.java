package io.github.twmeai.openbedwars.game;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PlayerCooldownTrackerTest {
    @Test
    void enforcesIndependentMonotonicDeadlines() {
        PlayerCooldownTracker tracker = new PlayerCooldownTracker();
        UUID first = UUID.randomUUID();
        UUID second = UUID.randomUUID();
        long startedAt = 1_000_000_000L;

        assertTrue(tracker.tryAcquire(first, startedAt, 10));
        assertFalse(tracker.tryAcquire(first, startedAt + 499_999_999L, 10));
        assertTrue(tracker.tryAcquire(second, startedAt + 1L, 10));
        assertTrue(tracker.tryAcquire(first, startedAt + 500_000_000L, 10));
    }

    @Test
    void zeroCooldownAndLifecycleClearsAllowImmediateUse() {
        PlayerCooldownTracker tracker = new PlayerCooldownTracker();
        UUID playerId = UUID.randomUUID();

        assertTrue(tracker.tryAcquire(playerId, 100L, 10));
        tracker.remove(playerId);
        assertTrue(tracker.tryAcquire(playerId, 101L, 10));
        tracker.clear();
        assertTrue(tracker.tryAcquire(playerId, 102L, 0));
        assertTrue(tracker.tryAcquire(playerId, 102L, 0));
    }

    @Test
    void rejectsNegativeCooldowns() {
        PlayerCooldownTracker tracker = new PlayerCooldownTracker();

        assertThrows(IllegalArgumentException.class,
                () -> tracker.tryAcquire(UUID.randomUUID(), 0L, -1));
    }
}
