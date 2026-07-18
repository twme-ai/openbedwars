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
import java.util.UUID;
import java.util.logging.Level;

public final class ArenaManager {
    private final OpenBedWarsPlugin plugin;
    private final Map<String, Arena> arenas = new LinkedHashMap<>();
    private final Map<UUID, Arena> playerArenas = new java.util.HashMap<>();
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

    public boolean leave(Player player, boolean notify) {
        Arena arena = playerArenas.get(player.getUniqueId());
        return arena != null && arena.leave(player, notify);
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
            arenas.put(definition.key(), new Arena(plugin, definition, settings, world, playerArenas::remove));
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
}
