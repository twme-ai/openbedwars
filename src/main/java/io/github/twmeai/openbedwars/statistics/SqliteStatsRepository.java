package io.github.twmeai.openbedwars.statistics;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public final class SqliteStatsRepository implements StatsRepository {
    private static final String UPSERT = """
            INSERT INTO player_stats (
                uuid, last_name, games, wins, losses, kills, final_kills,
                deaths, final_deaths, beds_broken, experience
            ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            ON CONFLICT(uuid) DO UPDATE SET
                last_name = excluded.last_name,
                games = player_stats.games + excluded.games,
                wins = player_stats.wins + excluded.wins,
                losses = player_stats.losses + excluded.losses,
                kills = player_stats.kills + excluded.kills,
                final_kills = player_stats.final_kills + excluded.final_kills,
                deaths = player_stats.deaths + excluded.deaths,
                final_deaths = player_stats.final_deaths + excluded.final_deaths,
                beds_broken = player_stats.beds_broken + excluded.beds_broken,
                experience = player_stats.experience + excluded.experience
            """;

    private final String jdbcUrl;
    private final ExecutorService executor;

    public SqliteStatsRepository(File file) {
        this.jdbcUrl = "jdbc:sqlite:" + file.getAbsolutePath();
        this.executor = Executors.newSingleThreadExecutor(
                Thread.ofPlatform().name("openbedwars-database").factory());
        try {
            Class.forName("org.sqlite.JDBC");
            initialize();
        } catch (ClassNotFoundException | SQLException exception) {
            executor.shutdownNow();
            throw new IllegalStateException("Could not initialize SQLite statistics", exception);
        }
    }

    @Override
    public CompletableFuture<PlayerStatistics> load(UUID uuid, String currentName) {
        return supply(() -> {
            try (Connection connection = connect();
                 PreparedStatement statement = connection.prepareStatement(
                         "SELECT * FROM player_stats WHERE uuid = ?")) {
                statement.setString(1, uuid.toString());
                try (ResultSet result = statement.executeQuery()) {
                    if (!result.next()) {
                        return PlayerStatistics.empty(new PlayerIdentity(uuid, currentName));
                    }
                    return map(result);
                }
            }
        });
    }

    @Override
    public CompletableFuture<Optional<PlayerStatistics>> findByName(String name) {
        return supply(() -> {
            try (Connection connection = connect();
                 PreparedStatement statement = connection.prepareStatement(
                         "SELECT * FROM player_stats WHERE last_name = ? COLLATE NOCASE LIMIT 1")) {
                statement.setString(1, name);
                try (ResultSet result = statement.executeQuery()) {
                    return result.next() ? Optional.of(map(result)) : Optional.empty();
                }
            }
        });
    }

    @Override
    public CompletableFuture<Void> apply(Map<PlayerIdentity, StatsDelta> deltas) {
        Map<PlayerIdentity, StatsDelta> snapshot = Map.copyOf(deltas);
        return run(() -> {
            try (Connection connection = connect()) {
                connection.setAutoCommit(false);
                try (PreparedStatement statement = connection.prepareStatement(UPSERT)) {
                    for (Map.Entry<PlayerIdentity, StatsDelta> entry : snapshot.entrySet()) {
                        bind(statement, entry.getKey(), entry.getValue());
                        statement.addBatch();
                    }
                    statement.executeBatch();
                    connection.commit();
                } catch (SQLException exception) {
                    connection.rollback();
                    throw exception;
                }
            }
        });
    }

    @Override
    public void close() {
        executor.shutdown();
        try {
            if (!executor.awaitTermination(5, TimeUnit.SECONDS)) {
                executor.shutdownNow();
            }
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            executor.shutdownNow();
        }
    }

    private void initialize() throws SQLException {
        try (Connection connection = connect(); Statement statement = connection.createStatement()) {
            statement.execute("PRAGMA journal_mode=WAL");
            statement.execute("""
                    CREATE TABLE IF NOT EXISTS player_stats (
                        uuid TEXT PRIMARY KEY NOT NULL,
                        last_name TEXT NOT NULL,
                        games INTEGER NOT NULL DEFAULT 0,
                        wins INTEGER NOT NULL DEFAULT 0,
                        losses INTEGER NOT NULL DEFAULT 0,
                        kills INTEGER NOT NULL DEFAULT 0,
                        final_kills INTEGER NOT NULL DEFAULT 0,
                        deaths INTEGER NOT NULL DEFAULT 0,
                        final_deaths INTEGER NOT NULL DEFAULT 0,
                        beds_broken INTEGER NOT NULL DEFAULT 0,
                        experience INTEGER NOT NULL DEFAULT 0
                    )
                    """);
            statement.execute("CREATE INDEX IF NOT EXISTS idx_player_stats_name ON player_stats(last_name COLLATE NOCASE)");
        }
    }

    private Connection connect() throws SQLException {
        Connection connection = DriverManager.getConnection(jdbcUrl);
        try (Statement statement = connection.createStatement()) {
            statement.execute("PRAGMA busy_timeout=5000");
            statement.execute("PRAGMA foreign_keys=ON");
        }
        return connection;
    }

    private PlayerStatistics map(ResultSet result) throws SQLException {
        return new PlayerStatistics(
                UUID.fromString(result.getString("uuid")),
                result.getString("last_name"),
                result.getInt("games"),
                result.getInt("wins"),
                result.getInt("losses"),
                result.getInt("kills"),
                result.getInt("final_kills"),
                result.getInt("deaths"),
                result.getInt("final_deaths"),
                result.getInt("beds_broken"),
                result.getLong("experience")
        );
    }

    private void bind(PreparedStatement statement, PlayerIdentity player, StatsDelta delta) throws SQLException {
        statement.setString(1, player.uuid().toString());
        statement.setString(2, player.name());
        statement.setInt(3, delta.games());
        statement.setInt(4, delta.wins());
        statement.setInt(5, delta.losses());
        statement.setInt(6, delta.kills());
        statement.setInt(7, delta.finalKills());
        statement.setInt(8, delta.deaths());
        statement.setInt(9, delta.finalDeaths());
        statement.setInt(10, delta.bedsBroken());
        statement.setLong(11, delta.experience());
    }

    private <T> CompletableFuture<T> supply(SqlSupplier<T> supplier) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return supplier.get();
            } catch (SQLException exception) {
                throw new CompletionException(exception);
            }
        }, executor);
    }

    private CompletableFuture<Void> run(SqlRunnable runnable) {
        return CompletableFuture.runAsync(() -> {
            try {
                runnable.run();
            } catch (SQLException exception) {
                throw new CompletionException(exception);
            }
        }, executor);
    }

    @FunctionalInterface
    private interface SqlSupplier<T> {
        T get() throws SQLException;
    }

    @FunctionalInterface
    private interface SqlRunnable {
        void run() throws SQLException;
    }
}
