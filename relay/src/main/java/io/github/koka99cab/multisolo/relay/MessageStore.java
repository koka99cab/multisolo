package io.github.koka99cab.multisolo.relay;

import io.github.koka99cab.multisolo.core.experience.CompatibilityKey;
import io.github.koka99cab.multisolo.core.message.LocatedMessage;
import io.github.koka99cab.multisolo.core.message.MessagePosition;
import io.github.koka99cab.multisolo.network.AtomicFiles;
import io.github.koka99cab.multisolo.network.Protocol;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Bounded single-process store. Disk is written before acknowledging a mutation. */
public final class MessageStore {
    private static final int MAX_MESSAGES = 10_000;
    private final Path directory;
    private final Map<UUID, LocatedMessage> messages = new HashMap<>();

    public MessageStore(Path directory) throws IOException {
        this.directory = directory;
        Files.createDirectories(directory);
        try (var files = Files.list(directory)) {
            for (Path file : files.filter(path -> path.getFileName().toString().endsWith(".json")).toList()) {
                try {
                    if (Files.size(file) > Protocol.MAX_BODY_BYTES) throw new IOException("Oversized stored message");
                    LocatedMessage message = Protocol.JSON.fromJson(Files.readString(file), LocatedMessage.class);
                    if (!file.getFileName().toString().equals(message.id() + ".json")
                            || messages.putIfAbsent(message.id(), message) != null) {
                        throw new IOException("Invalid or duplicate message filename");
                    }
                    if (messages.size() > MAX_MESSAGES) throw new IOException("Store capacity exceeded");
                } catch (RuntimeException exception) { throw new IOException("Invalid stored message: " + file.getFileName(), exception); }
            }
        }
    }

    public synchronized LocatedMessage publish(UUID id, String author, Protocol.Publish request, long now) throws IOException {
        LocatedMessage existing = messages.get(id);
        if (existing != null) {
            if (!existing.authorId().equals(author) || !existing.scope().equals(request.scope())
                    || !existing.position().equals(request.position()) || !existing.text().equals(request.text())) {
                throw new ConflictException();
            }
            return existing;
        }
        if (messages.size() >= MAX_MESSAGES) throw new CapacityException();
        LocatedMessage message = new LocatedMessage(id, author, request.scope(), request.position(), request.text(), now);
        AtomicFiles.write(directory.resolve(id + ".json"), Protocol.JSON.toJson(message));
        messages.put(id, message);
        return message;
    }

    public synchronized List<LocatedMessage> nearby(CompatibilityKey scope, MessagePosition center) {
        return messages.values().stream().filter(message -> scope.canShareWith(message.scope()))
                .filter(message -> center.distanceSquared(message.position()) <= Protocol.RADIUS * Protocol.RADIUS)
                .sorted(Comparator.comparingLong((LocatedMessage message) -> center.distanceSquared(message.position()))
                        .thenComparing(LocatedMessage::createdAt).thenComparing(message -> message.id().toString()))
                .limit(50).toList();
    }

    public synchronized boolean delete(UUID id, String author) throws IOException {
        LocatedMessage message = messages.get(id);
        if (message == null) return true;
        if (!message.authorId().equals(author)) return false;
        Files.delete(directory.resolve(id + ".json"));
        messages.remove(id);
        return true;
    }

    public static final class ConflictException extends IOException {}
    public static final class CapacityException extends IOException {}
}
