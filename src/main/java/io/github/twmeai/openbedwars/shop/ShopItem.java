package io.github.twmeai.openbedwars.shop;

import io.github.twmeai.openbedwars.game.ArmorTier;
import io.github.twmeai.openbedwars.game.ResourceType;
import org.bukkit.Material;

public enum ShopItem {
    WOOL(ShopCategory.BLOCKS, Material.WHITE_WOOL, 16, 4, ResourceType.IRON, Action.TEAM_WOOL),
    TERRACOTTA(ShopCategory.BLOCKS, Material.WHITE_TERRACOTTA, 16, 12, ResourceType.IRON, Action.TEAM_TERRACOTTA),
    GLASS(ShopCategory.BLOCKS, Material.WHITE_STAINED_GLASS, 4, 12, ResourceType.IRON, Action.TEAM_GLASS),
    END_STONE(ShopCategory.BLOCKS, Material.END_STONE, 12, 24, ResourceType.IRON, Action.NORMAL),
    LADDER(ShopCategory.BLOCKS, Material.LADDER, 16, 4, ResourceType.IRON, Action.NORMAL),
    PLANKS(ShopCategory.BLOCKS, Material.OAK_PLANKS, 16, 4, ResourceType.GOLD, Action.NORMAL),
    OBSIDIAN(ShopCategory.BLOCKS, Material.OBSIDIAN, 4, 4, ResourceType.EMERALD, Action.NORMAL),

    STONE_SWORD(ShopCategory.MELEE, Material.STONE_SWORD, 1, 10, ResourceType.IRON, Action.SWORD),
    IRON_SWORD(ShopCategory.MELEE, Material.IRON_SWORD, 1, 7, ResourceType.GOLD, Action.SWORD),
    DIAMOND_SWORD(ShopCategory.MELEE, Material.DIAMOND_SWORD, 1, 4, ResourceType.EMERALD, Action.SWORD),
    KNOCKBACK_STICK(ShopCategory.MELEE, Material.STICK, 1, 5, ResourceType.GOLD, Action.KNOCKBACK_STICK),

    CHAINMAIL_ARMOR(ShopCategory.ARMOR, Material.CHAINMAIL_BOOTS, 1, 40, ResourceType.IRON, Action.ARMOR, ArmorTier.CHAINMAIL),
    IRON_ARMOR(ShopCategory.ARMOR, Material.IRON_BOOTS, 1, 12, ResourceType.GOLD, Action.ARMOR, ArmorTier.IRON),
    DIAMOND_ARMOR(ShopCategory.ARMOR, Material.DIAMOND_BOOTS, 1, 6, ResourceType.EMERALD, Action.ARMOR, ArmorTier.DIAMOND),

    SHEARS(ShopCategory.TOOLS, Material.SHEARS, 1, 20, ResourceType.IRON, Action.SHEARS),
    PICKAXE(ShopCategory.TOOLS, Material.WOODEN_PICKAXE, 1, 10, ResourceType.IRON, Action.PICKAXE),
    AXE(ShopCategory.TOOLS, Material.WOODEN_AXE, 1, 10, ResourceType.IRON, Action.AXE),

    BOW(ShopCategory.RANGED, Material.BOW, 1, 12, ResourceType.GOLD, Action.NORMAL),
    POWER_BOW(ShopCategory.RANGED, Material.BOW, 1, 24, ResourceType.GOLD, Action.POWER_BOW),
    PUNCH_BOW(ShopCategory.RANGED, Material.BOW, 1, 6, ResourceType.EMERALD, Action.PUNCH_BOW),
    ARROWS(ShopCategory.RANGED, Material.ARROW, 8, 2, ResourceType.GOLD, Action.NORMAL),

    SPEED_POTION(ShopCategory.POTIONS, Material.POTION, 1, 1, ResourceType.EMERALD, Action.SPEED_POTION),
    JUMP_POTION(ShopCategory.POTIONS, Material.POTION, 1, 1, ResourceType.EMERALD, Action.JUMP_POTION),
    INVISIBILITY_POTION(ShopCategory.POTIONS, Material.POTION, 1, 2, ResourceType.EMERALD, Action.INVISIBILITY_POTION),

    GOLDEN_APPLE(ShopCategory.UTILITY, Material.GOLDEN_APPLE, 1, 3, ResourceType.GOLD, Action.NORMAL),
    BED_BUG(ShopCategory.UTILITY, Material.SNOWBALL, 1, 30, ResourceType.IRON, Action.BED_BUG),
    DREAM_DEFENDER(ShopCategory.UTILITY, Material.IRON_GOLEM_SPAWN_EGG, 1, 120, ResourceType.IRON, Action.DREAM_DEFENDER),
    FIREBALL(ShopCategory.UTILITY, Material.FIRE_CHARGE, 1, 40, ResourceType.IRON, Action.NORMAL),
    TNT(ShopCategory.UTILITY, Material.TNT, 1, 4, ResourceType.GOLD, Action.NORMAL),
    ENDER_PEARL(ShopCategory.UTILITY, Material.ENDER_PEARL, 1, 4, ResourceType.EMERALD, Action.NORMAL),
    WATER_BUCKET(ShopCategory.UTILITY, Material.WATER_BUCKET, 1, 3, ResourceType.EMERALD, Action.NORMAL),
    BRIDGE_EGG(ShopCategory.UTILITY, Material.EGG, 1, 1, ResourceType.EMERALD, Action.BRIDGE_EGG),
    MAGIC_MILK(ShopCategory.UTILITY, Material.MILK_BUCKET, 1, 4, ResourceType.GOLD, Action.MAGIC_MILK),
    SPONGE(ShopCategory.UTILITY, Material.SPONGE, 4, 3, ResourceType.GOLD, Action.NORMAL),
    POPUP_TOWER(ShopCategory.UTILITY, Material.CHEST, 1, 24, ResourceType.IRON, Action.POPUP_TOWER);

    private final ShopCategory category;
    private final Material icon;
    private final int amount;
    private final int cost;
    private final ResourceType currency;
    private final Action action;
    private final ArmorTier armorTier;

    ShopItem(ShopCategory category, Material icon, int amount, int cost, ResourceType currency, Action action) {
        this(category, icon, amount, cost, currency, action, null);
    }

    ShopItem(
            ShopCategory category,
            Material icon,
            int amount,
            int cost,
            ResourceType currency,
            Action action,
            ArmorTier armorTier
    ) {
        this.category = category;
        this.icon = icon;
        this.amount = amount;
        this.cost = cost;
        this.currency = currency;
        this.action = action;
        this.armorTier = armorTier;
    }

    public ShopCategory category() { return category; }
    public Material icon() { return icon; }
    public int amount() { return amount; }
    public int cost() { return cost; }
    public ResourceType currency() { return currency; }
    public Action action() { return action; }
    public ArmorTier armorTier() { return armorTier; }
    public String translationKey() { return "shop.item." + name().toLowerCase(java.util.Locale.ROOT); }

    public enum Action {
        NORMAL,
        TEAM_WOOL,
        TEAM_TERRACOTTA,
        TEAM_GLASS,
        SWORD,
        KNOCKBACK_STICK,
        ARMOR,
        SHEARS,
        PICKAXE,
        AXE,
        POWER_BOW,
        PUNCH_BOW,
        SPEED_POTION,
        JUMP_POTION,
        INVISIBILITY_POTION,
        BED_BUG,
        DREAM_DEFENDER,
        BRIDGE_EGG,
        MAGIC_MILK,
        POPUP_TOWER
    }
}
