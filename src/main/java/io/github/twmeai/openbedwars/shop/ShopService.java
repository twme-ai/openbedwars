package io.github.twmeai.openbedwars.shop;

import io.github.twmeai.openbedwars.OpenBedWarsPlugin;
import io.github.twmeai.openbedwars.game.Arena;
import io.github.twmeai.openbedwars.game.ArenaManager;
import io.github.twmeai.openbedwars.game.GamePhase;
import io.github.twmeai.openbedwars.game.PlayerState;
import io.github.twmeai.openbedwars.game.ResourceType;
import io.github.twmeai.openbedwars.game.TeamState;
import io.github.twmeai.openbedwars.game.ToolTier;
import io.github.twmeai.openbedwars.message.MessageService;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Fireball;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class ShopService implements Listener {
    private static final int[] ITEM_SLOTS = {
            19, 20, 21, 22, 23, 24, 25,
            28, 29, 30, 31, 32, 33, 34,
            37, 38, 39, 40, 41, 42, 43
    };
    private static final Set<Material> PICKAXES = EnumSet.of(
            Material.WOODEN_PICKAXE, Material.IRON_PICKAXE, Material.GOLDEN_PICKAXE, Material.DIAMOND_PICKAXE);
    private static final Set<Material> AXES = EnumSet.of(
            Material.WOODEN_AXE, Material.STONE_AXE, Material.IRON_AXE, Material.DIAMOND_AXE);

    private final OpenBedWarsPlugin plugin;
    private final ArenaManager arenas;
    private final MessageService messages;
    private final NamespacedKey shopTypeKey;
    private final NamespacedKey utilityTypeKey;

    public ShopService(OpenBedWarsPlugin plugin, ArenaManager arenas) {
        this.plugin = plugin;
        this.arenas = arenas;
        this.messages = plugin.messages();
        this.shopTypeKey = new NamespacedKey(plugin, "shop_type");
        this.utilityTypeKey = new NamespacedKey(plugin, "utility_type");
    }

    public boolean openItemShop(Player player) {
        Arena arena = playableArena(player);
        if (arena == null) {
            messages.send(player, "error.shop-game-only");
            return false;
        }
        openCategory(player, arena, ShopCategory.BLOCKS);
        return true;
    }

    private void openCategory(Player player, Arena arena, ShopCategory category) {
        ShopHolder holder = new ShopHolder(category);
        Inventory inventory = Bukkit.createInventory(holder, 54, messages.render(player, "shop.item-title"));
        holder.attach(inventory);
        render(player, arena, holder);
        player.openInventory(inventory);
    }

    private void render(Player player, Arena arena, ShopHolder holder) {
        Inventory inventory = holder.getInventory();
        inventory.clear();
        ItemStack filler = named(Material.GRAY_STAINED_GLASS_PANE, Component.text(" "));
        for (int slot = 9; slot <= 17; slot++) {
            inventory.setItem(slot, filler);
        }
        for (int index = 0; index < ShopCategory.values().length; index++) {
            ShopCategory category = ShopCategory.values()[index];
            ItemStack icon = named(category.icon(), messages.render(player, category.translationKey())
                    .color(category == holder.category() ? NamedTextColor.GREEN : NamedTextColor.GRAY));
            if (category == holder.category()) {
                ItemMeta meta = icon.getItemMeta();
                meta.addEnchant(Enchantment.UNBREAKING, 1, true);
                meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
                icon.setItemMeta(meta);
                inventory.setItem(9 + index, named(Material.LIME_STAINED_GLASS_PANE, Component.text(" ")));
            }
            inventory.setItem(index, icon);
        }

        Map<Integer, ShopItem> slotItems = new HashMap<>();
        int itemIndex = 0;
        PlayerState state = arena.playerState(player.getUniqueId()).orElseThrow();
        TeamState team = arena.teamOf(player.getUniqueId()).orElseThrow();
        for (ShopItem item : ShopItem.values()) {
            if (item.category() != holder.category() || itemIndex >= ITEM_SLOTS.length) {
                continue;
            }
            int slot = ITEM_SLOTS[itemIndex++];
            inventory.setItem(slot, displayItem(player, item, state, team));
            slotItems.put(slot, item);
        }
        holder.items(slotItems);
    }

    private ItemStack displayItem(Player player, ShopItem item, PlayerState state, TeamState team) {
        Offer offer = offer(item, state);
        Material material = displayMaterial(item, offer.tier());
        if (item.action() == ShopItem.Action.TEAM_WOOL) material = team.color().wool();
        if (item.action() == ShopItem.Action.TEAM_TERRACOTTA) material = team.color().terracotta();
        if (item.action() == ShopItem.Action.TEAM_GLASS) material = team.color().glass();
        ItemStack icon = new ItemStack(material, Math.min(item.amount(), material.getMaxStackSize()));
        ItemMeta meta = icon.getItemMeta();
        meta.displayName(messages.render(player, item.translationKey())
                .color(offer.maxed() ? NamedTextColor.GREEN : NamedTextColor.WHITE)
                .decoration(TextDecoration.ITALIC, false));
        List<Component> lore = new ArrayList<>();
        lore.add(messages.render(player, "shop.cost",
                MessageService.number("cost", offer.cost()),
                MessageService.text("resource", offer.currency().displayName(offer.cost())),
                currencyColor(offer.currency())));
        lore.add(Component.empty());
        lore.add(messages.render(player, offer.maxed() ? "shop.maxed" : "shop.click"));
        meta.lore(lore);
        meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
        icon.setItemMeta(meta);
        return icon;
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getInventory().getHolder() instanceof ShopHolder holder)) {
            return;
        }
        event.setCancelled(true);
        if (!(event.getWhoClicked() instanceof Player player) || event.getClickedInventory() != event.getInventory()) {
            return;
        }
        int categoryIndex = event.getRawSlot();
        if (categoryIndex >= 0 && categoryIndex < ShopCategory.values().length) {
            Arena arena = playableArena(player);
            if (arena != null) {
                holder.category(ShopCategory.values()[categoryIndex]);
                render(player, arena, holder);
            }
            return;
        }
        ShopItem item = holder.items().get(event.getRawSlot());
        if (item == null) {
            return;
        }
        Arena arena = playableArena(player);
        if (arena != null && purchase(player, arena, item)) {
            render(player, arena, holder);
        }
    }

    private boolean purchase(Player player, Arena arena, ShopItem item) {
        PlayerState state = arena.playerState(player.getUniqueId()).orElseThrow();
        TeamState team = arena.teamOf(player.getUniqueId()).orElseThrow();
        Offer offer = offer(item, state);
        if (offer.maxed()) {
            messages.send(player, "error.already-owned");
            return false;
        }
        int available = InventoryCurrency.count(player.getInventory(), offer.currency());
        if (available < offer.cost()) {
            messages.send(player, "error.not-enough-resources",
                    MessageService.number("cost", offer.cost() - available),
                    MessageService.text("resource", offer.currency().displayName(offer.cost() - available)));
            return false;
        }
        if (!canFit(player, item)) {
            messages.send(player, "error.inventory-full");
            return false;
        }
        InventoryCurrency.take(player.getInventory(), offer.currency(), offer.cost());
        grant(player, arena, state, team, item, offer);
        messages.send(player, "shop.purchased",
                MessageService.component("item", messages.render(player, item.translationKey())));
        return true;
    }

    private void grant(Player player, Arena arena, PlayerState state, TeamState team, ShopItem item, Offer offer) {
        switch (item.action()) {
            case ARMOR -> {
                state.armorTier(item.armorTier());
                arena.refreshPersistentEquipment(player);
            }
            case SHEARS -> {
                state.shears(true);
                arena.refreshPersistentEquipment(player);
            }
            case PICKAXE -> {
                removeMaterials(player, PICKAXES);
                state.pickaxeTier(offer.tier());
                arena.refreshPersistentEquipment(player);
            }
            case AXE -> {
                removeMaterials(player, AXES);
                state.axeTier(offer.tier());
                arena.refreshPersistentEquipment(player);
            }
            default -> {
                ItemStack granted = createGrantedItem(item, team);
                player.getInventory().addItem(granted);
                if (item.action() == ShopItem.Action.SWORD) {
                    arena.applyTeamEnchantments(team);
                }
            }
        }
    }

    private ItemStack createGrantedItem(ShopItem item, TeamState team) {
        Material material = switch (item.action()) {
            case TEAM_WOOL -> team.color().wool();
            case TEAM_TERRACOTTA -> team.color().terracotta();
            case TEAM_GLASS -> team.color().glass();
            default -> item.icon();
        };
        ItemStack stack = new ItemStack(material, item.amount());
        switch (item.action()) {
            case SWORD -> makeUnbreakable(stack);
            case KNOCKBACK_STICK -> stack.addUnsafeEnchantment(Enchantment.KNOCKBACK, 1);
            case POWER_BOW -> stack.addUnsafeEnchantment(Enchantment.POWER, 1);
            case PUNCH_BOW -> {
                stack.addUnsafeEnchantment(Enchantment.POWER, 1);
                stack.addUnsafeEnchantment(Enchantment.PUNCH, 1);
            }
            case SPEED_POTION -> configurePotion(stack, PotionEffectType.SPEED, 45 * 20, 1, Color.AQUA);
            case JUMP_POTION -> configurePotion(stack, PotionEffectType.JUMP_BOOST, 45 * 20, 4, Color.LIME);
            case INVISIBILITY_POTION -> configurePotion(stack, PotionEffectType.INVISIBILITY, 30 * 20, 0, Color.SILVER);
            case BED_BUG -> tagUtility(stack, "bed_bug");
            case DREAM_DEFENDER -> tagUtility(stack, "dream_defender");
            case BRIDGE_EGG -> tagUtility(stack, "bridge_egg");
            case MAGIC_MILK -> tagUtility(stack, "magic_milk");
            case POPUP_TOWER -> tagUtility(stack, "popup_tower");
            default -> {
            }
        }
        return stack;
    }

    private Offer offer(ShopItem item, PlayerState state) {
        if (item.action() == ShopItem.Action.PICKAXE) {
            return toolOffer(state.pickaxeTier());
        }
        if (item.action() == ShopItem.Action.AXE) {
            return toolOffer(state.axeTier());
        }
        boolean maxed = item.action() == ShopItem.Action.SHEARS && state.hasShears()
                || item.action() == ShopItem.Action.ARMOR && state.armorTier().ordinal() >= item.armorTier().ordinal();
        return new Offer(item.cost(), item.currency(), null, maxed);
    }

    private Offer toolOffer(ToolTier current) {
        return switch (current) {
            case NONE -> new Offer(10, ResourceType.IRON, ToolTier.WOOD, false);
            case WOOD -> new Offer(10, ResourceType.IRON, ToolTier.IRON, false);
            case IRON -> new Offer(3, ResourceType.GOLD, ToolTier.GOLD, false);
            case GOLD -> new Offer(6, ResourceType.GOLD, ToolTier.DIAMOND, false);
            case DIAMOND -> new Offer(0, ResourceType.IRON, ToolTier.DIAMOND, true);
        };
    }

    private Material displayMaterial(ShopItem item, ToolTier tier) {
        if (item.action() == ShopItem.Action.PICKAXE && tier != null) {
            return switch (tier) {
                case WOOD -> Material.WOODEN_PICKAXE;
                case IRON -> Material.IRON_PICKAXE;
                case GOLD -> Material.GOLDEN_PICKAXE;
                case DIAMOND -> Material.DIAMOND_PICKAXE;
                case NONE -> Material.WOODEN_PICKAXE;
            };
        }
        if (item.action() == ShopItem.Action.AXE && tier != null) {
            return switch (tier) {
                case WOOD -> Material.WOODEN_AXE;
                case IRON -> Material.STONE_AXE;
                case GOLD -> Material.IRON_AXE;
                case DIAMOND -> Material.DIAMOND_AXE;
                case NONE -> Material.WOODEN_AXE;
            };
        }
        return item.icon();
    }

    private boolean canFit(Player player, ShopItem item) {
        if (item.action() == ShopItem.Action.ARMOR || item.action() == ShopItem.Action.SHEARS
                || item.action() == ShopItem.Action.PICKAXE || item.action() == ShopItem.Action.AXE) {
            return true;
        }
        return player.getInventory().firstEmpty() >= 0
                || player.getInventory().contains(item.icon());
    }

    private void removeMaterials(Player player, Set<Material> materials) {
        for (ItemStack stack : player.getInventory().getStorageContents()) {
            if (stack != null && materials.contains(stack.getType())) {
                player.getInventory().remove(stack);
            }
        }
    }

    private void configurePotion(ItemStack stack, PotionEffectType type, int duration, int amplifier, Color color) {
        PotionMeta meta = (PotionMeta) stack.getItemMeta();
        meta.addCustomEffect(new PotionEffect(type, duration, amplifier), true);
        meta.setColor(color);
        stack.setItemMeta(meta);
    }

    private void makeUnbreakable(ItemStack stack) {
        ItemMeta meta = stack.getItemMeta();
        meta.setUnbreakable(true);
        meta.addItemFlags(ItemFlag.HIDE_UNBREAKABLE);
        stack.setItemMeta(meta);
    }

    private void tagUtility(ItemStack stack, String type) {
        ItemMeta meta = stack.getItemMeta();
        meta.getPersistentDataContainer().set(utilityTypeKey, PersistentDataType.STRING, type);
        stack.setItemMeta(meta);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onFireball(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_AIR && event.getAction() != Action.RIGHT_CLICK_BLOCK) {
            return;
        }
        ItemStack item = event.getItem();
        if (item == null || item.getType() != Material.FIRE_CHARGE) {
            return;
        }
        Arena arena = playableArena(event.getPlayer());
        if (arena == null) {
            return;
        }
        event.setCancelled(true);
        item.setAmount(item.getAmount() - 1);
        Fireball fireball = event.getPlayer().launchProjectile(Fireball.class);
        fireball.setYield(2.0f);
        fireball.setIsIncendiary(false);
        fireball.setVelocity(event.getPlayer().getLocation().getDirection().multiply(1.4));
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onShopkeeper(PlayerInteractEntityEvent event) {
        String type = event.getRightClicked().getPersistentDataContainer()
                .get(shopTypeKey, PersistentDataType.STRING);
        if (type == null) return;
        event.setCancelled(true);
        if (type.equals("item")) {
            openItemShop(event.getPlayer());
        } else if (type.equals("upgrades")) {
            plugin.upgradeService().openUpgradeShop(event.getPlayer());
        }
    }

    private Arena playableArena(Player player) {
        Arena arena = arenas.arenaOf(player).orElse(null);
        if (arena == null || arena.phase() != GamePhase.RUNNING) {
            return null;
        }
        PlayerState state = arena.playerState(player.getUniqueId()).orElse(null);
        return state != null && !state.eliminated() && !state.respawning() ? arena : null;
    }

    private ItemStack named(Material material, Component name) {
        ItemStack stack = new ItemStack(material);
        ItemMeta meta = stack.getItemMeta();
        meta.displayName(name.decoration(TextDecoration.ITALIC, false));
        meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
        stack.setItemMeta(meta);
        return stack;
    }

    private TagResolver currencyColor(ResourceType resource) {
        return MessageService.color("cost_color", switch (resource) {
            case IRON -> NamedTextColor.WHITE;
            case GOLD -> NamedTextColor.GOLD;
            case DIAMOND -> NamedTextColor.AQUA;
            case EMERALD -> NamedTextColor.GREEN;
        });
    }

    private record Offer(int cost, ResourceType currency, ToolTier tier, boolean maxed) {
    }
}
