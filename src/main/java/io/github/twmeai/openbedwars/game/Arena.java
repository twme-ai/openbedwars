package io.github.twmeai.openbedwars.game;

import io.github.twmeai.openbedwars.OpenBedWarsPlugin;
import io.github.twmeai.openbedwars.config.ArenaDefinition;
import io.github.twmeai.openbedwars.config.GameSettings;
import io.github.twmeai.openbedwars.config.Position;
import io.github.twmeai.openbedwars.message.MessageService;
import io.github.twmeai.openbedwars.statistics.PlayerIdentity;
import io.github.twmeai.openbedwars.statistics.StatsDelta;
import io.papermc.paper.registry.RegistryAccess;
import io.papermc.paper.registry.RegistryKey;
import io.papermc.paper.registry.keys.DamageTypeKeys;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.title.Title;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.GameMode;
import org.bukkit.GameRules;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.data.BlockData;
import org.bukkit.damage.DamageSource;
import org.bukkit.damage.DamageType;
import org.bukkit.entity.EnderDragon;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Display;
import org.bukkit.entity.Item;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.entity.Player;
import org.bukkit.entity.TextDisplay;
import org.bukkit.entity.Villager;
import org.bukkit.inventory.EntityEquipment;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.util.Transformation;
import org.bukkit.util.Vector;
import org.joml.AxisAngle4f;
import org.joml.Vector3f;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

public final class Arena {
    private static final DamageType VOID_DAMAGE_TYPE = RegistryAccess.registryAccess()
            .getRegistry(RegistryKey.DAMAGE_TYPE)
            .getOrThrow(DamageTypeKeys.OUT_OF_WORLD);

    private final OpenBedWarsPlugin plugin;
    private final MessageService messages;
    private final ArenaDefinition definition;
    private final GameSettings settings;
    private final World world;
    private final BiConsumer<UUID, PlayerSnapshot> playerRelease;
    private final KitService kits = new KitService();
    private final ScoreboardService scoreboards;
    private final NamespacedKey generatorResourceKey;
    private final Map<UUID, PlayerState> players = new LinkedHashMap<>();
    private final EnumMap<TeamColor, TeamState> teams = new EnumMap<>(TeamColor.class);
    private final Map<String, GeneratorClock> generatorClocks = new HashMap<>();
    private final Map<String, GeneratorDisplay> generatorDisplays = new HashMap<>();
    private final Map<BlockKey, BlockState> changedBlocks = new LinkedHashMap<>();
    private final Set<BlockKey> placedBlocks = new HashSet<>();
    private final List<Entity> spawnedEntities = new ArrayList<>();
    private final Set<BukkitTask> respawnTasks = new HashSet<>();
    private final Map<UUID, BukkitTask> disconnectTasks = new HashMap<>();
    private final Map<String, Long> trapCooldownUntil = new HashMap<>();
    private final Map<UUID, CombatHit> lastHits = new HashMap<>();
    private final Map<UUID, Long> trapImmuneUntil = new HashMap<>();
    private final RespawnProtectionTracker respawnProtection = new RespawnProtectionTracker();
    private final Set<UUID> invisiblePlayers = new HashSet<>();

    private GamePhase phase = GamePhase.WAITING;
    private int countdown;
    private int elapsedSeconds;
    private int endingCountdown;
    private boolean statsRecorded;
    private Boolean previousImmediateRespawn;

    public Arena(
            OpenBedWarsPlugin plugin,
            ArenaDefinition definition,
            GameSettings settings,
            World world,
            BiConsumer<UUID, PlayerSnapshot> playerRelease
    ) {
        this.plugin = plugin;
        this.messages = plugin.messages();
        this.definition = definition;
        this.settings = settings;
        this.world = world;
        this.playerRelease = playerRelease;
        this.scoreboards = new ScoreboardService(messages);
        this.generatorResourceKey = new NamespacedKey(plugin, "generator_resource");
        definition.teams().values().stream()
                .sorted(Comparator.comparing(team -> team.color().ordinal()))
                .forEach(team -> teams.put(team.color(), new TeamState(team)));
    }

    public String key() { return definition.key(); }
    public String displayName() { return definition.displayName(); }
    public ArenaDefinition definition() { return definition; }
    public GamePhase phase() { return phase; }
    public int playerCount() { return players.size(); }
    public int maxPlayers() { return definition.maxPlayers(); }
    public int elapsedSeconds() { return elapsedSeconds; }
    public GameSettings settings() { return settings; }
    public World world() { return world; }
    public Map<UUID, PlayerState> players() { return Map.copyOf(players); }
    public Map<TeamColor, TeamState> teams() { return Map.copyOf(teams); }

    public Optional<PlayerState> playerState(UUID playerId) {
        return Optional.ofNullable(players.get(playerId));
    }

    public Optional<TeamState> teamOf(UUID playerId) {
        PlayerState state = players.get(playerId);
        return state == null ? Optional.empty() : Optional.ofNullable(teams.get(state.team()));
    }

    public TeamChangeResult changeTeam(Player player, TeamColor target) {
        if (phase != GamePhase.WAITING && phase != GamePhase.STARTING) return TeamChangeResult.LOCKED;
        PlayerState state = players.get(player.getUniqueId());
        TeamState newTeam = teams.get(target);
        if (state == null || newTeam == null) return TeamChangeResult.INVALID;
        if (state.team() == target) return TeamChangeResult.SUCCESS;
        if (newTeam.size() >= definition.playersPerTeam()) return TeamChangeResult.FULL;
        teams.get(state.team()).removeMember(player.getUniqueId());
        newTeam.addMember(player.getUniqueId());
        state.team(target);
        return TeamChangeResult.SUCCESS;
    }

    public void refreshPersistentEquipment(Player player) {
        PlayerState state = players.get(player.getUniqueId());
        if (state != null) {
            kits.givePersistentEquipment(player, state, teams.get(state.team()));
            refreshInvisibleSubject(player);
        }
    }

    public void applyTeamEnchantments(TeamState team) {
        for (UUID playerId : team.members()) {
            Player player = Bukkit.getPlayer(playerId);
            if (player != null) {
                kits.applyTeamEnchantments(player, team);
                refreshInvisibleSubject(player);
                if (team.haste() > 0) {
                    player.addPotionEffect(new org.bukkit.potion.PotionEffect(
                            org.bukkit.potion.PotionEffectType.HASTE,
                            org.bukkit.potion.PotionEffect.INFINITE_DURATION,
                            team.haste() - 1,
                            false,
                            false,
                            true));
                }
            }
        }
    }

    public boolean areEnemies(Player first, Player second) {
        PlayerState firstState = players.get(first.getUniqueId());
        PlayerState secondState = players.get(second.getUniqueId());
        return firstState != null && secondState != null && firstState.team() != secondState.team();
    }

    public boolean isEnemy(TeamColor team, Player player) {
        PlayerState state = players.get(player.getUniqueId());
        return state != null && !state.eliminated() && !state.respawning() && state.team() != team;
    }

