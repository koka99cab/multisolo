package io.github.koka99cab.multisolo.core;

import io.github.koka99cab.multisolo.core.message.MessagePhrase;
import io.github.koka99cab.multisolo.core.message.MessagePosition;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class LocatedMessageTest {
    @Test void signsHaveExactlyFourCatalogLinesAndCannotBeEmpty() {
        assertThrows(IllegalArgumentException.class, () -> new MessagePhrase(java.util.List.of(MessagePhrase.Line.LAVA)));
        assertThrows(IllegalArgumentException.class, () -> new MessagePhrase(java.util.Collections.nCopies(4, MessagePhrase.Line.EMPTY)));
        assertThrows(NullPointerException.class, () -> new MessagePhrase(java.util.Arrays.asList(MessagePhrase.Line.LAVA, null, MessagePhrase.Line.EMPTY, MessagePhrase.Line.EMPTY)));
        var mutable = new java.util.ArrayList<>(MessagePhrase.example().lines());
        var phrase = new MessagePhrase(mutable);
        mutable.set(0, MessagePhrase.Line.EMPTY);
        assertEquals(MessagePhrase.Line.BEWARE, phrase.lines().getFirst());
        assertThrows(UnsupportedOperationException.class, () -> phrase.lines().set(0, MessagePhrase.Line.EMPTY));
    }
    @Test void distanceUsesThreeDimensionsAndDoesNotOverflowAtWorldBorder() {
        assertEquals(25, new MessagePosition(0, 0, 0).distanceSquared(new MessagePosition(0, 3, 4)));
        assertEquals(3_600_000_000_000_000L, new MessagePosition(-30_000_000, 0, 0).distanceSquared(new MessagePosition(30_000_000, 0, 0)));
        assertThrows(IllegalArgumentException.class, () -> new MessagePosition(Integer.MIN_VALUE, 64, 0));
    }
    @Test void placementRejectsArbitraryBlocksPropertiesAndWater() {
        var valid = java.util.Map.of("rotation", "15", "waterlogged", "false");
        assertDoesNotThrow(() -> new io.github.koka99cab.multisolo.core.message.SignPlacement("minecraft:oak_sign", valid));
        assertThrows(IllegalArgumentException.class, () -> new io.github.koka99cab.multisolo.core.message.SignPlacement("minecraft:command_block", valid));
        assertThrows(IllegalArgumentException.class, () -> new io.github.koka99cab.multisolo.core.message.SignPlacement("minecraft:oak_sign", java.util.Map.of("rotation", "16", "waterlogged", "false")));
        assertThrows(IllegalArgumentException.class, () -> new io.github.koka99cab.multisolo.core.message.SignPlacement("minecraft:oak_sign", java.util.Map.of("rotation", "0", "waterlogged", "true")));
        assertThrows(IllegalArgumentException.class, () -> new io.github.koka99cab.multisolo.core.message.SignPlacement("minecraft:oak_wall_sign", java.util.Map.of("facing", "up", "waterlogged", "false")));
        assertThrows(IllegalArgumentException.class, () -> new io.github.koka99cab.multisolo.core.message.SignPlacement("minecraft:oak_sign", java.util.Map.of("rotation", "0", "waterlogged", "false", "command", "unsafe")));
    }
    @Test void votesExtendAndShortenLifetimeWithinBounds() {
        var day = io.github.koka99cab.multisolo.core.message.MessageLifetime.DAY_MILLIS;
        assertEquals(7 * day, io.github.koka99cab.multisolo.core.message.MessageLifetime.durationMillis(2, 2));
        assertEquals(8 * day, io.github.koka99cab.multisolo.core.message.MessageLifetime.durationMillis(1, 0));
        assertEquals(day, io.github.koka99cab.multisolo.core.message.MessageLifetime.durationMillis(0, 1000));
        assertEquals(30 * day, io.github.koka99cab.multisolo.core.message.MessageLifetime.durationMillis(1000, 0));
        assertFalse(io.github.koka99cab.multisolo.core.message.MessageLifetime.activeAt(100, 1000, 0, 99));
        assertFalse(io.github.koka99cab.multisolo.core.message.MessageLifetime.activeAt(100, 0, 0, 100 + 7 * day));
        assertTrue(io.github.koka99cab.multisolo.core.message.MessageLifetime.activeAt(100, 1, 0, 100 + 7 * day));
        assertEquals(Long.MAX_VALUE, io.github.koka99cab.multisolo.core.message.MessageLifetime.expiresAt(Long.MAX_VALUE - 1, 1, 0));
    }
}
