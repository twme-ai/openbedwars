package io.github.twmeai.openbedwars.game;

import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.EntityEquipment;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.LeatherArmorMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

public final class KitService {
    private static final Set<Material> SWORDS = EnumSet.of(
            Material.WOODEN_SWORD,
            Material.STONE_SWORD,
            Material.IRON_SWORD,
            Material.DIAMOND_SWORD
    );

    public void prepareForGame(Player player, PlayerState state, TeamState team) {
        player.closeInventory();
        player.setItemOnCursor(new ItemStack(Material.AIR));
        player.getInventory().clear();
        for (PotionEffect effect : player.getActivePotionEffects()) {
            player.removePotionEffect(effect.getType());
        }
        player.setFoodLevel(20);
        player.setSaturation(20);
        player.setHealth(player.getAttribute(org.bukkit.attribute.Attribute.MAX_HEALTH).getValue());
        player.setFireTicks(0);
        player.setFallDistance(0);
        player.setLevel(0);
        player.setExp(0);
        player.setGameMode(org.bukkit.GameMode.SURVIVAL);
        player.setAllowFlight(false);
        givePersistentEquipment(player, state, team);
        player.updateInventory();
    }

    public void givePersistentEquipment(Player player, PlayerState state, TeamState team) {
        reconcileDefaultSword(player, team);
        equipArmor(player, state, team);
        giveTool(player, state.pickaxeTier(), true, team);
        giveTool(player, state.axeTier(), false, team);
        if (state.hasShears() && !player.getInventory().contains(Material.SHEARS)) {
            player.getInventory().addItem(unbreakable(new ItemStack(Material.SHEARS)));
        }
        if (team.haste() > 0) {
            player.addPotionEffect(new PotionEffect(
                    PotionEffectType.HASTE,
                    PotionEffect.INFINITE_DURATION,
                    team.haste() - 1,
                    false,
                    false,
                    true
            ));
        }
    }

    public void reconcileDefaultSword(Player player, TeamState team) {
        List<Material> contents = new ArrayList<>(Arrays.stream(player.getInventory().getContents())
                .filter(java.util.Objects::nonNull)
                .map(ItemStack::getType)
                .toList());
        contents.add(player.getItemOnCursor().getType());
        DefaultSwordPolicy.Decision decision = DefaultSwordPolicy.decide(contents);
        if (decision.removeWoodenSwords()) {
            player.getInventory().remove(Material.WOODEN_SWORD);
            if (player.getItemOnCursor().getType() == Material.WOODEN_SWORD) {
                player.setItemOnCursor(new ItemStack(Material.AIR));
            }
        }
        if (decision.addWoodenSword()) {
            ItemStack sword = unbreakable(new ItemStack(Material.WOODEN_SWORD));
            applySharpness(sword, team);
            player.getInventory().addItem(sword);
        }
        if (decision.removeWoodenSwords() || decision.addWoodenSword()) player.updateInventory();
    }

    public void applyTeamEnchantments(Player player, TeamState team) {
        for (ItemStack item : player.getInventory().getContents()) {
            if (item != null && SWORDS.contains(item.getType())) {
                applySharpness(item, team);
            }
        }
        EntityEquipment equipment = player.getEquipment();
        if (equipment != null) {
            for (ItemStack armor : equipment.getArmorContents()) {
                if (armor != null) {
                    applyProtection(armor, team);
                }
            }
        }
    }

    private void equipArmor(Player player, PlayerState state, TeamState team) {
        ItemStack helmet = leather(Material.LEATHER_HELMET, state.team());
        ItemStack chestplate = leather(Material.LEATHER_CHESTPLATE, state.team());
        ItemStack leggings;
        ItemStack boots;
        switch (state.armorTier()) {
            case LEATHER -> {
                leggings = leather(Material.LEATHER_LEGGINGS, state.team());
                boots = leather(Material.LEATHER_BOOTS, state.team());
            }
            case CHAINMAIL -> {
                leggings = unbreakable(new ItemStack(Material.CHAINMAIL_LEGGINGS));
                boots = unbreakable(new ItemStack(Material.CHAINMAIL_BOOTS));
            }
            case IRON -> {
                leggings = unbreakable(new ItemStack(Material.IRON_LEGGINGS));
                boots = unbreakable(new ItemStack(Material.IRON_BOOTS));
            }
            case DIAMOND -> {
                leggings = unbreakable(new ItemStack(Material.DIAMOND_LEGGINGS));
                boots = unbreakable(new ItemStack(Material.DIAMOND_BOOTS));
            }
            default -> throw new IllegalStateException("Unexpected armor tier " + state.armorTier());
        }
        for (ItemStack item : new ItemStack[]{helmet, chestplate, leggings, boots}) {
            applyProtection(item, team);
        }
        player.getInventory().setArmorContents(new ItemStack[]{boots, leggings, chestplate, helmet});
    }

    private void giveTool(Player player, ToolTier tier, boolean pickaxe, TeamState team) {
        if (tier == ToolTier.NONE) {
            return;
        }
        Material material = pickaxe ? pickaxe(tier) : axe(tier);
        if (player.getInventory().contains(material)) {
            return;
        }
        ItemStack tool = unbreakable(new ItemStack(material));
        int efficiency = switch (tier) {
            case WOOD -> 1;
            case IRON -> 2;
            case GOLD, DIAMOND -> 3;
            case NONE -> 0;
        };
        tool.addUnsafeEnchantment(Enchantment.EFFICIENCY, efficiency);
        if (pickaxe && tier == ToolTier.GOLD) {
            tool.addUnsafeEnchantment(Enchantment.SHARPNESS, 2);
        }
        player.getInventory().addItem(tool);
    }

    private Material pickaxe(ToolTier tier) {
        return switch (tier) {
            case WOOD -> Material.WOODEN_PICKAXE;
            case IRON -> Material.IRON_PICKAXE;
            case GOLD -> Material.GOLDEN_PICKAXE;
            case DIAMOND -> Material.DIAMOND_PICKAXE;
            case NONE -> throw new IllegalArgumentException("NONE has no pickaxe");
        };
    }

    private Material axe(ToolTier tier) {
        return switch (tier) {
            case WOOD -> Material.WOODEN_AXE;
            case IRON -> Material.STONE_AXE;
            case GOLD -> Material.IRON_AXE;
            case DIAMOND -> Material.DIAMOND_AXE;
            case NONE -> throw new IllegalArgumentException("NONE has no axe");
        };
    }

    private ItemStack leather(Material material, TeamColor color) {
        ItemStack item = unbreakable(new ItemStack(material));
        LeatherArmorMeta meta = (LeatherArmorMeta) item.getItemMeta();
        meta.setColor(color.leatherColor());
        item.setItemMeta(meta);
        return item;
    }

    private void applySharpness(ItemStack item, TeamState team) {
        if (team.sharpness()) {
            item.addUnsafeEnchantment(Enchantment.SHARPNESS, 1);
        }
    }

    private void applyProtection(ItemStack item, TeamState team) {
        if (team.protection() > 0) {
            item.addUnsafeEnchantment(Enchantment.PROTECTION, team.protection());
        }
    }

    private ItemStack unbreakable(ItemStack item) {
        ItemMeta meta = item.getItemMeta();
        meta.setUnbreakable(true);
        meta.addItemFlags(ItemFlag.HIDE_UNBREAKABLE);
        item.setItemMeta(meta);
        return item;
    }
}
