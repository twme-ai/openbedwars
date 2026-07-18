package io.github.twmeai.openbedwars.shop;

import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ShopInventoryPolicyTest {
    @Test
    void paymentCanCreateTheSpaceNeededForThePurchase() {
        ShopInventoryPolicy.Slot[] inventory = filledInventory();
        inventory[0] = currency(10);

        assertTrue(canFit(inventory, 10, 16, 64));
    }

    @Test
    void aMatchingButFullStackDoesNotHideAnOverflow() {
        ShopInventoryPolicy.Slot[] inventory = filledInventory();
        inventory[0] = currency(64);
        inventory[1] = similar(64);

        assertFalse(canFit(inventory, 4, 16, 64));
    }

    @Test
    void aPartialTeamColoredStackAcceptsTheWholeBundle() {
        ShopInventoryPolicy.Slot[] inventory = filledInventory();
        inventory[0] = currency(64);
        inventory[1] = similar(48);

        assertTrue(canFit(inventory, 4, 16, 64));
    }

    @Test
    void replacingTheDefaultSwordMakesRoomForThePurchasedSword() {
        ShopInventoryPolicy.Slot[] withoutReplacement = filledInventory();
        withoutReplacement[0] = currency(64);
        ShopInventoryPolicy.Slot[] withReplacement = withoutReplacement.clone();
        withReplacement[1] = new ShopInventoryPolicy.Slot(1, false, true, false);

        assertFalse(canFit(withoutReplacement, 10, 1, 1));
        assertTrue(canFit(withReplacement, 10, 1, 1));
    }

    @Test
    void rejectsInsufficientPaymentAndInvalidArguments() {
        List<ShopInventoryPolicy.Slot> inventory = List.of(currency(9), ShopInventoryPolicy.Slot.empty());

        assertFalse(ShopInventoryPolicy.canFitAfterPayment(inventory, 10, 16, 64));
        assertThrows(IllegalArgumentException.class, () -> ShopInventoryPolicy.canFitAfterPayment(
                inventory, -1, 16, 64));
        assertThrows(IllegalArgumentException.class, () -> ShopInventoryPolicy.canFitAfterPayment(
                inventory, 1, 0, 64));
        assertThrows(IllegalArgumentException.class, () -> new ShopInventoryPolicy.Slot(-1, false, false, false));
    }

    private boolean canFit(
            ShopInventoryPolicy.Slot[] inventory,
            int cost,
            int grantedAmount,
            int stackLimit
    ) {
        return ShopInventoryPolicy.canFitAfterPayment(List.of(inventory), cost, grantedAmount, stackLimit);
    }

    private ShopInventoryPolicy.Slot[] filledInventory() {
        ShopInventoryPolicy.Slot[] inventory = new ShopInventoryPolicy.Slot[36];
        Arrays.setAll(inventory, ignored -> new ShopInventoryPolicy.Slot(64, false, false, false));
        return inventory;
    }

    private ShopInventoryPolicy.Slot currency(int amount) {
        return new ShopInventoryPolicy.Slot(amount, true, false, false);
    }

    private ShopInventoryPolicy.Slot similar(int amount) {
        return new ShopInventoryPolicy.Slot(amount, false, false, true);
    }
}
