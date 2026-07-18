package io.github.twmeai.openbedwars.spectator;

import io.github.twmeai.openbedwars.OpenBedWarsPlugin;
import io.github.twmeai.openbedwars.game.Arena;
import io.github.twmeai.openbedwars.game.ArenaManager;
import io.github.twmeai.openbedwars.game.PlayerState;
import io.github.twmeai.openbedwars.message.MessageService;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.title.Title;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Sound;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.entity.EntityTargetLivingEntityEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;
import org.bukkit.event.player.PlayerToggleSneakEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.persistence.PersistentDataType;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

public final class SpectatorService implements Listener {
    private static final String TELEPORTER_ACTION = "teleporter";
    private static final String LEAVE_ACTION = "leave";

    private final OpenBedWarsPlugin plugin;
    private final ArenaManager arenas;
    private final MessageService messages;
    private final NamespacedKey actionKey;
    private final NamespacedKey targetKey;
    private final Set<UUID> observers = new HashSet<>();

    public SpectatorService(OpenBedWarsPlugin plugin, ArenaManager arenas) {
        this.plugin = plugin;
        this.arenas = arenas;
        this.messages = plugin.messages();
        this.actionKey = new NamespacedKey(plugin, "spectator_action");
        this.targetKey = new NamespacedKey(plugin, "spectator_target");
    }

    public void prepare(Player player, Arena arena) {
        player.closeInventory();
        if (player.getGameMode() == GameMode.SPECTATOR && player.getSpectatorTarget() != null) {
            player.setSpectatorTarget(null);
        }
        observers.add(player.getUniqueId());
        player.setGameMode(GameMode.ADVENTURE);
        player.setAllowFlight(true);
        player.setFlying(true);
        player.setCollidable(false);
        player.setFallDistance(0);
        player.getInventory().clear();
        player.getInventory().setArmorContents(null);
        player.getInventory().setItem(0, actionItem(player, Material.COMPASS, "spectator.teleporter-item",
                "spectator.teleporter-lore", TELEPORTER_ACTION));
        player.getInventory().setItem(8, actionItem(player, Material.RED_BED, "spectator.leave-item",
                "spectator.leave-lore", LEAVE_ACTION));
        player.getInventory().setHeldItemSlot(0);
        syncObserverVisibility(player, arena);
    }

    public void syncActiveViewer(Player viewer, Arena arena) {
        for (PlayerState state : arena.players().values()) {
            Player subject = Bukkit.getPlayer(state.playerId());
            if (subject == null || subject.equals(viewer)) continue;
            if (isObserver(state)) {
                viewer.hidePlayer(plugin, subject);
            } else {
                viewer.showPlayer(plugin, subject);
            }
        }
    }

    public void release(Player player) {
        observers.remove(player.getUniqueId());
        if (player.getGameMode() == GameMode.SPECTATOR && player.getSpectatorTarget() != null) {
            player.setSpectatorTarget(null);
        }
        for (Player other : Bukkit.getOnlinePlayers()) {
            if (other.equals(player)) continue;
            other.showPlayer(plugin, player);
            player.showPlayer(plugin, other);
        }
    }

    public void release(UUID playerId) {
        observers.remove(playerId);
    }

