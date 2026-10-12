package io.github.koka99cab.multisolo.relay;

import io.github.koka99cab.multisolo.core.echo.EchoClip;
import io.github.koka99cab.multisolo.core.experience.CompatibilityKey;
import io.github.koka99cab.multisolo.core.message.MessagePosition;
import io.github.koka99cab.multisolo.network.Protocol;
import java.time.Clock;
import java.util.*;

/** Ephemeral, bounded motion data. Never archived with persistent sign messages. */
final class EchoStore {
    private final Map<UUID, EchoClip> clips = new HashMap<>();
    private final Clock clock;
    EchoStore(Clock clock) { this.clock = clock; }
    private void expire() { clips.values().removeIf(clip -> !clip.activeAt(clock.millis())); }
    synchronized void prune() { expire(); }
    synchronized EchoClip publish(UUID id, String author, Protocol.PublishEcho request) throws java.io.IOException {
        expire();
        var existing = clips.get(id);
        if (existing != null) {
            if (!existing.authorId().equals(author) || !existing.scope().equals(request.scope()) || !existing.frames().equals(request.frames()))
                throw new MessageStore.ConflictException();
            return existing;
        }
        // Keep at most three recent clips per installation and scope.
        var owned = clips.values().stream().filter(clip -> clip.authorId().equals(author) && clip.scope().equals(request.scope()))
                .sorted(Comparator.comparingLong(EchoClip::createdAt).thenComparing(clip -> clip.id().toString())).toList();
        if (clips.size() >= 2000 && owned.size() < 3) throw new MessageStore.CapacityException();
        if (owned.size() >= 3) clips.remove(owned.getFirst().id());
        var clip = new EchoClip(id, author, request.scope(), request.frames(), clock.millis());
        clips.put(id, clip); return clip;
    }
    synchronized List<EchoClip> nearby(CompatibilityKey scope, MessagePosition position, String viewer) {
        expire();
        return clips.values().stream().filter(clip -> clip.scope().equals(scope) && !clip.authorId().equals(viewer)
                        && position.distanceSquared(clip.anchor()) <= Protocol.RADIUS * Protocol.RADIUS)
                .sorted(Comparator.comparingLong((EchoClip clip) -> position.distanceSquared(clip.anchor()))
                        .thenComparing(Comparator.comparingLong(EchoClip::createdAt).reversed()).thenComparing(clip -> clip.id().toString()))
                .limit(6).toList();
    }
}
