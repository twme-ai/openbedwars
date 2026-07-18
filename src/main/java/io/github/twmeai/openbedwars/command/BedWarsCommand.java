package io.github.twmeai.openbedwars.command;

import io.github.twmeai.openbedwars.OpenBedWarsPlugin;
import io.github.twmeai.openbedwars.game.Arena;
import io.github.twmeai.openbedwars.game.ArenaManager;
import io.github.twmeai.openbedwars.game.GamePhase;
import io.github.twmeai.openbedwars.game.TeamColor;
import io.github.twmeai.openbedwars.message.MessageService;
import io.github.twmeai.openbedwars.statistics.LeaderboardMetric;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.JoinConfiguration;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class BedWarsCommand implements TabExecutor {
    private final OpenBedWarsPlugin plugin;
    private final ArenaManager arenas;
    private final MessageService messages;

    public BedWarsCommand(OpenBedWarsPlugin plugin, ArenaManager arenas) {
        this.plugin = plugin;
        this.arenas = arenas;
        this.messages = plugin.messages();
    }

    @Override
    public boolean onCommand(
            @NotNull CommandSender sender,
            @NotNull Command command,
            @NotNull String label,
            @NotNull String[] args
    ) {
        if (args.length == 0 || args[0].equalsIgnoreCase("help")) {
            showHelp(sender);
            return true;
        }
        String subcommand = args[0].toLowerCase(Locale.ROOT);
        switch (subcommand) {
            case "list" -> list(sender);
            case "join" -> join(sender, args);
            case "leave" -> leave(sender);
            case "team" -> team(sender, args);
            case "language", "lang" -> language(sender, args);
            case "stats" -> statistics(sender, args);
            case "leaderboard", "top" -> leaderboard(sender, args);
            case "start" -> start(sender, args);
            case "stop" -> stop(sender, args);
            case "reload" -> reload(sender);
            case "setup" -> setup(sender, args);
            case "party" -> party(sender, args);
            case "shop" -> openShop(sender);
            case "upgrades" -> openUpgrades(sender);
            default -> messages.send(sender, "error.unknown-command");
        }
        return true;
    }

    private void showHelp(CommandSender sender) {
        sender.sendMessage(messages.render(sender, "help.header"));
        sender.sendMessage(messages.render(sender, "help.play"));
        if (sender.hasPermission("openbedwars.admin")) {
            sender.sendMessage(messages.render(sender, "help.admin"));
        }
    }

    private void list(CommandSender sender) {
        List<Component> entries = arenas.arenas().values().stream()
                .<Component>map(arena -> Component.text(arena.displayName() + " ["
                                + arena.playerCount() + "/" + arena.maxPlayers() + ", ")
                        .append(messages.render(sender, arena.phase().translationKey()))
                        .append(Component.text("]")))
                .toList();
        Component value = entries.isEmpty()
                ? messages.render(sender, "arena.none")
                : Component.join(JoinConfiguration.separator(Component.text(", ")), entries);
        messages.send(sender, "arena.list-header", MessageService.component("arenas", value));
    }

    private void join(CommandSender sender, String[] args) {
        Player player = requirePlayer(sender);
        if (player == null) {
            return;
        }
        if (!player.hasPermission("openbedwars.play")) {
            messages.send(player, "error.no-permission");
            return;
        }
        if (args.length < 2) {
            messages.send(player, "error.unknown-command");
            return;
        }
        Arena arena = args[1].equalsIgnoreCase("random")
                ? arenas.bestAvailableArena().orElse(null)
                : arenas.arena(args[1]).orElse(null);
        if (arena == null) {
            messages.send(player, "error.arena-not-found", MessageService.text("arena", args[1]));
            return;
        }
        plugin.partyService().joinArena(player, arena);
    }

    private void leave(CommandSender sender) {
        Player player = requirePlayer(sender);
        if (player != null && !arenas.leave(player, true)) {
            messages.send(player, "error.not-in-arena");
        }
    }

    private void team(CommandSender sender, String[] args) {
        Player player = requirePlayer(sender);
        if (player == null) return;
        TeamColor color = args.length >= 2 ? TeamColor.fromKey(args[1]).orElse(null) : null;
        if (color == null) {
            messages.send(player, "error.invalid-team");
            return;
        }
        Arena.TeamChangeResult result = arenas.changeTeam(player, color);
        switch (result) {
            case SUCCESS -> messages.send(player, "arena.team-selected",
                    MessageService.component("team", messages.render(player, color.translationKey())),
                    MessageService.teamColor("team_color", color));
            case FULL -> messages.send(player, "arena.team-full");
            case INVALID -> messages.send(player, "error.not-in-arena");
            case LOCKED -> messages.send(player, "arena.team-locked");
        }
    }

    private void language(CommandSender sender, String[] args) {
        Player player = requirePlayer(sender);
        if (player == null) {
            return;
        }
        if (args.length < 2 || !messages.setLocale(player, args[1])) {
            messages.send(player, "error.invalid-locale",
                    MessageService.text("locale", args.length < 2 ? "" : args[1]));
            return;
        }
        messages.send(player, "language.changed", MessageService.text("locale", messages.localeOf(player)));
    }

    private void statistics(CommandSender sender, String[] args) {
        if (args.length >= 2) {
            plugin.statisticsService().showByName(sender, args[1]);
        } else if (sender instanceof Player player) {
            plugin.statisticsService().showSelf(sender, player);
        } else {
            messages.send(sender, "error.player-only");
        }
    }

    private void leaderboard(CommandSender sender, String[] args) {
        LeaderboardMetric metric = args.length >= 2
                ? LeaderboardMetric.fromKey(args[1]).orElse(null)
                : LeaderboardMetric.WINS;
        if (metric == null) {
            messages.send(sender, "leaderboard.invalid");
            return;
        }
        plugin.statisticsService().showLeaderboard(sender, metric, 10);
    }

    private void start(CommandSender sender, String[] args) {
        if (!requireAdmin(sender)) {
            return;
        }
        Arena arena = resolveArena(sender, args);
        if (arena == null) {
            return;
        }
        if (!arena.forceStart()) {
            messages.send(sender, "error.cannot-start");
        }
    }

    private void stop(CommandSender sender, String[] args) {
        if (!requireAdmin(sender)) {
            return;
        }
        Arena arena = resolveArena(sender, args);
        if (arena == null) {
            return;
        }
        if (arena.phase() == GamePhase.WAITING && arena.playerCount() == 0) {
            messages.send(sender, "error.cannot-stop");
            return;
        }
        arena.forceStop();
    }

    private void reload(CommandSender sender) {
        if (!requireAdmin(sender)) {
            return;
        }
        if (arenas.hasActiveGames()) {
            messages.send(sender, "setup.active-games");
            return;
        }
        arenas.reload();
        messages.send(sender, "config.reloaded");
    }

    private void setup(CommandSender sender, String[] args) {
        Player player = requirePlayer(sender);
        if (player != null) {
            plugin.arenaSetupService().execute(player, args);
        }
    }

    private void party(CommandSender sender, String[] args) {
        Player player = requirePlayer(sender);
        if (player != null) {
            plugin.partyService().execute(player, args, 1);
        }
    }

    private void openShop(CommandSender sender) {
        Player player = requirePlayer(sender);
        if (player != null) {
            plugin.shopService().openItemShop(player);
        }
    }

    private void openUpgrades(CommandSender sender) {
        Player player = requirePlayer(sender);
        if (player != null) {
            plugin.upgradeService().openUpgradeShop(player);
        }
    }

    private Arena resolveArena(CommandSender sender, String[] args) {
        if (args.length >= 2) {
            Arena arena = arenas.arena(args[1]).orElse(null);
            if (arena == null) {
                messages.send(sender, "error.arena-not-found", MessageService.text("arena", args[1]));
            }
            return arena;
        }
        if (sender instanceof Player player) {
            Arena arena = arenas.arenaOf(player).orElse(null);
            if (arena == null) {
                messages.send(sender, "error.not-in-arena");
            }
            return arena;
        }
        messages.send(sender, "error.unknown-command");
        return null;
    }

    private boolean requireAdmin(CommandSender sender) {
        if (!sender.hasPermission("openbedwars.admin")) {
            messages.send(sender, "error.no-permission");
            return false;
        }
        return true;
    }

    private Player requirePlayer(CommandSender sender) {
        if (sender instanceof Player player) {
            return player;
        }
        messages.send(sender, "error.player-only");
        return null;
    }

    @Override
    public @Nullable List<String> onTabComplete(
            @NotNull CommandSender sender,
            @NotNull Command command,
            @NotNull String alias,
            @NotNull String[] args
    ) {
        if (args.length == 1) {
            List<String> commands = new ArrayList<>(List.of("help", "list", "join", "leave", "team", "stats", "leaderboard", "language", "party", "shop", "upgrades"));
            if (sender.hasPermission("openbedwars.admin")) {
                commands.addAll(List.of("start", "stop", "reload", "setup"));
            }
            return matches(commands, args[0]);
        }
        if (args.length == 2 && List.of("join", "start", "stop").contains(args[0].toLowerCase(Locale.ROOT))) {
            List<String> values = new ArrayList<>(arenas.arenas().keySet());
            if (args[0].equalsIgnoreCase("join")) values.add("random");
            return matches(values, args[1]);
        }
        if (args.length == 2 && args[0].equalsIgnoreCase("team")) {
            return matches(java.util.Arrays.stream(TeamColor.values()).map(TeamColor::key).toList(), args[1]);
        }
        if (args.length == 2 && List.of("language", "lang").contains(args[0].toLowerCase(Locale.ROOT))) {
            return matches(messages.availableLocales(), args[1]);
        }
        if (args.length == 2 && args[0].equalsIgnoreCase("stats")) {
            return matches(org.bukkit.Bukkit.getOnlinePlayers().stream().map(Player::getName).toList(), args[1]);
        }
        if (args.length == 2 && (args[0].equalsIgnoreCase("leaderboard") || args[0].equalsIgnoreCase("top"))) {
            return matches(java.util.Arrays.stream(LeaderboardMetric.values()).map(LeaderboardMetric::key).toList(), args[1]);
        }
        if (args.length >= 2 && args[0].equalsIgnoreCase("setup") && sender.hasPermission("openbedwars.admin")) {
            return plugin.arenaSetupService().tabComplete(args);
        }
        if (args.length >= 2 && args[0].equalsIgnoreCase("party") && sender instanceof Player player) {
            return plugin.partyService().tabComplete(player, args, 1);
        }
        return List.of();
    }

    private List<String> matches(java.util.Collection<String> values, String prefix) {
        return values.stream()
                .filter(value -> value.regionMatches(true, 0, prefix, 0, prefix.length()))
                .sorted()
                .toList();
    }
}
