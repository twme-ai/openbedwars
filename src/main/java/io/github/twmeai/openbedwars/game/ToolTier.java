package io.github.twmeai.openbedwars.game;

public enum ToolTier {
    NONE,
    WOOD,
    IRON,
    GOLD,
    DIAMOND;

    public ToolTier downgrade() {
        return switch (this) {
            case DIAMOND -> GOLD;
            case GOLD -> IRON;
            case IRON -> WOOD;
            case WOOD, NONE -> this;
        };
    }
}
