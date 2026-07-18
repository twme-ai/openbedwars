package io.github.twmeai.openbedwars.statistics;

import java.util.Arrays;
import java.util.Locale;
import java.util.Optional;

public enum LeaderboardMetric {
    WINS("wins"),
    KILLS("kills"),
    FINAL_KILLS("final_kills"),
    BEDS("beds_broken"),
    LEVEL("experience");

    private final String column;

    LeaderboardMetric(String column) {
        this.column = column;
    }

    String column() { return column; }
    public String key() { return name().toLowerCase(Locale.ROOT); }
    public String translationKey() { return "leaderboard.metric." + key(); }

    public long value(PlayerStatistics statistics) {
        return switch (this) {
            case WINS -> statistics.wins();
            case KILLS -> statistics.kills();
            case FINAL_KILLS -> statistics.finalKills();
            case BEDS -> statistics.bedsBroken();
            case LEVEL -> statistics.level();
        };
    }

    public static Optional<LeaderboardMetric> fromKey(String key) {
        return Arrays.stream(values()).filter(metric -> metric.key().equalsIgnoreCase(key)).findFirst();
    }
}
