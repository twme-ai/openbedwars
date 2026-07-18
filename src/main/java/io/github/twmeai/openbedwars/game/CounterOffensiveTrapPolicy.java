package io.github.twmeai.openbedwars.game;

final class CounterOffensiveTrapPolicy {
    static final int DURATION_TICKS = 15 * 20;
    static final int SPEED_AMPLIFIER = 0;
    static final int JUMP_BOOST_AMPLIFIER = 1;
    private static final double BASE_RADIUS_SQUARED = 7 * 7;

    private CounterOffensiveTrapPolicy() {
    }

    static boolean appliesToAlly(boolean active, double distanceSquaredFromBase) {
        return active
                && Double.isFinite(distanceSquaredFromBase)
                && distanceSquaredFromBase >= 0
                && distanceSquaredFromBase <= BASE_RADIUS_SQUARED;
    }
}
