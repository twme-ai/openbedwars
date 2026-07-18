package io.github.twmeai.openbedwars.game;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

final class ExclusiveArenaWorlds<T> {
    private final Map<UUID, T> owners = new LinkedHashMap<>();

    boolean claim(UUID worldId, T arena) {
        Objects.requireNonNull(worldId, "worldId");
        Objects.requireNonNull(arena, "arena");
        return owners.putIfAbsent(worldId, arena) == null;
    }

    Optional<T> owner(UUID worldId) {
        return Optional.ofNullable(owners.get(Objects.requireNonNull(worldId, "worldId")));
    }

    int size() {
        return owners.size();
    }

    void clear() {
        owners.clear();
    }
}
