package io.github.twmeai.openbedwars.game;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CounterOffensiveTrapPolicyTest {
    @Test
    void usesTheClassicFifteenSecondEffectLevels() {
        assertEquals(300, CounterOffensiveTrapPolicy.DURATION_TICKS);
        assertEquals(0, CounterOffensiveTrapPolicy.SPEED_AMPLIFIER);
        assertEquals(1, CounterOffensiveTrapPolicy.JUMP_BOOST_AMPLIFIER);
    }

    @Test
    void includesActiveAlliesThroughTheSevenBlockBoundary() {
        assertTrue(CounterOffensiveTrapPolicy.appliesToAlly(true, 0));
        assertTrue(CounterOffensiveTrapPolicy.appliesToAlly(true, 49));
    }

    @Test
    void excludesDistantInactiveAndInvalidCandidates() {
        assertFalse(CounterOffensiveTrapPolicy.appliesToAlly(true, 49.0001));
        assertFalse(CounterOffensiveTrapPolicy.appliesToAlly(false, 0));
        assertFalse(CounterOffensiveTrapPolicy.appliesToAlly(true, -1));
        assertFalse(CounterOffensiveTrapPolicy.appliesToAlly(true, Double.NaN));
        assertFalse(CounterOffensiveTrapPolicy.appliesToAlly(true, Double.POSITIVE_INFINITY));
    }
}
