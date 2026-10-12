package io.github.koka99cab.multisolo.fabric.client;

import io.github.koka99cab.multisolo.core.experience.CompatibilityKey;
import io.github.koka99cab.multisolo.core.message.LocatedMessage;
import io.github.koka99cab.multisolo.core.message.MessagePosition;
import io.github.koka99cab.multisolo.fabric.ExperienceCatalog;
import io.github.koka99cab.multisolo.fabric.ExperienceWorlds;
import io.github.koka99cab.multisolo.network.NetworkIdentity;
import io.github.koka99cab.multisolo.network.RelayClient;
import java.net.URI;
import java.nio.file.Path;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.function.Consumer;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;

public final class SocialController {
    private final Path directory = FabricLoader.getInstance().getConfigDir().resolve("multisolo");
    private final ExecutorService worker = Executors.newSingleThreadExecutor(task -> {
        Thread thread = new Thread(task, "multisolo-social"); thread.setDaemon(true); return thread;
    });
    private SocialSettings settings = SocialSettings.offline();
    private volatile NetworkIdentity identity;
    private volatile RelayClient transport;
    private Future<?> pending;
    private boolean busy;
    private long generation;
    private Object lastWorld;
    private List<LocatedMessage> messages = List.of();
    private String errorKey = "";
    private record Context(CompatibilityKey scope, MessagePosition position) {}
    private record Publication(UUID id, Context context, String text) {}
    private Publication retryPublication;

    SocialController() {
        try { settings = SocialSettings.load(directory.resolve("social.json")); }
        catch (Exception exception) { errorKey = "multisolo.error.settings"; }
    }
    public String endpoint() { return settings.relay(); }
    public boolean enabled() { return settings.messagesEnabled(); }
    public boolean busy() { return busy; }
    public String errorKey() { return errorKey; }
    public List<LocatedMessage> messages() { return messages; }
    public String identityId() { return identity == null ? "" : identity.id(); }

    public void configure(boolean enabled, String endpoint) throws Exception {
        SocialSettings next = new SocialSettings(1, enabled, endpoint.strip());
        if (!enabled) {
            // Revocation takes effect even if a full/read-only disk prevents persistence.
            cancel();
            settings = next;
            next.save(directory.resolve("social.json"));
            errorKey = "";
            return;
        }
        // Never enable network unless the explicit choice has been persisted successfully.
        next.save(directory.resolve("social.json"));
        cancel();
        settings = next;
        errorKey = "";
    }

    public void checkWorld(Minecraft client) {
        if (lastWorld != client.level) { cancel(); lastWorld = client.level; }
    }

    private void cancel() {
        generation++;
        if (pending != null) pending.cancel(true);
        RelayClient active = transport;
        if (active != null) active.close();
        transport = null;
        pending = null;
        busy = false;
        messages = List.of();
        retryPublication = null;
    }

    private Context context(Minecraft client) {
        if (!enabled() || client.player == null || client.level == null
                || !ExperienceWorlds.canShare(client.getSingleplayerServer())) {
            throw new IllegalStateException("multisolo.error.world");
        }
        var position = client.player.blockPosition();
        return new Context(new CompatibilityKey(ExperienceCatalog.origin(), ExperienceWorlds.gameVersion(), 1,
                client.level.dimension().identifier().toString()), new MessagePosition(position.getX(), position.getY(), position.getZ()));
    }

    @FunctionalInterface private interface Operation { List<LocatedMessage> run(RelayClient relay, Context context) throws Exception; }

    private void submit(Minecraft client, Operation operation, Consumer<Boolean> finished) {
        if (busy) return;
        Context context;
        try { context = context(client); }
        catch (IllegalStateException exception) { errorKey = exception.getMessage(); finished.accept(false); return; }
        long requestGeneration = generation;
        String endpoint = settings.relay();
        busy = true;
        errorKey = "";
        pending = worker.submit(() -> {
            try {
                if (identity == null) identity = NetworkIdentity.loadOrCreate(directory.resolve("identity.json"));
                try (RelayClient relay = new RelayClient(URI.create(endpoint), identity)) {
                    transport = relay;
                    if (Thread.currentThread().isInterrupted()) return;
                    List<LocatedMessage> result = operation.run(relay, context);
                    client.execute(() -> {
                        if (generation != requestGeneration) return;
                        messages = result; busy = false; finished.accept(true);
                    });
                }
            } catch (Exception exception) {
                client.execute(() -> {
                    if (generation != requestGeneration) return;
                    errorKey = "multisolo.error.relay"; busy = false; finished.accept(false);
                });
            } finally { transport = null; }
        });
    }

    public void refresh(Minecraft client, Consumer<Boolean> finished) {
        submit(client, (relay, context) -> relay.nearby(context.scope(), context.position()), finished);
    }
    public void publish(Minecraft client, String text, Consumer<Boolean> finished) {
        if (busy) return;
        String validated = LocatedMessage.validateText(text);
        Context current;
        try { current = context(client); }
        catch (IllegalStateException exception) { errorKey = exception.getMessage(); finished.accept(false); return; }
        if (retryPublication == null || !retryPublication.context().equals(current) || !retryPublication.text().equals(validated)) {
            retryPublication = new Publication(UUID.randomUUID(), current, validated);
        }
        UUID id = retryPublication.id();
        submit(client, (relay, context) -> {
            relay.publish(id, context.scope(), context.position(), validated);
            return relay.nearby(context.scope(), context.position());
        }, ok -> {
            if (ok) retryPublication = null;
            finished.accept(ok);
        });
    }
    public void delete(Minecraft client, UUID id, Consumer<Boolean> finished) {
        submit(client, (relay, context) -> {
            relay.delete(id);
            return relay.nearby(context.scope(), context.position());
        }, finished);
    }
}
