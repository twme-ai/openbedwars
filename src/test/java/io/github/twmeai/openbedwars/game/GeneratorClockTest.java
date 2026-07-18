package io.github.twmeai.openbedwars.game;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class GeneratorClockTest {
    @Test
    void countsDownAndResetsAfterSpawning() {
        GeneratorClock clock = new GeneratorClock();

        assertEquals(0, clock.advance(2.0, false));
        assertEquals(1, clock.secondsUntilNext(2.0));
        assertEquals(1, clock.advance(2.0, false));
        assertEquals(2, clock.secondsUntilNext(2.0));
    }

    @Test
    void pausesWhileFullAndSupportsFractionalForgePeriods() {
        GeneratorClock clock = new GeneratorClock();

        assertEquals(0, clock.advance(4.0, true));
        assertEquals(0.0, clock.progress());
        assertEquals(2, clock.advance(0.4, false));
        assertEquals(0.2, clock.progress(), 1.0E-9);
    }

    @Test
    void rejectsInvalidPeriods() {
        GeneratorClock clock = new GeneratorClock();
        assertThrows(IllegalArgumentException.class, () -> clock.advance(0, false));
        assertThrows(IllegalArgumentException.class, () -> clock.secondsUntilNext(Double.NaN));
    }
}
