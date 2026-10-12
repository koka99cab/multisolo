package io.github.koka99cab.multisolo.core.message;

import io.github.koka99cab.multisolo.core.experience.CompatibilityKey;
import java.util.Objects;
import java.util.UUID;

public record LocatedMessage(UUID id, String authorId, CompatibilityKey scope,
                             MessagePosition position, String text, long createdAt) {
    public LocatedMessage {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(scope, "scope");
        Objects.requireNonNull(position, "position");
        if (authorId == null || !authorId.matches("[a-f0-9]{64}")) {
            throw new IllegalArgumentException("Invalid network identity");
        }
        text = validateText(text);
        if (createdAt < 0) throw new IllegalArgumentException("Invalid creation time");
    }

    public static String validateText(String value) {
        if (value == null || value.isBlank() || value.codePointCount(0, value.length()) > 280
                || value.codePoints().anyMatch(c -> Character.isISOControl(c)
                    || Character.getType(c) == Character.FORMAT
                    || Character.getType(c) == Character.SURROGATE)) {
            throw new IllegalArgumentException("Use 1–280 characters without control characters");
        }
        return value.strip();
    }
}
