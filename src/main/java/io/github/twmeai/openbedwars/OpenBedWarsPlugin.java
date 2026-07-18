package io.github.twmeai.openbedwars;

import io.github.twmeai.openbedwars.command.BedWarsCommand;
import io.github.twmeai.openbedwars.command.PartyCommand;
import io.github.twmeai.openbedwars.config.ArenaSetupService;
import io.github.twmeai.openbedwars.game.ArenaManager;
import io.github.twmeai.openbedwars.listener.GameListener;
import io.github.twmeai.openbedwars.message.MessageService;
import io.github.twmeai.openbedwars.shop.ShopService;
import io.github.twmeai.openbedwars.shop.UpgradeService;
import io.github.twmeai.openbedwars.special.SpecialItemService;
import io.github.twmeai.openbedwars.statistics.StatisticsService;
import io.github.twmeai.openbedwars.party.PartyService;
import io.github.twmeai.openbedwars.spectator.SpectatorService;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.util.Objects;

public final class OpenBedWarsPlugin extends JavaPlugin {
    private MessageService messages;
    private ArenaManager arenaManager;
    private ShopService shopService;
    private UpgradeService upgradeService;
    private SpecialItemService specialItemService;
    private StatisticsService statisticsService;
    private ArenaSetupService arenaSetupService;
    private PartyService partyService;
    private SpectatorService spectatorService;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        saveResourceIfMissing("arenas.yml");
        messages = new MessageService(this);
        statisticsService = new StatisticsService(this);
        arenaManager = new ArenaManager(this);
        spectatorService = new SpectatorService(this, arenaManager);
        arenaSetupService = new ArenaSetupService(this, arenaManager);
        partyService = new PartyService(this, arenaManager);
        shopService = new ShopService(this, arenaManager);
        upgradeService = new UpgradeService(this, arenaManager);
        specialItemService = new SpecialItemService(this, arenaManager);
        BedWarsCommand command = new BedWarsCommand(this, arenaManager);
        Objects.requireNonNull(getCommand("bedwars"), "bedwars command").setExecutor(command);
        Objects.requireNonNull(getCommand("bedwars"), "bedwars command").setTabCompleter(command);
        PartyCommand partyCommand = new PartyCommand(this);
        Objects.requireNonNull(getCommand("party"), "party command").setExecutor(partyCommand);
        Objects.requireNonNull(getCommand("party"), "party command").setTabCompleter(partyCommand);
        getServer().getPluginManager().registerEvents(new GameListener(this, arenaManager), this);
        getServer().getPluginManager().registerEvents(shopService, this);
        getServer().getPluginManager().registerEvents(upgradeService, this);
        getServer().getPluginManager().registerEvents(specialItemService, this);
        getServer().getPluginManager().registerEvents(spectatorService, this);
    }

    @Override
    public void onDisable() {
        if (arenaManager != null) {
            arenaManager.shutdown();
        }
        if (specialItemService != null) {
            specialItemService.shutdown();
        }
        if (statisticsService != null) {
            statisticsService.close();
        }
        if (partyService != null) {
            partyService.shutdown();
        }
    }

    public MessageService messages() {
        return messages;
    }

    public ArenaManager arenaManager() {
        return arenaManager;
    }

    public ShopService shopService() {
        return shopService;
    }

    public UpgradeService upgradeService() {
        return upgradeService;
    }

    public StatisticsService statisticsService() {
        return statisticsService;
    }

    public ArenaSetupService arenaSetupService() {
        return arenaSetupService;
    }

    public PartyService partyService() {
        return partyService;
    }

    public SpectatorService spectatorService() {
        return spectatorService;
    }

    private void saveResourceIfMissing(String path) {
        if (!new File(getDataFolder(), path).exists()) {
            saveResource(path, false);
        }
    }
}
