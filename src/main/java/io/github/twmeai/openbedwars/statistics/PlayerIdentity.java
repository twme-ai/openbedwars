package io.github.twmeai.openbedwars.statistics;

import java.util.Objects;
import java.util.UUID;

public record PlayerIdentity(UUID uuid, String name) {
    public PlayerIdentity {
        Objects.requireNonNull(uuid, "uuid");
        Objects.requireNonNull(name, "name");
    }
}