    public void grantTrapImmunity(Player player, Duration duration) {
        trapImmuneUntil.put(player.getUniqueId(), System.currentTimeMillis() + duration.toMillis());
    }

    public void recordHit(Player victim, Player attacker) {
        if (areEnemies(victim, attacker)) {
            lastHits.put(victim.getUniqueId(), new CombatHit(attacker.getUniqueId(), System.currentTimeMillis()));
        }
    }

    public boolean isRespawnProtected(Player player) {
        return respawnProtection.isProtected(player.getUniqueId(), System.nanoTime());
    }

    public void removeRespawnProtection(Player player) {
        respawnProtection.remove(player.getUniqueId());
    }

    public void handleInvisibilityChange(Player player, boolean invisible) {
        PlayerState state = players.get(player.getUniqueId());
        boolean eligible = invisible
                && phase == GamePhase.RUNNING
                && state != null
                && !state.eliminated()
                && !state.respawning();
        if (eligible) {
            invisiblePlayers.add(player.getUniqueId());
            refreshInvisibleSubject(player);
            return;
        }
        if (!invisiblePlayers.remove(player.getUniqueId())) return;
        forEachOnlinePlayer(viewer -> sendArmor(viewer, player, false));
    }

    public void revealInvisibility(Player player) {
        handleInvisibilityChange(player, false);
        player.removePotionEffect(org.bukkit.potion.PotionEffectType.INVISIBILITY);
    }

    public Player creditedKiller(Player victim) {
        Player direct = victim.getKiller();
        if (direct != null && areEnemies(victim, direct)) {
            return direct;
        }
        CombatHit hit = lastHits.get(victim.getUniqueId());
        if (hit == null || System.currentTimeMillis() - hit.timestamp() > 10_000L) {
            return null;
        }
        Player attacker = Bukkit.getPlayer(hit.attacker());
        return attacker != null && areEnemies(victim, attacker) ? attacker : null;
    }

    public void trackEntity(Entity entity) {
        spawnedEntities.add(entity);
    }

    public void captureState(Block block) {
        if (block.getWorld().equals(world)) {
            captureBeforeChange(block);
        }
    }

    public boolean handleBucketEmpty(Player player, Block affectedBlock) {
        PlayerState state = players.get(player.getUniqueId());
        if (phase != GamePhase.RUNNING || state == null || state.eliminated() || state.respawning()
                || !definition.canBuildAt(affectedBlock.getY()) || isProtectedBlock(affectedBlock)) return false;
        captureBeforeChange(affectedBlock);
        placedBlocks.add(BlockKey.of(affectedBlock));
        return true;
    }

    public boolean handleBucketFill(Player player, Block affectedBlock) {
        PlayerState state = players.get(player.getUniqueId());
        if (phase != GamePhase.RUNNING || state == null || state.eliminated() || state.respawning()) return false;
        return placedBlocks.remove(BlockKey.of(affectedBlock));
    }

    public boolean handleFluidSpread(Block source, Block destination) {
        if (!placedBlocks.contains(BlockKey.of(source))) return true;
        if (!definition.canBuildAt(destination.getY()) || isProtectedBlock(destination)) return false;
        captureBeforeChange(destination);
        placedBlocks.add(BlockKey.of(destination));
        return true;
    }

    public void handleDeathResources(Player victim, Player killer, List<ItemStack> resources) {
        PlayerState victimState = players.get(victim.getUniqueId());
        PlayerState killerState = killer == null ? null : players.get(killer.getUniqueId());
        boolean rewardKiller = victimState != null
                && killerState != null
                && killerState.team() != victimState.team()
                && !killerState.eliminated()
                && !killerState.respawning();
        if (!rewardKiller) {
            resources.forEach(resource -> dropTrackedItem(victim.getLocation(), resource));
            return;
        }

        EnumMap<ResourceType, Integer> totals = new EnumMap<>(ResourceType.class);
        for (ItemStack resource : resources) {
            ResourceType type = ResourceType.fromMaterial(resource.getType()).orElse(null);
            if (type == null) continue;
            totals.merge(type, resource.getAmount(), Integer::sum);
            killer.getInventory().addItem(resource.clone()).values()
                    .forEach(leftover -> dropTrackedItem(killer.getLocation(), leftover));
        }
        for (ResourceType type : ResourceType.values()) {
            int amount = totals.getOrDefault(type, 0);
            if (amount <= 0) continue;
            messages.send(killer, "death.resource-loot",
                    MessageService.number("amount", amount),
                    MessageService.component("resource", messages.render(killer, type.translationKey())),
                    MessageService.color("resource_color", type.textColor()));
        }
    }

    public void handleGeneratorPickup(Player collector, Item item, int remaining) {
        PlayerState collectorState = players.get(collector.getUniqueId());
        if (phase != GamePhase.RUNNING || collectorState == null || collectorState.eliminated()
                || collectorState.respawning() || collectorState.disconnected()) return;

        ItemStack stack = item.getItemStack();
        ResourceType type = ResourceType.fromMaterial(stack.getType()).orElse(null);
        if (type == null || !settings.generatorSplitting().splits(type)) return;
        ItemMeta meta = stack.getItemMeta();
        if (!meta.getPersistentDataContainer().has(generatorResourceKey, PersistentDataType.BYTE)) return;

        meta.getPersistentDataContainer().remove(generatorResourceKey);
        stack.setItemMeta(meta);
        item.setItemStack(stack);
        int collected = Math.max(0, stack.getAmount() - remaining);
        if (collected == 0) return;

        double radius = settings.generatorSplitting().radius();
        world.getNearbyEntities(collector.getLocation(), radius, radius, radius).stream()
                .filter(Player.class::isInstance)
                .map(Player.class::cast)
                .filter(recipient -> !recipient.equals(collector))
                .filter(recipient -> canReceiveGeneratorSplit(collectorState, recipient))
                .forEach(recipient -> giveGeneratorSplit(recipient, type, collected));
    }

    public boolean placeGeneratedBlock(Block block, Material material) {
        return placeGeneratedBlock(block, material.createBlockData());
    }

    public boolean placeGeneratedBlock(Block block, BlockData data) {
        if (phase != GamePhase.RUNNING || !block.getWorld().equals(world)
                || !definition.canBuildAt(block.getY()) || isProtectedBlock(block)) return false;
        BlockKey key = BlockKey.of(block);
        if (!block.isEmpty() && !placedBlocks.contains(key)) return false;
        captureBeforeChange(block);
        placedBlocks.add(key);
        block.setBlockData(data, false);
        return true;
    }

    public void drainWaterAround(Block center, int radius) {
        for (int x = -radius; x <= radius; x++) {
            for (int y = -radius; y <= radius; y++) {
                for (int z = -radius; z <= radius; z++) {
                    Block block = center.getRelative(x, y, z);
                    if (block.getType() != Material.WATER) continue;
                    captureBeforeChange(block);
                    placedBlocks.remove(BlockKey.of(block));
                    block.setType(Material.AIR, false);
                }
            }
        }
    }

