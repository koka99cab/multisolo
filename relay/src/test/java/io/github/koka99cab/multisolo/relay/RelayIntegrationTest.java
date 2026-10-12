package io.github.koka99cab.multisolo.relay;

import io.github.koka99cab.multisolo.core.experience.CompatibilityKey;
import io.github.koka99cab.multisolo.core.experience.Difficulty;
import io.github.koka99cab.multisolo.core.experience.Experience;
import io.github.koka99cab.multisolo.core.message.*;
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
    private PhantomSign sign() { return new PhantomSign(MessagePhrase.example(), SignColor.parse("#12ABEF"), PhantomSign.Face.FRONT); }

    private final MessagePosition position = new MessagePosition(10, 64, -5);
    private CompatibilityKey scope(String version, String dimension) {
        return new CompatibilityKey(new Experience("multisolo:origin", 2, 0, Difficulty.NORMAL, Map.of()), version, Protocol.VERSION, dimension);
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
                var first = a.publish(id, scope, position, sign());
                assertEquals(alice.id(), first.authorId());
                assertEquals(first, a.publish(id, scope, position, first.sign()), "PUT retry is idempotent");
                assertEquals(first, b.nearby(scope, position).getFirst());
                assertThrows(IOException.class, () -> b.delete(id));
                assertThrows(IOException.class, () -> b.publish(id, scope, position, sign()));
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
                client.publish(UUID.randomUUID(), scope, position, sign());
                assertTrue(client.nearby(scope("26.3", "minecraft:overworld"), position).isEmpty());
                assertTrue(client.nearby(scope("26.4-snapshot-3", "minecraft:the_nether"), position).isEmpty());
                assertEquals(1, client.nearby(scope, new MessagePosition(138, 64, -5)).size());
                assertTrue(client.nearby(scope, new MessagePosition(139, 64, -5)).isEmpty());
                var wrongSeed = new CompatibilityKey(new Experience("multisolo:origin", 2, 123, Difficulty.NORMAL, Map.of()), "26.4-snapshot-3", Protocol.VERSION, "minecraft:overworld");
                assertThrows(IOException.class, () -> client.publish(UUID.randomUUID(), wrongSeed, position, sign()));
            }
        }
    }

    @Test void unsignedTamperedExpiredAndOversizedWritesAreRejected() throws Exception {
        try (RelayServer server = server(); HttpClient http = HttpClient.newHttpClient()) {
            server.start();
            String target = Protocol.path(UUID.randomUUID());
            URI uri = endpoint(server).resolve(target);
            NetworkIdentity identity = NetworkIdentity.generate();
            byte[] body = Protocol.JSON.toJson(new Protocol.Publish(scope("26.4-snapshot-3", "minecraft:overworld"), position, sign())).getBytes(StandardCharsets.UTF_8);
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
                for (int index = 0; index < 10; index++) client.publish(UUID.randomUUID(), scope("26.4-snapshot-3", "minecraft:overworld"), position, sign());
                IOException exception = assertThrows(IOException.class, () -> client.publish(UUID.randomUUID(), scope("26.4-snapshot-3", "minecraft:overworld"), position, sign()));
                assertTrue(exception.getMessage().contains("429"));
            }
        }
    }

    @Test void votesArePerIdentityReversiblePersistentAndForbiddenForTheAuthor() throws Exception {
        NetworkIdentity alice = NetworkIdentity.generate(), bob = NetworkIdentity.generate();
        UUID id = UUID.randomUUID();
        var scope = scope("26.4-snapshot-3", "minecraft:overworld");
        try (RelayServer server = server()) {
            server.start();
            try (RelayClient a = new RelayClient(endpoint(server), alice); RelayClient b = new RelayClient(endpoint(server), bob)) {
                a.publish(id, scope, position, sign());
                assertThrows(IOException.class, () -> a.vote(id, 1));
                b.vote(id, 1); b.vote(id, 1);
                var up = b.nearbyViews(scope, position).getFirst();
                assertEquals(1, up.upvotes()); assertEquals(0, up.downvotes()); assertEquals(1, up.ownVote());
                b.vote(id, -1);
                var down = b.nearbyViews(scope, position).getFirst();
                assertEquals(0, down.upvotes()); assertEquals(1, down.downvotes()); assertEquals(-1, down.ownVote());
            }
        }
        try (RelayServer server = server()) {
            server.start();
            try (RelayClient b = new RelayClient(endpoint(server), bob)) {
                assertEquals(-1, b.nearbyViews(scope, position).getFirst().ownVote());
                b.vote(id, 0);
                assertEquals(0, b.nearbyViews(scope, position).getFirst().downvotes());
                assertThrows(IOException.class, () -> b.vote(UUID.randomUUID(), 1));
            }
        }
    }

    @Test void signedFreeTextUnknownVocabularyAndWrongLineCountsAreRejected() throws Exception {
        try (RelayServer server = server(); HttpClient http = HttpClient.newHttpClient()) {
            server.start();
            var identity = NetworkIdentity.generate();
            String valid = Protocol.JSON.toJson(new Protocol.Publish(scope("26.4-snapshot-3", "minecraft:overworld"), position, sign()));
            for (String invalid : new String[]{valid.replace("BEWARE", "arbitrary free text"), valid.replace("\"BUT\",", ""), valid.replace("\"rgb\":1223663", "\"rgb\":-1"),
                    "{\"scope\":" + Protocol.JSON.toJson(scope("26.4-snapshot-3", "minecraft:overworld")) + ",\"position\":" + Protocol.JSON.toJson(position) + ",\"text\":\"free text\"}"}) {
                String target = Protocol.path(UUID.randomUUID());
                byte[] body = invalid.getBytes(StandardCharsets.UTF_8);
                String time = Long.toString(System.currentTimeMillis());
                HttpRequest request = HttpRequest.newBuilder(endpoint(server).resolve(target))
                        .header("X-Multisolo-Key", identity.publicKey()).header("X-Multisolo-Time", time)
                        .header("X-Multisolo-Signature", identity.sign(NetworkIdentity.signingBytes("PUT", target, time, body)))
                        .PUT(HttpRequest.BodyPublishers.ofByteArray(body)).build();
                assertEquals(400, http.send(request, HttpResponse.BodyHandlers.discarding()).statusCode(), invalid);
            }
        }
    }
}
