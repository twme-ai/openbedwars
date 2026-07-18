package io.github.twmeai.openbedwars.statistics;

public final class BedWarsLevel {
    private static final int LEVELS_PER_PRESTIGE = 100;
    private static final int PRESTIGE_EXPERIENCE = 487_000;
    private static final int[] EARLY_LEVEL_COSTS = {500, 1_000, 2_000, 3_500};
    private static final int STANDARD_LEVEL_COST = 5_000;

    private BedWarsLevel() {
    }

    public static int fromExperience(long totalExperience) {
        if (totalExperience < 0) {
            throw new IllegalArgumentException("Experience must not be negative");
        }
        long prestige = totalExperience / PRESTIGE_EXPERIENCE;
        long remaining = totalExperience % PRESTIGE_EXPERIENCE;
        int level = Math.toIntExact(prestige * LEVELS_PER_PRESTIGE);
        for (int cost : EARLY_LEVEL_COSTS) {
            if (remaining < cost) return level;
            remaining -= cost;
            level++;
        }
        return level + (int) Math.min(96, remaining / STANDARD_LEVEL_COST);
    }

    public static int experienceForNextLevel(long totalExperience) {
        if (totalExperience < 0) {
            throw new IllegalArgumentException("Experience must not be negative");
        }
        long remaining = totalExperience % PRESTIGE_EXPERIENCE;
        for (int cost : EARLY_LEVEL_COSTS) {
            if (remaining < cost) return (int) (cost - remaining);
            remaining -= cost;
        }
        return STANDARD_LEVEL_COST - (int) (remaining % STANDARD_LEVEL_COST);
    }
}
