package io.github.twmeai.openbedwars.party;

import org.junit.jupiter.api.Test;

import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
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

    @Test
    void transfersDisconnectedLeaderToFirstOnlineMemberWithoutRemovingThem() {
        UUID leader = UUID.randomUUID();
        UUID second = UUID.randomUUID();
        UUID third = UUID.randomUUID();
        Party party = new Party(leader, "Leader");
        party.add(second, "Second");
        party.add(third, "Third");

        UUID successor = party.transferLeadershipFrom(leader, Set.of(second, third));

        assertEquals(second, successor);
        assertEquals(second, party.leader());
        assertEquals(3, party.size());
        assertTrue(party.contains(leader));
    }

    @Test
    void skipsOfflineMembersWhenTransferringDisconnectedLeader() {
        UUID leader = UUID.randomUUID();
        UUID offlineSecond = UUID.randomUUID();
        UUID onlineThird = UUID.randomUUID();
        Party party = new Party(leader, "Leader");
        party.add(offlineSecond, "OfflineSecond");
        party.add(onlineThird, "OnlineThird");

        UUID successor = party.transferLeadershipFrom(leader, Set.of(onlineThird));

        assertEquals(onlineThird, successor);
        assertEquals(onlineThird, party.leader());
    }

    @Test
    void retainsLeaderWhenNoOtherMemberIsOnline() {
        UUID leader = UUID.randomUUID();
        Party party = new Party(leader, "Leader");
        party.add(UUID.randomUUID(), "OfflineMember");

        UUID successor = party.transferLeadershipFrom(leader, Set.of());

        assertNull(successor);
        assertEquals(leader, party.leader());
    }

    @Test
    void doesNotTransferLeadershipForAnOrdinaryMemberDisconnect() {
        UUID leader = UUID.randomUUID();
        UUID member = UUID.randomUUID();
        Party party = new Party(leader, "Leader");
        party.add(member, "Member");

        UUID successor = party.transferLeadershipFrom(member, Set.of(leader));

        assertNull(successor);
        assertEquals(leader, party.leader());
    }
}
