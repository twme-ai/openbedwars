package io.github.twmeai.openbedwars.listener;

import com.destroystokyo.paper.event.entity.EntityAddToWorldEvent;
import com.destroystokyo.paper.event.entity.EntityRemoveFromWorldEvent;
import io.github.twmeai.openbedwars.OpenBedWarsPlugin;
import io.github.twmeai.openbedwars.game.Arena;
import io.github.twmeai.openbedwars.game.ArenaManager;
import io.github.twmeai.openbedwars.game.GamePhase;
import io.github.twmeai.openbedwars.game.ResourceType;
import org.bukkit.Material;
import org.bukkit.entity.EnderDragon;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.entity.TNTPrimed;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockExplodeEvent;
import org.bukkit.event.block.BlockFromToEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.block.BlockPistonExtendEvent;
import org.bukkit.event.block.BlockPistonRetractEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityChangeBlockEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.entity.EntityPotionEffectEvent;
import org.bukkit.event.entity.FoodLevelChangeEvent;
import org.bukkit.event.entity.ItemSpawnEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.entity.ProjectileLaunchEvent;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerBucketEmptyEvent;
import org.bukkit.event.player.PlayerBucketFillEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.event.inventory.CraftItemEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.inventory.PrepareItemCraftEvent;
import org.bukkit.event.player.PlayerItemDamageEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffectType;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

public final class GameListener implements Listener {
    private final OpenBedWarsPlugin plugin;
    private final ArenaManager arenas;
    private final Set<UUID> pendingSwordReconciliations = new HashSet<>();

