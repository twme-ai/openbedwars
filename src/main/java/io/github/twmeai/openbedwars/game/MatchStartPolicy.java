package io.github.twmeai.openbedwars.game;

final class MatchStartPolicy {
    private MatchStartPolicy() {
    }

    static boolean canStartNormally(int playerCount, int minimumPlayers, int occupiedTeams) {
        validate(playerCount, occupiedTeams);
        if (minimumPlayers < 2) {
            throw new IllegalArgumentException("minimumPlayers must be at least two");
        }
        return playerCount >= minimumPlayers && hasCompetition(playerCount, occupiedTeams);
    }

    static boolean canForceStart(int playerCount, int occupiedTeams) {
        validate(playerCount, occupiedTeams);
        return hasCompetition(playerCount, occupiedTeams);
    }

    private static boolean hasCompetition(int playerCount, int occupiedTeams) {
        return playerCount >= 2 && occupiedTeams >= 2;
    }

    private static void validate(int playerCount, int occupiedTeams) {
        if (playerCount < 0 || occupiedTeams < 0 || occupiedTeams > playerCount) {
            throw new IllegalArgumentException("Invalid player or occupied-team count");
        }
    }
}
