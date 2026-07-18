package io.github.twmeai.openbedwars.party;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

final class Party {
    private UUID leader;
    private final LinkedHashMap<UUID, String> members = new LinkedHashMap<>();

    Party(UUID leader, String leaderName) {
        this.leader = leader;
        members.put(leader, leaderName);
    }

    UUID leader() { return leader; }
    void leader(UUID leader) { this.leader = leader; }
    Map<UUID, String> members() { return Map.copyOf(members); }
    int size() { return members.size(); }
    boolean contains(UUID playerId) { return members.containsKey(playerId); }
    void add(UUID playerId, String name) { members.put(playerId, name); }
    void remove(UUID playerId) { members.remove(playerId); }
    String name(UUID playerId) { return members.get(playerId); }
    UUID firstMember() { return members.keySet().iterator().next(); }

    UUID transferLeadershipFrom(UUID departingLeader, Set<UUID> onlineMembers) {
        if (!leader.equals(departingLeader)) return null;
        for (UUID member : members.keySet()) {
            if (!member.equals(departingLeader) && onlineMembers.contains(member)) {
                leader = member;
                return member;
            }
        }
        return null;
    }
}
