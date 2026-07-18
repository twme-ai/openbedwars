package io.github.twmeai.openbedwars.command;

import io.github.twmeai.openbedwars.OpenBedWarsPlugin;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public final class PartyCommand implements TabExecutor {
    private final OpenBedWarsPlugin plugin;

    public PartyCommand(OpenBedWarsPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
                             @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player player)) {
            plugin.messages().send(sender, "error.player-only");
            return true;
        }
        plugin.partyService().execute(player, args, 0);
        return true;
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command,
                                                 @NotNull String alias, @NotNull String[] args) {
        return sender instanceof Player player ? plugin.partyService().tabComplete(player, args, 0) : List.of();
    }
}
