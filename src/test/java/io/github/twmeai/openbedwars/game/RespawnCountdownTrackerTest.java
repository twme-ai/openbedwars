package io.github.twmeai.openbedwars.game;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RespawnCountdownTrackerTest {
    @Test
    void usesAnExclusiveMonotonicDeadline() {
        RespawnCountdownTracker<TestTask> countdowns = tracker();
        UUID playerId = UUID.randomUUID();
        long startedAt = 1_000L;

        countdowns.begin(playerId, startedAt, 5);

        assertEquals(5, countdowns.remainingSeconds(playerId, startedAt).orElseThrow());
        assertEquals(5, countdowns.remainingSeconds(playerId, startedAt + 1).orElseThrow());
        assertEquals(4, countdowns.remainingSeconds(playerId, startedAt + 1_000_000_000L).orElseThrow());
        assertEquals(1, countdowns.remainingSeconds(playerId, startedAt + 4_999_999_999L).orElseThrow());
        assertEquals(0, countdowns.remainingSeconds(playerId, startedAt + 5_000_000_000L).orElseThrow());
    }

    @Test
    void pausingCancelsOnlyTheTaskAndPreservesTheDeadline() {
        RespawnCountdownTracker<TestTask> countdowns = tracker();
        UUID playerId = UUID.randomUUID();
        TestTask first = new TestTask();
        TestTask resumed = new TestTask();
        countdowns.begin(playerId, 0L, 5);
        assertTrue(countdowns.attach(playerId, first));

        countdowns.pause(playerId);

        assertTrue(first.cancelled);
        assertTrue(countdowns.contains(playerId));
        assertEquals(3, countdowns.remainingSeconds(playerId, 2_000_000_000L).orElseThrow());
        assertTrue(countdowns.attach(playerId, resumed));
        assertTrue(countdowns.isCurrentTask(playerId, resumed));
    }

    @Test
    void replacingACountdownRejectsStaleTasks() {
        RespawnCountdownTracker<TestTask> countdowns = tracker();
        UUID playerId = UUID.randomUUID();
        TestTask stale = new TestTask();
        TestTask current = new TestTask();
        countdowns.begin(playerId, 0L, 5);
        countdowns.attach(playerId, stale);

        countdowns.begin(playerId, 10_000_000_000L, 2);
        assertTrue(stale.cancelled);
        assertFalse(countdowns.isCurrentTask(playerId, stale));
        assertTrue(countdowns.attach(playerId, current));
        assertEquals(2, countdowns.remainingSeconds(playerId, 10_000_000_000L).orElseThrow());

        countdowns.remove(playerId);
        assertTrue(current.cancelled);
        assertFalse(countdowns.contains(playerId));
        assertTrue(countdowns.remainingSeconds(playerId, Long.MAX_VALUE).isEmpty());
    }

    @Test
    void clearsIndependentPlayersAndRejectsInvalidInput() {
        RespawnCountdownTracker<TestTask> countdowns = tracker();
        TestTask first = new TestTask();
        TestTask second = new TestTask();
        countdowns.begin(UUID.randomUUID(), 0L, 0);
        UUID firstId = UUID.randomUUID();
        UUID secondId = UUID.randomUUID();
        countdowns.begin(firstId, 0L, 1);
        countdowns.begin(secondId, 0L, 1);
        countdowns.attach(firstId, first);
        countdowns.attach(secondId, second);
        assertEquals(3, countdowns.size());

        countdowns.clear();

        assertTrue(first.cancelled);
        assertTrue(second.cancelled);
        assertEquals(0, countdowns.size());
        assertThrows(IllegalArgumentException.class,
                () -> countdowns.begin(UUID.randomUUID(), 0L, -1));
        assertThrows(NullPointerException.class, () -> countdowns.begin(null, 0L, 1));
        TestTask unattached = new TestTask();
        assertFalse(countdowns.attach(UUID.randomUUID(), unattached));
        assertTrue(unattached.cancelled);
    }

    private RespawnCountdownTracker<TestTask> tracker() {
        return new RespawnCountdownTracker<>(TestTask::cancel);
    }

    private static final class TestTask {
        private boolean cancelled;

        private void cancel() {
            cancelled = true;
        }
    }
}
