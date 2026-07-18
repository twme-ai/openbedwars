package io.github.twmeai.openbedwars.game;

import java.util.Objects;
import java.util.UUID;

public final class PlayerState {
    private final UUID playerId;
    private final String playerName;
    private final PlayerSnapshot snapshot;
    private TeamColor team;
    private boolean eliminated;
    private boolean respawning;
    private int kills;
    private int finalKills;
    private int bedsBroken;
    private int deaths;
    private int finalDeaths;
    private ArmorTier armorTier = ArmorTier.LEATHER;
    private ToolTier pickaxeTier = ToolTier.NONE;
    private ToolTier axeTier = ToolTier.NONE;
    private boolean shears;

    public PlayerState(UUID playerId, String playerName, PlayerSnapshot snapshot, TeamColor team) {
        this.playerId = Objects.requireNonNull(playerId, "playerId");
        this.playerName = Objects.requireNonNull(playerName, "playerName");
        this.snapshot = Objects.requireNonNull(snapshot, "snapshot");
        this.team = Objects.requireNonNull(team, "team");
    }

    public UUID playerId() { return playerId; }
    public String playerName() { return playerName; }
    public PlayerSnapshot snapshot() { return snapshot; }
    public TeamColor team() { return team; }
    public boolean eliminated() { return eliminated; }
    public boolean respawning() { return respawning; }
    public int kills() { return kills; }
    public int finalKills() { return finalKills; }
    public int bedsBroken() { return bedsBroken; }
    public int deaths() { return deaths; }
    public int finalDeaths() { return finalDeaths; }
    public ArmorTier armorTier() { return armorTier; }
    public ToolTier pickaxeTier() { return pickaxeTier; }
    public ToolTier axeTier() { return axeTier; }
    public boolean hasShears() { return shears; }

    public void team(TeamColor team) { this.team = Objects.requireNonNull(team, "team"); }
    public void eliminated(boolean eliminated) { this.eliminated = eliminated; }
    public void respawning(boolean respawning) { this.respawning = respawning; }
    public void addKill() { kills++; }
    public void addFinalKill() { finalKills++; }
    public void addBedBroken() { bedsBroken++; }
    public void addDeath() { deaths++; }
    public void addFinalDeath() { finalDeaths++; }
    public void armorTier(ArmorTier armorTier) { this.armorTier = armorTier; }
    public void pickaxeTier(ToolTier tier) { pickaxeTier = tier; }
    public void axeTier(ToolTier tier) { axeTier = tier; }
    public void shears(boolean shears) { this.shears = shears; }

    public void downgradeTools() {
        pickaxeTier = pickaxeTier.downgrade();
        axeTier = axeTier.downgrade();
    }
}
