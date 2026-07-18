package io.github.twmeai.openbedwars.game;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ForgeUpgradePolicyTest {
    @Test
    void followsTheFourTierResourceProgression() {
        assertEquals(1.0, ForgeUpgradePolicy.ironGoldMultiplier(0));
        assertEquals(1.5, ForgeUpgradePolicy.ironGoldMultiplier(1));
        assertEquals(2.0, ForgeUpgradePolicy.ironGoldMultiplier(2));
        assertEquals(2.0, ForgeUpgradePolicy.ironGoldMultiplier(3));
        assertEquals(3.0, ForgeUpgradePolicy.ironGoldMultiplier(4));
    }

    @Test
    void emeraldForgeAddsEmeraldsWithoutAnotherIronGoldTier() {
        assertFalse(ForgeUpgradePolicy.generatesEmeralds(2));
        assertTrue(ForgeUpgradePolicy.generatesEmeralds(3));
        assertTrue(ForgeUpgradePolicy.generatesEmeralds(4));
        assertEquals(
                ForgeUpgradePolicy.ironGoldMultiplier(2),
                ForgeUpgradePolicy.ironGoldMultiplier(3)
        );
    }

    @Test
    void rejectsLevelsOutsideTheUpgradeTree() {
        assertThrows(IllegalArgumentException.class, () -> ForgeUpgradePolicy.ironGoldMultiplier(-1));
        assertThrows(IllegalArgumentException.class, () -> ForgeUpgradePolicy.generatesEmeralds(5));
    }
}
