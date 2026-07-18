package io.github.twmeai.openbedwars.config;

import io.github.twmeai.openbedwars.game.ResourceType;
import io.github.twmeai.openbedwars.game.TeamColor;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class ArenaConfigRepository {
    private final JavaPlugin plugin;
    private final File file;

    public ArenaConfigRepository(JavaPlugin plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "arenas.yml");
    }

    public Map<String, ArenaDefinition> load() {
        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(file);
        ConfigurationSection root = yaml.getConfigurationSection("arenas");
        if (root == null) {
            plugin.getLogger().warning("No 'arenas' section exists in arenas.yml");
            return Map.of();
        }

        Map<String, ArenaDefinition> definitions = new LinkedHashMap<>();
        for (String key : root.getKeys(false)) {
            ConfigurationSection section = root.getConfigurationSection(key);
            if (section == null || !section.getBoolean("enabled", false)) {
                continue;
            }
            try {
                ArenaDefinition definition = loadArena(key.toLowerCase(Locale.ROOT), section);
                definitions.put(definition.key(), definition);
            } catch (IllegalArgumentException exception) {
                plugin.getLogger().severe("Could not load arena '" + key + "': " + exception.getMessage());
            }
        }
        return Map.copyOf(definitions);
    }

    private ArenaDefinition loadArena(String key, ConfigurationSection section) {
        String world = requiredString(section, "world");
        Map<TeamColor, TeamDefinition> teams = loadTeams(section);
        Map<ResourceType, List<Position>> generators = loadGenerators(section);
        return new ArenaDefinition(
                key,
                section.getString("display-name", key),
                world,
                section.getInt("min-players", 2),
                section.getInt("max-players", teams.size()),
                section.getInt("players-per-team", 1),
                section.getInt("void-kill-y", 0),
                section.getInt("max-build-y", 180),
                new ArenaProtection(
                        section.getInt("protection.spawn-radius", 5),
                        section.getInt("protection.item-shop-radius", 1),
                        section.getInt("protection.upgrade-shop-radius", 1),
                        section.getInt("protection.generator-radius", 1)
                ),
                position(section, "lobby"),
                position(section, "spectator"),
                teams,
                generators
        );
    }

    private Map<TeamColor, TeamDefinition> loadTeams(ConfigurationSection arena) {
        ConfigurationSection section = requiredSection(arena, "teams");
        EnumMap<TeamColor, TeamDefinition> teams = new EnumMap<>(TeamColor.class);
        for (String key : section.getKeys(false)) {
            TeamColor color = TeamColor.fromKey(key)
                    .orElseThrow(() -> new IllegalArgumentException("Unknown team color '" + key + "'"));
            ConfigurationSection team = requiredSection(section, key);
            teams.put(color, new TeamDefinition(
                    color,
                    position(team, "spawn"),
                    position(team, "bed-head"),
                    position(team, "bed-foot"),
                    position(team, "item-shop"),
                    position(team, "upgrade-shop"),
                    position(team, "forge")
            ));
        }
        return teams;
    }

    private Map<ResourceType, List<Position>> loadGenerators(ConfigurationSection arena) {
        EnumMap<ResourceType, List<Position>> generators = new EnumMap<>(ResourceType.class);
        ConfigurationSection section = arena.getConfigurationSection("generators");
        if (section == null) {
            return generators;
        }
        for (String key : section.getKeys(false)) {
            ResourceType type;
            try {
                type = ResourceType.valueOf(key.toUpperCase(Locale.ROOT));
            } catch (IllegalArgumentException exception) {
                throw new IllegalArgumentException("Unknown generator type '" + key + "'");
            }
            List<Position> positions = new ArrayList<>();
            List<Map<?, ?>> maps = section.getMapList(key);
            for (Map<?, ?> map : maps) {
                positions.add(position(map, "generators." + key));
            }
            generators.put(type, positions);
        }
        return generators;
    }

    private Position position(ConfigurationSection parent, String path) {
        return parsePosition(requiredSection(parent, path), path);
    }

    private Position parsePosition(ConfigurationSection section, String path) {
        return new Position(
                requiredNumber(section, "x", path).doubleValue(),
                requiredNumber(section, "y", path).doubleValue(),
                requiredNumber(section, "z", path).doubleValue(),
                (float) section.getDouble("yaw", 0.0),
                (float) section.getDouble("pitch", 0.0)
        );
    }

    private Position position(Map<?, ?> map, String path) {
        return new Position(
                requiredNumber(map, "x", path).doubleValue(),
                requiredNumber(map, "y", path).doubleValue(),
                requiredNumber(map, "z", path).doubleValue(),
                number(map.get("yaw"), 0.0).floatValue(),
                number(map.get("pitch"), 0.0).floatValue()
        );
    }

    private ConfigurationSection requiredSection(ConfigurationSection parent, String path) {
        ConfigurationSection section = parent.getConfigurationSection(path);
        if (section == null) {
            throw new IllegalArgumentException("Missing section '" + path + "'");
        }
        return section;
    }

    private String requiredString(ConfigurationSection section, String path) {
        String value = section.getString(path);
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Missing value '" + path + "'");
        }
        return value;
    }

    private Number requiredNumber(ConfigurationSection section, String key, String path) {
        Object value = section.get(key);
        if (!(value instanceof Number number)) {
            throw new IllegalArgumentException("Missing number '" + path + "." + key + "'");
        }
        return number;
    }

    private Number requiredNumber(Map<?, ?> map, String key, String path) {
        Object value = map.get(key);
        if (!(value instanceof Number number)) {
            throw new IllegalArgumentException("Missing number '" + path + "." + key + "'");
        }
        return number;
    }

    private Number number(Object value, Number fallback) {
        return value instanceof Number number ? number : fallback;
    }
}
