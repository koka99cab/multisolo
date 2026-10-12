package io.github.koka99cab.multisolo.core.message;

import java.util.Map;
import java.util.Set;

/** Closed vanilla sign states; a publication can never request an arbitrary block. */
public record SignPlacement(String block, Map<String, String> properties) {
    public SignPlacement {
        if (block == null || !block.matches("minecraft:(oak|spruce|birch|jungle|acacia|dark_oak|mangrove|cherry|bamboo|crimson|warped|pale_oak)(?:_wall)?(?:_hanging)?_sign"))
            throw new IllegalArgumentException("Expected a vanilla sign block");
        properties = Map.copyOf(properties);
        boolean wall = block.contains("_wall"), hanging = block.contains("_hanging");
        Set<String> keys = wall ? Set.of("facing", "waterlogged") : hanging
                ? Set.of("rotation", "attached", "waterlogged") : Set.of("rotation", "waterlogged");
        if (!properties.keySet().equals(keys) || !"false".equals(properties.get("waterlogged")))
            throw new IllegalArgumentException("Invalid sign state");
        if (wall) {
            if (!Set.of("north", "south", "east", "west").contains(properties.get("facing")))
                throw new IllegalArgumentException("Invalid facing");
        } else {
            if (!properties.get("rotation").matches("[0-9]|1[0-5]")) throw new IllegalArgumentException("Invalid rotation");
            if (hanging && !Set.of("true", "false").contains(properties.get("attached"))) throw new IllegalArgumentException("Invalid attachment");
        }
    }
}
