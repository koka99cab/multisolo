package io.github.koka99cab.multisolo.fabric.client;

import io.github.koka99cab.multisolo.core.experience.CompatibilityKey;
import io.github.koka99cab.multisolo.core.message.LocatedMessage;
import io.github.koka99cab.multisolo.core.message.MessagePosition;
import io.github.koka99cab.multisolo.core.message.PhantomSign;
import io.github.koka99cab.multisolo.fabric.ExperienceCatalog;
import io.github.koka99cab.multisolo.fabric.ExperienceWorlds;
import io.github.koka99cab.multisolo.fabric.PhantomSignData;
import io.github.koka99cab.multisolo.network.NetworkIdentity;
import io.github.koka99cab.multisolo.network.RelayClient;
import io.github.koka99cab.multisolo.network.Protocol;
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
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.SignBlockEntity;
import net.minecraft.world.level.block.entity.SignTextSlot;

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
    private List<Protocol.MessageView> views = List.of();
    private long nextRefresh;
    private String errorKey = "";
    private record Context(CompatibilityKey scope, MessagePosition position) {}
    private record Publication(UUID id, Context context, PhantomSign sign) {}
    private Publication retryPublication;
    private LocatedMessage lastPublished;
    public LocatedMessage lastPublished() { return lastPublished; }

    SocialController() {
        try { settings = SocialSettings.load(directory.resolve("social.json")); }
        catch (Exception exception) { errorKey = "multisolo.error.settings"; }
    }
    public String endpoint() { return settings.relay(); }
    public boolean enabled() { return settings.messagesEnabled(); }
    public boolean busy() { return busy; }
    public String errorKey() { return errorKey; }
    public List<LocatedMessage> messages() { return views.stream().map(Protocol.MessageView::message).toList(); }
    public Protocol.MessageView view(LocatedMessage message) {
        return views.stream().filter(view -> view.message().id().equals(message.id())).findFirst()
                .orElse(new Protocol.MessageView(message, 0, 0, 0));
    }
    public String identityId() { return identity == null ? "" : identity.id(); }

    public void configure(boolean enabled, String endpoint) throws Exception {
        SocialSettings next = new SocialSettings(2, enabled, endpoint.strip());
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
        long now = System.nanoTime();
        if (!busy && enabled() && client.level != null && !client.gui.isPausing()
                && ExperienceWorlds.canShare(client.getSingleplayerServer()) && now >= nextRefresh) {
            nextRefresh = now + java.util.concurrent.TimeUnit.SECONDS.toNanos(10);
            refresh(client, ok -> {});
        }
    }

    private void cancel() {
        generation++;
        if (pending != null) pending.cancel(true);
        RelayClient active = transport;
        if (active != null) active.close();
        transport = null;
        pending = null;
        busy = false;
        views = List.of();
        nextRefresh = 0;
        retryPublication = null;
        lastPublished = null;
    }

    private Context context(Minecraft client) {
        if (!enabled() || client.player == null || client.level == null
                || !ExperienceWorlds.canShare(client.getSingleplayerServer())) {
            throw new IllegalStateException("multisolo.error.world");
        }
        var position = client.player.blockPosition();
        return new Context(new CompatibilityKey(ExperienceCatalog.origin(), ExperienceWorlds.gameVersion(), Protocol.VERSION,
                client.level.dimension().identifier().toString()), new MessagePosition(position.getX(), position.getY(), position.getZ()));
    }

    @FunctionalInterface private interface Operation { List<Protocol.MessageView> run(RelayClient relay, Context context) throws Exception; }

    private void submit(Minecraft client, Operation operation, Consumer<Boolean> finished) {
        if (busy) return;
        Context context;
        try { context = context(client); }
        catch (IllegalStateException exception) { errorKey = exception.getMessage(); finished.accept(false); return; }
        long requestGeneration = generation;
        String endpoint = settings.relay();
        busy = true;
        nextRefresh = System.nanoTime() + java.util.concurrent.TimeUnit.SECONDS.toNanos(10);
        errorKey = "";
        pending = worker.submit(() -> {
            try {
                if (identity == null) identity = NetworkIdentity.loadOrCreate(directory.resolve("identity.json"));
                try (RelayClient relay = new RelayClient(URI.create(endpoint), identity)) {
                    transport = relay;
                    if (Thread.currentThread().isInterrupted()) return;
                    List<Protocol.MessageView> result = operation.run(relay, context);
                    client.execute(() -> {
                        if (generation != requestGeneration) return;
                        views = result; busy = false;
                        finished.accept(true);
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
        submit(client, (relay, context) -> relay.nearbyViews(context.scope(), context.position()), finished);
    }
    public void publishSign(Minecraft client, SignBlockEntity block,
                            PhantomSign sign, Consumer<Boolean> finished) {
        if (busy) return;
        java.util.Objects.requireNonNull(sign);
        Context current;
        try { current = context(client); }
        catch (IllegalStateException exception) { errorKey = exception.getMessage(); finished.accept(false); return; }
        var pos = block.getBlockPos();
        if (block.isRemoved() || client.level.getBlockEntity(pos) != block
                || pos.distToCenterSqr(client.player.position()) > 64) {
            errorKey = "multisolo.error.sign"; finished.accept(false); return;
        }
        current = new Context(current.scope(), new MessagePosition(pos.getX(), pos.getY(), pos.getZ()));
        if (retryPublication == null || !retryPublication.context().equals(current) || !retryPublication.sign().equals(sign)) {
            retryPublication = new Publication(UUID.randomUUID(), current, sign);
        }
        Context publication = current;
        UUID id = retryPublication.id();
        var saved = new java.util.concurrent.atomic.AtomicReference<LocatedMessage>();
        var previous = ((PhantomSignData) block).multisolo$message(
                SignTextSlot.valueOf(sign.face().name()));
        submit(client, (relay, context) -> {
            saved.set(relay.publish(id, publication.scope(), publication.position(), sign));
            if (previous != null && previous.authorId().equals(identity.id()) && !previous.id().equals(id)) relay.delete(previous.id());
            return relay.nearbyViews(context.scope(), context.position());
        }, ok -> {
            if (ok) { retryPublication = null; lastPublished = saved.get(); }
            finished.accept(ok);
        });
    }
    public void delete(Minecraft client, UUID id, Consumer<Boolean> finished) {
        LocatedMessage known = messages().stream().filter(message -> message.id().equals(id)).findFirst().orElse(null);
        submit(client, (relay, context) -> {
            relay.delete(id);
            return relay.nearbyViews(context.scope(), context.position());
        }, ok -> { if (ok && known != null) forgetLocal(client, known); finished.accept(ok); });
    }
    static void forgetLocal(Minecraft client, LocatedMessage message) {
        var server = client.getSingleplayerServer();
        if (server == null || client.level == null || !client.level.dimension().identifier().toString().equals(message.scope().dimension())) return;
        var dimension = client.level.dimension();
        server.execute(() -> {
            if (!ExperienceWorlds.canShare(server)) return;
            var level = server.getLevel(dimension);
            var p = message.position();
            if (level != null && level.getBlockEntity(new BlockPos(p.x(), p.y(), p.z())) instanceof SignBlockEntity block) {
                var data = (PhantomSignData) block;
                var slot = SignTextSlot.valueOf(message.sign().face().name());
                var own = data.multisolo$message(slot);
                if (own != null && own.id().equals(message.id())) {
                    data.multisolo$message(slot, null);
                    level.sendBlockUpdated(block.getBlockPos(), block.getBlockState(), block.getBlockState(), 3);
                }
            }
        });
    }
    public void vote(Minecraft client, UUID id, int value, Consumer<Boolean> finished) {
        submit(client, (relay, context) -> {
            relay.vote(id, value);
            return relay.nearbyViews(context.scope(), context.position());
        }, finished);
    }
}
