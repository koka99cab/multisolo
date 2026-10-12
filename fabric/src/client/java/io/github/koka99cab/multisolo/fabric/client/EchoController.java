package io.github.koka99cab.multisolo.fabric.client;

import io.github.koka99cab.multisolo.core.echo.EchoClip;
import io.github.koka99cab.multisolo.core.echo.EchoFrame;
import io.github.koka99cab.multisolo.core.experience.CompatibilityKey;
import io.github.koka99cab.multisolo.core.message.MessagePosition;
import io.github.koka99cab.multisolo.fabric.ExperienceCatalog;
import io.github.koka99cab.multisolo.fabric.ExperienceWorlds;
import io.github.koka99cab.multisolo.network.Protocol;
import io.github.koka99cab.multisolo.network.RelayClient;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.*;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelExtractionContext;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.DefaultPlayerSkin;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.ItemTags;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.SwingAnimation;
import net.minecraft.world.phys.AABB;

public final class EchoController {
    private final ExecutorService worker = Executors.newSingleThreadExecutor(task -> {
        var thread = new Thread(task, "multisolo-echoes"); thread.setDaemon(true); return thread;
    });
    private volatile RelayClient transport;
    private Future<?> pending;
    private long generation, nextNetwork, nextCapture, ticks, nextAppearance;
    private Object world;
    private boolean busy;
    private String errorKey = "";
    private final List<EchoFrame> recording = new ArrayList<>();
    private List<EchoFrame> outgoing;
    private List<EchoClip> received = List.of();
    private final Map<UUID, Long> seen = new LinkedHashMap<>();
    private record Playback(EchoClip clip, long started) {}
    private final List<Playback> playing = new ArrayList<>();
    private EchoClip lastPublished;
    public String errorKey() { return errorKey; }
    int activeCount() { return playing.size(); }
    int recordedFrames() { return recording.size(); }
    EchoClip lastPublished() { return lastPublished; }
    public void reset() {
        generation++;
        if (pending != null) pending.cancel(true);
        var active = transport; if (active != null) active.close();
        transport = null; busy = false; pending = null; nextNetwork = nextCapture = nextAppearance = 0;
        recording.clear(); outgoing = null; received = List.of(); playing.clear(); seen.clear(); lastPublished = null; errorKey = "";
    }
    public void tick(Minecraft client) {
        if (world != client.level) { reset(); world = client.level; }
        var social = MultisoloClient.SOCIAL;
        if (!social.echoesEnabled() || client.player == null || client.level == null || !ExperienceWorlds.canShare(client.getSingleplayerServer())) {
            if (busy || !recording.isEmpty() || !playing.isEmpty() || !received.isEmpty()) reset();
            return;
        }
        if (client.gui.isPausing()) { recording.clear(); return; }
        ticks++;
        long now = System.nanoTime();
        if (!client.player.isAlive() || client.player.isPassenger() || client.player.isSpectator()) recording.clear();
        else if (now >= nextCapture && ticks % 4 == 0) {
            var p = client.player;
            var item = p.getMainHandItem();
            EchoFrame.Tool tool = item.is(ItemTags.PICKAXES) ? EchoFrame.Tool.PICKAXE : item.is(ItemTags.AXES) ? EchoFrame.Tool.AXE
                    : item.is(ItemTags.SHOVELS) ? EchoFrame.Tool.SHOVEL : item.is(ItemTags.HOES) ? EchoFrame.Tool.HOE
                    : item.is(ItemTags.SWORDS) ? EchoFrame.Tool.SWORD : EchoFrame.Tool.HAND;
            var frame = new EchoFrame(round(p.getX()), round(p.getY()), round(p.getZ()), Mth.wrapDegrees(p.getYRot()),
                    Math.clamp(p.getXRot(), -90, 90), p.isCrouching(), p.isSprinting(), p.getSwingAnimation(1), tool);
            if (!recording.isEmpty() && (recording.getLast().distanceSquared(frame) > 64 || recording.getFirst().distanceSquared(frame) > 4096)) recording.clear();
            recording.add(frame);
            if (recording.size() == EchoClip.MAX_FRAMES) {
                if (recording.stream().anyMatch(sample -> sample.distanceSquared(recording.getFirst()) > .04 || sample.swing() > 0)) outgoing = List.copyOf(recording);
                recording.clear(); nextCapture = now + TimeUnit.SECONDS.toNanos(60);
            }
        }
        if (!busy && (outgoing != null || now >= nextNetwork)) exchange(client);
        playing.removeIf(play -> (ticks - play.started()) * 50 >= play.clip().durationMillis()
                || !play.clip().activeAt(System.currentTimeMillis()) || !clear(client, play.clip().at((ticks - play.started()) * 50)));
        seen.entrySet().removeIf(entry -> System.currentTimeMillis() - entry.getValue() >= EchoClip.LIFETIME_MILLIS);
        if (ticks >= nextAppearance && playing.size() < 3) {
            for (var clip : received) {
                if (!clip.activeAt(System.currentTimeMillis()) || seen.containsKey(clip.id())) continue;
                double distance = distanceSquared(client, clip.frames().getFirst());
                if (distance < 64 || distance > 64 * 64 || clip.frames().stream().anyMatch(frame -> !clear(client, frame))) continue;
                playing.add(new Playback(clip, ticks)); seen.put(clip.id(), System.currentTimeMillis());
                if (seen.size() > 512) seen.remove(seen.keySet().iterator().next());
                nextAppearance = ticks + 240; break;
            }
        }
    }
    private static double round(double value) { return Math.rint(value * 1000) / 1000; }
    private static double distanceSquared(Minecraft client, EchoFrame frame) {
        return client.player.distanceToSqr(frame.x(), frame.y(), frame.z());
    }
    private static boolean clear(Minecraft client, EchoFrame frame) {
        var pos = BlockPos.containing(frame.x(), frame.y(), frame.z());
        if (!client.level.hasChunkAt(pos) || distanceSquared(client, frame) > 128 * 128) return false;
        // No terrain loading; skip paths intersecting the recipient's changed constructions.
        var box = new AABB(frame.x() - .29, frame.y() + .02, frame.z() - .29, frame.x() + .29,
                frame.y() + (frame.crouching() ? 1.48 : 1.78), frame.z() + .29);
        return !client.level.getBlockCollisions(null, box).iterator().hasNext();
    }
    private void exchange(Minecraft client) {
        var social = MultisoloClient.SOCIAL;
        var scope = new CompatibilityKey(ExperienceCatalog.origin(), ExperienceWorlds.gameVersion(), Protocol.VERSION,
                client.level.dimension().identifier().toString());
        var pos = client.player.blockPosition(); var center = new MessagePosition(pos.getX(), pos.getY(), pos.getZ());
        var frames = outgoing; outgoing = null;
        long requestGeneration = generation; String endpoint = social.endpoint();
        nextNetwork = System.nanoTime() + TimeUnit.SECONDS.toNanos(10); busy = true;
        pending = worker.submit(() -> {
            try (var relay = new RelayClient(URI.create(endpoint), social.networkIdentity())) {
                transport = relay;
                if (Thread.currentThread().isInterrupted()) return;
                var published = frames == null ? null : relay.publishEcho(UUID.randomUUID(), scope, frames);
                var result = relay.nearbyEchoes(scope, center);
                client.execute(() -> {
                    if (generation != requestGeneration) return;
                    if (published != null) lastPublished = published;
                    received = result; busy = false; errorKey = "";
                });
            } catch (Exception failure) {
                client.execute(() -> { if (generation == requestGeneration) { busy = false; errorKey = "multisolo.echo.error"; } });
            } finally { transport = null; }
        });
    }
    public void extract(LevelExtractionContext context) {
        var client = Minecraft.getInstance();
        if (client.level != world || !MultisoloClient.SOCIAL.echoesEnabled() || !ExperienceWorlds.canShare(client.getSingleplayerServer())) return;
        for (var play : playing) {
            double elapsed = (ticks - play.started() + context.levelState().worldPartialTicks) * 50;
            if (elapsed >= play.clip().durationMillis()) continue;
            var frame = play.clip().at(elapsed);
            var state = new EchoAvatarRenderState();
            state.entityType = EntityTypes.PLAYER; state.x = frame.x(); state.y = frame.y(); state.z = frame.z();
            state.bodyRot = frame.yaw(); state.xRot = frame.pitch(); state.ageInTicks = (float) (elapsed / 50);
            state.isCrouching = frame.crouching(); state.pose = frame.crouching() ? Pose.CROUCHING : Pose.STANDING;
            state.boundingBoxWidth = .6F; state.boundingBoxHeight = frame.crouching() ? 1.5F : 1.8F; state.eyeHeight = 1.62F;
            state.distanceToCameraSq = context.camera().position().distanceToSqr(frame.x(), frame.y(), frame.z());
            state.skin = DefaultPlayerSkin.get(UUID.nameUUIDFromBytes(play.clip().authorId().getBytes(StandardCharsets.UTF_8)));
            float fade = (float) Math.min(1, Math.min(elapsed / 600, (play.clip().durationMillis() - elapsed) / 800));
            state.tint = (Math.clamp((int) (110 * fade), 0, 110) << 24) | 0xE4F1FF;
            var previous = play.clip().at(Math.max(0, elapsed - 200));
            state.walkAnimationSpeed = (float) Math.min(1, Math.sqrt(previous.distanceSquared(frame)) * 1.5);
            state.walkAnimationPos = state.ageInTicks * (frame.sprinting() ? .8F : .5F);
            state.currentSwing = new LivingEntity.SwingDescription(InteractionHand.MAIN_HAND, SwingAnimation.DEFAULT, 6);
            state.swingAnimation = frame.swing();
            var tool = switch (frame.tool()) {
                case HAND -> ItemStack.EMPTY;
                case PICKAXE -> new ItemStack(Items.IRON_PICKAXE);
                case AXE -> new ItemStack(Items.IRON_AXE);
                case SHOVEL -> new ItemStack(Items.IRON_SHOVEL);
                case HOE -> new ItemStack(Items.IRON_HOE);
                case SWORD -> new ItemStack(Items.IRON_SWORD);
            };
            state.rightHandItemStack = tool;
            client.getItemModelResolver().updateForTopItem(state.rightHandItemState, tool, ItemDisplayContext.THIRD_PERSON_RIGHT_HAND, client.level, null, 0);
            context.levelState().entityRenderStates.add(state);
        }
    }
}
