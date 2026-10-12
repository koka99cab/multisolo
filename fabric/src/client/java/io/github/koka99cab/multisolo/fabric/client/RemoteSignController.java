package io.github.koka99cab.multisolo.fabric.client;

import io.github.koka99cab.multisolo.core.message.LocatedMessage;
import io.github.koka99cab.multisolo.fabric.ExperienceWorlds;
import io.github.koka99cab.multisolo.fabric.RemoteSignState;
import io.github.koka99cab.multisolo.fabric.RemoteSigns;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.AABB;

/** A conservative frustum gate for server-authoritative placement/removal in solo worlds. */
public final class RemoteSignController {
    private static final class Gate {
        int hiddenTicks;
        volatile boolean hidden;
        volatile long checkedAt;
        boolean permitted() { return hidden && System.nanoTime() - checkedAt < TimeUnit.MILLISECONDS.toNanos(100); }
    }
    private final Map<BlockPos, Gate> gates = new HashMap<>();
    private Object world;
    private volatile long generation;
    private boolean pending;
    private int ticks;
    private List<RemoteSignState> installed = List.of();
    public void cancelPending() { generation++; pending = false; gates.values().forEach(gate -> gate.hidden = false); gates.clear(); }
    private static BlockPos position(LocatedMessage message) {
        var p = message.position(); return new BlockPos(p.x(), p.y(), p.z());
    }
    public void tick(Minecraft client) {
        if (world != client.level) {
            cancelPending(); gates.clear(); installed = List.of(); pending = false; generation++; world = client.level;
        }
        var server = client.getSingleplayerServer();
        if (client.level == null || client.player == null || server == null || !ExperienceWorlds.canShare(server)) { cancelPending(); return; }
        var social = MultisoloClient.SOCIAL;
        long now = System.currentTimeMillis();
        var desired = new HashMap<BlockPos, LocatedMessage>();
        if (social.enabled()) social.messages().stream()
                .filter(message -> message.sign().placement() != null && !message.authorId().equals(social.identityId()))
                .sorted(Comparator.comparingInt((LocatedMessage message) -> social.view(message).downvotes() - social.view(message).upvotes())
                        .thenComparing(Comparator.comparingLong(LocatedMessage::createdAt).reversed()).thenComparing(message -> message.id().toString()))
                .forEach(message -> desired.putIfAbsent(position(message), message));
        var candidates = new HashSet<>(desired.keySet());
        installed.forEach(remote -> candidates.add(position(remote.message())));
        var camera = client.gameRenderer.mainCamera();
        for (BlockPos pos : candidates) {
            var gate = gates.computeIfAbsent(pos, key -> new Gate());
            boolean outside = client.gui.screen() == null && camera.isInitialized() && camera.getCapturedFrustum() == null
                    && pos.distToCenterSqr(camera.position()) >= 64
                    && !camera.getCullFrustum().isVisible(new AABB(pos).inflate(2));
            gate.hiddenTicks = outside ? Math.min(20, gate.hiddenTicks + 1) : 0;
            gate.checkedAt = System.nanoTime();
            gate.hidden = outside && gate.hiddenTicks >= 20;
        }
        gates.entrySet().removeIf(entry -> {
            if (candidates.contains(entry.getKey())) return false;
            entry.getValue().hidden = false; return true;
        });
        if (pending || ++ticks % 5 != 0) return;
        var remove = new ArrayList<LocatedMessage>();
        for (var remote : installed) {
            var message = remote.message(); var replacement = desired.get(position(message));
            if (!social.enabled() || (replacement == null && !remote.activeAt(now)) || (social.hasSnapshot() && (replacement == null || !replacement.id().equals(message.id())))) remove.add(message);
        }
        var add = new ArrayList<>(desired.values());
        var known = new HashSet<java.util.UUID>(); installed.forEach(remote -> known.add(remote.message().id()));
        add.removeIf(message -> known.contains(message.id()));
        // Pass revocable gates to the server. A camera turn/world change cancels queued mutations.
        var permits = new HashMap<>(gates);
        var dimension = client.level.dimension();
        Object requestWorld = world; long requestGeneration = generation;
        String viewer = social.identityId();
        var existing = installed;
        var views = new HashMap<java.util.UUID, io.github.koka99cab.multisolo.network.Protocol.MessageView>();
        desired.values().forEach(message -> views.put(message.id(), social.view(message)));
        pending = true;
        server.execute(() -> {
            if (generation != requestGeneration) return;
            var level = server.getLevel(dimension);
            List<RemoteSignState> result = List.of();
            if (level != null) {
                for (var message : remove) RemoteSigns.update(level, message, null);
                for (var remote : existing) {
                    var view = views.get(remote.message().id());
                    if (view != null) RemoteSigns.update(level, remote.message(), view);
                }
                for (var message : remove) {
                    var gate = permits.get(position(message));
                    if (gate != null && gate.permitted()) RemoteSigns.remove(level, message);
                }
                for (var message : add) {
                    var gate = permits.get(position(message));
                    if (gate != null && gate.permitted()) RemoteSigns.place(level, views.get(message.id()), viewer);
                }
                result = RemoteSigns.snapshot(level);
            }
            var snapshot = result;
            client.execute(() -> {
                if (world != requestWorld || generation != requestGeneration) return;
                installed = snapshot; pending = false;
            });
        });
    }
}
