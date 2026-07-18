package io.github.twmeai.openbedwars.shop;

import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.jetbrains.annotations.NotNull;

import java.util.Map;

final class ShopHolder implements InventoryHolder {
    private Inventory inventory;
    private ShopCategory category;
    private Map<Integer, ShopItem> items = Map.of();

    ShopHolder(ShopCategory category) {
        this.category = category;
    }

    void attach(Inventory inventory) { this.inventory = inventory; }
    ShopCategory category() { return category; }
    void category(ShopCategory category) { this.category = category; }
    Map<Integer, ShopItem> items() { return items; }
    void items(Map<Integer, ShopItem> items) { this.items = Map.copyOf(items); }

    @Override
    public @NotNull Inventory getInventory() {
        if (inventory == null) {
            throw new IllegalStateException("Inventory has not been attached");
        }
        return inventory;
    }
}
