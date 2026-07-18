package io.github.twmeai.openbedwars.special;

import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PopupTowerBlueprintTest {
    @Test
    void blueprintHasACompleteUniqueTowerShape() {
        List<PopupTowerBlueprint.Offset> blocks = PopupTowerBlueprint.blocks();

        assertEquals(116, blocks.size());
        assertEquals(116, new HashSet<>(blocks.stream()
                .map(block -> List.of(block.right(), block.y(), block.forward()))
                .toList()).size());
        assertEquals(111, blocks.stream()
                .filter(block -> block.kind() == PopupTowerBlueprint.Kind.WOOL)
                .count());
        assertEquals(5, blocks.stream()
                .filter(block -> block.kind() == PopupTowerBlueprint.Kind.LADDER)
                .count());
    }

    @Test
    void entranceRemainsOpenAndBattlementsStayOnThePerimeter() {
        List<PopupTowerBlueprint.Offset> blocks = PopupTowerBlueprint.blocks();

        assertFalse(hasBlock(blocks, 0, 0, -2));
        assertFalse(hasBlock(blocks, 0, 1, -2));
        assertEquals(8, blocks.stream().filter(block -> block.y() == 6).count());
        assertTrue(blocks.stream()
                .filter(block -> block.y() == 6)
                .allMatch(block -> Math.abs(block.right()) == 2 || Math.abs(block.forward()) == 2));
    }

    @Test
    void sharedBlueprintCannotBeMutated() {
        assertThrows(UnsupportedOperationException.class, () -> PopupTowerBlueprint.blocks().clear());
    }

    private boolean hasBlock(List<PopupTowerBlueprint.Offset> blocks, int right, int y, int forward) {
        return blocks.stream().anyMatch(block -> block.right() == right
                && block.y() == y
                && block.forward() == forward);
    }
}
