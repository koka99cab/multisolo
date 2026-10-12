package io.github.koka99cab.multisolo.fabric;

import io.github.koka99cab.multisolo.core.message.LocatedMessage;
import io.github.koka99cab.multisolo.network.Protocol;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerChunkEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.SignBlockEntity;
import net.minecraft.world.level.block.entity.SignText;
import net.minecraft.world.level.block.entity.SignTextSlot;

/** Server-thread-only management of real, marked, temporary vanilla blocks. */
public final class RemoteSigns {
    private static final Map<ServerLevel, Map<BlockPos, UUID>> LOADED = new IdentityHashMap<>();
    private static final Map<ServerLevel, Set<UUID>> DISMISSED = new IdentityHashMap<>();
    private RemoteSigns() {}
    public static void initialize() {
        ServerChunkEvents.CHUNK_LOAD.register((level, chunk, generated) -> {
            if (!ExperienceWorlds.connectedSolitude(level.getServer())) return;
            for (var block : new ArrayList<>(chunk.getBlockEntities().values())) {
                if (block instanceof SignBlockEntity sign && ((PhantomSignData) sign).multisolo$remote() != null) {
                    var remote = ((PhantomSignData) sign).multisolo$remote();
                    entries(level).put(sign.getBlockPos(), remote.message().id());
                    // The FULL future is not ready during CHUNK_LOAD. Work on the supplied
                    // chunk without Level lookups, neighbor callbacks or block-entity side effects.
                    // The client has not received it yet, so no visibility delay is needed.
                    if (!remote.activeAt(System.currentTimeMillis())) {
                        var air = BuiltInRegistries.BLOCK.getValue(Identifier.parse(remote.originalAir())).defaultBlockState();
                        chunk.setBlockState(sign.getBlockPos(), air, Block.UPDATE_SKIP_ALL_SIDEEFFECTS);
                        entries(level).remove(sign.getBlockPos());
                    }
                }
            }
        });
        ServerChunkEvents.CHUNK_UNLOAD.register((level, chunk) -> entries(level).keySet().removeIf(pos ->
                (pos.getX() >> 4) == chunk.getPos().x() && (pos.getZ() >> 4) == chunk.getPos().z()));
        ServerLifecycleEvents.SERVER_STOPPED.register(server -> { LOADED.clear(); DISMISSED.clear(); });
    }
    private static Map<BlockPos, UUID> entries(ServerLevel level) { return LOADED.computeIfAbsent(level, key -> new HashMap<>()); }
    private static Set<UUID> dismissed(ServerLevel level) { return DISMISSED.computeIfAbsent(level, key -> new HashSet<>()); }
    public static List<RemoteSignState> snapshot(ServerLevel level) {
        var result = new ArrayList<RemoteSignState>();
        var iterator = entries(level).entrySet().iterator();
        while (iterator.hasNext()) {
            var entry = iterator.next();
            if (!level.hasChunkAt(entry.getKey())) { iterator.remove(); continue; }
            var block = level.getBlockEntity(entry.getKey());
            if (block instanceof SignBlockEntity sign && ((PhantomSignData) sign).multisolo$remote() != null) {
                result.add(((PhantomSignData) sign).multisolo$remote());
            } else {
                // A locally broken/replaced copy stays dismissed until this play session ends.
                dismissed(level).add(entry.getValue()); iterator.remove();
            }
        }
        return List.copyOf(result);
    }
    public static boolean place(ServerLevel level, Protocol.MessageView view, String viewer) {
        var message = view.message();
        var p = message.position();
        var pos = new BlockPos(p.x(), p.y(), p.z());
        if (!ExperienceWorlds.canShare(level.getServer()) || !view.activeAt(System.currentTimeMillis())
                || message.authorId().equals(viewer) || message.sign().placement() == null
                || !message.scope().experience().equals(ExperienceCatalog.origin())
                || !message.scope().minecraftVersion().equals(ExperienceWorlds.gameVersion())
                || message.scope().protocolVersion() != Multisolo.PROTOCOL_VERSION
                || !message.scope().dimension().equals(level.dimension().identifier().toString())
                || entries(level).size() >= 256 || dismissed(level).contains(message.id())
                || !level.hasChunkAt(pos) || !level.getWorldBorder().isWithinBounds(pos)
                || level.isOutsideBuildHeight(pos) || level.players().stream().noneMatch(player -> pos.distToCenterSqr(player.position()) <= 128 * 128)) return false;
        // Require all adjacent chunks to be loaded; checking support must not generate terrain.
        for (var direction : net.minecraft.core.Direction.values()) if (!level.hasChunkAt(pos.relative(direction))) return false;
        var original = level.getBlockState(pos);
        if (!original.isAir() || !original.getFluidState().isEmpty()) return false;
        var state = SignPlacements.state(message.sign().placement());
        if (!state.canSurvive(level, pos) || !level.setBlock(pos, state, 3)) return false;
        if (!(level.getBlockEntity(pos) instanceof SignBlockEntity sign)) { level.setBlock(pos, original, 3); return false; }
        var data = (PhantomSignData) sign;
        data.multisolo$remote(new RemoteSignState(message, BuiltInRegistries.BLOCK.getKey(original.getBlock()).toString(), view.upvotes(), view.downvotes(), false));
        var lines = message.sign().phrase().lines().stream().map(line -> (Component) Component.translatable(
                "multisolo.phrase.line." + line.name().toLowerCase(Locale.ROOT))).toList();
        sign.setText(new SignText(lines, lines, DyeColor.BLACK, false), SignTextSlot.valueOf(message.sign().face().name()));
        entries(level).put(pos, message.id());
        sign.setWaxed(true);
        level.sendBlockUpdated(pos, state, state, 3);
        return true;
    }
    public static void update(ServerLevel level, LocatedMessage message, Protocol.MessageView view) {
        if (!ExperienceWorlds.canShare(level.getServer())) return;
        var p = message.position(); var pos = new BlockPos(p.x(), p.y(), p.z());
        if (!level.hasChunkAt(pos) || !(level.getBlockEntity(pos) instanceof SignBlockEntity sign)) return;
        var data = (PhantomSignData) sign; var remote = data.multisolo$remote();
        if (remote == null || !remote.message().id().equals(message.id())) return;
        var next = view == null ? new RemoteSignState(remote.message(), remote.originalAir(), remote.upvotes(), remote.downvotes(), true)
                : new RemoteSignState(remote.message(), remote.originalAir(), view.upvotes(), view.downvotes(), false);
        if (!remote.equals(next)) {
            data.multisolo$remote(next);
            level.sendBlockUpdated(pos, sign.getBlockState(), sign.getBlockState(), 3);
        }
    }
    public static boolean remove(ServerLevel level, LocatedMessage message) {
        if (!ExperienceWorlds.connectedSolitude(level.getServer())) return false;
        var p = message.position(); var pos = new BlockPos(p.x(), p.y(), p.z());
        if (!level.hasChunkAt(pos) || !(level.getBlockEntity(pos) instanceof SignBlockEntity sign)) return false;
        var remote = ((PhantomSignData) sign).multisolo$remote();
        if (remote == null || !remote.message().id().equals(message.id())) return false;
        var air = BuiltInRegistries.BLOCK.getValue(Identifier.parse(remote.originalAir())).defaultBlockState();
        entries(level).remove(pos);
        return level.setBlock(pos, air, 3);
    }
}
