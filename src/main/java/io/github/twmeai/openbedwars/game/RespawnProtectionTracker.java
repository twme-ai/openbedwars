package io.github.twmeai.openbedwars.game;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

final class RespawnProtectionTracker {
    private final Map<UUID, Long> protectedUntil = new HashMap<>();

    void grant(UUID playerId, long nowNanos, int seconds) {
        if (seconds <= 0) {
            protectedUntil.remove(playerId);
            return;
        }
        protectedUntil.put(playerId, nowNanos + seconds * 1_000_000_000L);
    }

    boolean isProtected(UUID playerId, long nowNanos) {
        Long expiry = protectedUntil.get(playerId);
        if (expiry == null) return false;
        if (nowNanos < expiry) return true;
        protectedUntil.remove(playerId);
        return false;
    }

    void remove(UUID playerId) {
        protectedUntil.remove(playerId);
    }

    void clear() {
        protectedUntil.clear();
    }
}
