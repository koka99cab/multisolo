package io.github.koka99cab.multisolo.core.message;

import io.github.koka99cab.multisolo.core.experience.CompatibilityKey;
import java.util.Objects;
import java.util.UUID;

public record LocatedMessage(UUID id, String authorId, CompatibilityKey scope,
                             MessagePosition position, PhantomSign sign, long createdAt) {
    public LocatedMessage {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(scope, "scope");
        Objects.requireNonNull(position, "position");
        if (authorId == null || !authorId.matches("[a-f0-9]{64}")) {
            throw new IllegalArgumentException("Invalid network identity");
        }
        Objects.requireNonNull(sign, "sign");
        if (createdAt < 0) throw new IllegalArgumentException("Invalid creation time");
    }

}
