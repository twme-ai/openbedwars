package io.github.twmeai.openbedwars.game;

import java.util.Comparator;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

public final class EventSchedule {
    private final Map<GameEventType, Integer> times;
    private final List<ScheduledEvent> orderedEvents;

    public EventSchedule(Map<GameEventType, Integer> times) {
        Objects.requireNonNull(times, "times");
        EnumMap<GameEventType, Integer> copy = new EnumMap<>(GameEventType.class);
        for (GameEventType type : GameEventType.values()) {
            Integer second = Objects.requireNonNull(times.get(type), "Missing event time for " + type);
            if (second < 1) {
                throw new IllegalArgumentException("Event times must be positive");
            }
            copy.put(type, second);
        }
        this.times = Map.copyOf(copy);
        this.orderedEvents = copy.entrySet().stream()
                .map(entry -> new ScheduledEvent(entry.getKey(), entry.getValue()))
                .sorted(Comparator.comparingInt(ScheduledEvent::elapsedSecond))
                .toList();

        for (int index = 1; index < orderedEvents.size(); index++) {
            if (orderedEvents.get(index - 1).elapsedSecond() >= orderedEvents.get(index).elapsedSecond()) {
                throw new IllegalArgumentException("Event times must be strictly increasing");
            }
        }
    }

    public int timeOf(GameEventType type) {
        return times.get(type);
    }

    public Optional<ScheduledEvent> eventAt(int elapsedSecond) {
        return orderedEvents.stream()
                .filter(event -> event.elapsedSecond() == elapsedSecond)
                .findFirst();
    }

    public Optional<UpcomingEvent> nextAfter(int elapsedSecond) {
        return orderedEvents.stream()
                .filter(event -> event.elapsedSecond() > elapsedSecond)
                .findFirst()
                .map(event -> new UpcomingEvent(event.type(), event.elapsedSecond() - elapsedSecond));
    }

    public List<ScheduledEvent> events() {
        return orderedEvents;
    }

    public record ScheduledEvent(GameEventType type, int elapsedSecond) {
    }

    public record UpcomingEvent(GameEventType type, int secondsRemaining) {
    }
}
