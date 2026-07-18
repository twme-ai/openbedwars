package io.github.twmeai.openbedwars.game;

import java.util.Collection;
import java.util.Comparator;
import java.util.Optional;

final class ArenaSelectionPolicy {
    private ArenaSelectionPolicy() {
    }

    static <T extends Candidate> Optional<T> fullestAvailable(Collection<T> candidates, int groupSize) {
        if (groupSize < 1) {
            throw new IllegalArgumentException("groupSize must be positive");
        }
        return candidates.stream()
                .filter(candidate -> canAccept(candidate, groupSize))
                .max(Comparator.comparingInt(Candidate::playerCount));
    }

    static boolean canAccept(Candidate candidate, int groupSize) {
        if (groupSize < 1) {
            throw new IllegalArgumentException("groupSize must be positive");
        }
        if (candidate.playerCount() < 0 || candidate.maxPlayers() < candidate.playerCount()) {
            throw new IllegalArgumentException("Invalid arena player capacity");
        }
        return (candidate.phase() == GamePhase.WAITING || candidate.phase() == GamePhase.STARTING)
                && candidate.playerCount() + groupSize <= candidate.maxPlayers();
    }

    interface Candidate {
        GamePhase phase();

        int playerCount();

        int maxPlayers();
    }
}
