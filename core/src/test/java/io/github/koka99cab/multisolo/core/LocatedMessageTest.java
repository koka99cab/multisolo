package io.github.koka99cab.multisolo.core;

import io.github.koka99cab.multisolo.core.message.MessagePhrase;
import io.github.koka99cab.multisolo.core.message.SignColor;
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
    @Test void hexColorIsNormalizedAndRejectsInvalidOrNonOpaqueFormats() {
        assertEquals("#12ABEF", SignColor.parse("12abef").hex());
        assertEquals(0xFFFFFF, SignColor.parse("#FFFFFF").rgb());
        for (String invalid : new String[]{"#fff", "#12345678", "#gg0000", "red", "-123456", "#12345;"})
            assertThrows(IllegalArgumentException.class, () -> SignColor.parse(invalid));
    }
    @Test void distanceUsesThreeDimensionsAndDoesNotOverflowAtWorldBorder() {
        assertEquals(25, new MessagePosition(0, 0, 0).distanceSquared(new MessagePosition(0, 3, 4)));
        assertEquals(3_600_000_000_000_000L, new MessagePosition(-30_000_000, 0, 0).distanceSquared(new MessagePosition(30_000_000, 0, 0)));
        assertThrows(IllegalArgumentException.class, () -> new MessagePosition(Integer.MIN_VALUE, 64, 0));
    }
}
