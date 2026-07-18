package io.github.twmeai.openbedwars.special;

import io.github.twmeai.openbedwars.OpenBedWarsPlugin;
import io.github.twmeai.openbedwars.game.Arena;
import io.github.twmeai.openbedwars.game.ArenaManager;
import io.github.twmeai.openbedwars.game.GamePhase;
import io.github.twmeai.openbedwars.game.PlayerState;
import io.github.twmeai.openbedwars.game.TeamColor;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.Directional;
import org.bukkit.entity.Entity;
import org.bukkit.entity.IronGolem;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.entity.Silverfish;
import org.bukkit.entity.TNTPrimed;
import org.bukkit.entity.ThrowableProjectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityTargetLivingEntityEvent;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.event.entity.ProjectileLaunchEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Vector;

import java.time.Duration;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class SpecialItemService implements Listener {
    private static final double BRIDGE_EGG_MAX_DISTANCE_SQUARED = 50 * 50;
    private static final double DEFENDER_TARGET_RANGE = 16;
    private static final double DEFENDER_TARGET_RANGE_SQUARED = DEFENDER_TARGET_RANGE * DEFENDER_TARGET_RANGE;

    private final OpenBedWarsPlugin plugin;
    private final ArenaManager arenas;
    private final NamespacedKey utilityTypeKey;
    private final Map<UUID, LaunchedUtility> launchedUtilities = new HashMap<>();
    private final Map<UUID, Defender> defenders = new HashMap<>();
    private final BukkitTask defenderTicker;

    public SpecialItemService(OpenBedWarsPlugin plugin, ArenaManager arenas) {
        this.plugin = plugin;
        this.arenas = arenas;
        this.utilityTypeKey = new NamespacedKey(plugin, "utility_type");
        this.defenderTicker = Bukkit.getScheduler().runTaskTimer(plugin, this::tickDefenders, 20L, 20L);
    }

    public void shutdown() {
        defenderTicker.cancel();
        launchedUtilities.clear();
        defenders.clear();
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onProjectileLaunch(ProjectileLaunchEvent event) {
        if (!(event.getEntity().getShooter() instanceof Player player)
                || !(event.getEntity() instanceof ThrowableProjectile projectile)) {
            return;
        }
        String type = utilityType(projectile.getItem());
        if (type == null) return;
        Arena arena = playableArena(player);
        if (arena == null) return;
        LaunchedUtility utility = new LaunchedUtility(type, player.getUniqueId(), arena);
        launchedUtilities.put(projectile.getUniqueId(), utility);
        if (type.equals("bridge_egg")) {
            startBridgeEgg(projectile, player, arena);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onProjectileHit(ProjectileHitEvent event) {
        LaunchedUtility utility = launchedUtilities.remove(event.getEntity().getUniqueId());
        if (utility == null || !utility.type().equals("bed_bug")) return;
        if (utility.arena().phase() != GamePhase.RUNNING) return;
        TeamColor team = utility.arena().playerState(utility.owner())
                .map(state -> state.team()).orElse(null);
        if (team == null) return;
        Silverfish silverfish = event.getEntity().getWorld().spawn(event.getEntity().getLocation(), Silverfish.class, mob -> {
            mob.customName(net.kyori.adventure.text.Component.text("Bed Bug", team.textColor()));
            mob.setCustomNameVisible(true);
            mob.setRemoveWhenFarAway(false);
        });
        registerDefender(silverfish, utility.arena(), team, Duration.ofSeconds(15));
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onUseSpecial(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_AIR && event.getAction() != Action.RIGHT_CLICK_BLOCK) {
            return;
        }
        ItemStack item = event.getItem();
        String type = utilityType(item);
        if (!"dream_defender".equals(type) && !"popup_tower".equals(type)) return;
        Arena arena = playableArena(event.getPlayer());
        if (arena == null) return;
        if (!UtilityInteractionPolicy.shouldHandle(
                event.getHand(), event.getPlayer().getInventory().getItemInMainHand().getType())) {
            event.setCancelled(true);
            return;
        }
        if (type.equals("dream_defender")) {
            event.setCancelled(true);
            spawnDreamDefender(event, arena);
        } else if (type.equals("popup_tower")) {
            event.setCancelled(true);
            if (buildPopupTower(event.getPlayer(), arena)) {
                consume(item);
            } else {
                plugin.messages().send(event.getPlayer(), "error.special-placement");
            }
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onConsume(PlayerItemConsumeEvent event) {
        if (!"magic_milk".equals(utilityType(event.getItem()))) return;
        Arena arena = playableArena(event.getPlayer());
        if (arena == null) return;
        arena.grantTrapImmunity(event.getPlayer(), Duration.ofSeconds(30));
        plugin.messages().send(event.getPlayer(), "special.magic-milk");
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onSpongePlace(BlockPlaceEvent event) {
        if (event.getBlockPlaced().getType() != Material.SPONGE) return;
        Arena arena = playableArena(event.getPlayer());
        if (arena != null) {
            Bukkit.getScheduler().runTask(plugin, () -> arena.drainWaterAround(event.getBlockPlaced(), 3));
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onDefenderDamage(EntityDamageByEntityEvent event) {
        Defender attacker = defenders.get(event.getDamager().getUniqueId());
        if (attacker != null && !isEnemyTarget(attacker, event.getEntity())) {
            event.setCancelled(true);
            return;
        }
        Defender defender = defenders.get(event.getEntity().getUniqueId());
        if (defender == null) return;
        Player player = playerDamager(event.getDamager());
        if (player != null && !isEnemyTarget(defender, player)) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onDefenderTarget(EntityTargetLivingEntityEvent event) {
        Defender defender = defenders.get(event.getEntity().getUniqueId());
        if (defender != null && event.getTarget() != null && !isEnemyTarget(defender, event.getTarget())) {
            event.setCancelled(true);
        }
    }

    private void startBridgeEgg(ThrowableProjectile projectile, Player owner, Arena arena) {
        Location origin = projectile.getLocation().clone();
        BukkitTask[] task = new BukkitTask[1];
        task[0] = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            if (!projectile.isValid() || projectile.isDead() || arena.phase() != GamePhase.RUNNING
                    || projectile.getLocation().distanceSquared(origin) > BRIDGE_EGG_MAX_DISTANCE_SQUARED) {
                task[0].cancel();
                launchedUtilities.remove(projectile.getUniqueId());
                return;
            }
            TeamColor team = arena.playerState(owner.getUniqueId()).map(state -> state.team()).orElse(null);
            if (team == null) {
                task[0].cancel();
                return;
            }
            Location below = projectile.getLocation().subtract(0, 2.75, 0);
            Vector velocity = projectile.getVelocity().setY(0);
            Vector side = velocity.lengthSquared() < 0.01
                    ? new Vector(1, 0, 0)
                    : new Vector(-velocity.getZ(), 0, velocity.getX()).normalize();
            arena.placeGeneratedBlock(below.getBlock(), team.wool());
            arena.placeGeneratedBlock(below.clone().add(side).getBlock(), team.wool());
            arena.placeGeneratedBlock(below.clone().subtract(side).getBlock(), team.wool());
        }, 0L, 1L);
    }

    private void spawnDreamDefender(PlayerInteractEvent event, Arena arena) {
        Player player = event.getPlayer();
        ItemStack item = event.getItem();
        Location location = event.getClickedBlock() == null
                ? player.getLocation().add(player.getLocation().getDirection().setY(0).normalize().multiply(2))
                : event.getClickedBlock().getRelative(event.getBlockFace()).getLocation().add(0.5, 0, 0.5);
        TeamColor team = arena.playerState(player.getUniqueId()).orElseThrow().team();
        IronGolem golem = location.getWorld().spawn(location, IronGolem.class, mob -> {
            mob.customName(net.kyori.adventure.text.Component.text("Dream Defender", team.textColor()));
            mob.setCustomNameVisible(true);
            mob.setPlayerCreated(true);
            mob.setRemoveWhenFarAway(false);
        });
        registerDefender(golem, arena, team, Duration.ofMinutes(4));
        consume(item);
    }

    private void registerDefender(Mob mob, Arena arena, TeamColor team, Duration lifetime) {
        arena.trackEntity(mob);
        defenders.put(mob.getUniqueId(), new Defender(
                arena,
                team,
                System.currentTimeMillis() + lifetime.toMillis()
        ));
    }

    private void tickDefenders() {
        long now = System.currentTimeMillis();
        defenders.entrySet().removeIf(entry -> {
            Defender defender = entry.getValue();
            if (defender.arena().phase() != GamePhase.RUNNING || defender.expiresAt() <= now) {
                defender.arena().discardTrackedEntity(entry.getKey());
                return true;
            }
            Entity entity = defender.arena().world().getEntity(entry.getKey());
            if (entity == null) return false;
            if (!(entity instanceof Mob mob) || !mob.isValid() || mob.isDead()) {
                defender.arena().discardTrackedEntity(entry.getKey());
                return true;
            }
            LivingEntity target = mob.getWorld()
                    .getNearbyEntities(mob.getLocation(),
                            DEFENDER_TARGET_RANGE, DEFENDER_TARGET_RANGE, DEFENDER_TARGET_RANGE).stream()
                    .filter(LivingEntity.class::isInstance)
                    .map(LivingEntity.class::cast)
                    .filter(candidate -> candidate instanceof Player
                            || defenders.containsKey(candidate.getUniqueId()))
                    .filter(candidate -> candidate.getLocation().distanceSquared(mob.getLocation())
                            <= DEFENDER_TARGET_RANGE_SQUARED)
                    .filter(candidate -> isEnemyTarget(defender, candidate))
                    .min(Comparator.comparingDouble(candidate ->
                            candidate.getLocation().distanceSquared(mob.getLocation())))
                    .orElse(null);
            mob.setTarget(target);
            return false;
        });
    }

    private boolean buildPopupTower(Player player, Arena arena) {
        BlockFace forward = cardinal(player.getFacing());
        BlockFace right = rightOf(forward);
        Block center = player.getLocation().getBlock().getRelative(forward, 3);
        Material wool = arena.playerState(player.getUniqueId()).orElseThrow().team().wool();
        Directional ladder = (Directional) Material.LADDER.createBlockData();
        ladder.setFacing(forward.getOppositeFace());
        List<Placement> placements = PopupTowerBlueprint.blocks().stream()
                .map(offset -> new Placement(
                        relative(center, right, forward, offset.right(), offset.y(), offset.forward()),
                        offset.kind() == PopupTowerBlueprint.Kind.WOOL
                                ? wool.createBlockData()
                                : ladder.clone()
                ))
                .toList();
        List<Block> blocks = placements.stream().map(Placement::block).toList();
        if (!arena.reserveGeneratedBlocks(blocks)) {
            return false;
        }

        final int[] index = {0};
        BukkitTask[] task = new BukkitTask[1];
        task[0] = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            if (arena.phase() != GamePhase.RUNNING || index[0] >= placements.size()) {
                arena.releaseGeneratedBlocks(blocks.subList(index[0], blocks.size()));
                task[0].cancel();
                return;
            }
            for (int count = 0; count < 8 && index[0] < placements.size(); count++) {
                Placement placement = placements.get(index[0]);
                if (!arena.placeReservedGeneratedBlock(placement.block(), placement.data())) {
                    arena.releaseGeneratedBlocks(blocks.subList(index[0], blocks.size()));
                    task[0].cancel();
                    return;
                }
                index[0]++;
            }
        }, 0L, 1L);
        return true;
    }

    private Block relative(Block center, BlockFace right, BlockFace forward, int x, int y, int z) {
        return center.getRelative(
                right.getModX() * x + forward.getModX() * z,
                y,
                right.getModZ() * x + forward.getModZ() * z
        );
    }

    private BlockFace cardinal(BlockFace face) {
        return switch (face) {
            case NORTH, SOUTH, EAST, WEST -> face;
            case NORTH_EAST, SOUTH_EAST -> BlockFace.EAST;
            case NORTH_WEST, SOUTH_WEST -> BlockFace.WEST;
            default -> BlockFace.NORTH;
        };
    }

    private BlockFace rightOf(BlockFace forward) {
        return switch (forward) {
            case NORTH -> BlockFace.EAST;
            case EAST -> BlockFace.SOUTH;
            case SOUTH -> BlockFace.WEST;
            case WEST -> BlockFace.NORTH;
            default -> throw new IllegalArgumentException("Expected a cardinal direction");
        };
    }

    private Arena playableArena(Player player) {
        Arena arena = arenas.arenaOf(player).orElse(null);
        if (arena == null || arena.phase() != GamePhase.RUNNING) return null;
        return arena.playerState(player.getUniqueId())
                .filter(state -> !state.eliminated() && !state.respawning())
                .map(ignored -> arena)
                .orElse(null);
    }

    private String utilityType(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return null;
        return item.getItemMeta().getPersistentDataContainer().get(utilityTypeKey, PersistentDataType.STRING);
    }

    private void consume(ItemStack item) {
        if (item != null) item.setAmount(item.getAmount() - 1);
    }

    private Player playerDamager(Entity entity) {
        if (entity instanceof Player player) return player;
        if (entity instanceof Projectile projectile && projectile.getShooter() instanceof Player player) return player;
        if (entity instanceof TNTPrimed tnt && tnt.getSource() instanceof Player player) return player;
        return null;
    }

    private boolean isEnemyTarget(Defender defender, Entity target) {
        if (target instanceof Player player) {
            PlayerState state = defender.arena()
                    .playerState(player.getUniqueId()).orElse(null);
            return state != null && DefenderCombatPolicy.isEnemy(
                    defender.team(), state.team(), true,
                    !state.eliminated() && !state.respawning() && !state.disconnected());
        }
        Defender targetDefender = defenders.get(target.getUniqueId());
        return targetDefender != null && DefenderCombatPolicy.isEnemy(
                defender.team(), targetDefender.team(), defender.arena() == targetDefender.arena(),
                target instanceof LivingEntity living && target.isValid() && !living.isDead());
    }

    private record LaunchedUtility(String type, UUID owner, Arena arena) {
    }

    private record Defender(Arena arena, TeamColor team, long expiresAt) {
    }

    private record Placement(Block block, BlockData data) {
    }
}
