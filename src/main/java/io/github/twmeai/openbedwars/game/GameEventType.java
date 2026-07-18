package io.github.twmeai.openbedwars.game;

public enum GameEventType {
    DIAMOND_II("Diamond Generators II"),
    EMERALD_II("Emerald Generators II"),
    DIAMOND_III("Diamond Generators III"),
    EMERALD_III("Emerald Generators III"),
    BED_DESTRUCTION("Bed Destruction"),
    SUDDEN_DEATH("Sudden Death"),
    GAME_END("Game End");

    private final String displayName;

    GameEventType(String displayName) {
        this.displayName = displayName;
    }

    public String displayName() {
        return displayName;
    }
}
