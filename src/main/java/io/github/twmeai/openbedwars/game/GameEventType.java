package io.github.twmeai.openbedwars.game;

public enum GameEventType {
    DIAMOND_II,
    EMERALD_II,
    DIAMOND_III,
    EMERALD_III,
    BED_DESTRUCTION,
    SUDDEN_DEATH,
    GAME_END;

    public String translationKey() {
        return "event." + name().toLowerCase(java.util.Locale.ROOT);
    }
}
