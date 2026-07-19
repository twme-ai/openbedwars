package io.github.twmeai.openbedwars.game;

import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.attribute.Attribute;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.scoreboard.Scoreboard;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

public final class PlayerSnapshot {
    private static final int SERIALIZATION_VERSION = 1;

    private final Location location;
    private final GameMode gameMode;
    private final ItemStack[] storage;
    private final ItemStack[] armor;
    private final ItemStack offHand;
    private final ItemStack cursor;
    private final int heldItemSlot;
    private final ItemStack[] enderChest;
    private final int level;
    private final float experience;
    private final int food;
    private final float saturation;
    private final double health;
    private final boolean allowFlight;
    private final boolean flying;
    private final boolean collidable;
    private final Collection<PotionEffect> effects;
    private final Scoreboard scoreboard;

    private PlayerSnapshot(Player player) {
        location = player.getLocation().clone();
        gameMode = player.getGameMode();
        storage = cloneItems(player.getInventory().getStorageContents());
        armor = cloneItems(player.getInventory().getArmorContents());
        offHand = cloneItem(player.getInventory().getItemInOffHand());
        cursor = cloneItem(player.getItemOnCursor());
        heldItemSlot = player.getInventory().getHeldItemSlot();
        enderChest = cloneItems(player.getEnderChest().getContents());
        level = player.getLevel();
        experience = player.getExp();
        food = player.getFoodLevel();
        saturation = player.getSaturation();
        health = player.getHealth();
        allowFlight = player.getAllowFlight();
        flying = player.isFlying();
        collidable = player.isCollidable();
        effects = ListCopy.effects(player.getActivePotionEffects());
        scoreboard = player.getScoreboard();
    }

    private PlayerSnapshot(
            Location location,
            GameMode gameMode,
            ItemStack[] storage,
            ItemStack[] armor,
            ItemStack offHand,
            ItemStack cursor,
            int heldItemSlot,
            ItemStack[] enderChest,
            int level,
            float experience,
            int food,
            float saturation,
            double health,
            boolean allowFlight,
            boolean flying,
            boolean collidable,
            Collection<PotionEffect> effects,
            Scoreboard scoreboard
    ) {
        this.location = location.clone();
        this.gameMode = gameMode;
        this.storage = cloneItems(storage);
        this.armor = cloneItems(armor);
        this.offHand = cloneItem(offHand);
        this.cursor = cloneItem(cursor);
        this.heldItemSlot = heldItemSlot;
        this.enderChest = cloneItems(enderChest);
        this.level = level;
        this.experience = experience;
        this.food = food;
        this.saturation = saturation;
        this.health = health;
        this.allowFlight = allowFlight;
        this.flying = flying;
        this.collidable = collidable;
        this.effects = List.copyOf(effects);
        this.scoreboard = scoreboard;
    }

    public static PlayerSnapshot capture(Player player) {
        return new PlayerSnapshot(player);
    }

    void writeTo(ConfigurationSection section) {
        World world = Objects.requireNonNull(location.getWorld(), "Snapshot location world");
        section.set("version", SERIALIZATION_VERSION);
        section.set("location.world-id", world.getUID().toString());
        section.set("location.world-name", world.getName());
        section.set("location.x", location.getX());
        section.set("location.y", location.getY());
        section.set("location.z", location.getZ());
        section.set("location.yaw", location.getYaw());
        section.set("location.pitch", location.getPitch());
        section.set("game-mode", gameMode.name());
        writeItems(section, "storage", storage);
        writeItems(section, "armor", armor);
        section.set("off-hand", cloneItem(offHand));
        section.set("cursor", cloneItem(cursor));
        section.set("held-item-slot", heldItemSlot);
        writeItems(section, "ender-chest", enderChest);
        section.set("level", level);
        section.set("experience", experience);
        section.set("food", food);
        section.set("saturation", saturation);
        section.set("health", health);
        section.set("allow-flight", allowFlight);
        section.set("flying", flying);
        section.set("collidable", collidable);
        section.set("effects", new ArrayList<>(effects));
    }

