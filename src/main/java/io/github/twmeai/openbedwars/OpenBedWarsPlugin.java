package io.github.twmeai.openbedwars;

import io.github.twmeai.openbedwars.command.BedWarsCommand;
import io.github.twmeai.openbedwars.game.ArenaManager;
import io.github.twmeai.openbedwars.listener.GameListener;
import io.github.twmeai.openbedwars.message.MessageService;
import io.github.twmeai.openbedwars.shop.ShopService;
import io.github.twmeai.openbedwars.shop.UpgradeService;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.util.Objects;

public final class OpenBedWarsPlugin extends JavaPlugin {
    private MessageService messages;
    private ArenaManager arenaManager;
    private ShopService shopService;
    private UpgradeService upgradeService;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        saveResourceIfMissing("arenas.yml");
        messages = new MessageService(this);
        arenaManager = new ArenaManager(this);
        shopService = new ShopService(this, arenaManager);
        upgradeService = new UpgradeService(this, arenaManager);
        BedWarsCommand command = new BedWarsCommand(this, arenaManager);
        Objects.requireNonNull(getCommand("bedwars"), "bedwars command").setExecutor(command);
        Objects.requireNonNull(getCommand("bedwars"), "bedwars command").setTabCompleter(command);
        getServer().getPluginManager().registerEvents(new GameListener(this, arenaManager), this);
        getServer().getPluginManager().registerEvents(shopService, this);
        getServer().getPluginManager().registerEvents(upgradeService, this);
    }

    @Override
    public void onDisable() {
        if (arenaManager != null) {
            arenaManager.shutdown();
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

    private void saveResourceIfMissing(String path) {
        if (!new File(getDataFolder(), path).exists()) {
            saveResource(path, false);
        }
    }
}
