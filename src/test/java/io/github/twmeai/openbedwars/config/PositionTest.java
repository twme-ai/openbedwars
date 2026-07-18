package io.github.twmeai.openbedwars.config;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PositionTest {
    @Test
    void floorsNegativeBlockCoordinatesCorrectly() {
        Position.BlockPosition block = new Position(-1.2, 64.9, -0.01, 0, 0).block();

        assertEquals(-2, block.x());
        assertEquals(64, block.y());
        assertEquals(-1, block.z());
    }
}
