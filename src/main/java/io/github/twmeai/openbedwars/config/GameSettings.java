package io.github.twmeai.openbedwars.config;

import io.github.twmeai.openbedwars.game.EventSchedule;
import io.github.twmeai.openbedwars.game.GameEventType;
import io.github.twmeai.openbedwars.game.ResourceType;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.EnumMap;

public record GameSettings(
        int minimumPlayers,
        int countdownSeconds,
        int respawnSeconds,
        int endingSeconds,
        int experiencePerMinute,
        int winBonusExperience,
        EventSchedule eventSchedule,
        GeneratorPeriods generatorPeriods
) {
    public GameSettings {
        if (minimumPlayers < 2 || countdownSeconds < 1 || respawnSeconds < 0 || endingSeconds < 1
                || experiencePerMinute < 0 || winBonusExperience < 0) {
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
                config.getInt("ending-seconds", 10),
                config.getInt("progression.experience-per-minute", 25),
                config.getInt("progression.win-bonus-experience", 100),
                new EventSchedule(events),
                new GeneratorPeriods(
                        config.getDouble("generator-periods.iron", 1.0),
                        config.getDouble("generator-periods.gold", 4.0),
                        tiered(config, ResourceType.DIAMOND, 30.0, 23.0, 12.0),
                        tiered(config, ResourceType.EMERALD, 65.0, 50.0, 35.0)
                )
        );
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
}
