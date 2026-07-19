package io.github.twmeai.openbedwars.listener;

import java.util.Collection;

final class DeathResourcePolicy {
    private DeathResourcePolicy() {
    }

    static <T> Collection<T> selectSource(
            boolean keepInventory,
            boolean hasTransferableDrops,
            Collection<T> inventoryContents,
            Collection<T> eventDrops
    ) {
        return keepInventory && !hasTransferableDrops ? inventoryContents : eventDrops;
    }
}
