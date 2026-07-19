package io.github.twmeai.openbedwars.listener;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class DeathResourcePolicyTest {
    @Test
    void usesEventDropsForOrdinaryDeaths() {
        assertEquals(List.of("drop:gold:4"), DeathResourcePolicy.selectSource(
                false,
                true,
                List.of("inventory:gold:9"),
                List.of("drop:gold:4")));
    }

    @Test
    void fallsBackToLiveInventoryWhenKeepInventorySuppressesDrops() {
        assertEquals(List.of("inventory:gold:4"), DeathResourcePolicy.selectSource(
                true,
                false,
                List.of("inventory:gold:4"),
                List.of()));
    }

    @Test
    void preservesExplicitEventDropsEvenWhenInventoryIsRetained() {
        assertEquals(List.of("plugin:drop:gold:2"), DeathResourcePolicy.selectSource(
                true,
                true,
                List.of("inventory:gold:4"),
                List.of("plugin:drop:gold:2")));
    }

    @Test
    void nonTransferablePluginDropsDoNotSuppressTheInventoryFallback() {
        assertEquals(List.of("inventory:gold:4"), DeathResourcePolicy.selectSource(
                true,
                false,
                List.of("inventory:gold:4"),
                List.of("plugin:death-token")));
    }
}
