package io.github.twmeai.openbedwars.game;

import org.bukkit.Material;

import java.util.Collection;
import java.util.EnumSet;
import java.util.Set;

final class DefaultSwordPolicy {
    private static final Set<Material> UPGRADED_SWORDS = EnumSet.of(
            Material.STONE_SWORD,
            Material.IRON_SWORD,
            Material.GOLDEN_SWORD,
            Material.DIAMOND_SWORD,
            Material.NETHERITE_SWORD
    );

    private DefaultSwordPolicy() {
    }

    static Decision decide(Collection<Material> contents) {
        long woodenSwords = contents.stream().filter(Material.WOODEN_SWORD::equals).count();
        boolean hasUpgradedSword = contents.stream().anyMatch(UPGRADED_SWORDS::contains);
        if (hasUpgradedSword) return new Decision(woodenSwords > 0, false);
        if (woodenSwords == 0) return new Decision(false, true);
        if (woodenSwords > 1) return new Decision(true, true);
        return new Decision(false, false);
    }

    record Decision(boolean removeWoodenSwords, boolean addWoodenSword) {
    }
}
