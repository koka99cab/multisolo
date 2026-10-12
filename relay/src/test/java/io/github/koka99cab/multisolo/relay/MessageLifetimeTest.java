package io.github.koka99cab.multisolo.relay;

import io.github.koka99cab.multisolo.core.experience.*;
import io.github.koka99cab.multisolo.core.message.*;
import io.github.koka99cab.multisolo.network.*;
import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.*;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;

class MessageLifetimeTest {
    @TempDir Path data;
    private static final class TestClock extends Clock {
        volatile long now = System.currentTimeMillis();
        @Override public ZoneId getZone() { return ZoneOffset.UTC; }
        @Override public Clock withZone(ZoneId zone) { return this; }
        @Override public Instant instant() { return Instant.ofEpochMilli(now); }
    }
    @Test void expiresAfterSevenDaysWithoutDeletingArchiveOrRenewingOnRetry() throws Exception {
        var clock = new TestClock();
        var alice = NetworkIdentity.generate(); var bob = NetworkIdentity.generate();
        var scope = new CompatibilityKey(new Experience("multisolo:origin", 2, 0, Difficulty.NORMAL, Map.of()), "26.4-snapshot-3", Protocol.VERSION, "minecraft:overworld");
        var position = new MessagePosition(1, 64, 1);
        var sign = new PhantomSign(MessagePhrase.example(), PhantomSign.Face.FRONT,
                new SignPlacement("minecraft:oak_sign", Map.of("rotation", "0", "waterlogged", "false")));
        var id = UUID.randomUUID();
        try (var server = new RelayServer(new InetSocketAddress("127.0.0.1", 0), data, clock)) {
            server.start(); var endpoint = URI.create("http://127.0.0.1:" + server.port());
            try (var a = new RelayClient(endpoint, alice); var b = new RelayClient(endpoint, bob)) {
                var original = a.publish(id, scope, position, sign);
                clock.now += LocatedMessage.LIFETIME_MILLIS - 1;
                assertEquals(1, b.nearby(scope, position).size());
                assertEquals(original, a.publish(id, scope, position, sign));
                clock.now++;
                assertTrue(b.nearby(scope, position).isEmpty());
                assertThrows(java.io.IOException.class, () -> b.vote(id, 1));
                assertTrue(Files.exists(data.resolve(id + ".json")), "Expiration must retain the archive");
            }
        }
        try (var server = new RelayServer(new InetSocketAddress("127.0.0.1", 0), data, clock)) {
            server.start();
            try (var client = new RelayClient(URI.create("http://127.0.0.1:" + server.port()), bob)) {
                assertTrue(client.nearby(scope, position).isEmpty(), "Restart must not revive expired messages");
                assertTrue(Files.readString(data.resolve(id + ".json")).contains("oak_sign"));
            }
        }
    }
    @Test void upvoteExtendsPresenceAndDownvoteCanArchiveAnOlderMessage() throws Exception {
        var clock = new TestClock();
        var alice = NetworkIdentity.generate(); var bob = NetworkIdentity.generate(); var charlie = NetworkIdentity.generate();
        var scope = new CompatibilityKey(new Experience("multisolo:origin", 2, 0, Difficulty.NORMAL, Map.of()), "26.4-snapshot-3", Protocol.VERSION, "minecraft:overworld");
        var pos = new MessagePosition(0, 64, 0); var id = UUID.randomUUID();
        try (var server = new RelayServer(new InetSocketAddress("127.0.0.1", 0), data, clock)) {
            server.start(); var endpoint = URI.create("http://127.0.0.1:" + server.port());
            try (var a = new RelayClient(endpoint, alice); var b = new RelayClient(endpoint, bob); var c = new RelayClient(endpoint, charlie)) {
                a.publish(id, scope, pos, new PhantomSign(MessagePhrase.example(), PhantomSign.Face.FRONT));
                b.vote(id, 1);
                clock.now += LocatedMessage.LIFETIME_MILLIS + 1;
                var extended = b.nearbyViews(scope, pos).getFirst();
                assertEquals(1, extended.upvotes());
                assertTrue(extended.activeAt(clock.millis()));
                c.vote(id, -1);
                assertTrue(b.nearby(scope, pos).isEmpty(), "A neutral score restores the original seven-day deadline");
                assertThrows(java.io.IOException.class, () -> c.vote(id, 1), "An expired message cannot be revived by voting");
                assertTrue(Files.exists(data.resolve(id + ".json")));
            }
        }
        try (var server = new RelayServer(new InetSocketAddress("127.0.0.1", 0), data, clock)) {
            server.start();
            try (var b = new RelayClient(URI.create("http://127.0.0.1:" + server.port()), bob)) {
                assertTrue(b.nearby(scope, pos).isEmpty());
            }
        }
    }
}
