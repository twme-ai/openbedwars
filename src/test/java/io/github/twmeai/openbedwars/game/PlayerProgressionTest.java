package io.github.twmeai.openbedwars.game;

import org.junit.jupiter.api.Test;
import org.bukkit.Material;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PlayerProgressionTest {
    @Test
    void toolsDowngradeOneTierButNeverBelowWood() {
        assertEquals(ToolTier.GOLD, ToolTier.DIAMOND.downgrade());
        assertEquals(ToolTier.IRON, ToolTier.GOLD.downgrade());
        assertEquals(ToolTier.WOOD, ToolTier.IRON.downgrade());
        assertEquals(ToolTier.WOOD, ToolTier.WOOD.downgrade());
        assertEquals(ToolTier.NONE, ToolTier.NONE.downgrade());
    }

    @Test
    void generatorMaterialsMapToResourceTypes() {
        assertEquals(ResourceType.IRON, ResourceType.fromMaterial(Material.IRON_INGOT).orElseThrow());
        assertEquals(ResourceType.GOLD, ResourceType.fromMaterial(Material.GOLD_INGOT).orElseThrow());
        assertEquals(ResourceType.DIAMOND, ResourceType.fromMaterial(Material.DIAMOND).orElseThrow());
        assertEquals(ResourceType.EMERALD, ResourceType.fromMaterial(Material.EMERALD).orElseThrow());
        assertEquals(java.util.Optional.empty(), ResourceType.fromMaterial(Material.WHITE_WOOL));
    }

    @Test
    void generatorCapsMatchClassicTierLimits() {
        assertEquals(48, ResourceType.IRON.generatorCap(1));
        assertEquals(8, ResourceType.GOLD.generatorCap(1));
        assertEquals(4, ResourceType.DIAMOND.generatorCap(1));
        assertEquals(6, ResourceType.DIAMOND.generatorCap(2));
        assertEquals(8, ResourceType.EMERALD.generatorCap(3));
    }
}
