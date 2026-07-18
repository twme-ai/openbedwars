package io.github.twmeai.openbedwars.listener;

import org.bukkit.Material;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryAction;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PersistentItemTransferPolicyTest {
    @Test
    void blocksEverySourceThatCanSwapPersistentEquipmentIntoTheTopInventory() {
        assertTrue(cancel(InventoryAction.HOTBAR_SWAP, ClickType.NUMBER_KEY,
                true, true, null, null, Material.WOODEN_SWORD, null));
        assertTrue(cancel(InventoryAction.HOTBAR_SWAP, ClickType.SWAP_OFFHAND,
                true, true, null, null, null, Material.SHEARS));
        assertTrue(cancel(InventoryAction.SWAP_WITH_CURSOR, ClickType.LEFT,
                true, true, Material.WHITE_WOOL, Material.DIAMOND_PICKAXE, null, null));
        assertTrue(cancel(InventoryAction.PLACE_ALL, ClickType.LEFT,
                true, true, null, Material.IRON_AXE, null, null));
        assertTrue(cancel(InventoryAction.PLACE_ONE, ClickType.RIGHT,
                true, true, null, Material.SHEARS, null, null));
    }

    @Test
    void blocksShiftMovingPersistentEquipmentFromPlayerInventoryToContainer() {
        assertTrue(cancel(InventoryAction.MOVE_TO_OTHER_INVENTORY, ClickType.SHIFT_LEFT,
                false, true, Material.IRON_SWORD, null, null, null));
    }

    @Test
    void allowsTakingAnAlreadyStoredPersistentItemBackIntoPlayerInventory() {
        assertFalse(cancel(InventoryAction.PICKUP_ALL, ClickType.LEFT,
                true, true, Material.WOODEN_AXE, null, null, null));
        assertFalse(cancel(InventoryAction.MOVE_TO_OTHER_INVENTORY, ClickType.SHIFT_LEFT,
                true, true, Material.WOODEN_AXE, null, null, null));
    }

    @Test
    void allowsOrdinaryInventoryOrganizationAndNonPersistentContainerTransfers() {
        assertFalse(cancel(InventoryAction.HOTBAR_SWAP, ClickType.NUMBER_KEY,
                false, false, Material.WHITE_WOOL, null, Material.WOODEN_SWORD, null));
        assertFalse(cancel(InventoryAction.HOTBAR_SWAP, ClickType.NUMBER_KEY,
                true, true, null, null, Material.WHITE_WOOL, null));
        assertFalse(cancel(InventoryAction.MOVE_TO_OTHER_INVENTORY, ClickType.SHIFT_LEFT,
                false, true, Material.IRON_INGOT, null, null, null));
        assertFalse(cancel(InventoryAction.MOVE_TO_OTHER_INVENTORY, ClickType.SHIFT_LEFT,
                false, false, Material.WOODEN_SWORD, null, null, null));
    }

    @Test
    void blocksHidingPersistentEquipmentInsideBundlesInEitherInventory() {
        assertTrue(cancel(InventoryAction.PLACE_ALL_INTO_BUNDLE, ClickType.RIGHT,
                false, false, Material.BUNDLE, Material.STONE_SWORD, null, null));
        assertTrue(cancel(InventoryAction.PLACE_SOME_INTO_BUNDLE, ClickType.RIGHT,
                true, true, Material.BUNDLE, Material.GOLDEN_PICKAXE, null, null));
        assertTrue(cancel(InventoryAction.PICKUP_ALL_INTO_BUNDLE, ClickType.RIGHT,
                false, false, Material.SHEARS, Material.BUNDLE, null, null));
        assertTrue(cancel(InventoryAction.PICKUP_SOME_INTO_BUNDLE, ClickType.RIGHT,
                true, true, Material.IRON_AXE, Material.BUNDLE, null, null));
    }

    @Test
    void blocksDroppingPersistentEquipmentFromCursorOrClickedSlot() {
        assertTrue(cancel(InventoryAction.DROP_ALL_CURSOR, ClickType.WINDOW_BORDER_LEFT,
                false, false, null, Material.DIAMOND_SWORD, null, null));
        assertTrue(cancel(InventoryAction.DROP_ONE_SLOT, ClickType.DROP,
                false, false, Material.WOODEN_PICKAXE, null, null, null));
        assertFalse(cancel(InventoryAction.DROP_ALL_SLOT, ClickType.DROP,
                false, false, Material.WHITE_WOOL, null, null, null));
    }

    @Test
    void blocksPersistentDragsOnlyWhenTheyTouchTheTopInventory() {
        assertTrue(PersistentItemTransferPolicy.shouldCancelDrag(Material.SHEARS, true));
        assertFalse(PersistentItemTransferPolicy.shouldCancelDrag(Material.SHEARS, false));
        assertFalse(PersistentItemTransferPolicy.shouldCancelDrag(Material.WHITE_WOOL, true));
    }

    private boolean cancel(
            InventoryAction action,
            ClickType click,
            boolean clickedTop,
            boolean externalTop,
            Material current,
            Material cursor,
            Material hotbar,
            Material offhand
    ) {
        return PersistentItemTransferPolicy.shouldCancelClick(
                action, click, clickedTop, externalTop, current, cursor, hotbar, offhand);
    }
}
