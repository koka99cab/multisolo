package io.github.koka99cab.multisolo.core.experience;

import java.util.Map;
import java.util.Objects;

/** Immutable rules. A change to seed, difficulty or world generation needs a new revision. */
public record Experience(
        String id,
        int revision,
        long seed,
        Difficulty difficulty,
        Map<String, String> worldgenMods
) {
    public Experience {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(difficulty, "difficulty");
        if (!id.matches("[a-z0-9_-]+:[a-z0-9_/-]+")) {
            throw new IllegalArgumentException("Expected a namespaced experience id");
        }
        if (revision < 1) {
            throw new IllegalArgumentException("revision must be positive");
        }
        worldgenMods = Map.copyOf(worldgenMods);
        worldgenMods.forEach((mod, version) -> {
            if (mod.isBlank() || version.isBlank()) {
                throw new IllegalArgumentException("Worldgen mods need exact ids and versions");
            }
        });
    }

    public SpawnPolicy spawnPolicy() {
        return SpawnPolicy.VANILLA;
    }

    public enum SpawnPolicy {
        VANILLA
    }
}
