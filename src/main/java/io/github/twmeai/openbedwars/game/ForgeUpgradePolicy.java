package io.github.twmeai.openbedwars.game;

final class ForgeUpgradePolicy {
    private ForgeUpgradePolicy() {
    }

    static double ironGoldMultiplier(int tier) {
        validateTier(tier);
        return switch (tier) {
            case 0 -> 1.0;
            case 1 -> 1.5;
            case 2, 3 -> 2.0;
            case 4 -> 3.0;
            default -> throw new IllegalStateException("Unexpected Forge tier " + tier);
        };
    }

    static boolean generatesEmeralds(int tier) {
        validateTier(tier);
        return tier >= 3;
    }

    private static void validateTier(int tier) {
        if (tier < 0 || tier > 4) {
            throw new IllegalArgumentException("Forge tier must be between 0 and 4");
        }
    }
}
