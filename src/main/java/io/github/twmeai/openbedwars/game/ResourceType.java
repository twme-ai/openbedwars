package io.github.twmeai.openbedwars.game;

import org.bukkit.Material;

import java.util.Locale;

public enum ResourceType {
    IRON(Material.IRON_INGOT),
    GOLD(Material.GOLD_INGOT),
    DIAMOND(Material.DIAMOND),
    EMERALD(Material.EMERALD);

    private final Material material;

    ResourceType(Material material) {
        this.material = material;
    }

    public Material material() {
        return material;
    }

    public String displayName(int amount) {
        String name = name().toLowerCase(Locale.ROOT);
        if (this == IRON || this == GOLD || amount == 1) {
            return name;
        }
        return name + "s";
    }
}
