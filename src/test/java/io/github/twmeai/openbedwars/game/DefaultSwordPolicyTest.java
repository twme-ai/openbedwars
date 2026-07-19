package io.github.twmeai.openbedwars.game;

import org.bukkit.Material;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class DefaultSwordPolicyTest {
    @Test
    void addsFallbackWhenNoSwordRemains() {
        assertEquals(new DefaultSwordPolicy.Decision(false, true),
                DefaultSwordPolicy.decide(List.of(Material.WHITE_WOOL, Material.IRON_INGOT)));
    }

    @Test
    void removesFallbackWhenAnUpgradedSwordExists() {
        assertEquals(new DefaultSwordPolicy.Decision(true, false),
                DefaultSwordPolicy.decide(List.of(Material.WOODEN_SWORD, Material.STONE_SWORD)));
        assertEquals(new DefaultSwordPolicy.Decision(false, false),
                DefaultSwordPolicy.decide(List.of(Material.DIAMOND_SWORD)));
    }

    @Test
    void keepsExactlyOneFallbackSword() {
        assertEquals(new DefaultSwordPolicy.Decision(false, false),
                DefaultSwordPolicy.decide(List.of(Material.WOODEN_SWORD)));
        assertEquals(new DefaultSwordPolicy.Decision(true, true),
                DefaultSwordPolicy.decide(List.of(Material.WOODEN_SWORD, Material.WOODEN_SWORD)));
    }
}
