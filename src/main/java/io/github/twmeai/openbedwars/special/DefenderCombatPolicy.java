package io.github.twmeai.openbedwars.special;

import io.github.twmeai.openbedwars.game.TeamColor;

import java.util.Objects;

final class DefenderCombatPolicy {
    private DefenderCombatPolicy() {
    }

    static boolean isEnemy(TeamColor owner, TeamColor target, boolean sameArena, boolean targetActive) {
        Objects.requireNonNull(owner, "owner");
        Objects.requireNonNull(target, "target");
        return sameArena && targetActive && owner != target;
    }
}
