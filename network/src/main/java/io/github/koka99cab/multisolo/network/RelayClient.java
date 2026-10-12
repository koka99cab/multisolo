package io.github.koka99cab.multisolo.network;

import io.github.koka99cab.multisolo.core.experience.CompatibilityKey;
import io.github.koka99cab.multisolo.core.message.LocatedMessage;
import io.github.koka99cab.multisolo.core.message.MessagePosition;
import io.github.koka99cab.multisolo.core.message.PhantomSign;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.time.Duration;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/** Blocking transport. Call on a worker, never on the Minecraft render/server thread. */
public final class RelayClient implements AutoCloseable {
    private final URI base;
    private final NetworkIdentity identity;
    private final HttpClient client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5))
            .followRedirects(HttpClient.Redirect.NEVER).build();

    public RelayClient(URI base, NetworkIdentity identity) {
        validateEndpoint(base);
        this.base = base;
        this.identity = java.util.Objects.requireNonNull(identity);
    }

    public static void validateEndpoint(URI uri) {
        boolean loopback = "localhost".equals(uri.getHost()) || "127.0.0.1".equals(uri.getHost())
                || "[::1]".equals(uri.getHost());
        if (uri.getHost() == null || uri.getUserInfo() != null || uri.getQuery() != null
                || uri.getFragment() != null || !(uri.getPath().isEmpty() || uri.getPath().equals("/"))
                || !("https".equals(uri.getScheme()) || ("http".equals(uri.getScheme()) && loopback))) {
            throw new IllegalArgumentException("Use an HTTPS origin, or HTTP on localhost for development");
        }
    }

    public LocatedMessage publish(UUID id, CompatibilityKey scope, MessagePosition position, PhantomSign sign)
            throws IOException, InterruptedException, GeneralSecurityException {
        LocatedMessage message = Protocol.JSON.fromJson(request("PUT", Protocol.path(id),
                Protocol.JSON.toJson(new Protocol.Publish(scope, position, sign))), LocatedMessage.class);
        if (!message.id().equals(id) || !message.authorId().equals(identity.id())
                || !message.scope().equals(scope) || !message.position().equals(position)
                || !message.sign().equals(sign)) {
            throw new IOException("Relay returned a different publication");
        }
        return message;
    }

    public List<LocatedMessage> nearby(CompatibilityKey scope, MessagePosition position)
            throws IOException, InterruptedException, GeneralSecurityException {
        return nearbyViews(scope, position).stream().map(Protocol.MessageView::message).toList();
    }

    public List<Protocol.MessageView> nearbyViews(CompatibilityKey scope, MessagePosition position)
            throws IOException, InterruptedException, GeneralSecurityException {
        Protocol.Messages result = Protocol.JSON.fromJson(request("GET", Protocol.query(scope, position), ""), Protocol.Messages.class);
        if (result.messages().size() > 50 || result.messages().stream().anyMatch(view ->
                !scope.canShareWith(view.message().scope()) || position.distanceSquared(view.message().position()) > Protocol.RADIUS * Protocol.RADIUS)) {
            throw new IOException("Relay returned messages outside the requested scope");
        }
        return result.messages();
    }

    public void vote(UUID id, int value) throws IOException, InterruptedException, GeneralSecurityException {
        request("PUT", Protocol.path(id) + "/vote", Protocol.JSON.toJson(new Protocol.Vote(value)));
    }

    public void delete(UUID id) throws IOException, InterruptedException, GeneralSecurityException {
        request("DELETE", Protocol.path(id), "");
    }

    private String request(String method, String target, String text)
            throws IOException, InterruptedException, GeneralSecurityException {
        byte[] body = text.getBytes(StandardCharsets.UTF_8);
        if (body.length > Protocol.MAX_BODY_BYTES) throw new IOException("Request too large");
        String time = Long.toString(System.currentTimeMillis());
        HttpRequest request = HttpRequest.newBuilder(base.resolve(target)).timeout(Duration.ofSeconds(10))
                .header("Content-Type", "application/json; charset=utf-8")
                .header("X-Multisolo-Key", identity.publicKey())
                .header("X-Multisolo-Time", time)
                .header("X-Multisolo-Signature", identity.sign(NetworkIdentity.signingBytes(method, target, time, body)))
                .method(method, HttpRequest.BodyPublishers.ofByteArray(body)).build();
        var pending = client.sendAsync(request, HttpResponse.BodyHandlers.limiting(
                HttpResponse.BodyHandlers.ofByteArray(), Protocol.MAX_RESPONSE_BYTES));
        HttpResponse<byte[]> response;
        try {
            // Includes body delivery: headers alone must not let a slow relay hold the worker forever.
            response = pending.get(10, TimeUnit.SECONDS);
        } catch (ExecutionException exception) {
            throw new IOException("Relay request failed", exception.getCause());
        } catch (TimeoutException exception) {
            pending.cancel(true);
            throw new IOException("Relay request timed out", exception);
        } catch (InterruptedException exception) {
            pending.cancel(true);
            throw exception;
        }
        if (response.statusCode() / 100 != 2) throw new IOException("Relay HTTP " + response.statusCode());
        return new String(response.body(), StandardCharsets.UTF_8);
    }

    @Override public void close() { client.shutdownNow(); }
}
