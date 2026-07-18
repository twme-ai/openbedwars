package io.github.twmeai.openbedwars.spectator;

final class SpectatorTargetPolicy {
    private SpectatorTargetPolicy() {
    }

    static boolean isEligible(
            boolean eliminated,
            boolean respawning,
            boolean disconnected,
            boolean online,
            boolean dead
    ) {
        return !eliminated && !respawning && !disconnected && online && !dead;
    }
}