    public void openTeleporter(Player viewer, Arena arena) {
        List<Target> targets = targets(arena);
        int size = Math.min(54, Math.max(9, ((targets.size() + 8) / 9) * 9));
        SpectatorHolder holder = new SpectatorHolder(arena.key());
        Inventory inventory = Bukkit.createInventory(holder, size, messages.render(viewer, "spectator.menu-title"));
        holder.attach(inventory);
        if (targets.isEmpty()) {
            inventory.setItem(4, named(Material.BARRIER, messages.render(viewer, "spectator.no-targets")));
        } else {
            for (int index = 0; index < Math.min(size, targets.size()); index++) {
                inventory.setItem(index, targetItem(viewer, targets.get(index)));
            }
        }
        viewer.openInventory(inventory);
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onInteract(PlayerInteractEvent event) {
        Arena arena = observerArena(event.getPlayer());
        if (arena == null) return;
        event.setCancelled(true);
        if (event.getHand() != EquipmentSlot.HAND) return;
        String action = action(event.getItem());
        if (TELEPORTER_ACTION.equals(action)) {
            openTeleporter(event.getPlayer(), arena);
        } else if (LEAVE_ACTION.equals(action)) {
            arenas.leave(event.getPlayer(), true);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onInteractEntity(PlayerInteractEntityEvent event) {
        Arena arena = observerArena(event.getPlayer());
        if (arena == null) return;
        event.setCancelled(true);
        String action = action(event.getPlayer().getInventory().getItem(event.getHand()));
        if (LEAVE_ACTION.equals(action)) {
            arenas.leave(event.getPlayer(), true);
            return;
        }
        if (TELEPORTER_ACTION.equals(action)) {
            openTeleporter(event.getPlayer(), arena);
            return;
        }
        if (!(event.getRightClicked() instanceof Player target) || !isTarget(arena, target)) return;
        event.getPlayer().setGameMode(GameMode.SPECTATOR);
        event.getPlayer().setSpectatorTarget(target);
        event.getPlayer().showTitle(Title.title(
                messages.render(event.getPlayer(), "spectator.first-person-title",
                        MessageService.text("player", target.getName())),
                messages.render(event.getPlayer(), "spectator.first-person-subtitle"),
                Title.Times.times(Duration.ZERO, Duration.ofSeconds(2), Duration.ofMillis(250))));
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        if (event.getInventory().getHolder() instanceof SpectatorHolder holder) {
            event.setCancelled(true);
            Arena arena = observerArena(player);
            if (arena == null || !arena.key().equals(holder.arenaKey())) {
                player.closeInventory();
                return;
            }
            if (event.getClickedInventory() != event.getInventory()) return;
            String storedTarget = target(event.getCurrentItem());
            if (storedTarget == null) return;
            Player selected;
            try {
                selected = Bukkit.getPlayer(UUID.fromString(storedTarget));
            } catch (IllegalArgumentException ignored) {
                selected = null;
            }
            if (selected == null || !isTarget(arena, selected)) {
                messages.send(player, "spectator.target-unavailable");
                player.closeInventory();
                Bukkit.getScheduler().runTask(plugin, () -> {
                    if (observerArena(player) == arena) openTeleporter(player, arena);
                });
                return;
            }
            player.closeInventory();
            player.teleportAsync(selected.getLocation());
            player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.8f, 1.2f);
            return;
        }
        if (observerArena(player) != null) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onDrag(InventoryDragEvent event) {
        if (event.getInventory().getHolder() instanceof SpectatorHolder
                || event.getWhoClicked() instanceof Player player && observerArena(player) != null) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onPickup(EntityPickupItemEvent event) {
        if (event.getEntity() instanceof Player player && observerArena(player) != null) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onDrop(PlayerDropItemEvent event) {
        if (observerArena(event.getPlayer()) != null) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onSwapHand(PlayerSwapHandItemsEvent event) {
        if (observerArena(event.getPlayer()) != null) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onDamage(EntityDamageByEntityEvent event) {
        if (event.getDamager() instanceof Player player && observerArena(player) != null) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onTarget(EntityTargetLivingEntityEvent event) {
        if (event.getTarget() instanceof Player player && observerArena(player) != null) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onDeath(PlayerDeathEvent event) {
        restoreViewersOf(event.getPlayer());
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onQuit(PlayerQuitEvent event) {
        restoreViewersOf(event.getPlayer());
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onJoin(PlayerJoinEvent event) {
        if (arenas.arenaOf(event.getPlayer()).isEmpty()) {
            release(event.getPlayer());
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onSneak(PlayerToggleSneakEvent event) {
        Player player = event.getPlayer();
        if (!event.isSneaking() || player.getGameMode() != GameMode.SPECTATOR
                || player.getSpectatorTarget() == null) return;
        Arena arena = observerArena(player);
        if (arena != null) {
            Bukkit.getScheduler().runTask(plugin, () -> {
                if (observerArena(player) == arena) prepare(player, arena);
            });
        }
    }

    private void restoreViewersOf(Player target) {
        Arena arena = arenas.arenaOf(target).orElse(null);
        if (arena == null) return;
        for (PlayerState state : arena.players().values()) {
            Player viewer = Bukkit.getPlayer(state.playerId());
            if (viewer == null || viewer.getGameMode() != GameMode.SPECTATOR
                    || !target.equals(viewer.getSpectatorTarget())) continue;
            Bukkit.getScheduler().runTask(plugin, () -> {
                if (observerArena(viewer) == arena) prepare(viewer, arena);
            });
        }
    }

    private void syncObserverVisibility(Player observer, Arena arena) {
        for (PlayerState state : arena.players().values()) {
            Player other = Bukkit.getPlayer(state.playerId());
            if (other == null || other.equals(observer)) continue;
            observer.showPlayer(plugin, other);
            if (isObserver(state)) {
                other.showPlayer(plugin, observer);
            } else {
                other.hidePlayer(plugin, observer);
            }
        }
    }

    private Arena observerArena(Player player) {
        Arena arena = arenas.arenaOf(player).orElse(null);
        if (arena == null) return null;
        PlayerState state = arena.playerState(player.getUniqueId()).orElse(null);
        return state != null && isObserver(state) ? arena : null;
    }

    private boolean isObserver(PlayerState state) {
        return observers.contains(state.playerId());
    }

    private boolean isTarget(Arena arena, Player player) {
        PlayerState state = arena.playerState(player.getUniqueId()).orElse(null);
        return state != null && SpectatorTargetPolicy.isEligible(
                state.eliminated(), state.respawning(), state.disconnected(), player.isOnline(), player.isDead());
    }

    private List<Target> targets(Arena arena) {
        List<Target> result = new ArrayList<>();
        for (PlayerState state : arena.players().values()) {
            Player player = Bukkit.getPlayer(state.playerId());
            if (player != null && SpectatorTargetPolicy.isEligible(
                    state.eliminated(), state.respawning(), state.disconnected(), player.isOnline(), player.isDead())) {
                result.add(new Target(player, state));
            }
        }
        result.sort(Comparator.comparingInt((Target target) -> target.state().team().ordinal())
                .thenComparing(target -> target.player().getName(), String.CASE_INSENSITIVE_ORDER));
        return result;
    }

    private ItemStack actionItem(
            Player player,
            Material material,
            String nameKey,
            String loreKey,
            String action
    ) {
        ItemStack stack = named(material, messages.render(player, nameKey));
        ItemMeta meta = stack.getItemMeta();
        meta.lore(List.of(noItalic(messages.render(player, loreKey))));
        meta.getPersistentDataContainer().set(actionKey, PersistentDataType.STRING, action);
        stack.setItemMeta(meta);
        return stack;
    }

    private ItemStack targetItem(Player viewer, Target target) {
        ItemStack stack = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta meta = (SkullMeta) stack.getItemMeta();
        meta.setPlayerProfile(target.player().getPlayerProfile());
        meta.displayName(noItalic(messages.render(viewer, "spectator.target-name",
                MessageService.text("player", target.player().getName()),
                MessageService.teamColor("team_color", target.state().team()))));
        double maxHealth = target.player().getAttribute(Attribute.MAX_HEALTH).getValue();
        String health = String.format(Locale.ROOT, "%.1f/%.1f", target.player().getHealth(), maxHealth);
        meta.lore(List.of(
                noItalic(messages.render(viewer, "spectator.target-health",
                        MessageService.text("health", health))),
                noItalic(messages.render(viewer, "spectator.target-food",
                        MessageService.number("food", target.player().getFoodLevel()))),
                Component.empty(),
                noItalic(messages.render(viewer, "spectator.target-click"))
        ));
        meta.getPersistentDataContainer().set(targetKey, PersistentDataType.STRING,
                target.player().getUniqueId().toString());
        stack.setItemMeta(meta);
        return stack;
    }

    private ItemStack named(Material material, Component name) {
        ItemStack stack = new ItemStack(material);
        ItemMeta meta = stack.getItemMeta();
        meta.displayName(noItalic(name));
        stack.setItemMeta(meta);
        return stack;
    }

    private Component noItalic(Component component) {
        return component.decoration(TextDecoration.ITALIC, false);
    }

    private String action(ItemStack stack) {
        return stack == null ? null : stack.getItemMeta().getPersistentDataContainer()
                .get(actionKey, PersistentDataType.STRING);
    }

    private String target(ItemStack stack) {
        return stack == null ? null : stack.getItemMeta().getPersistentDataContainer()
                .get(targetKey, PersistentDataType.STRING);
    }

    private record Target(Player player, PlayerState state) {
    }
}
