package io.github.twmeai.openbedwars.special;

import org.bukkit.Material;
import org.bukkit.inventory.EquipmentSlot;

import java.util.EnumSet;
import java.util.Set;

public final class UtilityInteractionPolicy {
    private static final Set<Material> MAIN_HAND_PRIORITY = EnumSet.of(
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

    private UtilityInteractionPolicy() {
    }

    public static boolean shouldHandle(EquipmentSlot hand, Material mainHand) {
        return hand == EquipmentSlot.HAND || mainHand == null || !MAIN_HAND_PRIORITY.contains(mainHand);
    }
}
