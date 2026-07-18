package io.github.twmeai.openbedwars.game;

import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.scoreboard.Scoreboard;

import java.util.Collection;

public final class PlayerSnapshot {
    private final Location location;
    private final GameMode gameMode;
    private final ItemStack[] storage;
    private final ItemStack[] armor;
    private final ItemStack offHand;
    private final ItemStack[] enderChest;
    private final int level;
    private final float experience;
    private final int food;
    private final float saturation;
    private final double health;
    private final boolean allowFlight;
    private final boolean flying;
    private final Collection<PotionEffect> effects;
    private final Scoreboard scoreboard;

    private PlayerSnapshot(Player player) {
        location = player.getLocation().clone();
        gameMode = player.getGameMode();
        storage = cloneItems(player.getInventory().getStorageContents());
        armor = cloneItems(player.getInventory().getArmorContents());
        offHand = cloneItem(player.getInventory().getItemInOffHand());
        enderChest = cloneItems(player.getEnderChest().getContents());
        level = player.getLevel();
        experience = player.getExp();
        food = player.getFoodLevel();
        saturation = player.getSaturation();
        health = player.getHealth();
        allowFlight = player.getAllowFlight();
        flying = player.isFlying();
        effects = ListCopy.effects(player.getActivePotionEffects());
        scoreboard = player.getScoreboard();
    }

    public static PlayerSnapshot capture(Player player) {
        return new PlayerSnapshot(player);
    }

    public void restore(Player player) {
        player.getInventory().clear();
        player.getInventory().setStorageContents(cloneItems(storage));
        player.getInventory().setArmorContents(cloneItems(armor));
        player.getInventory().setItemInOffHand(cloneItem(offHand));
        player.getEnderChest().clear();
        player.getEnderChest().setContents(cloneItems(enderChest));
        player.setLevel(level);
        player.setExp(experience);
        player.setFoodLevel(food);
        player.setSaturation(saturation);
        for (PotionEffect effect : player.getActivePotionEffects()) {
            player.removePotionEffect(effect.getType());
        }
        player.addPotionEffects(effects);
        player.setGameMode(gameMode);
        player.setAllowFlight(allowFlight);
        if (allowFlight) {
            player.setFlying(flying);
        }
        double maxHealth = player.getAttribute(Attribute.MAX_HEALTH).getValue();
        player.setHealth(Math.max(0.1, Math.min(health, maxHealth)));
        player.teleportAsync(location);
        player.setScoreboard(scoreboard);
    }

    private static ItemStack[] cloneItems(ItemStack[] items) {
        ItemStack[] clone = new ItemStack[items.length];
        for (int index = 0; index < items.length; index++) {
            clone[index] = cloneItem(items[index]);
        }
        return clone;
    }

    private static ItemStack cloneItem(ItemStack item) {
        return item == null ? null : item.clone();
    }

    private static final class ListCopy {
        private ListCopy() {
        }

        private static Collection<PotionEffect> effects(Collection<PotionEffect> effects) {
            return java.util.List.copyOf(effects);
        }
    }
}
