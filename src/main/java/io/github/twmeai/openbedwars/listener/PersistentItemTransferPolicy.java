package io.github.twmeai.openbedwars.listener;

import org.bukkit.Material;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryAction;

import java.util.EnumSet;
import java.util.Set;

final class PersistentItemTransferPolicy {
    private static final Set<Material> PERSISTENT_ITEMS = EnumSet.of(
            Material.WOODEN_SWORD,
            Material.WOODEN_PICKAXE,
            Material.IRON_PICKAXE,
            Material.GOLDEN_PICKAXE,
            Material.DIAMOND_PICKAXE,
            Material.WOODEN_AXE,
            Material.STONE_AXE,
            Material.IRON_AXE,
            Material.DIAMOND_AXE,
            Material.SHEARS
    );

    private PersistentItemTransferPolicy() {
    }

    static boolean isPersistent(Material material) {
        return material != null && PERSISTENT_ITEMS.contains(material);
    }

    static boolean shouldCancelClick(
            InventoryAction action,
            ClickType click,
            boolean clickedTop,
            boolean externalTop,
            Material current,
            Material cursor,
            Material hotbar,
            Material offhand
    ) {
        if (action == InventoryAction.MOVE_TO_OTHER_INVENTORY) {
            return !clickedTop && externalTop && isPersistent(current);
        }
        if (action == InventoryAction.DROP_ALL_CURSOR || action == InventoryAction.DROP_ONE_CURSOR) {
            return isPersistent(cursor);
        }
        if (action == InventoryAction.DROP_ALL_SLOT || action == InventoryAction.DROP_ONE_SLOT) {
            return isPersistent(current);
        }
        if (action == InventoryAction.PLACE_ALL_INTO_BUNDLE
                || action == InventoryAction.PLACE_SOME_INTO_BUNDLE) {
            return isPersistent(cursor);
        }
        if (action == InventoryAction.PICKUP_ALL_INTO_BUNDLE
                || action == InventoryAction.PICKUP_SOME_INTO_BUNDLE) {
            return isPersistent(current);
        }
        if (!clickedTop) {
            return false;
        }
        return switch (action) {
            case PLACE_ALL, PLACE_SOME, PLACE_ONE, SWAP_WITH_CURSOR -> isPersistent(cursor);
            case HOTBAR_SWAP -> click == ClickType.SWAP_OFFHAND
                    ? isPersistent(offhand)
                    : isPersistent(hotbar);
            case UNKNOWN -> isPersistent(cursor) || isPersistent(hotbar) || isPersistent(offhand);
            default -> false;
        };
    }

    static boolean shouldCancelDrag(Material oldCursor, boolean touchesTopInventory) {
        return touchesTopInventory && isPersistent(oldCursor);
    }
}
