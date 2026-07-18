package io.github.twmeai.openbedwars.statistics;

import io.github.twmeai.openbedwars.OpenBedWarsPlugin;
import io.github.twmeai.openbedwars.message.MessageService;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.io.File;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Level;

public final class StatisticsService {
    private final OpenBedWarsPlugin plugin;
    private final StatsRepository repository;

    public StatisticsService(OpenBedWarsPlugin plugin) {
        this.plugin = plugin;
        this.repository = new SqliteStatsRepository(new File(plugin.getDataFolder(), "statistics.db"));
    }

    public void record(Map<PlayerIdentity, StatsDelta> deltas) {
        if (deltas.isEmpty()) return;
        repository.apply(deltas).whenComplete((ignored, failure) -> {
            if (failure != null) {
                plugin.getLogger().log(Level.SEVERE, "Could not persist Bed Wars statistics", failure);
            }
        });
    }

    public void showSelf(CommandSender sender, Player player) {
        displayWhenReady(sender, repository.load(player.getUniqueId(), player.getName()));
    }

    public void showByName(CommandSender sender, String name) {
        repository.findByName(name).whenComplete((statistics, failure) -> runOnMain(sender, () -> {
            if (failure != null) {
                plugin.getLogger().log(Level.SEVERE, "Could not load Bed Wars statistics", failure);
                plugin.messages().send(sender, "error.database");
            } else if (statistics.isEmpty()) {
                plugin.messages().send(sender, "error.stats-not-found", MessageService.text("player", name));
            } else {
                display(sender, statistics.orElseThrow());
            }
        }));
    }

    public void close() {
        repository.close();
    }

    private void displayWhenReady(CommandSender sender, CompletableFuture<PlayerStatistics> future) {
        future.whenComplete((statistics, failure) -> runOnMain(sender, () -> {
            if (failure != null) {
                plugin.getLogger().log(Level.SEVERE, "Could not load Bed Wars statistics", failure);
                plugin.messages().send(sender, "error.database");
            } else {
                display(sender, statistics);
            }
        }));
    }

    private void display(CommandSender sender, PlayerStatistics statistics) {
        plugin.messages().send(sender, "stats.header",
                MessageService.text("player", statistics.name()),
                MessageService.number("level", statistics.level()));
        sender.sendMessage(plugin.messages().render(sender, "stats.body",
                MessageService.number("games", statistics.games()),
                MessageService.number("wins", statistics.wins()),
                MessageService.number("losses", statistics.losses()),
                MessageService.number("kills", statistics.kills()),
                MessageService.number("final_kills", statistics.finalKills()),
                MessageService.number("deaths", statistics.deaths()),
                MessageService.number("final_deaths", statistics.finalDeaths()),
                MessageService.number("beds", statistics.bedsBroken()),
                MessageService.text("experience", Long.toString(statistics.experience())),
                MessageService.number("next_level", BedWarsLevel.experienceForNextLevel(statistics.experience()))));
    }

    private void runOnMain(CommandSender sender, Runnable action) {
        if (!plugin.isEnabled()) return;
        Bukkit.getScheduler().runTask(plugin, () -> {
            if (sender instanceof Player player && !player.isOnline()) return;
            action.run();
        });
    }
}
