package io.github.twmeai.openbedwars.spectator;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SpectatorTargetPolicyTest {
    @Test
    void acceptsOnlyConnectedLivingActivePlayers() {
        assertTrue(SpectatorTargetPolicy.isEligible(false, false, false, true, false));

        assertFalse(SpectatorTargetPolicy.isEligible(true, false, false, true, false));
        assertFalse(SpectatorTargetPolicy.isEligible(false, true, false, true, false));
        assertFalse(SpectatorTargetPolicy.isEligible(false, false, true, true, false));
        assertFalse(SpectatorTargetPolicy.isEligible(false, false, false, false, false));
        assertFalse(SpectatorTargetPolicy.isEligible(false, false, false, true, true));
    }
}