    static PlayerSnapshot readFrom(ConfigurationSection section, Player player) {
        int version = readInt(section, "version");
        if (version != SERIALIZATION_VERSION) {
            throw new IllegalArgumentException("Unsupported player snapshot version " + version);
        }
        String worldId = requireString(section, "location.world-id");
        String worldName = requireString(section, "location.world-name");
        World world = Bukkit.getWorld(UUID.fromString(worldId));
        if (world == null) world = Bukkit.getWorld(worldName);
        if (world == null) {
            throw new IllegalStateException("Snapshot world '" + worldName + "' is not loaded");
        }
        Location location = new Location(
                world,
                readDouble(section, "location.x"),
                readDouble(section, "location.y"),
                readDouble(section, "location.z"),
                (float) readDouble(section, "location.yaw"),
                (float) readDouble(section, "location.pitch")
        );
        GameMode gameMode = GameMode.valueOf(requireString(section, "game-mode"));
        Object effectValue = section.get("effects");
        if (!(effectValue instanceof List<?> serializedEffects)) {
            throw new IllegalArgumentException("Missing or invalid player snapshot effects");
        }
        List<PotionEffect> effects = serializedEffects.stream()
                .filter(PotionEffect.class::isInstance)
                .map(PotionEffect.class::cast)
                .toList();
        if (effects.size() != serializedEffects.size()) {
            throw new IllegalArgumentException("Player snapshot contains an invalid potion effect");
        }
        int heldItemSlot = readInt(section, "held-item-slot");
        if (heldItemSlot < 0 || heldItemSlot > 8) {
            throw new IllegalArgumentException("Invalid held item slot " + heldItemSlot);
        }
        ItemStack[] storage = readItems(section, "storage");
        ItemStack[] armor = readItems(section, "armor");
        ItemStack[] enderChest = readItems(section, "ender-chest");
        if (storage.length != player.getInventory().getStorageContents().length
                || armor.length != player.getInventory().getArmorContents().length
                || enderChest.length != player.getEnderChest().getSize()) {
            throw new IllegalArgumentException("Player snapshot inventory dimensions do not match the server");
        }
        int level = readInt(section, "level");
        float experience = (float) readDouble(section, "experience");
        int food = readInt(section, "food");
        float saturation = (float) readDouble(section, "saturation");
        double health = readDouble(section, "health");
        if (level < 0 || experience < 0 || experience > 1
                || food < 0 || food > 20 || saturation < 0 || health <= 0) {
            throw new IllegalArgumentException("Player snapshot contains an invalid survival state");
        }
        return new PlayerSnapshot(
                location,
                gameMode,
                storage,
                armor,
                readItem(section, "off-hand"),
                readItem(section, "cursor"),
                heldItemSlot,
                enderChest,
                level,
                experience,
                food,
                saturation,
                health,
                readBoolean(section, "allow-flight"),
                readBoolean(section, "flying"),
                readBoolean(section, "collidable"),
                effects,
                player.getScoreboard()
        );
    }

    public void restore(Player player) {
        player.closeInventory();
        player.setItemOnCursor(new ItemStack(Material.AIR));
        player.getInventory().clear();
        player.getInventory().setStorageContents(cloneItems(storage));
        player.getInventory().setArmorContents(cloneItems(armor));
        player.getInventory().setItemInOffHand(cloneItem(offHand));
        player.getInventory().setHeldItemSlot(heldItemSlot);
        player.setItemOnCursor(cloneItem(cursor));
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
        player.setCollidable(collidable);
        double maxHealth = player.getAttribute(Attribute.MAX_HEALTH).getValue();
        player.setHealth(Math.max(0.1, Math.min(health, maxHealth)));
        player.teleportAsync(location);
        player.setScoreboard(scoreboard);
        player.updateInventory();
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

    private static void writeItems(ConfigurationSection section, String path, ItemStack[] items) {
        ConfigurationSection contents = section.createSection(path);
        contents.set("size", items.length);
        ConfigurationSection slots = contents.createSection("slots");
        for (int index = 0; index < items.length; index++) {
            ItemStack item = items[index];
            if (item != null && !item.isEmpty()) slots.set(Integer.toString(index), item.clone());
        }
    }

    private static ItemStack[] readItems(ConfigurationSection section, String path) {
        ConfigurationSection contents = section.getConfigurationSection(path);
        if (contents == null) throw new IllegalArgumentException("Missing player snapshot " + path);
        int size = contents.getInt("size", -1);
        if (size < 0 || size > 100) {
            throw new IllegalArgumentException("Invalid player snapshot " + path + " size " + size);
        }
        ItemStack[] items = new ItemStack[size];
        ConfigurationSection slots = contents.getConfigurationSection("slots");
        if (slots == null) return items;
        for (String key : slots.getKeys(false)) {
            int index;
            try {
                index = Integer.parseInt(key);
            } catch (NumberFormatException exception) {
                throw new IllegalArgumentException("Invalid player snapshot slot '" + key + "'", exception);
            }
            if (index < 0 || index >= size) {
                throw new IllegalArgumentException("Player snapshot slot " + index + " is outside " + path);
            }
            Object value = slots.get(key);
            if (!(value instanceof ItemStack item)) {
                throw new IllegalArgumentException("Invalid item in player snapshot " + path + " slot " + index);
            }
            items[index] = item.clone();
        }
        return items;
    }

    private static ItemStack readItem(ConfigurationSection section, String path) {
        Object value = section.get(path);
        if (!(value instanceof ItemStack item)) {
            throw new IllegalArgumentException("Missing or invalid player snapshot " + path);
        }
        return item.clone();
    }

    private static int readInt(ConfigurationSection section, String path) {
        Object value = section.get(path);
        if (!(value instanceof Number number)
                || !Double.isFinite(number.doubleValue())
                || number.doubleValue() != number.intValue()) {
            throw new IllegalArgumentException("Missing or invalid player snapshot " + path);
        }
        return number.intValue();
    }

    private static double readDouble(ConfigurationSection section, String path) {
        Object value = section.get(path);
        if (!(value instanceof Number number) || !Double.isFinite(number.doubleValue())) {
            throw new IllegalArgumentException("Missing or invalid player snapshot " + path);
        }
        return number.doubleValue();
    }

    private static boolean readBoolean(ConfigurationSection section, String path) {
        Object value = section.get(path);
        if (!(value instanceof Boolean flag)) {
            throw new IllegalArgumentException("Missing or invalid player snapshot " + path);
        }
        return flag;
    }

    private static String requireString(ConfigurationSection section, String path) {
        String value = section.getString(path);
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Missing player snapshot " + path);
        }
        return value;
    }

    private static final class ListCopy {
        private ListCopy() {
        }

        private static Collection<PotionEffect> effects(Collection<PotionEffect> effects) {
            return java.util.List.copyOf(effects);
        }
    }
}
