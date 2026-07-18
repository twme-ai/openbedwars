package io.github.twmeai.openbedwars.shop;

import io.github.twmeai.openbedwars.game.TrapType;
import org.bukkit.Material;

public enum UpgradeType {
    SHARPNESS("Sharpened Swords", Material.IRON_SWORD, null),
    PROTECTION("Reinforced Armor", Material.IRON_CHESTPLATE, null),
    HASTE("Maniac Miner", Material.GOLDEN_PICKAXE, null),
    FORGE("Forge", Material.FURNACE, null),
    HEAL_POOL("Heal Pool", Material.BEACON, null),
    DRAGON_BUFF("Dragon Buff", Material.DRAGON_EGG, null),
    ITS_A_TRAP("It's a Trap!", Material.TRIPWIRE_HOOK, TrapType.ITS_A_TRAP),
    COUNTER_OFFENSIVE("Counter-Offensive Trap", Material.FEATHER, TrapType.COUNTER_OFFENSIVE),
    ALARM("Alarm Trap", Material.REDSTONE_TORCH, TrapType.ALARM),
    MINER_FATIGUE("Miner Fatigue Trap", Material.IRON_PICKAXE, TrapType.MINER_FATIGUE);

    private final String displayName;
    private final Material icon;
    private final TrapType trap;

    UpgradeType(String displayName, Material icon, TrapType trap) {
        this.displayName = displayName;
        this.icon = icon;
        this.trap = trap;
    }

    public String displayName() { return displayName; }
    public Material icon() { return icon; }
    public TrapType trap() { return trap; }
    public boolean isTrap() { return trap != null; }
}
