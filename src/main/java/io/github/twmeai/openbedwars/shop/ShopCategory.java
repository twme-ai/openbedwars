package io.github.twmeai.openbedwars.shop;

import org.bukkit.Material;

public enum ShopCategory {
    QUICK_BUY(Material.NETHER_STAR),
    BLOCKS(Material.WHITE_WOOL),
    MELEE(Material.GOLDEN_SWORD),
    ARMOR(Material.CHAINMAIL_BOOTS),
    TOOLS(Material.IRON_PICKAXE),
    RANGED(Material.BOW),
    POTIONS(Material.BREWING_STAND),
    UTILITY(Material.TNT);

    private final Material icon;

    ShopCategory(Material icon) {
        this.icon = icon;
    }

    public Material icon() { return icon; }
    public String translationKey() { return "shop.category." + name().toLowerCase(java.util.Locale.ROOT); }
}
