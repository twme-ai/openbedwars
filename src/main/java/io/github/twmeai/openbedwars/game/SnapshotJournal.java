package io.github.twmeai.openbedwars.game;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.util.Optional;
import java.util.UUID;

final class SnapshotJournal {
    private final Path directory;

    SnapshotJournal(Path directory) {
        this.directory = directory;
    }

    void write(UUID playerId, String contents) throws IOException {
        Files.createDirectories(directory);
        Path target = path(playerId);
        Path temporary = directory.resolve(playerId + ".yml.tmp");
        ByteBuffer bytes = StandardCharsets.UTF_8.encode(contents);
        try (FileChannel channel = FileChannel.open(
                temporary,
                StandardOpenOption.CREATE,
                StandardOpenOption.TRUNCATE_EXISTING,
                StandardOpenOption.WRITE
        )) {
            while (bytes.hasRemaining()) channel.write(bytes);
            channel.force(true);
        }
        try {
            Files.move(temporary, target, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
        } catch (AtomicMoveNotSupportedException ignored) {
            Files.move(temporary, target, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    Optional<String> read(UUID playerId) throws IOException {
        Path path = path(playerId);
        if (!Files.isRegularFile(path)) return Optional.empty();
        return Optional.of(Files.readString(path, StandardCharsets.UTF_8));
    }

    boolean exists(UUID playerId) {
        return Files.isRegularFile(path(playerId));
    }

    void delete(UUID playerId) throws IOException {
        Files.deleteIfExists(path(playerId));
    }

    private Path path(UUID playerId) {
        return directory.resolve(playerId + ".yml");
    }
}
