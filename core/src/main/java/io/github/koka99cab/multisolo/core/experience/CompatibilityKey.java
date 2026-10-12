package io.github.koka99cab.multisolo.core.experience;

import java.util.Objects;

/** Only equal keys may share messages, echoes or an assistance session. */
public record CompatibilityKey(
        Experience experience,
        String minecraftVersion,
        int protocolVersion,
        String dimension
) {
    public CompatibilityKey {
        Objects.requireNonNull(experience, "experience");
        if (minecraftVersion == null || minecraftVersion.isBlank()) {
            throw new IllegalArgumentException("minecraftVersion is required");
        }
        if (protocolVersion < 1) {
            throw new IllegalArgumentException("protocolVersion must be positive");
        }
        if (dimension == null || !dimension.matches("[a-z0-9_-]+:[a-z0-9_/-]+")) {
            throw new IllegalArgumentException("Expected a namespaced dimension id");
        }
    }

    public boolean canShareWith(CompatibilityKey other) {
        return equals(other);
    }
}
