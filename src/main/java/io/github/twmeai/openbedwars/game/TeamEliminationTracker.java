package io.github.twmeai.openbedwars.game;

import java.util.EnumSet;

final class TeamEliminationTracker {
    private final EnumSet<TeamColor> eliminatedTeams = EnumSet.noneOf(TeamColor.class);

    boolean eliminate(TeamColor team) {
        return eliminatedTeams.add(team);
    }

    void clear() {
        eliminatedTeams.clear();
    }
}
