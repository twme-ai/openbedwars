package io.github.twmeai.openbedwars.game;

import java.util.Collection;
import java.util.Comparator;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

final class DragonTargetPolicy {
    private DragonTargetPolicy() {
    }

    static Optional<UUID> nearestEnemy(TeamColor owner, Collection<Candidate> candidates) {
        Objects.requireNonNull(owner, "owner");
        Objects.requireNonNull(candidates, "candidates");
        return candidates.stream()
                .filter(Candidate::active)
                .filter(candidate -> candidate.team() != owner)
                .min(Comparator.comparingDouble(Candidate::distanceSquared)
                        .thenComparing(Candidate::playerId))
                .map(Candidate::playerId);
    }

    static boolean isFriendly(TeamColor owner, TeamColor playerTeam) {
        return owner == playerTeam;
    }

    record Candidate(UUID playerId, TeamColor team, boolean active, double distanceSquared) {
        Candidate {
            Objects.requireNonNull(playerId, "playerId");
            Objects.requireNonNull(team, "team");
            if (!Double.isFinite(distanceSquared) || distanceSquared < 0) {
                throw new IllegalArgumentException("Dragon target distance must be finite and non-negative");
            }
        }
    }
}
