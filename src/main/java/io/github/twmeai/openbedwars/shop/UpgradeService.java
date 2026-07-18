package io.github.twmeai.openbedwars.shop;

import io.github.twmeai.openbedwars.OpenBedWarsPlugin;
import io.github.twmeai.openbedwars.game.Arena;
import io.github.twmeai.openbedwars.game.ArenaManager;
import io.github.twmeai.openbedwars.game.GamePhase;
import io.github.twmeai.openbedwars.game.PlayerState;
import io.github.twmeai.openbedwars.game.ResourceType;
import io.github.twmeai.openbedwars.game.TeamState;
import io.github.twmeai.openbedwars.message.MessageService;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public final class UpgradeService implements Listener {
    private static final Map<Integer, UpgradeType> LAYOUT = Map.ofEntries(
            Map.entry(10, UpgradeType.SHARPNESS),
            Map.entry(11, UpgradeType.PROTECTION),
            Map.entry(12, UpgradeType.HASTE),
            Map.entry(13, UpgradeType.FORGE),
            Map.entry(14, UpgradeType.HEAL_POOL),
            Map.entry(15, UpgradeType.DRAGON_BUFF),
            Map.entry(29, UpgradeType.ITS_A_TRAP),
            Map.entry(30, UpgradeType.COUNTER_OFFENSIVE),
            Map.entry(31, UpgradeType.ALARM),
            Map.entry(32, UpgradeType.MINER_FATIGUE)
    );

    private final ArenaManager arenas;
    private final MessageService messages;

    public UpgradeService(OpenBedWarsPlugin plugin, ArenaManager arenas) {
        this.arenas = arenas;
        this.messages = plugin.messages();
    }

    public boolean openUpgradeShop(Player player) {
        Arena arena = playableArena(player);
        if (arena == null) {
            messages.send(player, "error.shop-game-only");
            return false;
        }
        UpgradeHolder holder = new UpgradeHolder();
        Inventory inventory = Bukkit.createInventory(holder, 45, messages.render(player, "shop.upgrade-title"));
        holder.attach(inventory);
        holder.upgrades(LAYOUT);
        render(player, arena, holder);
        player.openInventory(inventory);
        return true;
    }

    private void render(Player player, Arena arena, UpgradeHolder holder) {
        Inventory inventory = holder.getInventory();
        inventory.clear();
        TeamState team = arena.teamOf(player.getUniqueId()).orElseThrow();
        for (Map.Entry<Integer, UpgradeType> entry : holder.upgrades().entrySet()) {
            Offer offer = offer(entry.getValue(), team, arena.definition().playersPerTeam());
            inventory.setItem(entry.getKey(), display(player, entry.getValue(), offer));
        }
        ItemStack separator = named(org.bukkit.Material.GRAY_STAINED_GLASS_PANE, Component.text(" "));
        for (int slot = 18; slot < 27; slot++) inventory.setItem(slot, separator);
    }

    private ItemStack display(Player player, UpgradeType type, Offer offer) {
        ItemStack stack = new ItemStack(type.icon());
        ItemMeta meta = stack.getItemMeta();
        meta.displayName(messages.render(player, type.translationKey())
                .color(offer.maxed() ? NamedTextColor.GREEN : NamedTextColor.WHITE)
                .decoration(TextDecoration.ITALIC, false));
        List<Component> lore = new ArrayList<>();
        if (offer.level() > 0) {
            lore.add(messages.render(player, "shop.current-tier", MessageService.number("tier", offer.level()))
                    .decoration(TextDecoration.ITALIC, false));
        }
        lore.add(messages.render(player, "shop.cost",
                MessageService.number("cost", offer.cost()),
                MessageService.component("resource", messages.render(player, ResourceType.DIAMOND.translationKey())),
                MessageService.color("cost_color", NamedTextColor.AQUA)));
        lore.add(Component.empty());
        lore.add(messages.render(player, offer.maxed() ? "shop.maxed" : "shop.click"));
        meta.lore(lore);
        meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
        if (offer.maxed()) {
            meta.addEnchant(Enchantment.UNBREAKING, 1, true);
            meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
        }
        stack.setItemMeta(meta);
        return stack;
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onClick(InventoryClickEvent event) {
        if (!(event.getInventory().getHolder() instanceof UpgradeHolder holder)) return;
        event.setCancelled(true);
        if (!(event.getWhoClicked() instanceof Player player) || event.getClickedInventory() != event.getInventory()) return;
        UpgradeType type = holder.upgrades().get(event.getRawSlot());
        Arena arena = playableArena(player);
        if (type == null || arena == null) return;
        TeamState team = arena.teamOf(player.getUniqueId()).orElseThrow();
        Offer offer = offer(type, team, arena.definition().playersPerTeam());
        if (offer.maxed()) {
            messages.send(player, type.isTrap() ? "upgrade.trap-full" : "error.already-owned");
            return;
        }
        int available = InventoryCurrency.count(player.getInventory(), ResourceType.DIAMOND);
        if (available < offer.cost()) {
            messages.send(player, "error.not-enough-resources",
                    MessageService.number("cost", offer.cost() - available),
                    MessageService.component("resource",
                            messages.render(player, ResourceType.DIAMOND.translationKey())));
            return;
        }
        InventoryCurrency.take(player.getInventory(), ResourceType.DIAMOND, offer.cost());
        grant(arena, team, type);
        for (java.util.UUID memberId : team.members()) {
            Player member = Bukkit.getPlayer(memberId);
            if (member != null) {
                messages.send(member, type.isTrap() ? "upgrade.trap-added" : "upgrade.purchased",
                        MessageService.component(type.isTrap() ? "trap" : "upgrade",
                                messages.render(member, type.translationKey())));
            }
        }
        render(player, arena, holder);
    }

    private void grant(Arena arena, TeamState team, UpgradeType type) {
        switch (type) {
            case SHARPNESS -> team.sharpness(true);
            case PROTECTION -> team.protection(team.protection() + 1);
            case HASTE -> team.haste(team.haste() + 1);
            case FORGE -> team.forge(team.forge() + 1);
            case HEAL_POOL -> team.healPool(true);
            case DRAGON_BUFF -> team.dragonBuff(true);
            default -> team.traps().add(type.trap());
        }
        arena.applyTeamEnchantments(team);
    }

    private Offer offer(UpgradeType type, TeamState team, int playersPerTeam) {
        boolean large = playersPerTeam > 2;
        if (type.isTrap()) {
            int size = team.traps().size();
            return new Offer(size >= 3 ? 0 : new int[]{1, 2, 4}[size], size, size >= 3);
        }
        return switch (type) {
            case SHARPNESS -> new Offer(large ? 8 : 4, team.sharpness() ? 1 : 0, team.sharpness());
            case PROTECTION -> tiered(team.protection(), large ? new int[]{5, 10, 20, 30} : new int[]{2, 4, 8, 16});
            case HASTE -> tiered(team.haste(), large ? new int[]{4, 6} : new int[]{2, 4});
            case FORGE -> tiered(team.forge(), large ? new int[]{4, 8, 12, 16} : new int[]{2, 4, 6, 8});
            case HEAL_POOL -> new Offer(large ? 3 : 1, team.healPool() ? 1 : 0, team.healPool());
            case DRAGON_BUFF -> new Offer(5, team.dragonBuff() ? 1 : 0, team.dragonBuff());
            default -> throw new IllegalStateException("Unexpected upgrade " + type);
        };
    }

    private Offer tiered(int currentLevel, int[] costs) {
        return currentLevel >= costs.length
                ? new Offer(0, currentLevel, true)
                : new Offer(costs[currentLevel], currentLevel, false);
    }

    private Arena playableArena(Player player) {
        Arena arena = arenas.arenaOf(player).orElse(null);
        if (arena == null || arena.phase() != GamePhase.RUNNING) return null;
        PlayerState state = arena.playerState(player.getUniqueId()).orElse(null);
        return state != null && !state.eliminated() && !state.respawning() ? arena : null;
    }

    private ItemStack named(org.bukkit.Material material, Component name) {
        ItemStack stack = new ItemStack(material);
        ItemMeta meta = stack.getItemMeta();
        meta.displayName(name.decoration(TextDecoration.ITALIC, false));
        stack.setItemMeta(meta);
        return stack;
    }

    private record Offer(int cost, int level, boolean maxed) {
    }
}
