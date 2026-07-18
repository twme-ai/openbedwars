package io.github.twmeai.openbedwars.statistics;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class BedWarsLevelTest {
    @Test
    void followsEarlyLevelAndPrestigeExperienceCurve() {
        assertEquals(0, BedWarsLevel.fromExperience(0));
        assertEquals(1, BedWarsLevel.fromExperience(500));
        assertEquals(2, BedWarsLevel.fromExperience(1_500));
        assertEquals(3, BedWarsLevel.fromExperience(3_500));
        assertEquals(4, BedWarsLevel.fromExperience(7_000));
        assertEquals(5, BedWarsLevel.fromExperience(12_000));
        assertEquals(100, BedWarsLevel.fromExperience(487_000));
        assertEquals(101, BedWarsLevel.fromExperience(487_500));
    }

    @Test
    void reportsExperienceRemainingInCurrentLevel() {
        assertEquals(500, BedWarsLevel.experienceForNextLevel(0));
        assertEquals(1, BedWarsLevel.experienceForNextLevel(499));
        assertEquals(1_000, BedWarsLevel.experienceForNextLevel(500));
        assertThrows(IllegalArgumentException.class, () -> BedWarsLevel.fromExperience(-1));
    }
}
