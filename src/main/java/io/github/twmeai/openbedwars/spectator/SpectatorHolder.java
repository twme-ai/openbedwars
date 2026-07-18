package io.github.twmeai.openbedwars.spectator;

import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.jetbrains.annotations.NotNull;

final class SpectatorHolder implements InventoryHolder {
    private final String arenaKey;
    private Inventory inventory;

    SpectatorHolder(String arenaKey) {
        this.arenaKey = arenaKey;
    }

    String arenaKey() {
        return arenaKey;
    }

    void attach(Inventory inventory) {
        this.inventory = inventory;
    }

    @Override
    public @NotNull Inventory getInventory() {
        if (inventory == null) {
            throw new IllegalStateException("Inventory has not been attached");
        }
        return inventory;
    }
}
