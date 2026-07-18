package io.github.twmeai.openbedwars.shop;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

final class ShopInventoryPolicy {
    private ShopInventoryPolicy() {
    }

    static boolean canFitAfterPayment(
            ItemStack[] storage,
            int inventoryStackLimit,
            Material currency,
            int cost,
            Set<Material> replacedMaterials,
            ItemStack granted
    ) {
        List<Slot> slots = new ArrayList<>(storage.length);
        for (ItemStack existing : storage) {
            slots.add(existing == null || existing.isEmpty()
                    ? Slot.empty()
                    : new Slot(
                            existing.getAmount(),
                            existing.getType() == currency,
                            replacedMaterials.contains(existing.getType()),
                            existing.isSimilar(granted)
                    ));
        }
        return canFitAfterPayment(
                slots,
                cost,
                granted.getAmount(),
                Math.min(inventoryStackLimit, granted.getMaxStackSize())
        );
    }

    static boolean canFitAfterPayment(List<Slot> storage, int cost, int grantedAmount, int grantedStackLimit) {
        if (cost < 0) {
            throw new IllegalArgumentException("cost must not be negative");
        }
        if (grantedAmount <= 0 || grantedStackLimit <= 0) {
            throw new IllegalArgumentException("grant amount and stack limit must be positive");
        }

        int[] amounts = storage.stream().mapToInt(Slot::amount).toArray();
        int unpaid = removeCurrency(storage, amounts, cost);
        if (unpaid > 0) {
            return false;
        }
        for (int slot = 0; slot < storage.size(); slot++) {
            if (storage.get(slot).replaced()) {
                amounts[slot] = 0;
            }
        }

        int remaining = grantedAmount;
        for (int slot = 0; slot < storage.size(); slot++) {
            if (amounts[slot] > 0 && storage.get(slot).similarToGrant()) {
                remaining -= Math.min(remaining, Math.max(0, grantedStackLimit - amounts[slot]));
                if (remaining == 0) {
                    return true;
                }
            }
        }
        for (int amount : amounts) {
            if (amount == 0) {
                remaining -= Math.min(remaining, grantedStackLimit);
                if (remaining == 0) {
                    return true;
                }
            }
        }
        return false;
    }

    private static int removeCurrency(List<Slot> storage, int[] amounts, int cost) {
        int remaining = cost;
        for (int slot = 0; slot < storage.size() && remaining > 0; slot++) {
            if (!storage.get(slot).currency()) {
                continue;
            }
            int removed = Math.min(remaining, amounts[slot]);
            remaining -= removed;
            amounts[slot] -= removed;
        }
        return remaining;
    }

    record Slot(int amount, boolean currency, boolean replaced, boolean similarToGrant) {
        Slot {
            if (amount < 0) {
                throw new IllegalArgumentException("slot amount must not be negative");
            }
        }

        static Slot empty() {
            return new Slot(0, false, false, false);
        }
    }
}
