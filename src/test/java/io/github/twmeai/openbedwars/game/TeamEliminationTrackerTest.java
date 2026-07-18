package io.github.twmeai.openbedwars.game;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TeamEliminationTrackerTest {
    @Test
    void reportsEachTeamEliminationOnlyOnce() {
        TeamEliminationTracker tracker = new TeamEliminationTracker();

        assertTrue(tracker.eliminate(TeamColor.RED));
        assertFalse(tracker.eliminate(TeamColor.RED));
        assertTrue(tracker.eliminate(TeamColor.BLUE));
        assertFalse(tracker.eliminate(TeamColor.BLUE));
    }

    @Test
    void clearPermitsFreshEliminationsForTheNextMatch() {
        TeamEliminationTracker tracker = new TeamEliminationTracker();
        tracker.eliminate(TeamColor.RED);

        tracker.clear();

        assertTrue(tracker.eliminate(TeamColor.RED));
    }
}
