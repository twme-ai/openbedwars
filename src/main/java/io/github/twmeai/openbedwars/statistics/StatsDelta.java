package io.github.twmeai.openbedwars.statistics;

public record StatsDelta(
        int games,
        int wins,
        int losses,
        int kills,
        int finalKills,
        int deaths,
        int finalDeaths,
        int bedsBroken,
        long experience
) {
    public StatsDelta {
        if (games < 0 || wins < 0 || losses < 0 || kills < 0 || finalKills < 0
                || deaths < 0 || finalDeaths < 0 || bedsBroken < 0 || experience < 0) {
            throw new IllegalArgumentException("Statistic deltas must not be negative");
        }
    }
}
