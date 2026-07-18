package io.github.twmeai.openbedwars.game;

import io.github.twmeai.openbedwars.config.TeamDefinition;

import java.util.ArrayDeque;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Queue;
import java.util.Set;
import java.util.UUID;

public final class TeamState {
    private final TeamDefinition definition;
    private final Set<UUID> members = new LinkedHashSet<>();
    private final Queue<TrapType> traps = new ArrayDeque<>();
    private boolean bedAlive = true;
    private boolean sharpness;
    private int protection;
    private int haste;
    private int forge;
    private boolean healPool;
    private boolean dragonBuff;

    public TeamState(TeamDefinition definition) {
        this.definition = Objects.requireNonNull(definition, "definition");
    }

    public TeamDefinition definition() { return definition; }
    public TeamColor color() { return definition.color(); }
    public Set<UUID> members() { return Collections.unmodifiableSet(members); }
    public int size() { return members.size(); }
    public boolean bedAlive() { return bedAlive; }
    public boolean sharpness() { return sharpness; }
    public int protection() { return protection; }
    public int haste() { return haste; }
    public int forge() { return forge; }
    public boolean healPool() { return healPool; }
    public boolean dragonBuff() { return dragonBuff; }
    public Queue<TrapType> traps() { return traps; }

    public void addMember(UUID playerId) { members.add(playerId); }
    public void removeMember(UUID playerId) { members.remove(playerId); }
    public void destroyBed() { bedAlive = false; }
    public void restoreBed() { bedAlive = true; }
    public void sharpness(boolean value) { sharpness = value; }
    public void protection(int value) { protection = value; }
    public void haste(int value) { haste = value; }
    public void forge(int value) { forge = value; }
    public void healPool(boolean value) { healPool = value; }
    public void dragonBuff(boolean value) { dragonBuff = value; }

    public boolean isAlive(java.util.Map<UUID, PlayerState> players) {
        return members.stream()
                .map(players::get)
                .anyMatch(state -> state != null && !state.eliminated());
    }

    public void reset() {
        members.clear();
        traps.clear();
        bedAlive = true;
        sharpness = false;
        protection = 0;
        haste = 0;
        forge = 0;
        healPool = false;
        dragonBuff = false;
    }
}
