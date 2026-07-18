package io.github.twmeai.openbedwars.shop;

import io.github.twmeai.openbedwars.game.TrapType;
import org.bukkit.Material;

public enum UpgradeType {
    SHARPNESS(Material.IRON_SWORD, null),
    PROTECTION(Material.IRON_CHESTPLATE, null),
    HASTE(Material.GOLDEN_PICKAXE, null),
    FORGE(Material.FURNACE, null),
    HEAL_POOL(Material.BEACON, null),
    DRAGON_BUFF(Material.DRAGON_EGG, null),
    ITS_A_TRAP(Material.TRIPWIRE_HOOK, TrapType.ITS_A_TRAP),
    COUNTER_OFFENSIVE(Material.FEATHER, TrapType.COUNTER_OFFENSIVE),
    ALARM(Material.REDSTONE_TORCH, TrapType.ALARM),
    MINER_FATIGUE(Material.IRON_PICKAXE, TrapType.MINER_FATIGUE);

    private final Material icon;
    private final TrapType trap;

    UpgradeType(Material icon, TrapType trap) {
        this.icon = icon;
        this.trap = trap;
    }

    public Material icon() { return icon; }
    public TrapType trap() { return trap; }
    public boolean isTrap() { return trap != null; }
    public String translationKey() { return "upgrade.name." + name().toLowerCase(java.util.Locale.ROOT); }
}