    public void handleMovement(Player intruder) {
        PlayerState intruderState = players.get(intruder.getUniqueId());
        if (intruderState == null) {
            return;
        }
        if (definition.isVoid(intruder.getLocation().getY())) {
            if (phase == GamePhase.RUNNING && !intruderState.eliminated() && !intruderState.respawning()) {
                intruder.damage(1000.0, DamageSource.builder(VOID_DAMAGE_TYPE)
                        .withDamageLocation(intruder.getLocation())
                        .build());
            } else {
                Location destination = switch (phase) {
                    case WAITING, STARTING -> definition.lobby().toLocation(world);
                    case RUNNING -> intruderState.eliminated()
                            ? definition.spectator().toLocation(world)
                            : teams.get(intruderState.team()).definition().spawn().toLocation(world);
                    case ENDING -> definition.spectator().toLocation(world);
                };
                intruder.setFallDistance(0);
                intruder.teleportAsync(destination);
            }
            return;
        }
        if (phase != GamePhase.RUNNING || intruderState.eliminated() || intruderState.respawning()) {
            return;
        }
        if (trapImmuneUntil.getOrDefault(intruder.getUniqueId(), 0L) > System.currentTimeMillis()) {
            return;
        }
        for (TeamState defenders : teams.values()) {
            if (defenders.color() == intruderState.team() || defenders.traps().isEmpty() || !defenders.isAlive(players)) {
                continue;
            }
            if (intruder.getLocation().distanceSquared(defenders.definition().spawn().toLocation(world)) > 7 * 7) {
                continue;
            }
            String cooldownKey = intruder.getUniqueId() + ":" + defenders.color();
            long now = System.currentTimeMillis();
            if (trapCooldownUntil.getOrDefault(cooldownKey, 0L) > now) {
                continue;
            }
            TrapType trap = defenders.traps().poll();
            trapCooldownUntil.put(cooldownKey, now + 10_000L);
            applyTrap(trap, intruder, defenders);
            for (UUID memberId : defenders.members()) {
                Player member = Bukkit.getPlayer(memberId);
                if (member != null) {
                    messages.send(member, "upgrade.triggered",
                            MessageService.component("trap", messages.render(member, trapTranslationKey(trap))));
                }
            }
        }
    }

    private void applyTrap(TrapType trap, Player intruder, TeamState defenders) {
        switch (trap) {
            case ITS_A_TRAP -> {
                intruder.addPotionEffect(new org.bukkit.potion.PotionEffect(
                        org.bukkit.potion.PotionEffectType.BLINDNESS, 8 * 20, 0));
                intruder.addPotionEffect(new org.bukkit.potion.PotionEffect(
                        org.bukkit.potion.PotionEffectType.SLOWNESS, 8 * 20, 0));
            }
            case COUNTER_OFFENSIVE -> {
                for (UUID memberId : defenders.members()) {
                    Player member = Bukkit.getPlayer(memberId);
                    if (member != null) {
                        member.addPotionEffect(new org.bukkit.potion.PotionEffect(
                                org.bukkit.potion.PotionEffectType.SPEED, 15 * 20, 1));
                        member.addPotionEffect(new org.bukkit.potion.PotionEffect(
                                org.bukkit.potion.PotionEffectType.JUMP_BOOST, 15 * 20, 1));
                    }
                }
            }
            case ALARM -> intruder.removePotionEffect(org.bukkit.potion.PotionEffectType.INVISIBILITY);
            case MINER_FATIGUE -> intruder.addPotionEffect(new org.bukkit.potion.PotionEffect(
                    org.bukkit.potion.PotionEffectType.MINING_FATIGUE, 10 * 20, 0));
        }
    }

    private String trapTranslationKey(TrapType trap) {
        return switch (trap) {
            case ITS_A_TRAP -> "upgrade.name.its_a_trap";
            case COUNTER_OFFENSIVE -> "upgrade.name.counter_offensive";
            case ALARM -> "upgrade.name.alarm";
            case MINER_FATIGUE -> "upgrade.name.miner_fatigue";
        };
    }

    public JoinResult join(Player player) {
        return join(player, null);
    }

    JoinResult join(Player player, TeamColor preferredTeam) {
        if (phase == GamePhase.RUNNING || phase == GamePhase.ENDING) {
            return JoinResult.RUNNING;
        }
        if (players.containsKey(player.getUniqueId())) {
            return JoinResult.ALREADY_JOINED;
        }
        if (players.size() >= definition.maxPlayers()) {
            return JoinResult.FULL;
        }
        TeamState team = preferredTeam == null ? null : teams.get(preferredTeam);
        if (team != null && team.size() >= definition.playersPerTeam()) team = null;
        if (team == null) {
            team = teams.values().stream()
                    .filter(candidate -> candidate.size() < definition.playersPerTeam())
                    .min(Comparator.comparingInt(TeamState::size).thenComparingInt(candidate -> candidate.color().ordinal()))
                    .orElse(null);
        }
        if (team == null) {
            return JoinResult.FULL;
        }

        PlayerState state = new PlayerState(player.getUniqueId(), player.getName(), PlayerSnapshot.capture(player), team.color());
        players.put(player.getUniqueId(), state);
        team.addMember(player.getUniqueId());
        prepareWaitingPlayer(player);
        broadcast("arena.player-joined",
                MessageService.text("player", player.getName()),
                MessageService.number("current", players.size()),
                MessageService.number("maximum", definition.maxPlayers()),
                MessageService.teamColor("team_color", team.color()));

        if (phase == GamePhase.WAITING && hasEnoughPlayers()) {
            beginCountdown();
        }
        return JoinResult.SUCCESS;
    }

    boolean canAccept(int count) {
        return (phase == GamePhase.WAITING || phase == GamePhase.STARTING)
                && players.size() + count <= definition.maxPlayers();
    }

    TeamColor preferredTeam(int desiredPlayers) {
        TeamColor enoughSpace = teams.values().stream()
                .filter(team -> definition.playersPerTeam() - team.size() >= desiredPlayers)
                .min(Comparator.comparingInt(TeamState::size).thenComparingInt(team -> team.color().ordinal()))
                .map(TeamState::color)
                .orElse(null);
        if (enoughSpace != null) return enoughSpace;
        return teams.values().stream()
                .filter(team -> team.size() < definition.playersPerTeam())
                .min(Comparator.comparingInt(TeamState::size).thenComparingInt(team -> team.color().ordinal()))
                .map(TeamState::color)
                .orElse(null);
    }

