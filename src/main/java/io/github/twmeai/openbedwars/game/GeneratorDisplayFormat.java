package io.github.twmeai.openbedwars.game;

import java.util.Locale;

final class GeneratorDisplayFormat {
    private GeneratorDisplayFormat() {
    }

    static String countdown(int seconds) {
        if (seconds < 0) throw new IllegalArgumentException("seconds must not be negative");
        return String.format(Locale.ROOT, "%02d:%02d", seconds / 60, seconds % 60);
    }

    static String romanTier(int tier) {
        return switch (tier) {
            case 1 -> "I";
            case 2 -> "II";
            case 3 -> "III";
            default -> throw new IllegalArgumentException("tier must be between 1 and 3");
        };
    }
}
