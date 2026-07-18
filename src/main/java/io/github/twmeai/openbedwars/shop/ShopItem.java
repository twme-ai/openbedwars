package io.github.twmeai.openbedwars.shop;

import io.github.twmeai.openbedwars.game.ArmorTier;
import io.github.twmeai.openbedwars.game.ResourceType;
import org.bukkit.Material;

public enum ShopItem {
    WOOL(ShopCategory.BLOCKS, "Wool", Material.WHITE_WOOL, 16, 4, ResourceType.IRON, Action.TEAM_WOOL),
    TERRACOTTA(ShopCategory.BLOCKS, "Hardened Clay", Material.WHITE_TERRACOTTA, 16, 12, ResourceType.IRON, Action.TEAM_TERRACOTTA),
    GLASS(ShopCategory.BLOCKS, "Blast-Proof Glass", Material.WHITE_STAINED_GLASS, 4, 12, ResourceType.IRON, Action.TEAM_GLASS),
    END_STONE(ShopCategory.BLOCKS, "End Stone", Material.END_STONE, 12, 24, ResourceType.IRON, Action.NORMAL),
    LADDER(ShopCategory.BLOCKS, "Ladder", Material.LADDER, 16, 4, ResourceType.IRON, Action.NORMAL),
    PLANKS(ShopCategory.BLOCKS, "Oak Wood Planks", Material.OAK_PLANKS, 16, 4, ResourceType.GOLD, Action.NORMAL),
    OBSIDIAN(ShopCategory.BLOCKS, "Obsidian", Material.OBSIDIAN, 4, 4, ResourceType.EMERALD, Action.NORMAL),

    STONE_SWORD(ShopCategory.MELEE, "Stone Sword", Material.STONE_SWORD, 1, 10, ResourceType.IRON, Action.SWORD),
    IRON_SWORD(ShopCategory.MELEE, "Iron Sword", Material.IRON_SWORD, 1, 7, ResourceType.GOLD, Action.SWORD),
    DIAMOND_SWORD(ShopCategory.MELEE, "Diamond Sword", Material.DIAMOND_SWORD, 1, 4, ResourceType.EMERALD, Action.SWORD),
    KNOCKBACK_STICK(ShopCategory.MELEE, "Knockback Stick", Material.STICK, 1, 5, ResourceType.GOLD, Action.KNOCKBACK_STICK),

    CHAINMAIL_ARMOR(ShopCategory.ARMOR, "Permanent Chainmail Armor", Material.CHAINMAIL_BOOTS, 1, 40, ResourceType.IRON, Action.ARMOR, ArmorTier.CHAINMAIL),
    IRON_ARMOR(ShopCategory.ARMOR, "Permanent Iron Armor", Material.IRON_BOOTS, 1, 12, ResourceType.GOLD, Action.ARMOR, ArmorTier.IRON),
    DIAMOND_ARMOR(ShopCategory.ARMOR, "Permanent Diamond Armor", Material.DIAMOND_BOOTS, 1, 6, ResourceType.EMERALD, Action.ARMOR, ArmorTier.DIAMOND),

    SHEARS(ShopCategory.TOOLS, "Permanent Shears", Material.SHEARS, 1, 20, ResourceType.IRON, Action.SHEARS),
    PICKAXE(ShopCategory.TOOLS, "Pickaxe", Material.WOODEN_PICKAXE, 1, 10, ResourceType.IRON, Action.PICKAXE),
    AXE(ShopCategory.TOOLS, "Axe", Material.WOODEN_AXE, 1, 10, ResourceType.IRON, Action.AXE),

    BOW(ShopCategory.RANGED, "Bow", Material.BOW, 1, 12, ResourceType.GOLD, Action.NORMAL),
    POWER_BOW(ShopCategory.RANGED, "Bow (Power I)", Material.BOW, 1, 24, ResourceType.GOLD, Action.POWER_BOW),
    PUNCH_BOW(ShopCategory.RANGED, "Bow (Power I, Punch I)", Material.BOW, 1, 6, ResourceType.EMERALD, Action.PUNCH_BOW),
    ARROWS(ShopCategory.RANGED, "Arrow", Material.ARROW, 8, 2, ResourceType.GOLD, Action.NORMAL),

    SPEED_POTION(ShopCategory.POTIONS, "Speed II Potion (45 seconds)", Material.POTION, 1, 1, ResourceType.EMERALD, Action.SPEED_POTION),
    JUMP_POTION(ShopCategory.POTIONS, "Jump V Potion (45 seconds)", Material.POTION, 1, 1, ResourceType.EMERALD, Action.JUMP_POTION),
    INVISIBILITY_POTION(ShopCategory.POTIONS, "Invisibility Potion (30 seconds)", Material.POTION, 1, 2, ResourceType.EMERALD, Action.INVISIBILITY_POTION),

    GOLDEN_APPLE(ShopCategory.UTILITY, "Golden Apple", Material.GOLDEN_APPLE, 1, 3, ResourceType.GOLD, Action.NORMAL),
    FIREBALL(ShopCategory.UTILITY, "Fireball", Material.FIRE_CHARGE, 1, 40, ResourceType.IRON, Action.NORMAL),
    TNT(ShopCategory.UTILITY, "TNT", Material.TNT, 1, 4, ResourceType.GOLD, Action.NORMAL),
    ENDER_PEARL(ShopCategory.UTILITY, "Ender Pearl", Material.ENDER_PEARL, 1, 4, ResourceType.EMERALD, Action.NORMAL),
    WATER_BUCKET(ShopCategory.UTILITY, "Water Bucket", Material.WATER_BUCKET, 1, 3, ResourceType.EMERALD, Action.NORMAL),
    BRIDGE_EGG(ShopCategory.UTILITY, "Bridge Egg", Material.EGG, 1, 1, ResourceType.EMERALD, Action.NORMAL),
    MAGIC_MILK(ShopCategory.UTILITY, "Magic Milk", Material.MILK_BUCKET, 1, 4, ResourceType.GOLD, Action.NORMAL),
    SPONGE(ShopCategory.UTILITY, "Sponge", Material.SPONGE, 4, 3, ResourceType.GOLD, Action.NORMAL),
    POPUP_TOWER(ShopCategory.UTILITY, "Compact Pop-up Tower", Material.CHEST, 1, 24, ResourceType.IRON, Action.NORMAL);

    private final ShopCategory category;
    private final String displayName;
    private final Material icon;
    private final int amount;
    private final int cost;
    private final ResourceType currency;
    private final Action action;
    private final ArmorTier armorTier;

    ShopItem(ShopCategory category, String displayName, Material icon, int amount, int cost, ResourceType currency, Action action) {
        this(category, displayName, icon, amount, cost, currency, action, null);
    }

    ShopItem(
            ShopCategory category,
            String displayName,
            Material icon,
            int amount,
            int cost,
            ResourceType currency,
            Action action,
            ArmorTier armorTier
    ) {
        this.category = category;
        this.displayName = displayName;
        this.icon = icon;
        this.amount = amount;
        this.cost = cost;
        this.currency = currency;
        this.action = action;
        this.armorTier = armorTier;
    }

    public ShopCategory category() { return category; }
    public String displayName() { return displayName; }
    public Material icon() { return icon; }
    public int amount() { return amount; }
    public int cost() { return cost; }
    public ResourceType currency() { return currency; }
    public Action action() { return action; }
    public ArmorTier armorTier() { return armorTier; }

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
        INVISIBILITY_POTION
    }
}
