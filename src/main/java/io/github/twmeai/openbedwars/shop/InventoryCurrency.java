package io.github.twmeai.openbedwars.shop;

import io.github.twmeai.openbedwars.game.ResourceType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

public final class InventoryCurrency {
    private InventoryCurrency() {
    }

    public static int count(Inventory inventory, ResourceType resource) {
        int total = 0;
        for (ItemStack item : inventory.getStorageContents()) {
            if (item != null && item.getType() == resource.material()) {
                total += item.getAmount();
            }
        }
        return total;
    }

    public static boolean take(Inventory inventory, ResourceType resource, int amount) {
        if (amount < 0) {
            throw new IllegalArgumentException("amount must not be negative");
        }
        if (count(inventory, resource) < amount) {
            return false;
        }
        int remaining = amount;
        ItemStack[] contents = inventory.getStorageContents();
        for (int slot = 0; slot < contents.length && remaining > 0; slot++) {
            ItemStack item = contents[slot];
            if (item == null || item.getType() != resource.material()) {
                continue;
            }
            int removed = Math.min(remaining, item.getAmount());
            item.setAmount(item.getAmount() - removed);
            remaining -= removed;
            if (item.getAmount() == 0) {
                contents[slot] = null;
            }
        }
        inventory.setStorageContents(contents);
        return true;
    }
}
