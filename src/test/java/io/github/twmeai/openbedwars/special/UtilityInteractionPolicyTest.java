package io.github.twmeai.openbedwars.special;

import org.bukkit.Material;
import org.bukkit.inventory.EquipmentSlot;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class UtilityInteractionPolicyTest {
    @Test
    void alwaysHandlesTheMainHand() {
        assertTrue(UtilityInteractionPolicy.shouldHandle(EquipmentSlot.HAND, Material.CHEST));
    }

    @Test
    void handlesOffhandUtilityWhenMainHandHasNoCompetingUse() {
        assertTrue(UtilityInteractionPolicy.shouldHandle(EquipmentSlot.OFF_HAND, Material.AIR));
        assertTrue(UtilityInteractionPolicy.shouldHandle(EquipmentSlot.OFF_HAND, Material.WOODEN_SWORD));
        assertTrue(UtilityInteractionPolicy.shouldHandle(EquipmentSlot.OFF_HAND, null));
    }

    @Test
    void givesEveryInteractiveUtilityMaterialMainHandPriority() {
        List<Material> utilities = List.of(
                Material.GOLDEN_APPLE,
                Material.SNOWBALL,
                Material.IRON_GOLEM_SPAWN_EGG,
                Material.FIRE_CHARGE,
                Material.TNT,
                Material.ENDER_PEARL,
                Material.WATER_BUCKET,
                Material.EGG,
                Material.MILK_BUCKET,
                Material.SPONGE,
                Material.CHEST
        );

        utilities.forEach(material -> assertFalse(
                UtilityInteractionPolicy.shouldHandle(EquipmentSlot.OFF_HAND, material), material.name()));
    }
}
