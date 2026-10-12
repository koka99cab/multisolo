package io.github.koka99cab.multisolo.relay;

import io.github.koka99cab.multisolo.core.echo.*;
import io.github.koka99cab.multisolo.core.experience.*;
import io.github.koka99cab.multisolo.core.message.MessagePosition;
import io.github.koka99cab.multisolo.network.*;
import java.net.*;
import java.nio.file.*;
import java.time.*;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;

class EchoIntegrationTest {
    @TempDir Path data;
    private CompatibilityKey scope(String dimension) { return new CompatibilityKey(new Experience("multisolo:origin", 2, 0, Difficulty.NORMAL, Map.of()), "26.4-snapshot-3", 2, dimension); }
    private List<EchoFrame> frames() { return List.of(new EchoFrame(0, 64, 0, 179, 0, false, true, 0, EchoFrame.Tool.PICKAXE), new EchoFrame(1, 64, 0, -179, 30, true, false, .5F, EchoFrame.Tool.PICKAXE)); }
    @Test void sharesSignedMotionOnlyWithOtherCompatibleNearbyInstallations() throws Exception {
        var alice = NetworkIdentity.generate(); var bob = NetworkIdentity.generate();
        var scope = scope("minecraft:overworld"); var center = new MessagePosition(0, 64, 0);
        try (var server = new RelayServer(new InetSocketAddress("127.0.0.1", 0), data)) {
            server.start(); var endpoint = URI.create("http://127.0.0.1:" + server.port());
            try (var a = new RelayClient(endpoint, alice); var b = new RelayClient(endpoint, bob)) {
                var id = UUID.randomUUID(); var clip = a.publishEcho(id, scope, frames());
                assertEquals(alice.id(), clip.authorId()); assertEquals(200, clip.durationMillis());
                assertEquals(clip, a.publishEcho(id, scope, frames()));
                assertEquals(List.of(clip), b.nearbyEchoes(scope, center));
                assertTrue(a.nearbyEchoes(scope, center).isEmpty());
                assertTrue(b.nearbyEchoes(scope("minecraft:the_nether"), center).isEmpty());
                assertTrue(b.nearbyEchoes(scope, new MessagePosition(129, 64, 0)).isEmpty());
                assertThrows(java.io.IOException.class, () -> b.publishEcho(id, scope, frames()));
            }
        }
        try (var files = Files.list(data)) { assertEquals(0, files.filter(path -> path.getFileName().toString().endsWith(".json")).count(), "Motion must never be archived with signs"); }
        try (var server = new RelayServer(new InetSocketAddress("127.0.0.1", 0), data)) {
            server.start(); try (var b = new RelayClient(URI.create("http://127.0.0.1:" + server.port()), bob)) { assertTrue(b.nearbyEchoes(scope, center).isEmpty()); }
        }
    }
    @Test void expiresAtFifteenMinutesAndBoundsPerAuthorRetention() throws Exception {
        Instant start = Instant.now();
        class MutableClock extends Clock {
            Instant now = start;
            public ZoneId getZone() { return ZoneOffset.UTC; }
            public Clock withZone(ZoneId zone) { return this; }
            public Instant instant() { return now; }
        }
        var clock = new MutableClock(); var store = new EchoStore(clock); var request = new Protocol.PublishEcho(scope("minecraft:overworld"), frames());
        for (int i = 0; i < 4; i++) { store.publish(UUID.randomUUID(), "a".repeat(64), request); clock.now = clock.now.plusMillis(1); }
        assertEquals(3, store.nearby(request.scope(), new MessagePosition(0, 64, 0), "b".repeat(64)).size());
        clock.now = start.plusMillis(EchoClip.LIFETIME_MILLIS + 3);
        assertTrue(store.nearby(request.scope(), new MessagePosition(0, 64, 0), "b".repeat(64)).isEmpty());
    }
    @Test void largestMotionRequestFitsTheExistingWireLimit() {
        var frame = new EchoFrame(29_999_999.999, 2047.999, -29_999_999.999, -179.999F, -89.999F, false, false, .999F, EchoFrame.Tool.PICKAXE);
        var request = new Protocol.PublishEcho(scope("minecraft:overworld"), Collections.nCopies(EchoClip.MAX_FRAMES, frame));
        assertTrue(Protocol.JSON.toJson(request).getBytes(java.nio.charset.StandardCharsets.UTF_8).length <= Protocol.MAX_BODY_BYTES);
    }
}
