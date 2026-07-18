package io.github.twmeai.openbedwars.game;

import org.junit.jupiter.api.Test;

import java.util.EnumMap;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EventScheduleTest {
    @Test
    void reportsCurrentAndUpcomingEvents() {
        EventSchedule schedule = standardSchedule();

        assertEquals(GameEventType.DIAMOND_II, schedule.eventAt(360).orElseThrow().type());
        EventSchedule.UpcomingEvent upcoming = schedule.nextAfter(361).orElseThrow();
        assertEquals(GameEventType.EMERALD_II, upcoming.type());
        assertEquals(359, upcoming.secondsRemaining());
        assertTrue(schedule.nextAfter(3000).isEmpty());
    }

    @Test
    void rejectsDuplicateEventTimes() {
        EnumMap<GameEventType, Integer> times = times();
        times.put(GameEventType.EMERALD_II, 360);

        assertThrows(IllegalArgumentException.class, () -> new EventSchedule(times));
    }

    private static EventSchedule standardSchedule() {
        return new EventSchedule(times());
    }

    private static EnumMap<GameEventType, Integer> times() {
        EnumMap<GameEventType, Integer> times = new EnumMap<>(GameEventType.class);
        times.put(GameEventType.DIAMOND_II, 360);
        times.put(GameEventType.EMERALD_II, 720);
        times.put(GameEventType.DIAMOND_III, 1080);
        times.put(GameEventType.EMERALD_III, 1440);
        times.put(GameEventType.BED_DESTRUCTION, 1800);
        times.put(GameEventType.SUDDEN_DEATH, 2400);
        times.put(GameEventType.GAME_END, 3000);
        return times;
    }
}
