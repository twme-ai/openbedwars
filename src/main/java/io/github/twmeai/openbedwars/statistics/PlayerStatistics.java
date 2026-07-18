package io.github.twmeai.openbedwars.statistics;

import java.util.Objects;
import java.util.UUID;

public record PlayerStatistics(
        UUID uuid,
        String name,
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
    public PlayerStatistics {
        Objects.requireNonNull(uuid, "uuid");
        Objects.requireNonNull(name, "name");
    }

    public int level() {
        return BedWarsLevel.fromExperience(experience);
    }

    public static PlayerStatistics empty(PlayerIdentity player) {
        return new PlayerStatistics(player.uuid(), player.name(), 0, 0, 0, 0, 0, 0, 0, 0, 0);
    }
}
