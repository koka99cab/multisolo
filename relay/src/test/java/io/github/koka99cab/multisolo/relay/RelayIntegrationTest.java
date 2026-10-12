package io.github.koka99cab.multisolo.relay;

import io.github.koka99cab.multisolo.core.experience.CompatibilityKey;
import io.github.koka99cab.multisolo.core.experience.Difficulty;
import io.github.koka99cab.multisolo.core.experience.Experience;
import io.github.koka99cab.multisolo.core.message.MessagePosition;
import io.github.koka99cab.multisolo.network.NetworkIdentity;
import io.github.koka99cab.multisolo.network.Protocol;
import io.github.koka99cab.multisolo.network.RelayClient;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;

class RelayIntegrationTest {
    @TempDir Path data;
    private final MessagePosition position = new MessagePosition(10, 64, -5);
    private CompatibilityKey scope(String version, String dimension) {
        return new CompatibilityKey(new Experience("multisolo:origin", 2, 0, Difficulty.NORMAL, Map.of()), version, 1, dimension);
    }
    private RelayServer server() throws IOException { return new RelayServer(new InetSocketAddress("127.0.0.1", 0), data); }
    private URI endpoint(RelayServer server) { return URI.create("http://127.0.0.1:" + server.port()); }

    @Test void twoIndependentPlayersSharePersistentMessagesAndOnlyAuthorCanDelete() throws Exception {
        NetworkIdentity alice = NetworkIdentity.generate(), bob = NetworkIdentity.generate();
        UUID id = UUID.randomUUID();
        var scope = scope("26.4-snapshot-3", "minecraft:overworld");
        try (RelayServer server = server()) {
            server.start();
            try (RelayClient a = new RelayClient(endpoint(server), alice); RelayClient b = new RelayClient(endpoint(server), bob)) {
                var first = a.publish(id, scope, position, "Un village à l'est 😀");
                assertEquals(alice.id(), first.authorId());
                assertEquals(first, a.publish(id, scope, position, first.text()), "PUT retry is idempotent");
                assertEquals(first, b.nearby(scope, position).getFirst());
                assertThrows(IOException.class, () -> b.delete(id));
                assertThrows(IOException.class, () -> b.publish(id, scope, position, "Impersonation"));
            }
        }
        try (RelayServer restarted = server()) {
            restarted.start();
            try (RelayClient a = new RelayClient(endpoint(restarted), alice)) {
                assertEquals(id, a.nearby(scope, position).getFirst().id());
                a.delete(id);
                assertTrue(a.nearby(scope, position).isEmpty());
            }
        }
        try (RelayServer restarted = server()) {
            restarted.start();
            try (RelayClient a = new RelayClient(endpoint(restarted), alice)) { assertTrue(a.nearby(scope, position).isEmpty()); }
        }
    }

    @Test void versionDimensionDistanceAndExperienceSeparateMessages() throws Exception {
        try (RelayServer server = server()) {
            server.start();
            try (RelayClient client = new RelayClient(endpoint(server), NetworkIdentity.generate())) {
                var scope = scope("26.4-snapshot-3", "minecraft:overworld");
                client.publish(UUID.randomUUID(), scope, position, "hello");
                assertTrue(client.nearby(scope("26.3", "minecraft:overworld"), position).isEmpty());
                assertTrue(client.nearby(scope("26.4-snapshot-3", "minecraft:the_nether"), position).isEmpty());
                assertEquals(1, client.nearby(scope, new MessagePosition(138, 64, -5)).size());
                assertTrue(client.nearby(scope, new MessagePosition(139, 64, -5)).isEmpty());
                var wrongSeed = new CompatibilityKey(new Experience("multisolo:origin", 2, 123, Difficulty.NORMAL, Map.of()), "26.4-snapshot-3", 1, "minecraft:overworld");
                assertThrows(IOException.class, () -> client.publish(UUID.randomUUID(), wrongSeed, position, "wrong"));
            }
        }
    }

    @Test void unsignedTamperedExpiredAndOversizedWritesAreRejected() throws Exception {
        try (RelayServer server = server(); HttpClient http = HttpClient.newHttpClient()) {
            server.start();
            String target = Protocol.path(UUID.randomUUID());
            URI uri = endpoint(server).resolve(target);
            NetworkIdentity identity = NetworkIdentity.generate();
            byte[] body = Protocol.JSON.toJson(new Protocol.Publish(scope("26.4-snapshot-3", "minecraft:overworld"), position, "hello")).getBytes(StandardCharsets.UTF_8);
            assertEquals(401, http.send(HttpRequest.newBuilder(uri).PUT(HttpRequest.BodyPublishers.ofByteArray(body)).build(), HttpResponse.BodyHandlers.discarding()).statusCode());
            String time = Long.toString(System.currentTimeMillis());
            String signature = identity.sign(NetworkIdentity.signingBytes("PUT", target, time, body));
            HttpRequest tampered = HttpRequest.newBuilder(uri).header("X-Multisolo-Key", identity.publicKey())
                    .header("X-Multisolo-Time", time).header("X-Multisolo-Signature", signature)
                    .PUT(HttpRequest.BodyPublishers.ofString("{}")).build();
            assertEquals(401, http.send(tampered, HttpResponse.BodyHandlers.discarding()).statusCode());
            String expired = "1";
            HttpRequest stale = HttpRequest.newBuilder(uri).header("X-Multisolo-Key", identity.publicKey())
                    .header("X-Multisolo-Time", expired).header("X-Multisolo-Signature", identity.sign(NetworkIdentity.signingBytes("PUT", target, expired, body)))
                    .PUT(HttpRequest.BodyPublishers.ofByteArray(body)).build();
            assertEquals(401, http.send(stale, HttpResponse.BodyHandlers.discarding()).statusCode());
            assertEquals(413, http.send(HttpRequest.newBuilder(uri).PUT(HttpRequest.BodyPublishers.ofString("x".repeat(8193))).build(), HttpResponse.BodyHandlers.discarding()).statusCode());
        }
    }

    @Test void publicationRateIsBounded() throws Exception {
        try (RelayServer server = server()) {
            server.start();
            try (RelayClient client = new RelayClient(endpoint(server), NetworkIdentity.generate())) {
                for (int index = 0; index < 10; index++) client.publish(UUID.randomUUID(), scope("26.4-snapshot-3", "minecraft:overworld"), position, "message " + index);
                IOException exception = assertThrows(IOException.class, () -> client.publish(UUID.randomUUID(), scope("26.4-snapshot-3", "minecraft:overworld"), position, "too many"));
                assertTrue(exception.getMessage().contains("429"));
            }
        }
    }
}
