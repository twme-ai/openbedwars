package io.github.twmeai.openbedwars.game;

import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

final class EnemyBaseEntryTracker {
    private final Map<UUID, EnumSet<TeamColor>> occupiedBases = new HashMap<>();

    boolean enter(UUID playerId, TeamColor base) {
        return occupiedBases.computeIfAbsent(playerId, ignored -> EnumSet.noneOf(TeamColor.class)).add(base);
    }

    void leave(UUID playerId, TeamColor base) {
        EnumSet<TeamColor> bases = occupiedBases.get(playerId);
        if (bases == null) return;
        bases.remove(base);
        if (bases.isEmpty()) occupiedBases.remove(playerId);
    }

    void remove(UUID playerId) {
        occupiedBases.remove(playerId);
    }

    void clear() {
        occupiedBases.clear();
    }
}
