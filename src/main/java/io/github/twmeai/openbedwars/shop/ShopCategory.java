package io.github.twmeai.openbedwars.shop;

import org.bukkit.Material;

public enum ShopCategory {
    QUICK_BUY("Quick Buy", Material.NETHER_STAR),
    BLOCKS("Blocks", Material.WHITE_WOOL),
    MELEE("Melee", Material.GOLDEN_SWORD),
    ARMOR("Armor", Material.CHAINMAIL_BOOTS),
    TOOLS("Tools", Material.IRON_PICKAXE),
    RANGED("Ranged", Material.BOW),
    POTIONS("Potions", Material.BREWING_STAND),
    UTILITY("Utility", Material.TNT);

    private final String displayName;
    private final Material icon;

    ShopCategory(String displayName, Material icon) {
        this.displayName = displayName;
        this.icon = icon;
    }

    public String displayName() { return displayName; }
    public Material icon() { return icon; }
    public String translationKey() { return "shop.category." + name().toLowerCase(java.util.Locale.ROOT); }
}