    public boolean leave(Player player, boolean notify) {
        PlayerState state = players.get(player.getUniqueId());
        if (state == null) {
            return false;
        }
        if (phase == GamePhase.RUNNING) {
            recordStatistics(Map.of(state, false));
        }
        revealInvisibility(player);
        players.remove(player.getUniqueId());
        TeamState team = teams.get(state.team());
        team.removeMember(player.getUniqueId());
        cancelDisconnectTask(player.getUniqueId());
        lastHits.remove(player.getUniqueId());
        respawnProtection.remove(player.getUniqueId());
        lastHits.entrySet().removeIf(entry -> entry.getValue().attacker().equals(player.getUniqueId()));
        cancelRespawnTasksFor(player.getUniqueId());
        playerRelease.accept(player.getUniqueId(), state.snapshot());
        scoreboards.remove(player);
        state.snapshot().restore(player);
        if (notify) {
            messages.send(player, "arena.left");
        }
        broadcast("arena.player-left",
                MessageService.text("player", player.getName()),
                MessageService.teamColor("team_color", state.team()));

        if (phase == GamePhase.STARTING && !hasEnoughPlayers()) {
            cancelCountdown();
        } else if (phase == GamePhase.RUNNING) {
            checkVictory();
        }
        return true;
    }

    public boolean disconnect(Player player) {
        PlayerState state = players.get(player.getUniqueId());
        if (state == null) return false;
        if (phase != GamePhase.RUNNING || settings.reconnectGraceSeconds() <= 0) {
            return leave(player, false);
        }
        state.disconnected(true);
        cancelDisconnectTask(player.getUniqueId());
        broadcast("arena.player-disconnected",
                MessageService.text("player", player.getName()),
                MessageService.number("seconds", settings.reconnectGraceSeconds()),
                MessageService.teamColor("team_color", state.team()));
        BukkitTask task = Bukkit.getScheduler().runTaskLater(plugin,
                () -> expireDisconnect(state), settings.reconnectGraceSeconds() * 20L);
        disconnectTasks.put(player.getUniqueId(), task);
        return true;
    }

    public boolean reconnect(Player player) {
        PlayerState state = players.get(player.getUniqueId());
        if (state == null || !state.disconnected()) return false;
        state.disconnected(false);
        cancelDisconnectTask(player.getUniqueId());
        TeamState team = teams.get(state.team());
        if (phase == GamePhase.WAITING || phase == GamePhase.STARTING) {
            prepareWaitingPlayer(player);
        } else if (phase == GamePhase.RUNNING) {
            if (state.eliminated()) {
                player.getInventory().clear();
                player.setGameMode(GameMode.SPECTATOR);
                player.teleportAsync(definition.spectator().toLocation(world));
            } else if (state.respawning()) {
                state.respawning(false);
                kits.prepareForGame(player, state, team);
                player.teleportAsync(team.definition().spawn().toLocation(world));
            } else {
                player.setGameMode(GameMode.SURVIVAL);
                player.setAllowFlight(false);
                kits.givePersistentEquipment(player, state, team);
            }
            scoreboards.update(this);
        } else {
            player.setGameMode(GameMode.SPECTATOR);
            player.teleportAsync(definition.spectator().toLocation(world));
        }
        if (phase == GamePhase.RUNNING) {
            syncInvisibilityFor(player);
            refreshInvisibleSubject(player);
        }
        broadcast("arena.player-reconnected",
                MessageService.text("player", player.getName()),
                MessageService.teamColor("team_color", state.team()));
        return true;
    }

    public void tick() {
        switch (phase) {
            case WAITING -> {
                if (hasEnoughPlayers()) {
                    beginCountdown();
                }
            }
            case STARTING -> tickCountdown();
            case RUNNING -> tickRunning();
            case ENDING -> tickEnding();
        }
    }

    public boolean forceStart() {
        if ((phase != GamePhase.WAITING && phase != GamePhase.STARTING) || players.size() < 2) {
            return false;
        }
        startGame();
        return true;
    }

    public void forceStop() {
        if (phase == GamePhase.WAITING && players.isEmpty()) {
            return;
        }
        broadcast("arena.stopped");
        beginEnding(null, false);
    }

    public void shutdown() {
        resetArena();
    }

    public PlaceResult handlePlace(Player player, Block block, BlockState replacedState) {
        PlayerState state = players.get(player.getUniqueId());
        if (phase != GamePhase.RUNNING || state == null || state.eliminated() || state.respawning()) {
            return PlaceResult.PROTECTED;
        }
        if (!definition.canBuildAt(block.getY())) return PlaceResult.BUILD_LIMIT;
        if (isProtectedBlock(block)) return PlaceResult.PROTECTED;
        BlockKey key = BlockKey.of(block);
        changedBlocks.putIfAbsent(key, replacedState);
        placedBlocks.add(key);
        return PlaceResult.ALLOWED;
    }

    public BreakResult handleBreak(Player player, Block block) {
        PlayerState playerState = players.get(player.getUniqueId());
        if (phase != GamePhase.RUNNING || playerState == null || playerState.eliminated() || playerState.respawning()) {
            return BreakResult.PROTECTED;
        }
        TeamState bedTeam = bedTeam(block).orElse(null);
        if (bedTeam != null && bedTeam.bedAlive()) {
            if (bedTeam.color() == playerState.team()) {
                return BreakResult.OWN_BED;
            }
            destroyBed(bedTeam, player, playerState);
            return BreakResult.BED_DESTROYED;
        }
        BlockKey key = BlockKey.of(block);
        if (!placedBlocks.remove(key)) {
            return BreakResult.PROTECTED;
        }
        return BreakResult.ALLOWED;
    }

    public boolean isPlacedBlock(Block block) {
        return placedBlocks.contains(BlockKey.of(block));
    }

    public void removePlacedBlock(Block block) {
        placedBlocks.remove(BlockKey.of(block));
    }

