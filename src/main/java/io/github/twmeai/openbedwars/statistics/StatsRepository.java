package io.github.twmeai.openbedwars.statistics;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public interface StatsRepository extends AutoCloseable {
    CompletableFuture<PlayerStatistics> load(UUID uuid, String currentName);

    CompletableFuture<Optional<PlayerStatistics>> findByName(String name);

    CompletableFuture<Void> apply(Map<PlayerIdentity, StatsDelta> deltas);

    @Override
    void close();
}
