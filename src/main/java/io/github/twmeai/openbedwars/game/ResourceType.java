package io.github.twmeai.openbedwars.game;

import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.Material;

import java.util.Locale;
import java.util.Optional;

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

    public String translationKey() {
        return "resource." + name().toLowerCase(Locale.ROOT);
    }

    public TextColor textColor() {
        return switch (this) {
            case IRON -> NamedTextColor.WHITE;
            case GOLD -> NamedTextColor.GOLD;
            case DIAMOND -> NamedTextColor.AQUA;
            case EMERALD -> NamedTextColor.GREEN;
        };
    }

    public int generatorCap(int tier) {
        return switch (this) {
            case IRON -> 48;
            case GOLD -> 8;
            case DIAMOND, EMERALD -> tier <= 1 ? 4 : tier == 2 ? 6 : 8;
        };
    }

    public static Optional<ResourceType> fromMaterial(Material material) {
        for (ResourceType type : values()) {
            if (type.material == material) return Optional.of(type);
        }
        return Optional.empty();
    }

}
