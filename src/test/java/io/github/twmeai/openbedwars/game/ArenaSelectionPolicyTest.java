package io.github.twmeai.openbedwars.game;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ArenaSelectionPolicyTest {
    @Test
    void skipsTheFullerArenaWhenItCannotFitTheWholeGroup() {
        StubArena crowded = new StubArena("crowded", GamePhase.WAITING, 1, 2);
        StubArena empty = new StubArena("empty", GamePhase.WAITING, 0, 4);

        assertEquals(empty, ArenaSelectionPolicy.fullestAvailable(List.of(crowded, empty), 2).orElseThrow());
        assertEquals(crowded, ArenaSelectionPolicy.fullestAvailable(List.of(crowded, empty), 1).orElseThrow());
    }

    @Test
    void acceptsWaitingAndStartingArenasButNotActiveOnes() {
        assertTrue(ArenaSelectionPolicy.canAccept(new StubArena("waiting", GamePhase.WAITING, 1, 4), 2));
        assertTrue(ArenaSelectionPolicy.canAccept(new StubArena("starting", GamePhase.STARTING, 1, 4), 2));
        assertFalse(ArenaSelectionPolicy.canAccept(new StubArena("running", GamePhase.RUNNING, 0, 4), 1));
        assertFalse(ArenaSelectionPolicy.canAccept(new StubArena("ending", GamePhase.ENDING, 0, 4), 1));
    }

    @Test
    void returnsEmptyWhenNoArenaFitsAndRejectsInvalidCapacity() {
        StubArena oneSlot = new StubArena("one-slot", GamePhase.WAITING, 3, 4);

        assertTrue(ArenaSelectionPolicy.fullestAvailable(List.of(oneSlot), 2).isEmpty());
        assertThrows(IllegalArgumentException.class,
                () -> ArenaSelectionPolicy.fullestAvailable(List.of(oneSlot), 0));
        assertThrows(IllegalArgumentException.class,
                () -> ArenaSelectionPolicy.canAccept(new StubArena("invalid", GamePhase.WAITING, 5, 4), 1));
    }

    private record StubArena(String key, GamePhase phase, int playerCount, int maxPlayers)
            implements ArenaSelectionPolicy.Candidate {
    }
}
