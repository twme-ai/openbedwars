package io.github.twmeai.openbedwars.game;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SnapshotJournalTest {
    @TempDir
    Path temporaryDirectory;

    @Test
    void writesReplacesAndDeletesSnapshots() throws Exception {
        SnapshotJournal journal = new SnapshotJournal(temporaryDirectory.resolve("pending-restores"));
        UUID playerId = UUID.randomUUID();

        assertFalse(journal.exists(playerId));
        assertTrue(journal.read(playerId).isEmpty());

        journal.write(playerId, "version: 1\nvalue: first\n");
        assertTrue(journal.exists(playerId));
        assertEquals("version: 1\nvalue: first\n", journal.read(playerId).orElseThrow());
        assertFalse(Files.exists(temporaryDirectory.resolve("pending-restores").resolve(playerId + ".yml.tmp")));

        journal.write(playerId, "version: 1\nvalue: second\n");
        assertEquals("version: 1\nvalue: second\n", journal.read(playerId).orElseThrow());

        journal.delete(playerId);
        assertFalse(journal.exists(playerId));
        assertTrue(journal.read(playerId).isEmpty());
    }

    @Test
    void ignoresAnInterruptedTemporaryWrite() throws Exception {
        Path directory = temporaryDirectory.resolve("pending-restores");
        Files.createDirectories(directory);
        UUID playerId = UUID.randomUUID();
        Files.writeString(directory.resolve(playerId + ".yml.tmp"), "partial");
        SnapshotJournal journal = new SnapshotJournal(directory);

        assertFalse(journal.exists(playerId));
        assertTrue(journal.read(playerId).isEmpty());
    }
}
