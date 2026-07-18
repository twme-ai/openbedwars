package io.github.twmeai.openbedwars.game;

import io.github.twmeai.openbedwars.OpenBedWarsPlugin;
import io.github.twmeai.openbedwars.config.ArenaConfigRepository;
import io.github.twmeai.openbedwars.config.ArenaDefinition;
import io.github.twmeai.openbedwars.config.GameSettings;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.List;
import java.util.UUID;
import java.util.logging.Level;

public final class ArenaManager {
    private final OpenBedWarsPlugin plugin;
    private final Map<String, Arena> arenas = new LinkedHashMap<>();
    private final Map<UUID, Arena> playerArenas = new java.util.HashMap<>();
    private final Map<UUID, PlayerSnapshot> pendingRestores = new java.util.HashMap<>();
    private BukkitTask ticker;

    public ArenaManager(OpenBedWarsPlugin plugin) {
        this.plugin = plugin;
        reload();
        ticker = Bukkit.getScheduler().runTaskTimer(plugin, this::tick, 20L, 20L);
    }

    public Map<String, Arena> arenas() {
        return Map.copyOf(arenas);
    }

    public Optional<Arena> arena(String key) {
        return Optional.ofNullable(arenas.get(key.toLowerCase(java.util.Locale.ROOT)));
    }

    public Optional<Arena> bestAvailableArena() {
        return arenas.values().stream()
                .filter(arena -> (arena.phase() == GamePhase.WAITING || arena.phase() == GamePhase.STARTING)
                        && arena.playerCount() < arena.maxPlayers())
                .max(java.util.Comparator.comparingInt(Arena::playerCount));
    }

    public Optional<Arena> arenaOf(Player player) {
        return Optional.ofNullable(playerArenas.get(player.getUniqueId()));
    }

    public Optional<Arena> arenaIn(World world) {
        return arenas.values().stream().filter(arena -> arena.world().equals(world)).findFirst();
    }

    public boolean hasActiveGames() {
        return arenas.values().stream().anyMatch(arena -> arena.playerCount() > 0 || arena.phase() != GamePhase.WAITING);
    }

    public Arena.JoinResult join(Player player, Arena arena) {
        if (playerArenas.containsKey(player.getUniqueId())) {
            return Arena.JoinResult.ALREADY_JOINED;
        }
        Arena.JoinResult result = arena.join(player);
        if (result == Arena.JoinResult.SUCCESS) {
            playerArenas.put(player.getUniqueId(), arena);
        }
        return result;
    }

    public Arena.JoinResult joinGroup(List<Player> players, Arena arena) {
        List<Player> group = List.copyOf(players);
        if (group.stream().anyMatch(player -> playerArenas.containsKey(player.getUniqueId()))) {
            return Arena.JoinResult.ALREADY_JOINED;
        }
        if (arena.phase() == GamePhase.RUNNING || arena.phase() == GamePhase.ENDING) {
            return Arena.JoinResult.RUNNING;
        }
        if (!arena.canAccept(group.size())) {
            return Arena.JoinResult.FULL;
        }

        java.util.ArrayList<Player> joined = new java.util.ArrayList<>();
        TeamColor preferred = arena.preferredTeam(Math.min(group.size(), arena.definition().playersPerTeam()));
        for (int index = 0; index < group.size(); index++) {
            Player player = group.get(index);
            Arena.JoinResult result = arena.join(player, preferred);
            if (result != Arena.JoinResult.SUCCESS) {
                for (Player rollback : joined) arena.leave(rollback, false);
                return result;
            }
            playerArenas.put(player.getUniqueId(), arena);
            joined.add(player);
            if (arena.teamOf(player.getUniqueId()).map(TeamState::size).orElse(0) >= arena.definition().playersPerTeam()) {
                int remaining = group.size() - index - 1;
                preferred = arena.preferredTeam(Math.min(remaining, arena.definition().playersPerTeam()));
            }
        }
        return Arena.JoinResult.SUCCESS;
    }

    public boolean leave(Player player, boolean notify) {
        Arena arena = playerArenas.get(player.getUniqueId());
        return arena != null && arena.leave(player, notify);
    }

    public Arena.TeamChangeResult changeTeam(Player player, TeamColor team) {
        Arena arena = playerArenas.get(player.getUniqueId());
        return arena == null ? Arena.TeamChangeResult.INVALID : arena.changeTeam(player, team);
    }

    public boolean disconnect(Player player) {
        Arena arena = playerArenas.get(player.getUniqueId());
        return arena != null && arena.disconnect(player);
    }

    public boolean handleJoin(Player player) {
        Arena arena = playerArenas.get(player.getUniqueId());
        if (arena != null && arena.reconnect(player)) {
            return true;
        }
        PlayerSnapshot snapshot = pendingRestores.remove(player.getUniqueId());
        if (snapshot != null) {
            Bukkit.getScheduler().runTask(plugin, () -> snapshot.restore(player));
            return true;
        }
        return false;
    }

    public void reload() {
        shutdownArenas();
        plugin.reloadConfig();
        plugin.messages().reload();
        GameSettings settings = GameSettings.from(plugin.getConfig());
        Map<String, ArenaDefinition> definitions = new ArenaConfigRepository(plugin).load();
        for (ArenaDefinition definition : definitions.values()) {
            World world = Bukkit.getWorld(definition.worldName());
            if (world == null) {
                plugin.getLogger().severe("Arena '" + definition.key() + "' skipped: world '"
                        + definition.worldName() + "' is not loaded");
                continue;
            }
            arenas.put(definition.key(), new Arena(plugin, definition, settings, world, this::releasePlayer));
        }
        plugin.getLogger().info("Loaded " + arenas.size() + " enabled Bed Wars arena(s).");
    }

    public void shutdown() {
        if (ticker != null) {
            ticker.cancel();
            ticker = null;
        }
        shutdownArenas();
    }

    private void shutdownArenas() {
        for (Arena arena : arenas.values()) {
            arena.shutdown();
        }
        arenas.clear();
        playerArenas.clear();
    }

    private void tick() {
        for (Arena arena : arenas.values()) {
            try {
                arena.tick();
            } catch (RuntimeException exception) {
                plugin.getLogger().log(Level.SEVERE, "Arena '" + arena.key() + "' tick failed", exception);
                arena.forceStop();
            }
        }
    }

    private void releasePlayer(UUID playerId, PlayerSnapshot snapshot) {
        playerArenas.remove(playerId);
        if (Bukkit.getPlayer(playerId) == null) {
            pendingRestores.put(playerId, snapshot);
        }
    }
}
