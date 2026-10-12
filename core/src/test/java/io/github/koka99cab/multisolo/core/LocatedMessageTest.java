package io.github.koka99cab.multisolo.core;

import io.github.koka99cab.multisolo.core.message.LocatedMessage;
import io.github.koka99cab.multisolo.core.message.MessagePosition;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class LocatedMessageTest {
    @Test void textIsBoundedByUnicodeCodePointsAndRejectsControls() {
        assertEquals("hello", LocatedMessage.validateText(" hello "));
        assertEquals(560, LocatedMessage.validateText("😀".repeat(280)).length());
        for (String invalid : new String[]{"", "   ", "x".repeat(281), "hello\nworld", "a\u202Eb", "a\u001Bb", "\uD800"}) {
            assertThrows(IllegalArgumentException.class, () -> LocatedMessage.validateText(invalid));
        }
    }
    @Test void distanceUsesThreeDimensionsAndDoesNotOverflowAtWorldBorder() {
        assertEquals(25, new MessagePosition(0, 0, 0).distanceSquared(new MessagePosition(0, 3, 4)));
        assertEquals(3_600_000_000_000_000L, new MessagePosition(-30_000_000, 0, 0).distanceSquared(new MessagePosition(30_000_000, 0, 0)));
        assertThrows(IllegalArgumentException.class, () -> new MessagePosition(Integer.MIN_VALUE, 64, 0));
    }
}
