package io.github.twmeai.openbedwars.game;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ExclusiveArenaWorldsTest {
    @Test
    void keepsTheFirstArenaThatClaimsAWorld() {
        ExclusiveArenaWorlds<String> worlds = new ExclusiveArenaWorlds<>();
        UUID worldId = UUID.randomUUID();

        assertTrue(worlds.claim(worldId, "first"));
        assertFalse(worlds.claim(worldId, "duplicate"));
        assertEquals("first", worlds.owner(worldId).orElseThrow());
        assertEquals(1, worlds.size());
    }

    @Test
    void tracksIndependentWorldsAndCanBeRebuiltOnReload() {
        ExclusiveArenaWorlds<String> worlds = new ExclusiveArenaWorlds<>();
        UUID first = UUID.randomUUID();
        UUID second = UUID.randomUUID();

        assertTrue(worlds.claim(first, "first"));
        assertTrue(worlds.claim(second, "second"));
        assertEquals(2, worlds.size());

        worlds.clear();
        assertEquals(0, worlds.size());
        assertTrue(worlds.owner(first).isEmpty());
        assertTrue(worlds.claim(first, "reloaded"));
        assertEquals("reloaded", worlds.owner(first).orElseThrow());
    }

    @Test
    void rejectsNullWorldsAndOwners() {
        ExclusiveArenaWorlds<String> worlds = new ExclusiveArenaWorlds<>();
        UUID worldId = UUID.randomUUID();

        assertThrows(NullPointerException.class, () -> worlds.claim(null, "arena"));
        assertThrows(NullPointerException.class, () -> worlds.claim(worldId, null));
        assertThrows(NullPointerException.class, () -> worlds.owner(null));
    }
}
