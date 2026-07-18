package io.github.twmeai.openbedwars.game;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

final class PlayerCooldownTracker {
    private static final long NANOS_PER_TICK = 50_000_000L;

    private final Map<UUID, Long> deadlines = new HashMap<>();

    boolean tryAcquire(UUID playerId, long nowNanos, int cooldownTicks) {
        if (cooldownTicks < 0) {
            throw new IllegalArgumentException("Cooldown ticks cannot be negative");
        }
        Long deadline = deadlines.get(playerId);
        if (deadline != null && deadline - nowNanos > 0) {
            return false;
        }
        if (cooldownTicks == 0) {
            deadlines.remove(playerId);
        } else {
            deadlines.put(playerId, nowNanos + cooldownTicks * NANOS_PER_TICK);
        }
        return true;
    }

    void remove(UUID playerId) {
        deadlines.remove(playerId);
    }

    void clear() {
        deadlines.clear();
    }
}
