package io.github.koka99cab.multisolo.core;

import io.github.koka99cab.multisolo.core.echo.*;
import java.util.List;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class EchoClipTest {
    private EchoFrame frame(double x, float yaw) { return new EchoFrame(x, 64, 0, yaw, 0, false, false, 0, EchoFrame.Tool.HAND); }
    @Test void interpolatesMotionAndTakesShortestTurnAcrossNorth() {
        var middle = frame(0, 179).interpolate(frame(2, -179), .5);
        assertEquals(1, middle.x()); assertEquals(180, Math.abs(middle.yaw()));
        assertEquals(0, frame(0, 0).interpolate(frame(2, 90), -10).x());
    }
    @Test void rejectsUnboundedOrNonFiniteMotionAndTeleports() {
        assertThrows(IllegalArgumentException.class, () -> frame(Double.NaN, 0));
        assertThrows(IllegalArgumentException.class, () -> frame(30_000_001, 0));
        assertThrows(IllegalArgumentException.class, () -> EchoClip.validateFrames(List.of(frame(0, 0), frame(9, 0))));
        assertThrows(IllegalArgumentException.class, () -> EchoClip.validateFrames(java.util.Collections.nCopies(42, frame(0, 0))));
        assertThrows(IllegalArgumentException.class, () -> EchoClip.validateFrames(List.of(frame(0, 0))));
    }
    @Test void recordingIsImmutableAndCannotExceedTheSpatialEnvelope() {
        var mutable = new java.util.ArrayList<>(List.of(frame(0, 0), frame(1, 0)));
        var safe = EchoClip.validateFrames(mutable); mutable.clear();
        assertEquals(2, safe.size()); assertThrows(UnsupportedOperationException.class, safe::clear);
        var wide = new java.util.ArrayList<EchoFrame>();
        for (int i = 0; i <= 10; i++) wide.add(frame(i * 7, 0));
        assertThrows(IllegalArgumentException.class, () -> EchoClip.validateFrames(wide));
    }
}
