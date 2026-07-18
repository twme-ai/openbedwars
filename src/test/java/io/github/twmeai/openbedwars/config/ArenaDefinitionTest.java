package io.github.twmeai.openbedwars.config;

import io.github.twmeai.openbedwars.game.ResourceType;
import io.github.twmeai.openbedwars.game.TeamColor;
import org.junit.jupiter.api.Test;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ArenaDefinitionTest {
    @Test
    void appliesExclusiveBuildLimitAndInclusiveVoidLine() {
        ArenaDefinition definition = definition(0, 180);

        assertTrue(definition.isVoid(0));
        assertFalse(definition.isVoid(0.01));
        assertTrue(definition.canBuildAt(179));
        assertFalse(definition.canBuildAt(180));
    }

    @Test
    void rejectsBuildLimitAtOrBelowVoidLine() {
        assertThrows(IllegalArgumentException.class, () -> definition(10, 10));
        assertThrows(IllegalArgumentException.class, () -> definition(10, 9));
    }

    private ArenaDefinition definition(int voidKillY, int maxBuildY) {
        Position position = new Position(0, 64, 0, 0, 0);
        EnumMap<TeamColor, TeamDefinition> teams = new EnumMap<>(TeamColor.class);
        teams.put(TeamColor.RED, team(TeamColor.RED, position));
        teams.put(TeamColor.BLUE, team(TeamColor.BLUE, position));
        return new ArenaDefinition(
                "test", "Test", "world", 2, 2, 1, voidKillY, maxBuildY,
                position, position, teams, Map.<ResourceType, List<Position>>of()
        );
    }

    private TeamDefinition team(TeamColor color, Position position) {
        return new TeamDefinition(color, position, position, position, position, position, position);
    }
}