    public GameListener(OpenBedWarsPlugin plugin, ArenaManager arenas) {
        this.plugin = plugin;
        this.arenas = arenas;
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBlockPlace(BlockPlaceEvent event) {
        arenas.arenaOf(event.getPlayer()).ifPresent(arena -> {
            Arena.PlaceResult result = arena.handlePlace(
                    event.getPlayer(), event.getBlockPlaced(), event.getBlockReplacedState());
            if (result != Arena.PlaceResult.ALLOWED) {
                event.setCancelled(true);
                if (result == Arena.PlaceResult.BUILD_LIMIT) {
                    plugin.messages().send(event.getPlayer(), "error.build-height");
                } else if (arena.phase() == GamePhase.RUNNING) {
                    plugin.messages().send(event.getPlayer(), "error.block-place-protected");
                }
                return;
            }
            if (event.getBlockPlaced().getType() == Material.TNT) {
                arena.removePlacedBlock(event.getBlockPlaced());
                event.getBlockPlaced().setType(Material.AIR, false);
                TNTPrimed tnt = event.getBlockPlaced().getWorld().spawn(
                        event.getBlockPlaced().getLocation().add(0.5, 0, 0.5), TNTPrimed.class);
                tnt.setFuseTicks(40);
                tnt.setSource(event.getPlayer());
                arena.trackTransientEntity(tnt);
            }
        });
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onProjectileLaunch(ProjectileLaunchEvent event) {
        if (event.getEntity().getShooter() instanceof Player player) {
            arenas.arenaOf(player).ifPresent(arena -> arena.trackTransientEntity(event.getEntity()));
        } else if (event.getEntity().getShooter() instanceof EnderDragon dragon) {
            arenas.arenaIn(dragon.getWorld())
                    .filter(arena -> arena.isArenaDragon(dragon))
                    .ifPresent(arena -> arena.trackTransientEntity(event.getEntity()));
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onItemSpawn(ItemSpawnEvent event) {
        arenas.arenaIn(event.getLocation().getWorld())
                .filter(arena -> arena.phase() == GamePhase.RUNNING || arena.phase() == GamePhase.ENDING)
                .ifPresent(arena -> arena.trackTransientEntity(event.getEntity()));
    }

    @EventHandler
    public void onEntityAdded(EntityAddToWorldEvent event) {
        arenas.arenaIn(event.getWorld())
                .ifPresent(arena -> arena.handleTrackedEntityAdded(event.getEntity()));
    }

    @EventHandler
    public void onEntityRemoved(EntityRemoveFromWorldEvent event) {
        arenas.arenaIn(event.getWorld())
                .ifPresent(arena -> arena.handleTrackedEntityRemoved(event.getEntity()));
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        arenas.arenaOf(event.getPlayer()).ifPresent(arena -> {
            Arena.BreakResult result = arena.handleBreak(event.getPlayer(), event.getBlock());
            switch (result) {
                case ALLOWED -> {
                }
                case BED_DESTROYED -> {
                    event.setCancelled(true);
                    event.getBlock().setType(Material.AIR, false);
                }
                case OWN_BED -> {
                    event.setCancelled(true);
                    plugin.messages().send(event.getPlayer(), "error.bed-own");
                }
                case PROTECTED -> {
                    event.setCancelled(true);
                    plugin.messages().send(event.getPlayer(), "error.block-protected");
                }
            }
        });
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onDeath(PlayerDeathEvent event) {
        Player victim = event.getPlayer();
        Arena arena = arenas.arenaOf(victim).orElse(null);
        if (arena == null || arena.phase() != GamePhase.RUNNING) {
            return;
        }
        boolean hasResourceDrops = event.getDrops().stream()
                .filter(Objects::nonNull)
                .anyMatch(item -> ResourceType.fromMaterial(item.getType()).isPresent());
        java.util.List<ItemStack> resources = DeathResourcePolicy.selectSource(
                event.getKeepInventory(),
                hasResourceDrops,
                Arrays.asList(victim.getInventory().getContents()),
                event.getDrops()).stream()
                .filter(Objects::nonNull)
                .filter(item -> !item.isEmpty())
                .filter(item -> ResourceType.fromMaterial(item.getType()).isPresent())
                .map(ItemStack::clone)
                .toList();
        event.setKeepInventory(false);
        event.getDrops().clear();
        event.setDroppedExp(0);
        event.setKeepLevel(true);
        event.deathMessage(null);
        Player killer = arena.creditedKiller(victim);
        arena.handleDeathResources(victim, killer, resources);
        arena.handleDeath(victim, killer);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onItemPickup(EntityPickupItemEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;
        arenas.arenaOf(player).ifPresent(arena -> {
            arena.handleGeneratorPickup(player, event.getItem(), event.getRemaining());
            scheduleSwordReconciliation(player);
        });
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onPlayerDamage(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof Player victim)) return;
        Arena victimArena = arenas.arenaOf(victim).orElse(null);
        if (victimArena != null && victimArena.isFriendlyDragonDamage(event.getDamager(), victim)) {
            event.setCancelled(true);
            return;
        }
        Player attacker = attackingPlayer(event.getDamager());
        if (attacker == null || attacker.equals(victim)) return;
        Arena arena = victimArena;
        Arena attackerArena = arenas.arenaOf(attacker).orElse(null);
        if (arena == null) {
            if (attackerArena != null) event.setCancelled(true);
            return;
        }
        if (attackerArena != arena || !arena.areEnemies(victim, attacker)) {
            event.setCancelled(true);
            return;
        }
        arena.revealInvisibility(attacker);
        arena.removeRespawnProtection(attacker);
        if (arena.isRespawnProtected(victim)) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerDamageResolved(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof Player victim)) return;
        Player attacker = attackingPlayer(event.getDamager());
        if (attacker == null || attacker.equals(victim)) return;
        Arena arena = arenas.arenaOf(victim).orElse(null);
        if (arena != null
                && arenas.arenaOf(attacker).orElse(null) == arena
                && arena.areEnemies(victim, attacker)) {
            arena.recordHit(victim, attacker);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onRespawn(PlayerRespawnEvent event) {
        Arena arena = arenas.arenaOf(event.getPlayer()).orElse(null);
        if (arena == null) {
            return;
        }
        event.setRespawnLocation(arena.respawnLocation(event.getPlayer()));
        org.bukkit.Bukkit.getScheduler().runTask(plugin, () -> arena.afterRespawn(event.getPlayer()));
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player player)) {
            return;
        }
        arenas.arenaOf(player).ifPresent(arena -> {
            boolean vulnerable = arena.phase() == GamePhase.RUNNING
                    && arena.playerState(player.getUniqueId()).map(state -> !state.eliminated() && !state.respawning()).orElse(false);
            boolean protectedDamage = event.getCause() != EntityDamageEvent.DamageCause.VOID
                    && arena.isRespawnProtected(player);
            if (!vulnerable || protectedDamage) {
                event.setCancelled(true);
            }
        });
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onDamageCompleted(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;
        arenas.arenaOf(player).ifPresent(arena -> arena.revealInvisibility(player));
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPotionEffect(EntityPotionEffectEvent event) {
        if (!(event.getEntity() instanceof Player player)
                || event.getModifiedType() != PotionEffectType.INVISIBILITY) return;
        arenas.arenaOf(player).ifPresent(arena ->
                arena.handleInvisibilityChange(player, event.getNewEffect() != null));
    }

    @EventHandler(ignoreCancelled = true)
    public void onFood(FoodLevelChangeEvent event) {
        if (event.getEntity() instanceof Player player && arenas.arenaOf(player).isPresent()) {
            event.setCancelled(true);
            player.setFoodLevel(20);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onDrop(PlayerDropItemEvent event) {
        Player player = event.getPlayer();
        Arena arena = arenas.arenaOf(player).orElse(null);
        if (arena == null) return;
        if (PersistentItemTransferPolicy.isPersistent(event.getItemDrop().getItemStack().getType())) {
            event.setCancelled(true);
        } else {
            arena.trackEntity(event.getItemDrop());
            scheduleSwordReconciliation(player);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player) || arenas.arenaOf(player).isEmpty()) return;
        scheduleSwordReconciliation(player);
        if (event.getSlotType() == InventoryType.SlotType.ARMOR) {
            event.setCancelled(true);
            return;
        }
        int hotbarButton = event.getHotbarButton();
        ItemStack hotbar = hotbarButton >= 0 ? player.getInventory().getItem(hotbarButton) : null;
        boolean clickedTop = event.getClickedInventory() == event.getView().getTopInventory();
        InventoryType topType = event.getView().getTopInventory().getType();
        boolean externalTop = topType != InventoryType.CRAFTING && topType != InventoryType.CREATIVE;
        if (PersistentItemTransferPolicy.shouldCancelClick(
                event.getAction(),
                event.getClick(),
                clickedTop,
                externalTop,
                material(event.getCurrentItem()),
                material(event.getCursor()),
                material(hotbar),
                player.getInventory().getItemInOffHand().getType())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onInventoryDrag(InventoryDragEvent event) {
        if (!(event.getWhoClicked() instanceof Player player) || arenas.arenaOf(player).isEmpty()) return;
        scheduleSwordReconciliation(player);
        if (event.getRawSlots().stream()
                .anyMatch(slot -> event.getView().getSlotType(slot) == InventoryType.SlotType.ARMOR)) {
            event.setCancelled(true);
            return;
        }
        int topSize = event.getView().getTopInventory().getSize();
        boolean touchesTop = event.getRawSlots().stream().anyMatch(slot -> slot < topSize);
        if (PersistentItemTransferPolicy.shouldCancelDrag(
                material(event.getOldCursor()), touchesTop)) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onInventoryClose(InventoryCloseEvent event) {
        if (event.getPlayer() instanceof Player player && arenas.arenaOf(player).isPresent()) {
            scheduleSwordReconciliation(player);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPrepareCraft(PrepareItemCraftEvent event) {
        boolean arenaViewer = event.getViewers().stream()
                .filter(Player.class::isInstance)
                .map(Player.class::cast)
                .anyMatch(player -> arenas.arenaOf(player).isPresent());
        if (arenaViewer) event.getInventory().setResult(null);
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onCraft(CraftItemEvent event) {
        if (event.getWhoClicked() instanceof Player player && arenas.arenaOf(player).isPresent()) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onItemDamage(PlayerItemDamageEvent event) {
        if (arenas.arenaOf(event.getPlayer()).isPresent()) event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBucketEmpty(PlayerBucketEmptyEvent event) {
        arenas.arenaOf(event.getPlayer()).ifPresent(arena -> {
            if (!arena.handleBucketEmpty(event.getPlayer(), event.getBlock())) event.setCancelled(true);
        });
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBucketFill(PlayerBucketFillEvent event) {
        arenas.arenaOf(event.getPlayer()).ifPresent(arena -> {
            if (!arena.handleBucketFill(event.getPlayer(), event.getBlock())) event.setCancelled(true);
        });
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onFluidFlow(BlockFromToEvent event) {
        arenas.arenaIn(event.getBlock().getWorld())
                .ifPresent(arena -> {
                    if (!arena.handleFluidSpread(event.getBlock(), event.getToBlock())) event.setCancelled(true);
                });
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onInventoryOpen(InventoryOpenEvent event) {
        org.bukkit.Location location = event.getInventory().getLocation();
        if (location != null) {
            arenas.arenaIn(location.getWorld()).ifPresent(arena -> arena.captureState(location.getBlock()));
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onPistonExtend(BlockPistonExtendEvent event) {
        if (arenas.arenaIn(event.getBlock().getWorld()).isPresent()) event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onPistonRetract(BlockPistonRetractEvent event) {
        if (arenas.arenaIn(event.getBlock().getWorld()).isPresent()) event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onEntityChangeBlock(EntityChangeBlockEvent event) {
        if (arenas.arenaIn(event.getBlock().getWorld()).isPresent()) event.setCancelled(true);
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        arenas.disconnect(event.getPlayer());
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        arenas.handleJoin(event.getPlayer());
    }

    @EventHandler(ignoreCancelled = true)
    public void onMove(PlayerMoveEvent event) {
        if (event.getTo() == null
                || event.getFrom().getBlockX() == event.getTo().getBlockX()
                && event.getFrom().getBlockY() == event.getTo().getBlockY()
                && event.getFrom().getBlockZ() == event.getTo().getBlockZ()) {
            return;
        }
        arenas.arenaOf(event.getPlayer()).ifPresent(arena -> arena.handleMovement(event.getPlayer(), event.getTo()));
    }

    @EventHandler(ignoreCancelled = true)
    public void onTeleport(PlayerTeleportEvent event) {
        if (event.getTo() == null) return;
        arenas.arenaOf(event.getPlayer()).ifPresent(arena -> arena.handleMovement(event.getPlayer(), event.getTo()));
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onWorldChange(PlayerChangedWorldEvent event) {
        Player player = event.getPlayer();
        Arena arena = arenas.arenaOf(player).orElse(null);
        if (arena != null && !player.getWorld().equals(arena.world())) {
            arenas.leave(player, false);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onEntityExplode(EntityExplodeEvent event) {
        arenas.arenaIn(event.getLocation().getWorld()).ifPresent(arena -> event.blockList().removeIf(block -> {
            if (block.getType().name().endsWith("_STAINED_GLASS")) return true;
            if (!arena.isPlacedBlock(block)) {
                return true;
            }
            arena.removePlacedBlock(block);
            return false;
        }));
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBlockExplode(BlockExplodeEvent event) {
        arenas.arenaIn(event.getBlock().getWorld()).ifPresent(arena -> event.blockList().removeIf(block -> {
            if (block.getType().name().endsWith("_STAINED_GLASS")) return true;
            if (!arena.isPlacedBlock(block)) {
                return true;
            }
            arena.removePlacedBlock(block);
            return false;
        }));
    }

    private Player attackingPlayer(org.bukkit.entity.Entity damager) {
        if (damager instanceof Player player) return player;
        if (damager instanceof Projectile projectile && projectile.getShooter() instanceof Player player) return player;
        if (damager instanceof TNTPrimed tnt && tnt.getSource() instanceof Player player) return player;
        return null;
    }

    private Material material(ItemStack item) {
        return item == null || item.isEmpty() ? null : item.getType();
    }

    private void scheduleSwordReconciliation(Player player) {
        UUID playerId = player.getUniqueId();
        if (!pendingSwordReconciliations.add(playerId)) return;
        org.bukkit.Bukkit.getScheduler().runTask(plugin, () -> {
            pendingSwordReconciliations.remove(playerId);
            arenas.arenaOf(player).ifPresent(arena -> arena.reconcileDefaultSword(player));
        });
    }
}
