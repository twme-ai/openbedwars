package io.github.twmeai.openbedwars.config;

import io.github.twmeai.openbedwars.game.EventSchedule;
import io.github.twmeai.openbedwars.game.GameEventType;
import io.github.twmeai.openbedwars.game.ResourceType;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.EnumMap;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public record GameSettings(
        int minimumPlayers,
        int countdownSeconds,
        int respawnSeconds,
        int respawnProtectionSeconds,
        int endingSeconds,
        int reconnectGraceSeconds,
        int experiencePerMinute,
        int winBonusExperience,
        EventSchedule eventSchedule,
        GeneratorPeriods generatorPeriods,
        GeneratorDisplaySettings generatorDisplays,
        GeneratorSplitSettings generatorSplitting,
        FireballSettings fireballs
) {
    public GameSettings {
        if (minimumPlayers < 2 || countdownSeconds < 1 || respawnSeconds < 0 || respawnProtectionSeconds < 0
                || endingSeconds < 1
                || reconnectGraceSeconds < 0 || experiencePerMinute < 0 || winBonusExperience < 0) {
            throw new IllegalArgumentException("Invalid game timing configuration");
        }
    }

    public static GameSettings from(FileConfiguration config) {
        EnumMap<GameEventType, Integer> events = new EnumMap<>(GameEventType.class);
        events.put(GameEventType.DIAMOND_II, config.getInt("events.diamond-ii", 360));
        events.put(GameEventType.EMERALD_II, config.getInt("events.emerald-ii", 720));
        events.put(GameEventType.DIAMOND_III, config.getInt("events.diamond-iii", 1080));
        events.put(GameEventType.EMERALD_III, config.getInt("events.emerald-iii", 1440));
        events.put(GameEventType.BED_DESTRUCTION, config.getInt("events.bed-destruction", 1800));
        events.put(GameEventType.SUDDEN_DEATH, config.getInt("events.sudden-death", 2400));
        events.put(GameEventType.GAME_END, config.getInt("events.game-end", 3000));

        return new GameSettings(
                config.getInt("minimum-players", 2),
                config.getInt("countdown-seconds", 20),
                config.getInt("respawn-seconds", 5),
                config.getInt("respawn-protection-seconds", 3),
                config.getInt("ending-seconds", 10),
                config.getInt("reconnect-grace-seconds", 120),
                config.getInt("progression.experience-per-minute", 25),
                config.getInt("progression.win-bonus-experience", 100),
                new EventSchedule(events),
                new GeneratorPeriods(
                        config.getDouble("generator-periods.iron", 1.0),
                        config.getDouble("generator-periods.gold", 4.0),
                        tiered(config, ResourceType.DIAMOND, 30.0, 23.0, 12.0),
                        tiered(config, ResourceType.EMERALD, 65.0, 50.0, 35.0)
                ),
                new GeneratorDisplaySettings(
                        config.getBoolean("generator-displays.enabled", true),
                        config.getDouble("generator-displays.item-height", 1.35),
                        config.getDouble("generator-displays.text-height", 2.35)
                ),
                new GeneratorSplitSettings(
                        config.getBoolean("generator-splitting.enabled", true),
                        config.getDouble("generator-splitting.radius", 3.0),
                        splitResources(config)
                ),
                new FireballSettings(
                        config.getInt("fireballs.cooldown-ticks", 10),
                        config.getInt("fireballs.slowness-amplifier", 3)
                )
        );
    }

    private static Set<ResourceType> splitResources(FileConfiguration config) {
        List<String> configured = config.isList("generator-splitting.resources")
                ? config.getStringList("generator-splitting.resources")
                : List.of("iron", "gold");
        java.util.EnumSet<ResourceType> resources = java.util.EnumSet.noneOf(ResourceType.class);
        for (String value : configured) {
            try {
                resources.add(ResourceType.valueOf(value.toUpperCase(Locale.ROOT)));
            } catch (IllegalArgumentException exception) {
                throw new IllegalArgumentException("Unknown generator splitting resource: " + value);
            }
        }
        return resources;
    }

    private static TieredPeriod tiered(
            FileConfiguration config,
            ResourceType type,
            double tierOne,
            double tierTwo,
            double tierThree
    ) {
        String root = "generator-periods." + type.name().toLowerCase(java.util.Locale.ROOT);
        return new TieredPeriod(
                config.getDouble(root + ".tier-1", tierOne),
                config.getDouble(root + ".tier-2", tierTwo),
                config.getDouble(root + ".tier-3", tierThree)
        );
    }

    public record GeneratorPeriods(double iron, double gold, TieredPeriod diamond, TieredPeriod emerald) {
        public GeneratorPeriods {
            if (iron <= 0 || gold <= 0) {
                throw new IllegalArgumentException("Generator periods must be positive");
            }
        }

        public double period(ResourceType type, int tier) {
            return switch (type) {
                case IRON -> iron;
                case GOLD -> gold;
                case DIAMOND -> diamond.atTier(tier);
                case EMERALD -> emerald.atTier(tier);
            };
        }
    }

    public record TieredPeriod(double tierOne, double tierTwo, double tierThree) {
        public TieredPeriod {
            if (tierOne <= 0 || tierTwo <= 0 || tierThree <= 0) {
                throw new IllegalArgumentException("Generator periods must be positive");
            }
        }

        public double atTier(int tier) {
            return tier <= 1 ? tierOne : tier == 2 ? tierTwo : tierThree;
        }
    }

    public record GeneratorDisplaySettings(boolean enabled, double itemHeight, double textHeight) {
        public GeneratorDisplaySettings {
            if (!Double.isFinite(itemHeight) || !Double.isFinite(textHeight)
                    || itemHeight < 0 || itemHeight > 10 || textHeight < 0 || textHeight > 10) {
                throw new IllegalArgumentException("Generator display heights must be between 0 and 10");
            }
        }
    }

    public record GeneratorSplitSettings(boolean enabled, double radius, Set<ResourceType> resources) {
        public GeneratorSplitSettings {
            if (!Double.isFinite(radius) || radius <= 0 || radius > 16) {
                throw new IllegalArgumentException("Generator splitting radius must be greater than 0 and at most 16");
            }
            resources = Set.copyOf(resources);
        }

        public boolean splits(ResourceType type) {
            return enabled && resources.contains(type);
        }
    }

    public record FireballSettings(int cooldownTicks, int slownessAmplifier) {
        public FireballSettings {
            if (cooldownTicks < 0 || cooldownTicks > 1_200
                    || slownessAmplifier < 0 || slownessAmplifier > 255) {
                throw new IllegalArgumentException("Invalid fireball cooldown configuration");
            }
        }
    }
}
