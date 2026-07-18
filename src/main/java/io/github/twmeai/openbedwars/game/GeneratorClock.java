package io.github.twmeai.openbedwars.game;

final class GeneratorClock {
    private double progress;

    int advance(double period, boolean full) {
        validatePeriod(period);
        if (full) return 0;
        progress += 1.0;
        int spawns = 0;
        while (progress + 1.0E-9 >= period) {
            progress -= period;
            spawns++;
        }
        return spawns;
    }

    int secondsUntilNext(double period) {
        validatePeriod(period);
        return Math.max(1, (int) Math.ceil(period - progress - 1.0E-9));
    }

    double progress() {
        return progress;
    }

    private void validatePeriod(double period) {
        if (!Double.isFinite(period) || period <= 0) {
            throw new IllegalArgumentException("Generator period must be positive and finite");
        }
    }
}
