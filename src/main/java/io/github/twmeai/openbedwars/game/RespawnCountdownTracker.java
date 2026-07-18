package io.github.twmeai.openbedwars.game;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.OptionalInt;
import java.util.UUID;
import java.util.function.Consumer;

final class RespawnCountdownTracker<T> {
    private static final long NANOS_PER_SECOND = 1_000_000_000L;

    private final Consumer<T> taskCanceller;
    private final Map<UUID, Countdown<T>> countdowns = new HashMap<>();

    RespawnCountdownTracker(Consumer<T> taskCanceller) {
        this.taskCanceller = Objects.requireNonNull(taskCanceller, "taskCanceller");
    }

    void begin(UUID playerId, long nowNanos, int seconds) {
        Objects.requireNonNull(playerId, "playerId");
        if (seconds < 0) {
            throw new IllegalArgumentException("Respawn seconds cannot be negative");
        }
        Countdown<T> previous = countdowns.put(
                playerId,
                new Countdown<>(nowNanos + seconds * NANOS_PER_SECOND)
        );
        cancelTask(previous);
    }

    boolean attach(UUID playerId, T task) {
        Objects.requireNonNull(playerId, "playerId");
        Objects.requireNonNull(task, "task");
        Countdown<T> countdown = countdowns.get(playerId);
        if (countdown == null) {
            taskCanceller.accept(task);
            return false;
        }
        cancelTask(countdown);
        countdown.task = task;
        return true;
    }

    OptionalInt remainingSeconds(UUID playerId, long nowNanos) {
        Countdown<T> countdown = countdowns.get(Objects.requireNonNull(playerId, "playerId"));
        if (countdown == null) return OptionalInt.empty();
        long remainingNanos = Math.max(0L, countdown.readyAtNanos - nowNanos);
        long seconds = remainingNanos / NANOS_PER_SECOND;
        if (remainingNanos % NANOS_PER_SECOND != 0) seconds++;
        return OptionalInt.of((int) Math.min(Integer.MAX_VALUE, seconds));
    }

    boolean isCurrentTask(UUID playerId, T task) {
        Countdown<T> countdown = countdowns.get(Objects.requireNonNull(playerId, "playerId"));
        return countdown != null && countdown.task == Objects.requireNonNull(task, "task");
    }

    boolean contains(UUID playerId) {
        return countdowns.containsKey(Objects.requireNonNull(playerId, "playerId"));
    }

    void pause(UUID playerId) {
        Countdown<T> countdown = countdowns.get(Objects.requireNonNull(playerId, "playerId"));
        cancelTask(countdown);
    }

    void remove(UUID playerId) {
        cancelTask(countdowns.remove(Objects.requireNonNull(playerId, "playerId")));
    }

    int size() {
        return countdowns.size();
    }

    void clear() {
        countdowns.values().forEach(this::cancelTask);
        countdowns.clear();
    }

    private void cancelTask(Countdown<T> countdown) {
        if (countdown == null || countdown.task == null) return;
        taskCanceller.accept(countdown.task);
        countdown.task = null;
    }

    private static final class Countdown<T> {
        private final long readyAtNanos;
        private T task;

        private Countdown(long readyAtNanos) {
            this.readyAtNanos = readyAtNanos;
        }
    }
}
