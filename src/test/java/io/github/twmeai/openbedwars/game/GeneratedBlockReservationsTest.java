package io.github.twmeai.openbedwars.game;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GeneratedBlockReservationsTest {
    private final UUID world = UUID.randomUUID();

    @Test
    void reservesAndClaimsACompleteBlueprint() {
        GeneratedBlockReservations reservations = new GeneratedBlockReservations();
        BlockKey first = block(1);
        BlockKey second = block(2);

        assertTrue(reservations.reserve(List.of(first, second)));
        assertTrue(reservations.contains(first));
        assertTrue(reservations.claim(first));
        assertFalse(reservations.contains(first));
        assertTrue(reservations.contains(second));
    }

    @Test
    void rejectsDuplicateAndOverlappingBlueprintsAtomically() {
        GeneratedBlockReservations reservations = new GeneratedBlockReservations();
        BlockKey first = block(1);
        BlockKey second = block(2);
        BlockKey third = block(3);

        assertTrue(reservations.reserve(List.of(first, second)));
        assertFalse(reservations.reserve(List.of(second, third)));
        assertFalse(reservations.contains(third));
        assertFalse(reservations.reserve(List.of(third, third)));
        assertFalse(reservations.contains(third));
    }

    @Test
    void releasesRemainingBlueprintOrAllStateAtReset() {
        GeneratedBlockReservations reservations = new GeneratedBlockReservations();
        BlockKey first = block(1);
        BlockKey second = block(2);

        assertTrue(reservations.reserve(List.of(first, second)));
        reservations.release(List.of(first));
        assertFalse(reservations.contains(first));
        assertTrue(reservations.contains(second));

        reservations.clear();
        assertFalse(reservations.contains(second));
    }

    private BlockKey block(int x) {
        return new BlockKey(world, x, 100, 0);
    }
}
