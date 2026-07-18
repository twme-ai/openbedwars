package io.github.twmeai.openbedwars.config;

import io.github.twmeai.openbedwars.game.ResourceType;
import io.github.twmeai.openbedwars.game.TeamColor;

import java.util.List;
import java.util.Map;
import java.util.Objects;

public record ArenaDefinition(
        String key,
        String displayName,
        String worldName,
        int minPlayers,
        int maxPlayers,
        int playersPerTeam,
        int voidKillY,
        int maxBuildY,
        ArenaProtection protection,
        Position lobby,
        Position spectator,
        Map<TeamColor, TeamDefinition> teams,
        Map<ResourceType, List<Position>> generators
) {
    public ArenaDefinition {
        Objects.requireNonNull(key, "key");
        Objects.requireNonNull(displayName, "displayName");
        Objects.requireNonNull(worldName, "worldName");
        Objects.requireNonNull(protection, "protection");
        Objects.requireNonNull(lobby, "lobby");
        Objects.requireNonNull(spectator, "spectator");
        teams = Map.copyOf(teams);
        generators = generators.entrySet().stream()
                .collect(java.util.stream.Collectors.toUnmodifiableMap(Map.Entry::getKey, entry -> List.copyOf(entry.getValue())));
        if (minPlayers < 1 || maxPlayers < minPlayers) {
            throw new IllegalArgumentException("Invalid player limits for arena " + key);
        }
        if (playersPerTeam < 1 || maxPlayers > teams.size() * playersPerTeam) {
            throw new IllegalArgumentException("Team capacity is smaller than max-players for arena " + key);
        }
        if (teams.size() < 2) {
            throw new IllegalArgumentException("Arena " + key + " requires at least two teams");
        }
        if (maxBuildY <= voidKillY) {
            throw new IllegalArgumentException("max-build-y must be above void-kill-y for arena " + key);
        }
    }

    public boolean isVoid(double y) {
        return y <= voidKillY;
    }

    public boolean canBuildAt(int y) {
        return y < maxBuildY;
    }

    public boolean isProtectedBlock(int x, int y, int z) {
        for (TeamDefinition team : teams.values()) {
            if (protection.protectsSpawn(team.spawn(), x, y, z)
                    || protection.protectsItemShop(team.itemShop(), x, y, z)
                    || protection.protectsUpgradeShop(team.upgradeShop(), x, y, z)
                    || protection.protectsGenerator(team.forge(), x, y, z)) {
                return true;
            }
        }
        return generators.values().stream()
                .flatMap(List::stream)
                .anyMatch(generator -> protection.protectsGenerator(generator, x, y, z));
    }
}