    public void handleDeath(Player victim, Player killer) {
        PlayerState victimState = players.get(victim.getUniqueId());
        if (phase != GamePhase.RUNNING || victimState == null || victimState.eliminated()) {
            return;
        }
        TeamState victimTeam = teams.get(victimState.team());
        PlayerState killerState = killer == null ? null : players.get(killer.getUniqueId());
        lastHits.remove(victim.getUniqueId());
        respawnProtection.remove(victim.getUniqueId());
        revealInvisibility(victim);
        boolean finalDeath = !victimTeam.bedAlive();
        if (finalDeath) {
            dropFinalEnderChest(victim, victimTeam);
        }
        victimState.addDeath();
        if (finalDeath) victimState.addFinalDeath();
        victimState.downgradeTools();
        victimState.respawning(!finalDeath);
        victimState.eliminated(finalDeath);

        if (killerState != null && killerState != victimState && killerState.team() != victimState.team()) {
            killerState.addKill();
            if (finalDeath) {
                killerState.addFinalKill();
            }
            broadcast("death.killed",
                    MessageService.text("player", victim.getName()),
                    MessageService.text("killer", killer.getName()),
                    MessageService.teamColor("team_color", victimState.team()),
                    MessageService.teamColor("killer_color", killerState.team()));
        } else {
            broadcast("death.normal",
                    MessageService.text("player", victim.getName()),
                    MessageService.teamColor("team_color", victimState.team()));
        }
        if (finalDeath) {
            broadcast("player.eliminated",
                    MessageService.text("player", victim.getName()),
                    MessageService.teamColor("team_color", victimState.team()));
        }

        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (!victim.isOnline() || !players.containsKey(victim.getUniqueId())) {
                return;
            }
            victim.spigot().respawn();
        }, 3L);
        if (finalDeath) {
            checkVictory();
        }
    }

    public Location respawnLocation(Player player) {
        PlayerState state = players.get(player.getUniqueId());
        if (state == null) {
            return definition.spectator().toLocation(world);
        }
        return state.eliminated()
                ? definition.spectator().toLocation(world)
                : teams.get(state.team()).definition().spawn().toLocation(world);
    }

    public void afterRespawn(Player player) {
        PlayerState state = players.get(player.getUniqueId());
        if (state == null) {
            return;
        }
        if (state.eliminated()) {
            player.getInventory().clear();
            player.setGameMode(GameMode.SPECTATOR);
            player.teleportAsync(definition.spectator().toLocation(world));
            return;
        }
        if (phase != GamePhase.RUNNING) {
            player.setGameMode(GameMode.SPECTATOR);
            return;
        }
        beginRespawnCountdown(player, state);
    }

    private void beginCountdown() {
        phase = GamePhase.STARTING;
        countdown = settings.countdownSeconds();
    }

    private void tickCountdown() {
        if (!hasEnoughPlayers()) {
            cancelCountdown();
            return;
        }
        if (countdown <= 0) {
            startGame();
            return;
        }
        if (countdown <= 5 || countdown == 10 || countdown == 20 || countdown == 30) {
            broadcast("arena.countdown", MessageService.number("seconds", countdown));
        }
        countdown--;
    }

    private void cancelCountdown() {
        phase = GamePhase.WAITING;
        countdown = 0;
        broadcast("arena.countdown-cancelled");
    }

    private boolean hasEnoughPlayers() {
        return players.size() >= Math.max(definition.minPlayers(), settings.minimumPlayers());
    }

    private void startGame() {
        if (players.size() < 2) {
            cancelCountdown();
            return;
        }
        phase = GamePhase.RUNNING;
        previousImmediateRespawn = world.getGameRuleValue(GameRules.IMMEDIATE_RESPAWN);
        if (!world.setGameRule(GameRules.IMMEDIATE_RESPAWN, true)) {
            plugin.getLogger().warning("Could not enable immediate respawn for arena '" + key() + "'");
        }
        elapsedSeconds = 0;
        statsRecorded = false;
        generatorClocks.clear();
        trapCooldownUntil.clear();
        for (TeamState team : teams.values()) {
            team.restoreBed();
        }
        for (PlayerState state : players.values()) {
            state.eliminated(false);
            state.respawning(false);
            state.disconnected(false);
            Player player = Bukkit.getPlayer(state.playerId());
            if (player == null) {
                continue;
            }
            TeamState team = teams.get(state.team());
            kits.prepareForGame(player, state, team);
            player.teleportAsync(team.definition().spawn().toLocation(world));
        }
        spawnShopkeepers();
        spawnGeneratorDisplays();
        broadcast("arena.started");
    }

    private void tickRunning() {
        elapsedSeconds++;
        settings.eventSchedule().eventAt(elapsedSeconds).ifPresent(event -> handleEvent(event.type()));
        tickGenerators();
        tickHealPools();
        scoreboards.update(this);
    }

    private void handleEvent(GameEventType type) {
        switch (type) {
            case BED_DESTRUCTION -> {
                destroyAllBeds();
                broadcast("arena.beds-destroyed");
            }
            case SUDDEN_DEATH -> {
                spawnDragons();
                broadcast("arena.sudden-death");
            }
            case GAME_END -> beginEnding(null, true);
            default -> broadcast("arena.event", MessageService.text("event", type.displayName()));
        }
    }

    private void tickGenerators() {
        int diamondTier = elapsedSeconds >= settings.eventSchedule().timeOf(GameEventType.DIAMOND_III) ? 3
                : elapsedSeconds >= settings.eventSchedule().timeOf(GameEventType.DIAMOND_II) ? 2 : 1;
        int emeraldTier = elapsedSeconds >= settings.eventSchedule().timeOf(GameEventType.EMERALD_III) ? 3
                : elapsedSeconds >= settings.eventSchedule().timeOf(GameEventType.EMERALD_II) ? 2 : 1;

        spawnMapGenerators(ResourceType.DIAMOND, diamondTier);
        spawnMapGenerators(ResourceType.EMERALD, emeraldTier);
        for (TeamState team : teams.values()) {
            if (team.members().isEmpty()) {
                continue;
            }
            double multiplier = 1.0 + (team.forge() * 0.5);
            spawnGenerator("forge:" + team.color() + ":iron", ResourceType.IRON,
                    team.definition().forge(), settings.generatorPeriods().iron() / multiplier,
                    ResourceType.IRON.generatorCap(1));
            spawnGenerator("forge:" + team.color() + ":gold", ResourceType.GOLD,
                    team.definition().forge(), settings.generatorPeriods().gold() / multiplier,
                    ResourceType.GOLD.generatorCap(1));
            if (team.forge() >= 3) {
                spawnGenerator("forge:" + team.color() + ":emerald", ResourceType.EMERALD,
                        team.definition().forge(), team.forge() >= 4 ? 8.0 : 12.0,
                        ResourceType.EMERALD.generatorCap(1));
            }
        }
    }

    private void spawnMapGenerators(ResourceType type, int tier) {
        List<Position> positions = definition.generators().getOrDefault(type, List.of());
        for (int index = 0; index < positions.size(); index++) {
            String key = type + ":" + index;
            GeneratorStatus status = spawnGenerator(key, type, positions.get(index),
                    settings.generatorPeriods().period(type, tier), type.generatorCap(tier));
            updateGeneratorDisplay(key, type, tier, status);
        }
    }

    private GeneratorStatus spawnGenerator(
            String key,
            ResourceType type,
            Position position,
            double period,
            int cap
    ) {
        Location location = position.toLocation(world);
        GeneratorClock clock = generatorClocks.computeIfAbsent(key, ignored -> new GeneratorClock());
        boolean full = isGeneratorFull(type, location, cap);
        int spawns = clock.advance(period, full);
        for (int count = 0; count < spawns; count++) {
            if (!dropResource(type, location, cap)) break;
        }
        boolean nowFull = isGeneratorFull(type, location, cap);
        return new GeneratorStatus(nowFull, clock.secondsUntilNext(period));
    }

    private boolean dropResource(ResourceType type, Location location, int cap) {
        if (isGeneratorFull(type, location, cap)) return false;
        ItemStack stack = new ItemStack(type.material());
        if (settings.generatorSplitting().splits(type)) {
            ItemMeta meta = stack.getItemMeta();
            meta.getPersistentDataContainer().set(generatorResourceKey, PersistentDataType.BYTE, (byte) 1);
            stack.setItemMeta(meta);
        }
        Item item = world.dropItem(location.clone().add(0, 0.25, 0), stack);
        item.setVelocity(new Vector());
        item.setPickupDelay(0);
        trackEntity(item);
        return true;
    }

    private boolean canReceiveGeneratorSplit(PlayerState collectorState, Player recipient) {
        PlayerState recipientState = players.get(recipient.getUniqueId());
        return recipientState != null
                && recipientState.team() == collectorState.team()
                && !recipientState.eliminated()
                && !recipientState.respawning()
                && !recipientState.disconnected();
    }

    private void giveGeneratorSplit(Player recipient, ResourceType type, int amount) {
        ItemStack share = new ItemStack(type.material(), amount);
        recipient.getInventory().addItem(share).values()
                .forEach(leftover -> dropTrackedItem(recipient.getLocation(), leftover));
        recipient.playSound(recipient.getLocation(), Sound.ENTITY_ITEM_PICKUP, 0.6f, 1.3f);
    }

    private boolean isGeneratorFull(ResourceType type, Location location, int cap) {
        int nearby = world.getNearbyEntities(location, 1.75, 1.5, 1.75).stream()
                .filter(Item.class::isInstance)
                .map(Item.class::cast)
                .filter(item -> item.getItemStack().getType() == type.material())
                .mapToInt(item -> item.getItemStack().getAmount())
                .sum();
        return nearby >= cap;
    }

    private void spawnGeneratorDisplays() {
        if (!settings.generatorDisplays().enabled()) return;
        for (ResourceType type : List.of(ResourceType.DIAMOND, ResourceType.EMERALD)) {
            List<Position> positions = definition.generators().getOrDefault(type, List.of());
            for (int index = 0; index < positions.size(); index++) {
                String key = type + ":" + index;
                GeneratorDisplay display = createGeneratorDisplay(type, positions.get(index));
                generatorDisplays.put(key, display);
                int remaining = (int) Math.ceil(settings.generatorPeriods().period(type, 1));
                updateGeneratorDisplay(key, type, 1, new GeneratorStatus(false, remaining));
            }
        }
    }

    private GeneratorDisplay createGeneratorDisplay(ResourceType type, Position position) {
        GameSettings.GeneratorDisplaySettings displaySettings = settings.generatorDisplays();
        Location base = position.toLocation(world);
        Material blockMaterial = type == ResourceType.DIAMOND ? Material.DIAMOND_BLOCK : Material.EMERALD_BLOCK;
        ItemDisplay item = world.spawn(base.clone().add(0, displaySettings.itemHeight(), 0), ItemDisplay.class, entity -> {
            entity.setItemStack(new ItemStack(blockMaterial));
            entity.setItemDisplayTransform(ItemDisplay.ItemDisplayTransform.FIXED);
            entity.setBillboard(Display.Billboard.FIXED);
            entity.setBrightness(new Display.Brightness(15, 15));
            entity.setGravity(false);
            entity.setInvulnerable(true);
            entity.setPersistent(false);
            entity.setInterpolationDuration(20);
            entity.setTransformation(displayTransformation(0));
        });
        TextDisplay text = world.spawn(base.clone().add(0, displaySettings.textHeight(), 0), TextDisplay.class, entity -> {
            entity.setBillboard(Display.Billboard.CENTER);
            entity.setAlignment(TextDisplay.TextAlignment.CENTER);
            entity.setBackgroundColor(Color.fromARGB(80, 0, 0, 0));
            entity.setBrightness(new Display.Brightness(15, 15));
            entity.setShadowed(true);
            entity.setSeeThrough(false);
            entity.setLineWidth(80);
            entity.setGravity(false);
            entity.setInvulnerable(true);
            entity.setPersistent(false);
        });
        trackEntity(item);
        trackEntity(text);
        return new GeneratorDisplay(item, text);
    }

    private void updateGeneratorDisplay(String key, ResourceType type, int tier, GeneratorStatus status) {
        GeneratorDisplay display = generatorDisplays.get(key);
        if (display == null || !display.item().isValid() || !display.text().isValid()) return;
        display.item().setInterpolationDelay(0);
        display.item().setTransformation(displayTransformation((float) (elapsedSeconds * Math.PI / 10.0)));
        Component countdown = status.full()
                ? Component.text("--:--", NamedTextColor.RED)
                : Component.text(GeneratorDisplayFormat.countdown(status.remainingSeconds()), type.textColor());
        display.text().text(Component.text(GeneratorDisplayFormat.romanTier(tier), NamedTextColor.YELLOW)
                .append(Component.newline())
                .append(countdown));
    }

    private Transformation displayTransformation(float angle) {
        return new Transformation(
                new Vector3f(),
                new AxisAngle4f(angle, 0, 1, 0),
                new Vector3f(0.6f, 0.6f, 0.6f),
                new AxisAngle4f()
        );
    }

    private void tickHealPools() {
        for (TeamState team : teams.values()) {
            if (!team.healPool()) {
                continue;
            }
            Location base = team.definition().spawn().toLocation(world);
            for (UUID playerId : team.members()) {
                Player player = Bukkit.getPlayer(playerId);
                if (player != null && player.getLocation().distanceSquared(base) <= 15 * 15) {
                    player.addPotionEffect(new org.bukkit.potion.PotionEffect(
                            org.bukkit.potion.PotionEffectType.REGENERATION, 40, 0, false, false, true));
                }
            }
        }
    }

    private Optional<TeamState> bedTeam(Block block) {
        return teams.values().stream().filter(team -> {
            Position.BlockPosition blockPosition = new Position(block.getX(), block.getY(), block.getZ(), 0, 0).block();
            return blockPosition.equals(team.definition().bedHead().block())
                    || blockPosition.equals(team.definition().bedFoot().block());
        }).findFirst();
    }

    private void destroyBed(TeamState bedTeam, Player destroyer, PlayerState destroyerState) {
        bedTeam.destroyBed();
        destroyerState.addBedBroken();
        setBedAir(bedTeam);
        for (UUID memberId : bedTeam.members()) {
            Player member = Bukkit.getPlayer(memberId);
            if (member != null) {
                messages.send(member, "bed.destroyed-own");
            }
        }
        broadcast("bed.destroyed-global",
                MessageService.text("bed_symbol", "X"),
                MessageService.text("team", bedTeam.color().displayName()),
                MessageService.text("player", destroyer.getName()),
                MessageService.teamColor("team_color", bedTeam.color()),
                MessageService.teamColor("destroyer_color", destroyerState.team()));
    }

    private void destroyAllBeds() {
        for (TeamState team : teams.values()) {
            if (team.bedAlive()) {
                team.destroyBed();
                setBedAir(team);
            }
        }
    }

    private void setBedAir(TeamState team) {
        for (Position position : List.of(team.definition().bedHead(), team.definition().bedFoot())) {
            Block block = world.getBlockAt(position.block().x(), position.block().y(), position.block().z());
            captureBeforeChange(block);
            block.setType(Material.AIR, false);
        }
    }

    private void spawnDragons() {
        int dragonCount = teams.values().stream()
                .filter(team -> team.isAlive(players))
                .mapToInt(team -> team.dragonBuff() ? 2 : 1)
                .sum();
        for (int index = 0; index < dragonCount; index++) {
            Location location = definition.spectator().toLocation(world).add(0, 15 + index * 3, 0);
            EnderDragon dragon = world.spawn(location, EnderDragon.class, entity -> {
                entity.customName(net.kyori.adventure.text.Component.text("Bed Wars Dragon"));
                entity.setRemoveWhenFarAway(false);
            });
            spawnedEntities.add(dragon);
        }
    }

    private void spawnShopkeepers() {
        NamespacedKey shopType = new NamespacedKey(plugin, "shop_type");
        for (TeamState team : teams.values()) {
            if (team.members().isEmpty()) continue;
            spawnedEntities.add(spawnShopkeeper(team.definition().itemShop(), "ITEM SHOP", "item", shopType));
            spawnedEntities.add(spawnShopkeeper(team.definition().upgradeShop(), "TEAM UPGRADES", "upgrades", shopType));
        }
    }

    private Villager spawnShopkeeper(Position position, String name, String type, NamespacedKey key) {
        return world.spawn(position.toLocation(world), Villager.class, villager -> {
            villager.customName(net.kyori.adventure.text.Component.text(name, net.kyori.adventure.text.format.NamedTextColor.YELLOW));
            villager.setCustomNameVisible(true);
            villager.setAI(false);
            villager.setSilent(true);
            villager.setInvulnerable(true);
            villager.setCollidable(false);
            villager.setGravity(false);
            villager.setRemoveWhenFarAway(false);
            villager.setProfession(Villager.Profession.NITWIT);
            villager.getPersistentDataContainer().set(key, PersistentDataType.STRING, type);
        });
    }

    private void beginRespawnCountdown(Player player, PlayerState state) {
        player.getInventory().clear();
        player.setGameMode(GameMode.SPECTATOR);
        player.teleportAsync(teams.get(state.team()).definition().spawn().toLocation(world));
        final int[] remaining = {settings.respawnSeconds()};
        BukkitTask[] holder = new BukkitTask[1];
        holder[0] = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            if (phase != GamePhase.RUNNING || players.get(player.getUniqueId()) != state || !player.isOnline()) {
                holder[0].cancel();
                respawnTasks.remove(holder[0]);
                return;
            }
            if (remaining[0] <= 0) {
                state.respawning(false);
                kits.prepareForGame(player, state, teams.get(state.team()));
                respawnProtection.grant(player.getUniqueId(), System.nanoTime(),
                        settings.respawnProtectionSeconds());
                syncInvisibilityFor(player);
                player.teleportAsync(teams.get(state.team()).definition().spawn().toLocation(world));
                player.showTitle(Title.title(messages.render(player, "respawn.done"), net.kyori.adventure.text.Component.empty(),
                        Title.Times.times(Duration.ZERO, Duration.ofSeconds(1), Duration.ofMillis(250))));
                holder[0].cancel();
                respawnTasks.remove(holder[0]);
                return;
            }
            player.showTitle(Title.title(
                    messages.render(player, "respawn.title"),
                    messages.render(player, "respawn.subtitle", MessageService.number("seconds", remaining[0])),
                    Title.Times.times(Duration.ZERO, Duration.ofMillis(1100), Duration.ZERO)));
            remaining[0]--;
        }, 0L, 20L);
        respawnTasks.add(holder[0]);
    }

    private void cancelRespawnTasksFor(UUID playerId) {
        PlayerState state = players.get(playerId);
        if (state != null) {
            state.respawning(false);
        }
    }

    private void expireDisconnect(PlayerState state) {
        if (players.get(state.playerId()) != state || !state.disconnected()) return;
        disconnectTasks.remove(state.playerId());
        recordStatistics(Map.of(state, false));
        players.remove(state.playerId());
        teams.get(state.team()).removeMember(state.playerId());
        state.eliminated(true);
        respawnProtection.remove(state.playerId());
        invisiblePlayers.remove(state.playerId());
        playerRelease.accept(state.playerId(), state.snapshot());
        broadcast("arena.reconnect-expired",
                MessageService.text("player", state.playerName()),
                MessageService.teamColor("team_color", state.team()));
        checkVictory();
    }

    private void cancelDisconnectTask(UUID playerId) {
        BukkitTask task = disconnectTasks.remove(playerId);
        if (task != null) task.cancel();
    }

    private void checkVictory() {
        if (phase != GamePhase.RUNNING) {
            return;
        }
        List<TeamState> alive = teams.values().stream().filter(team -> team.isAlive(players)).toList();
        if (alive.size() <= 1) {
            beginEnding(alive.isEmpty() ? null : alive.getFirst(), true);
        }
    }

    private void beginEnding(TeamState winner, boolean recordStatistics) {
        if (phase == GamePhase.ENDING) {
            return;
        }
        if (recordStatistics && !statsRecorded) {
            Map<PlayerState, Boolean> results = new LinkedHashMap<>();
            for (PlayerState state : players.values()) {
                results.put(state, winner != null && state.team() == winner.color());
            }
            recordStatistics(results);
            statsRecorded = true;
        }
        phase = GamePhase.ENDING;
        endingCountdown = settings.endingSeconds();
        if (winner == null) {
            broadcast("arena.draw");
        } else {
            broadcast("arena.victory",
                    MessageService.text("team", winner.color().displayName()),
                    MessageService.teamColor("team_color", winner.color()));
        }
    }

    private void recordStatistics(Map<PlayerState, Boolean> results) {
        long playExperience = (long) elapsedSeconds * settings.experiencePerMinute() / 60L;
        Map<PlayerIdentity, StatsDelta> deltas = new LinkedHashMap<>();
        for (Map.Entry<PlayerState, Boolean> entry : results.entrySet()) {
            PlayerState state = entry.getKey();
            boolean won = entry.getValue();
            deltas.put(new PlayerIdentity(state.playerId(), state.playerName()), new StatsDelta(
                    1,
                    won ? 1 : 0,
                    won ? 0 : 1,
                    state.kills(),
                    state.finalKills(),
                    state.deaths(),
                    state.finalDeaths(),
                    state.bedsBroken(),
                    playExperience + (won ? settings.winBonusExperience() : 0)
            ));
        }
        plugin.statisticsService().record(deltas);
    }

    private void tickEnding() {
        endingCountdown--;
        if (endingCountdown <= 0) {
            resetArena();
        }
    }

    private void resetArena() {
        if (previousImmediateRespawn != null) {
            world.setGameRule(GameRules.IMMEDIATE_RESPAWN, previousImmediateRespawn);
            previousImmediateRespawn = null;
        }
        for (BukkitTask task : List.copyOf(respawnTasks)) {
            task.cancel();
        }
        respawnTasks.clear();
        for (BukkitTask task : disconnectTasks.values()) {
            task.cancel();
        }
        disconnectTasks.clear();
        for (Entity entity : List.copyOf(spawnedEntities)) {
            if (entity.isValid()) {
                entity.remove();
            }
        }
        spawnedEntities.clear();
        for (BlockState state : changedBlocks.values()) {
            state.update(true, false);
        }
        changedBlocks.clear();
        placedBlocks.clear();
        for (UUID playerId : List.copyOf(invisiblePlayers)) {
            Player invisible = Bukkit.getPlayer(playerId);
            if (invisible != null) handleInvisibilityChange(invisible, false);
        }
        invisiblePlayers.clear();

        for (PlayerState state : List.copyOf(players.values())) {
            Player player = Bukkit.getPlayer(state.playerId());
            playerRelease.accept(state.playerId(), state.snapshot());
            if (player != null) {
                scoreboards.remove(player);
                state.snapshot().restore(player);
            }
        }
        players.clear();
        teams.values().forEach(TeamState::reset);
        generatorClocks.clear();
        generatorDisplays.clear();
        trapCooldownUntil.clear();
        lastHits.clear();
        trapImmuneUntil.clear();
        respawnProtection.clear();
        scoreboards.clear();
        elapsedSeconds = 0;
        countdown = 0;
        endingCountdown = 0;
        statsRecorded = false;
        phase = GamePhase.WAITING;
    }

    private void captureBeforeChange(Block block) {
        changedBlocks.putIfAbsent(BlockKey.of(block), block.getState());
    }

    private boolean isProtectedBlock(Block block) {
        return definition.isProtectedBlock(block.getX(), block.getY(), block.getZ());
    }

    private void syncInvisibilityFor(Player viewer) {
        for (UUID subjectId : List.copyOf(invisiblePlayers)) {
            Player subject = Bukkit.getPlayer(subjectId);
            if (subject == null) {
                invisiblePlayers.remove(subjectId);
                continue;
            }
            if (!subject.hasPotionEffect(org.bukkit.potion.PotionEffectType.INVISIBILITY)) {
                handleInvisibilityChange(subject, false);
                continue;
            }
            sendArmor(viewer, subject, shouldHideArmor(subject, viewer));
        }
    }

    private void refreshInvisibleSubject(Player subject) {
        if (!invisiblePlayers.contains(subject.getUniqueId())) return;
        forEachOnlinePlayer(viewer -> sendArmor(viewer, subject, shouldHideArmor(subject, viewer)));
    }

    private boolean shouldHideArmor(Player subject, Player viewer) {
        PlayerState subjectState = players.get(subject.getUniqueId());
        PlayerState viewerState = players.get(viewer.getUniqueId());
        return subjectState != null
                && viewerState != null
                && InvisibilityPolicy.hidesArmor(
                        subjectState.team(), viewerState.team(), viewerState.eliminated(), viewerState.respawning());
    }

    private void sendArmor(Player viewer, Player subject, boolean hidden) {
        viewer.sendEquipmentChange(subject, hidden ? hiddenArmor() : actualArmor(subject));
    }

    private Map<EquipmentSlot, ItemStack> hiddenArmor() {
        return Map.of(
                EquipmentSlot.HEAD, new ItemStack(Material.AIR),
                EquipmentSlot.CHEST, new ItemStack(Material.AIR),
                EquipmentSlot.LEGS, new ItemStack(Material.AIR),
                EquipmentSlot.FEET, new ItemStack(Material.AIR)
        );
    }

    private Map<EquipmentSlot, ItemStack> actualArmor(Player subject) {
        EntityEquipment equipment = subject.getEquipment();
        return Map.of(
                EquipmentSlot.HEAD, itemOrAir(equipment.getHelmet()),
                EquipmentSlot.CHEST, itemOrAir(equipment.getChestplate()),
                EquipmentSlot.LEGS, itemOrAir(equipment.getLeggings()),
                EquipmentSlot.FEET, itemOrAir(equipment.getBoots())
        );
    }

    private ItemStack itemOrAir(ItemStack item) {
        return item == null || item.isEmpty() ? new ItemStack(Material.AIR) : item.clone();
    }

    private void forEachOnlinePlayer(Consumer<Player> action) {
        for (UUID playerId : players.keySet()) {
            Player player = Bukkit.getPlayer(playerId);
            if (player != null) action.accept(player);
        }
    }

    private void prepareWaitingPlayer(Player player) {
        player.getInventory().clear();
        player.getEnderChest().clear();
        player.setGameMode(GameMode.ADVENTURE);
        player.setAllowFlight(false);
        player.setHealth(player.getAttribute(org.bukkit.attribute.Attribute.MAX_HEALTH).getValue());
        player.setFoodLevel(20);
        player.teleportAsync(definition.lobby().toLocation(world));
    }

    private void dropFinalEnderChest(Player victim, TeamState team) {
        List<ItemStack> contents = java.util.Arrays.stream(victim.getEnderChest().getContents())
                .filter(java.util.Objects::nonNull)
                .filter(item -> !item.isEmpty())
                .map(ItemStack::clone)
                .toList();
        victim.getEnderChest().clear();
        if (contents.isEmpty()) return;
        Location forge = team.definition().forge().toLocation(world).add(0, 0.25, 0);
        contents.forEach(item -> dropTrackedItem(forge, item));
        messages.send(victim, "death.ender-chest-dropped");
    }

    private void dropTrackedItem(Location location, ItemStack stack) {
        Item dropped = world.dropItemNaturally(location, stack.clone());
        dropped.setPickupDelay(10);
        trackEntity(dropped);
    }

    private void broadcast(String key, net.kyori.adventure.text.minimessage.tag.resolver.TagResolver... resolvers) {
        for (UUID playerId : players.keySet()) {
            Player player = Bukkit.getPlayer(playerId);
            if (player != null) {
                messages.send(player, key, resolvers);
            }
        }
    }

    public enum JoinResult {
        SUCCESS,
        ALREADY_JOINED,
        FULL,
        RUNNING
    }

    public enum BreakResult {
        ALLOWED,
        PROTECTED,
        OWN_BED,
        BED_DESTROYED
    }

    public enum PlaceResult {
        ALLOWED,
        PROTECTED,
        BUILD_LIMIT
    }

    public enum TeamChangeResult {
        SUCCESS,
        FULL,
        INVALID,
        LOCKED
    }

    private record CombatHit(UUID attacker, long timestamp) {
    }

    private record GeneratorStatus(boolean full, int remainingSeconds) {
    }

    private record GeneratorDisplay(ItemDisplay item, TextDisplay text) {
    }
}
