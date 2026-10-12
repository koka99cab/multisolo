package io.github.koka99cab.multisolo.fabric;

import io.github.koka99cab.multisolo.core.message.LocatedMessage;
import io.github.koka99cab.multisolo.core.message.MessageLifetime;
import java.util.Objects;
import java.util.Set;

/** Marks only a temporary Multisolo copy and remembers the precise air state it replaced. */
public record RemoteSignState(LocatedMessage message, String originalAir, int upvotes, int downvotes, boolean retired) {
    public RemoteSignState {
        Objects.requireNonNull(message);
        if (message.sign().placement() == null || !Set.of("minecraft:air", "minecraft:cave_air", "minecraft:void_air").contains(originalAir))
            throw new IllegalArgumentException("Invalid temporary sign metadata");
        MessageLifetime.durationMillis(upvotes, downvotes);
    }
    public RemoteSignState(LocatedMessage message, String originalAir) { this(message, originalAir, 0, 0, false); }
    public boolean activeAt(long now) { return !retired && MessageLifetime.activeAt(message.createdAt(), upvotes, downvotes, now); }
}
