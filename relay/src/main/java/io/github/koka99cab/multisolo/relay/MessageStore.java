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
    private final java.time.Clock clock;
    private final Map<UUID, StoredMessage> messages = new HashMap<>();
    private record StoredMessage(int schemaVersion, LocatedMessage message, Map<String, Integer> votes) {
        StoredMessage {
            if (schemaVersion != 2) throw new IllegalArgumentException("Use a new v2 data directory; legacy free text is not imported");
            java.util.Objects.requireNonNull(message);
            votes = Map.copyOf(votes);
            if (votes.size() > 1000) throw new IllegalArgumentException("Vote capacity exceeded");
            votes.forEach((id, value) -> {
                if (!id.matches("[a-f0-9]{64}") || (value != -1 && value != 1) || id.equals(message.authorId()))
                    throw new IllegalArgumentException("Invalid persisted vote");
            });
        }
        Protocol.MessageView view(String viewer) {
            return new Protocol.MessageView(message,
                    (int) votes.values().stream().filter(value -> value == 1).count(),
                    (int) votes.values().stream().filter(value -> value == -1).count(), votes.getOrDefault(viewer, 0));
        }
    }

    public MessageStore(Path directory) throws IOException {
        this(directory, java.time.Clock.systemUTC());
    }
    public MessageStore(Path directory, java.time.Clock clock) throws IOException {
        this.directory = directory;
        this.clock = java.util.Objects.requireNonNull(clock);
        Files.createDirectories(directory);
        try (var files = Files.list(directory)) {
            for (Path file : files.filter(path -> path.getFileName().toString().endsWith(".json")).toList()) {
                try {
                    if (Files.size(file) > Protocol.MAX_RESPONSE_BYTES) throw new IOException("Oversized stored message");
                    StoredMessage stored = Protocol.JSON.fromJson(Files.readString(file), StoredMessage.class);
                    LocatedMessage message = stored.message();
                    if (!file.getFileName().toString().equals(message.id() + ".json")
                            || messages.putIfAbsent(message.id(), stored) != null) {
                        throw new IOException("Invalid or duplicate message filename");
                    }
                    if (messages.size() > MAX_MESSAGES) throw new IOException("Store capacity exceeded");
                } catch (RuntimeException exception) { throw new IOException("Invalid stored message: " + file.getFileName(), exception); }
            }
        }
    }

    public synchronized LocatedMessage publish(UUID id, String author, Protocol.Publish request, long now) throws IOException {
        StoredMessage previous = messages.get(id);
        if (previous != null) {
            LocatedMessage existing = previous.message();
            if (!existing.authorId().equals(author) || !existing.scope().equals(request.scope())
                    || !existing.position().equals(request.position()) || !existing.sign().equals(request.sign())) {
                throw new ConflictException();
            }
            return existing;
        }
        if (messages.size() >= MAX_MESSAGES) throw new CapacityException();
        LocatedMessage message = new LocatedMessage(id, author, request.scope(), request.position(), request.sign(), now);
        persist(new StoredMessage(2, message, Map.of()));
        return message;
    }

    public synchronized List<Protocol.MessageView> nearby(CompatibilityKey scope, MessagePosition center, String viewer) {
        long now = clock.millis();
        return messages.values().stream().filter(stored -> scope.canShareWith(stored.message().scope()))
                .filter(stored -> center.distanceSquared(stored.message().position()) <= Protocol.RADIUS * Protocol.RADIUS)
                .map(stored -> stored.view(viewer)).filter(view -> view.activeAt(now))
                .sorted(Comparator.comparingLong((Protocol.MessageView view) -> center.distanceSquared(view.message().position()))
                        .thenComparing(view -> view.message().createdAt()).thenComparing(view -> view.message().id().toString()))
                .limit(50).toList();
    }

    public synchronized boolean delete(UUID id, String author) throws IOException {
        StoredMessage stored = messages.get(id);
        if (stored == null) return true;
        LocatedMessage message = stored.message();
        if (!message.authorId().equals(author)) return false;
        Files.delete(directory.resolve(id + ".json"));
        messages.remove(id);
        return true;
    }

    public synchronized void vote(UUID id, String voter, int value) throws IOException {
        StoredMessage stored = messages.get(id);
        if (stored == null || !stored.view(voter).activeAt(clock.millis())) throw new MissingException();
        if (stored.message().authorId().equals(voter)) throw new ForbiddenException();
        if (value < -1 || value > 1) throw new IllegalArgumentException("Invalid vote");
        Map<String, Integer> votes = new HashMap<>(stored.votes());
        if (value == 0) votes.remove(voter); else votes.put(voter, value);
        if (votes.size() > 1000) throw new CapacityException();
        persist(new StoredMessage(2, stored.message(), votes));
    }

    private void persist(StoredMessage stored) throws IOException {
        AtomicFiles.write(directory.resolve(stored.message().id() + ".json"), Protocol.JSON.toJson(stored));
        messages.put(stored.message().id(), stored);
    }

    public static final class ConflictException extends IOException {}
    public static final class CapacityException extends IOException {}
    public static final class ForbiddenException extends IOException {}
    public static final class MissingException extends IOException {}
}
