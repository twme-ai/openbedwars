package io.github.twmeai.openbedwars.game;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.IOException;
import java.util.Optional;
import java.util.UUID;
import java.util.logging.Level;

final class PlayerSnapshotStore {
    private final JavaPlugin plugin;
    private final SnapshotJournal journal;

    PlayerSnapshotStore(JavaPlugin plugin) {
        this.plugin = plugin;
        this.journal = new SnapshotJournal(plugin.getDataFolder().toPath().resolve("pending-restores"));
    }

    boolean save(UUID playerId, PlayerSnapshot snapshot) {
        try {
            YamlConfiguration configuration = new YamlConfiguration();
            snapshot.writeTo(configuration.createSection("snapshot"));
            journal.write(playerId, configuration.saveToString());
            return true;
        } catch (IOException | RuntimeException exception) {
            plugin.getLogger().log(Level.SEVERE,
                    "Could not persist the pre-game snapshot for player " + playerId, exception);
            return false;
        }
    }

    Optional<PlayerSnapshot> load(Player player) {
        try {
            Optional<String> serialized = journal.read(player.getUniqueId());
            if (serialized.isEmpty()) return Optional.empty();
            YamlConfiguration configuration = new YamlConfiguration();
            configuration.loadFromString(serialized.orElseThrow());
            ConfigurationSection section = configuration.getConfigurationSection("snapshot");
            if (section == null) throw new InvalidConfigurationException("Missing snapshot section");
            return Optional.of(PlayerSnapshot.readFrom(section, player));
        } catch (IOException | InvalidConfigurationException | RuntimeException exception) {
            plugin.getLogger().log(Level.SEVERE,
                    "Could not load the pending snapshot for player " + player.getUniqueId(), exception);
            return Optional.empty();
        }
    }

    boolean hasPending(UUID playerId) {
        return journal.exists(playerId);
    }

    boolean delete(UUID playerId) {
        try {
            journal.delete(playerId);
            return true;
        } catch (IOException exception) {
            plugin.getLogger().log(Level.SEVERE,
                    "Could not delete the restored snapshot for player " + playerId, exception);
            return false;
        }
    }
}
