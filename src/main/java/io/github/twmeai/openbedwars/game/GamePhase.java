package io.github.twmeai.openbedwars.game;

public enum GamePhase {
    WAITING,
    STARTING,
    RUNNING,
    ENDING;

    public String translationKey() {
        return "phase." + name().toLowerCase(java.util.Locale.ROOT);
    }
}
