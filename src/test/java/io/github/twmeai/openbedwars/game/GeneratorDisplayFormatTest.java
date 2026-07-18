package io.github.twmeai.openbedwars.game;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class GeneratorDisplayFormatTest {
    @Test
    void formatsTierAndCountdownText() {
        assertEquals("I", GeneratorDisplayFormat.romanTier(1));
        assertEquals("II", GeneratorDisplayFormat.romanTier(2));
        assertEquals("III", GeneratorDisplayFormat.romanTier(3));
        assertEquals("00:00", GeneratorDisplayFormat.countdown(0));
        assertEquals("01:05", GeneratorDisplayFormat.countdown(65));
    }

    @Test
    void rejectsUnsupportedValues() {
        assertThrows(IllegalArgumentException.class, () -> GeneratorDisplayFormat.romanTier(4));
        assertThrows(IllegalArgumentException.class, () -> GeneratorDisplayFormat.countdown(-1));
    }
}
