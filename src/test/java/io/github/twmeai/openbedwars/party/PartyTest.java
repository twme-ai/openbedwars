package io.github.twmeai.openbedwars.party;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PartyTest {
    @Test
    void keepsStableMembershipOrderForLeaderTransfer() {
        UUID leader = UUID.randomUUID();
        UUID second = UUID.randomUUID();
        UUID third = UUID.randomUUID();
        Party party = new Party(leader, "Leader");
        party.add(second, "Second");
        party.add(third, "Third");

        party.remove(leader);
        party.leader(party.firstMember());

        assertEquals(second, party.leader());
        assertEquals(2, party.size());
        assertTrue(party.contains(third));
        assertFalse(party.contains(leader));
    }
}
