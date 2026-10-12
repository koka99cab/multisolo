package io.github.koka99cab.multisolo.relay;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import io.github.koka99cab.multisolo.core.experience.CompatibilityKey;
import io.github.koka99cab.multisolo.core.experience.Difficulty;
import io.github.koka99cab.multisolo.core.message.MessagePosition;
import io.github.koka99cab.multisolo.network.NetworkIdentity;
import io.github.koka99cab.multisolo.network.Protocol;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.security.GeneralSecurityException;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public final class RelayServer implements AutoCloseable {
    private final HttpServer server;
    private final MessageStore store;
    private final ThreadPoolExecutor workers = new ThreadPoolExecutor(4, 4, 0, TimeUnit.SECONDS,
            new ArrayBlockingQueue<>(128));
    private final Map<String, Window> rates = new HashMap<>();
    private record Window(long startedAt, int count) {}

    public RelayServer(InetSocketAddress address, Path directory) throws IOException {
        store = new MessageStore(directory);
        server = HttpServer.create(address, 64);
        server.setExecutor(workers);
        server.createContext("/", this::handle);
    }

    public void start() { server.start(); }
    public int port() { return server.getAddress().getPort(); }

    private void handle(HttpExchange exchange) throws IOException {
        try (exchange) {
            try {
                String method = exchange.getRequestMethod();
                String target = exchange.getRequestURI().toASCIIString();
                String path = exchange.getRequestURI().getPath();
                if (target.length() > 4096) { respond(exchange, 414, "{}"); return; }
                if ("GET".equals(method) && "/health".equals(target)) {
                    respond(exchange, 200, "{\"status\":\"ok\",\"protocol\":1}"); return;
                }
                if (!path.equals("/v1/messages") && !path.startsWith("/v1/messages/")) {
                    respond(exchange, 404, "{}"); return;
                }
                byte[] body = exchange.getRequestBody().readNBytes(Protocol.MAX_BODY_BYTES + 1);
                if (body.length > Protocol.MAX_BODY_BYTES) { respond(exchange, 413, "{}"); return; }
                String author = authenticate(exchange, method, target, body);
                if (author == null) { respond(exchange, 401, "{}"); return; }
                if (!allow("read:" + author, 120)) { respond(exchange, 429, "{}"); return; }
                if ("GET".equals(method) && path.equals("/v1/messages")) {
                    if (body.length != 0) throw new IllegalArgumentException("Unexpected body");
                    Map<String, String> query = query(exchange.getRequestURI().getRawQuery());
                    if (!query.keySet().equals(Set.of("scope", "x", "y", "z"))) throw new IllegalArgumentException("Query fields");
                    CompatibilityKey scope = Protocol.JSON.fromJson(new String(Base64.getUrlDecoder()
                            .decode(query.get("scope")), StandardCharsets.UTF_8), CompatibilityKey.class);
                    validateScope(scope);
                    MessagePosition center = new MessagePosition(Integer.parseInt(query.get("x")),
                            Integer.parseInt(query.get("y")), Integer.parseInt(query.get("z")));
                    respond(exchange, 200, Protocol.JSON.toJson(new Protocol.Messages(store.nearby(scope, center))));
                } else if (path.startsWith("/v1/messages/") && exchange.getRequestURI().getRawQuery() == null) {
                    UUID id = UUID.fromString(path.substring("/v1/messages/".length()));
                    if (!path.equals(Protocol.path(id))) throw new IllegalArgumentException("Non-canonical id");
                    if (!allow("write:" + author, 10)) { respond(exchange, 429, "{}"); return; }
                    if ("PUT".equals(method)) {
                        Protocol.Publish request = Protocol.JSON.fromJson(new String(body, StandardCharsets.UTF_8), Protocol.Publish.class);
                        validateScope(request.scope());
                        respond(exchange, 200, Protocol.JSON.toJson(store.publish(id, author, request, System.currentTimeMillis())));
                    } else if ("DELETE".equals(method) && body.length == 0) {
                        respond(exchange, store.delete(id, author) ? 200 : 403, "{}");
                    } else respond(exchange, 405, "{}");
                } else respond(exchange, 405, "{}");
            } catch (MessageStore.ConflictException exception) { respond(exchange, 409, "{}"); }
            catch (MessageStore.CapacityException exception) { respond(exchange, 507, "{}"); }
            catch (RuntimeException exception) { respond(exchange, 400, "{}"); }
            catch (IOException exception) {
                System.err.println("Relay storage/connection error: " + exception.getClass().getSimpleName());
                respond(exchange, 500, "{}");
            }
        }
    }

    private static String authenticate(HttpExchange exchange, String method, String target, byte[] body) {
        try {
            String key = exchange.getRequestHeaders().getFirst("X-Multisolo-Key");
            String time = exchange.getRequestHeaders().getFirst("X-Multisolo-Time");
            String signature = exchange.getRequestHeaders().getFirst("X-Multisolo-Signature");
            if (key == null || time == null || signature == null || key.length() > 128
                    || time.length() > 20 || signature.length() > 128) return null;
            long timestamp = Long.parseLong(time), now = System.currentTimeMillis();
            if (timestamp < now - 300_000 || timestamp > now + 300_000) return null;
            return NetworkIdentity.verify(key, NetworkIdentity.signingBytes(method, target, time, body), signature)
                    ? NetworkIdentity.id(key) : null;
        } catch (RuntimeException | GeneralSecurityException exception) { return null; }
    }

    private synchronized boolean allow(String key, int limit) {
        long now = System.currentTimeMillis();
        rates.entrySet().removeIf(entry -> entry.getValue().startedAt < now - 60_000);
        Window current = rates.get(key);
        if (current == null && rates.size() >= 20_000) return false;
        if (current != null && current.count >= limit) return false;
        rates.put(key, new Window(current == null ? now : current.startedAt, current == null ? 1 : current.count + 1));
        return true;
    }

    private static Map<String, String> query(String raw) {
        Map<String, String> result = new HashMap<>();
        if (raw == null) throw new IllegalArgumentException("Missing query");
        for (String part : raw.split("&")) {
            String[] pair = part.split("=", 2);
            if (pair.length != 2 || result.putIfAbsent(pair[0], pair[1]) != null) throw new IllegalArgumentException("Invalid query");
        }
        return result;
    }

    private static void validateScope(CompatibilityKey scope) {
        var experience = scope.experience();
        if (scope.protocolVersion() != 1 || !scope.minecraftVersion().matches("26\\.[1-9][0-9]*(?:[.-][a-zA-Z0-9.-]+)?")
                || !Set.of("minecraft:overworld", "minecraft:the_nether", "minecraft:the_end").contains(scope.dimension())
                || !experience.id().equals("multisolo:origin") || experience.revision() != 2
                || experience.seed() != 0 || experience.difficulty() != Difficulty.NORMAL || !experience.worldgenMods().isEmpty()) {
            throw new IllegalArgumentException("Unsupported experience");
        }
    }

    private static void respond(HttpExchange exchange, int status, String json) throws IOException {
        byte[] bytes = json.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "application/json; charset=utf-8");
        exchange.getResponseHeaders().set("Cache-Control", "no-store");
        exchange.getResponseHeaders().set("X-Content-Type-Options", "nosniff");
        exchange.sendResponseHeaders(status, bytes.length);
        exchange.getResponseBody().write(bytes);
    }

    @Override public void close() { server.stop(0); workers.shutdownNow(); }
}
