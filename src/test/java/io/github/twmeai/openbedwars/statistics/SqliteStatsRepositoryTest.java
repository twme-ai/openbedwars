package io.github.twmeai.openbedwars.statistics;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SqliteStatsRepositoryTest {
    @TempDir
    Path temporaryDirectory;

    @Test
    void atomicallyAccumulatesStatisticsAndFindsNamesCaseInsensitively() {
        UUID uuid = UUID.randomUUID();
        PlayerIdentity player = new PlayerIdentity(uuid, "ExamplePlayer");
        try (SqliteStatsRepository repository = new SqliteStatsRepository(
                temporaryDirectory.resolve("statistics.db").toFile())) {
            repository.apply(Map.of(player, new StatsDelta(1, 1, 0, 3, 1, 2, 0, 1, 225))).join();
            repository.apply(Map.of(player, new StatsDelta(1, 0, 1, 2, 2, 3, 1, 0, 125))).join();

            PlayerStatistics statistics = repository.load(uuid, player.name()).join();
            assertEquals(2, statistics.games());
            assertEquals(1, statistics.wins());
            assertEquals(5, statistics.kills());
            assertEquals(3, statistics.finalKills());
            assertEquals(5, statistics.deaths());
            assertEquals(350, statistics.experience());
            assertTrue(repository.findByName("exampleplayer").join().isPresent());
        }
    }
}
