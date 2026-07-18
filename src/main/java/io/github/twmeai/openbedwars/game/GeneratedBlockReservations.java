package io.github.twmeai.openbedwars.game;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

final class GeneratedBlockReservations {
    private final Set<BlockKey> reserved = new HashSet<>();

    boolean reserve(List<BlockKey> blocks) {
        Set<BlockKey> requested = new HashSet<>(blocks);
        if (requested.size() != blocks.size() || requested.stream().anyMatch(reserved::contains)) {
            return false;
        }
        reserved.addAll(requested);
        return true;
    }

    boolean contains(BlockKey block) {
        return reserved.contains(block);
    }

    boolean claim(BlockKey block) {
        return reserved.remove(block);
    }

    void release(List<BlockKey> blocks) {
        reserved.removeAll(blocks);
    }

    void clear() {
        reserved.clear();
    }
}
