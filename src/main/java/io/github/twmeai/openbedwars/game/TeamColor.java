package io.github.twmeai.openbedwars.game;

import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Color;
import org.bukkit.DyeColor;
import org.bukkit.Material;

import java.util.Arrays;
import java.util.Locale;
import java.util.Optional;

public enum TeamColor {
    RED("Red", NamedTextColor.RED, DyeColor.RED, Color.RED, Material.RED_WOOL),
    BLUE("Blue", NamedTextColor.BLUE, DyeColor.BLUE, Color.BLUE, Material.BLUE_WOOL),
    GREEN("Green", NamedTextColor.GREEN, DyeColor.LIME, Color.LIME, Material.LIME_WOOL),
    YELLOW("Yellow", NamedTextColor.YELLOW, DyeColor.YELLOW, Color.YELLOW, Material.YELLOW_WOOL),
    AQUA("Aqua", NamedTextColor.AQUA, DyeColor.LIGHT_BLUE, Color.AQUA, Material.LIGHT_BLUE_WOOL),
    WHITE("White", NamedTextColor.WHITE, DyeColor.WHITE, Color.WHITE, Material.WHITE_WOOL),
    PINK("Pink", NamedTextColor.LIGHT_PURPLE, DyeColor.PINK, Color.FUCHSIA, Material.PINK_WOOL),
    GRAY("Gray", NamedTextColor.GRAY, DyeColor.GRAY, Color.GRAY, Material.GRAY_WOOL);

    private final String displayName;
    private final NamedTextColor textColor;
    private final DyeColor dyeColor;
    private final Color leatherColor;
    private final Material wool;

    TeamColor(
            String displayName,
            NamedTextColor textColor,
            DyeColor dyeColor,
            Color leatherColor,
            Material wool
    ) {
        this.displayName = displayName;
        this.textColor = textColor;
        this.dyeColor = dyeColor;
        this.leatherColor = leatherColor;
        this.wool = wool;
    }

    public String key() {
        return name().toLowerCase(Locale.ROOT);
    }

    public String displayName() {
        return displayName;
    }

    public NamedTextColor textColor() {
        return textColor;
    }

    public DyeColor dyeColor() {
        return dyeColor;
    }

    public Color leatherColor() {
        return leatherColor;
    }

    public Material wool() {
        return wool;
    }

    public Material terracotta() {
        return switch (this) {
            case RED -> Material.RED_TERRACOTTA;
            case BLUE -> Material.BLUE_TERRACOTTA;
            case GREEN -> Material.LIME_TERRACOTTA;
            case YELLOW -> Material.YELLOW_TERRACOTTA;
            case AQUA -> Material.LIGHT_BLUE_TERRACOTTA;
            case WHITE -> Material.WHITE_TERRACOTTA;
            case PINK -> Material.PINK_TERRACOTTA;
            case GRAY -> Material.GRAY_TERRACOTTA;
        };
    }

    public Material glass() {
        return switch (this) {
            case RED -> Material.RED_STAINED_GLASS;
            case BLUE -> Material.BLUE_STAINED_GLASS;
            case GREEN -> Material.LIME_STAINED_GLASS;
            case YELLOW -> Material.YELLOW_STAINED_GLASS;
            case AQUA -> Material.LIGHT_BLUE_STAINED_GLASS;
            case WHITE -> Material.WHITE_STAINED_GLASS;
            case PINK -> Material.PINK_STAINED_GLASS;
            case GRAY -> Material.GRAY_STAINED_GLASS;
        };
    }

    public static Optional<TeamColor> fromKey(String key) {
        return Arrays.stream(values())
                .filter(color -> color.key().equalsIgnoreCase(key))
                .findFirst();
    }
}
