package io.github.twmeai.openbedwars.shop;

import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.jetbrains.annotations.NotNull;

import java.util.Map;

final class UpgradeHolder implements InventoryHolder {
    private Inventory inventory;
    private Map<Integer, UpgradeType> upgrades = Map.of();

    void attach(Inventory inventory) { this.inventory = inventory; }
    Map<Integer, UpgradeType> upgrades() { return upgrades; }
    void upgrades(Map<Integer, UpgradeType> upgrades) { this.upgrades = Map.copyOf(upgrades); }

    @Override
    public @NotNull Inventory getInventory() {
        if (inventory == null) throw new IllegalStateException("Inventory has not been attached");
        return inventory;
    }
}
