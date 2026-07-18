package io.github.twmeai.openbedwars.game;

final class InvisibilityPolicy {
    private InvisibilityPolicy() {
    }

    static boolean hidesArmor(
            TeamColor subjectTeam,
            TeamColor viewerTeam,
            boolean viewerEliminated,
            boolean viewerRespawning
    ) {
        return subjectTeam != viewerTeam && !viewerEliminated && !viewerRespawning;
    }
}
