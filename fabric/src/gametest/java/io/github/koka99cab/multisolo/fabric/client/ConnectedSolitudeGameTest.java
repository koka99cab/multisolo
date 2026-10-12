package io.github.koka99cab.multisolo.fabric.client;

import io.github.koka99cab.multisolo.core.experience.CompatibilityKey;
import io.github.koka99cab.multisolo.core.message.*;
import io.github.koka99cab.multisolo.fabric.PhantomSignData;
import io.github.koka99cab.multisolo.network.Protocol;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.SignBlockEntity;
import net.minecraft.world.level.block.entity.SignTextSlot;
import net.minecraft.client.gui.screens.inventory.SignEditScreen;
import net.minecraft.client.input.CharacterEvent;
import io.github.koka99cab.multisolo.fabric.ExperienceCatalog;
import io.github.koka99cab.multisolo.fabric.ExperienceWorlds;
import io.github.koka99cab.multisolo.network.NetworkIdentity;
import io.github.koka99cab.multisolo.network.RelayClient;
import io.github.koka99cab.multisolo.relay.RelayServer;
import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.file.Files;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.world.Difficulty;
import net.minecraft.world.level.storage.LevelResource;

public final class ConnectedSolitudeGameTest implements FabricClientGameTest {
    private static void check(boolean value, String message) { if (!value) throw new AssertionError(message); }
    private static void disconnect(ClientGameTestContext context) {
        var server = context.computeOnClient(client -> client.getSingleplayerServer());
        if (server == null) return;
        context.runOnClient(client -> client.disconnectWithSavingScreen());
        context.waitFor(client -> server.isShutdown() && client.player == null, 1200);
        context.waitTicks(2);
        context.setScreen(TitleScreen::new);
    }
    private static void placeSign(ClientGameTestContext context, BlockPos pos) {
        var playerId = context.computeOnClient(client -> client.player.getUUID());
        context.runOnClient(client -> client.getSingleplayerServer().execute(() -> {
            var level = client.getSingleplayerServer().overworld();
            level.setBlock(pos.below(), Blocks.STONE.defaultBlockState(), 3);
            level.setBlock(pos, Blocks.OAK_SIGN.defaultBlockState(), 3);
            ((SignBlockEntity) level.getBlockEntity(pos)).setAllowedPlayerEditor(playerId);
        }));
        context.waitFor(client -> client.level.getBlockEntity(pos) instanceof SignBlockEntity, 200);
    }
    private static void teleport(ClientGameTestContext context, BlockPos pos) {
        context.runOnClient(client -> client.getSingleplayerServer().execute(() ->
                client.getSingleplayerServer().getPlayerList().getPlayers().getFirst().teleportTo(pos.getX() + .5, pos.getY(), pos.getZ() + .5)));
        context.waitFor(client -> client.player.blockPosition().equals(pos), 200);
    }
    private static void echoes(ClientGameTestContext context, BlockPos origin) throws Exception {
        var otherIdentity = NetworkIdentity.generate();
        try (var relay = new RelayServer(new InetSocketAddress("127.0.0.1", 0), Files.createTempDirectory("multisolo-echo-test"))) {
            relay.start(); String endpoint = "http://127.0.0.1:" + relay.port();
            teleport(context, origin.offset(-2, 0, 0));
            var target = origin.offset(-2, 0, 12);
            AtomicBoolean prepared = new AtomicBoolean();
            context.runOnClient(client -> client.getSingleplayerServer().execute(() -> {
                var level = client.getSingleplayerServer().overworld();
                for (int x = -2; x <= 4; x++) for (int z = -12; z <= 1; z++) {
                    level.setBlock(target.offset(x, -1, z), Blocks.STONE.defaultBlockState(), 3);
                    for (int y = 0; y < 4; y++) {
                        var pos = target.offset(x, y, z);
                        if (!pos.equals(origin)) level.setBlock(pos, Blocks.AIR.defaultBlockState(), 3);
                    }
                }
                client.getSingleplayerServer().getPlayerList().getPlayers().getFirst().setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND,
                        new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.IRON_PICKAXE));
                prepared.set(true);
            }));
            context.waitFor(client -> prepared.get(), 200);
            var scope = new CompatibilityKey(ExperienceCatalog.origin(), ExperienceWorlds.gameVersion(), Protocol.VERSION, "minecraft:overworld");
            var frames = new java.util.ArrayList<io.github.koka99cab.multisolo.core.echo.EchoFrame>();
            for (int i = 0; i < 41; i++) frames.add(new io.github.koka99cab.multisolo.core.echo.EchoFrame(
                    target.getX() + .5 + i * .05, target.getY(), target.getZ() + .5, 180, 25, i > 20, false,
                    (i % 3) / 3F, io.github.koka99cab.multisolo.core.echo.EchoFrame.Tool.PICKAXE));
            try (var other = new RelayClient(URI.create(endpoint), otherIdentity)) {
                other.publishEcho(UUID.randomUUID(), scope, frames);
                check(!context.computeOnClient(client -> MultisoloClient.SOCIAL.echoesEnabled()), "Echo sharing was enabled without separate consent");
                context.runOnClient(client -> MultisoloClient.SOCIAL.configure(false, endpoint));
                AtomicReference<EchoAvatarRenderState> drawn = new AtomicReference<>();
                net.fabricmc.fabric.api.client.rendering.v1.level.LevelExtractionEvents.END_EXTRACTION.register(render -> {
                    for (var state : render.levelState().entityRenderStates) if (state instanceof EchoAvatarRenderState echo) drawn.set(echo);
                });
                context.setScreen(() -> new MultisoloScreen(new TitleScreen()));
                context.clickScreenButton("multisolo.echo.enable");
                context.runOnClient(client -> { client.gui.setScreen(null); client.player.setYRot(0); client.player.setXRot(0); });
                context.waitFor(client -> MultisoloClient.ECHOES.activeCount() == 1 && drawn.get() != null, 400);
                check(!context.computeOnClient(client -> MultisoloClient.SOCIAL.enabled()), "Echo consent also enabled sign sharing");
                double initialX = drawn.get().x;
                context.waitFor(client -> MultisoloClient.ECHOES.recordedFrames() > 0, 100);
                context.runOnClient(client -> client.player.swing(net.minecraft.world.InteractionHand.MAIN_HAND,
                        net.minecraft.world.item.component.SwingAnimation.DEFAULT, false));
                context.waitTicks(25);
                check(drawn.get().x > initialX && drawn.get().rightHandItemStack.is(net.minecraft.world.item.Items.IRON_PICKAXE), "Echo motion/tool did not reach the native renderer");
                check((drawn.get().tint >>> 24) > 0 && (drawn.get().tint >>> 24) < 255 && drawn.get().nameTag == null, "Echo is opaque or has a name tag");
                context.takeScreenshot("connected-solitude-echo");
                check(context.computeOnClient(client -> client.level.players().size() == 1 && client.getSingleplayerServer().getPlayerList().getPlayers().size() == 1), "A cosmetic echo became a real world player");
                context.waitFor(client -> drawn.get().isCrouching, 200);
                context.waitFor(client -> MultisoloClient.ECHOES.lastPublished() != null, 500);
                var captured = context.computeOnClient(client -> MultisoloClient.ECHOES.lastPublished());
                check(captured.frames().size() == 41 && captured.frames().stream().anyMatch(frame -> frame.swing() > 0), "Local gestures were not captured and uploaded");
                check(other.nearbyEchoes(scope, captured.anchor()).stream().anyMatch(clip -> clip.id().equals(captured.id())), "Second installation cannot retrieve captured motion");
                context.setScreen(() -> new MultisoloScreen(new TitleScreen()));
                context.clickScreenButton("multisolo.echo.disable");
                check(context.computeOnClient(client -> !MultisoloClient.SOCIAL.echoesEnabled() && MultisoloClient.ECHOES.activeCount() == 0
                        && MultisoloClient.ECHOES.recordedFrames() == 0), "Revoking consent did not discard capture and playback");
                context.runOnClient(client -> client.gui.setScreen(null));
            }
        }
    }
    @Override public void runTest(ClientGameTestContext context) {
        try {
            var invalidSettings = Files.createTempFile("multisolo-settings-test", ".json");
            Files.writeString(invalidSettings, "null");
            try {
                SocialSettings.load(invalidSettings);
                throw new AssertionError("Null preferences must fail closed");
            } catch (java.io.IOException expected) { check(Files.readString(invalidSettings).equals("null"), "Invalid preferences were rewritten"); }
            Files.writeString(invalidSettings, "{\"schemaVersion\":2,\"messagesEnabled\":true,\"relay\":\"http://127.0.0.1:8787\",\"signColor\":\"#12ABEF\"}");
            var legacySettings = SocialSettings.load(invalidSettings);
            check(legacySettings.messagesEnabled() && !legacySettings.echoesEnabled() && legacySettings.relay().equals("http://127.0.0.1:8787"), "Legacy settings changed consent or implicitly enabled echoes");
            legacySettings.save(invalidSettings);
            check(!Files.readString(invalidSettings).contains("signColor"), "Obsolete color preference was saved again");
            Files.delete(invalidSettings);
            context.setScreen(TitleScreen::new);
            context.clickScreenButton("Multisolo");
            context.waitForScreen(MultisoloScreen.class);
            context.takeScreenshot("connected-solitude-menu");
            context.clickScreenButton("multisolo.create");
            context.waitFor(client -> client.player != null && client.level != null && client.gui.screen() == null, 2400);
            AtomicBoolean checked = new AtomicBoolean();
            AtomicReference<Throwable> error = new AtomicReference<>();
            context.runOnClient(client -> client.getSingleplayerServer().execute(() -> {
                try {
                    var server = client.getSingleplayerServer();
                    check(ExperienceWorlds.connectedSolitude(server), "Experience marker was not loaded");
                    check(server.getWorldGenSettings().options().seed() == 0L, "Seed differs from zero");
                    server.setDifficulty(Difficulty.HARD, true);
                    server.setDifficultyLocked(false);
                    check(server.getWorldData().getDifficulty() == Difficulty.NORMAL, "Difficulty changed");
                    check(server.getWorldData().isDifficultyLocked(), "Difficulty unlocked");
                    check(!server.getWorldData().isAllowCommands(), "Cheats enabled");
                } catch (Throwable failure) { error.set(failure); }
                finally { checked.set(true); }
            }));
            context.waitFor(client -> checked.get());
            if (error.get() != null) throw new AssertionError(error.get());

            BlockPos signPosition = context.computeOnClient(client -> client.player.blockPosition().offset(2, 0, 0));
            var relayData = Files.createTempDirectory("multisolo-relay-test");
            // Backdate posts to test client expiry soon, including a relay with a stale clock.
            var relayClock = java.time.Clock.offset(java.time.Clock.systemUTC(), java.time.Duration.ofMillis(-LocatedMessage.LIFETIME_MILLIS + 25_000));
            try (RelayServer relay = new RelayServer(new InetSocketAddress("127.0.0.1", 0), relayData, relayClock)) {
                relay.start();
                String endpoint = "http://127.0.0.1:" + relay.port();
                placeSign(context, signPosition);
                context.runOnClient(client -> {
                    MultisoloClient.SOCIAL.configure(true, endpoint);
                    client.gui.setScreen(new SignEditScreen((SignBlockEntity) client.level.getBlockEntity(signPosition), SignTextSlot.FRONT, false));
                });
                context.waitFor(client -> !MultisoloClient.SOCIAL.busy(), 600);
                context.clickScreenButton("multisolo.sign.off");
                context.runOnClient(client -> {
                    var screen = (SignEditScreen) client.gui.screen();
                    var editor = (PhantomSignEditor) screen;
                    check(editor.multisolo$session().active(), "Phantom mode did not activate in the vanilla sign screen");
                    String[] before = editor.multisolo$lines();
                    screen.charTyped(new CharacterEvent('X'));
                    check(java.util.Arrays.equals(before, editor.multisolo$lines()), "Free text reached a phantom sign");
                    for (var line : MessagePhrase.Line.values()) check(client.font.width(PhraseText.line(line)) <= 60, "Catalog line is too wide for a hanging sign: " + line);
                });
                context.takeScreenshot("connected-solitude-vanilla-sign-editor");
                context.waitTicks(5);
                context.waitFor(client -> !MultisoloClient.SOCIAL.busy(), 600);
                context.clickScreenButton("multisolo.sign.publish");
                context.waitTicks(5);
                context.waitFor(client -> (client.gui.screen() == null && !MultisoloClient.SOCIAL.messages().isEmpty()) || !MultisoloClient.SOCIAL.errorKey().isEmpty(), 600);
                check(context.computeOnClient(client -> MultisoloClient.SOCIAL.errorKey().isEmpty()), "Publication error: " + context.computeOnClient(client -> MultisoloClient.SOCIAL.errorKey()));
                var located = context.computeOnClient(client -> MultisoloClient.SOCIAL.messages().getFirst());
                check(located.position().equals(new MessagePosition(signPosition.getX(), signPosition.getY(), signPosition.getZ())), "Publication used player position instead of the placed sign");
                check(located.sign().phrase().lines().size() == 4, "Four shared lines were lost");
                check(located.sign().color().rgb() == 0, "New publication did not neutralize the legacy color field");
                context.waitFor(client -> ((PhantomSignData) client.level.getBlockEntity(signPosition)).multisolo$message(SignTextSlot.FRONT) != null, 600);
                try (RelayClient other = new RelayClient(URI.create(endpoint), NetworkIdentity.generate())) {
                    var scope = new CompatibilityKey(ExperienceCatalog.origin(), ExperienceWorlds.gameVersion(), Protocol.VERSION, "minecraft:overworld");
                    check(other.nearby(scope, located.position()).size() == 1, "A second identity cannot read the placed sign");
                    other.vote(located.id(), 1);
                    BlockPos receiverPosition = signPosition.offset(-2, 0, 12);
                    BlockPos occupied = receiverPosition.offset(2, 0, 0), unsupported = receiverPosition.offset(4, 0, 0);
                    AtomicBoolean prepared = new AtomicBoolean();
                    context.runOnClient(client -> client.getSingleplayerServer().execute(() -> {
                        var level = client.getSingleplayerServer().overworld();
                        level.setBlock(receiverPosition.below(), Blocks.STONE.defaultBlockState(), 3);
                        level.setBlock(receiverPosition, Blocks.AIR.defaultBlockState(), 3);
                        level.setBlock(occupied, Blocks.STONE.defaultBlockState(), 3);
                        level.setBlock(unsupported, Blocks.AIR.defaultBlockState(), 3);
                        level.setBlock(unsupported.below(), Blocks.AIR.defaultBlockState(), 3);
                        prepared.set(true);
                    }));
                    context.waitFor(client -> prepared.get() && client.level.getBlockState(receiverPosition).isAir(), 200);
                    var placement = io.github.koka99cab.multisolo.fabric.SignPlacements.capture(Blocks.OAK_SIGN.defaultBlockState());
                    var remote = other.publish(UUID.randomUUID(), scope,
                            new MessagePosition(receiverPosition.getX(), receiverPosition.getY(), receiverPosition.getZ()),
                            new PhantomSign(MessagePhrase.example(), new SignColor(0xFE1288), PhantomSign.Face.FRONT, placement));
                    for (var pos : new BlockPos[]{occupied, unsupported}) other.publish(UUID.randomUUID(), scope,
                            new MessagePosition(pos.getX(), pos.getY(), pos.getZ()), new PhantomSign(MessagePhrase.example(), PhantomSign.Face.FRONT, placement));
                    context.runOnClient(client -> { client.player.setYRot(0); client.player.setXRot(0); });
                    context.waitFor(client -> !MultisoloClient.SOCIAL.busy(), 600);
                    AtomicBoolean read = new AtomicBoolean();
                    context.runOnClient(client -> MultisoloClient.SOCIAL.refresh(client, read::set));
                    context.waitFor(client -> read.get(), 600);
                    context.waitTicks(35);
                    check(context.computeOnClient(client -> client.gameRenderer.mainCamera().getCullFrustum().isVisible(new net.minecraft.world.phys.AABB(receiverPosition).inflate(2))), "Test target was not in the camera frustum");
                    check(context.computeOnClient(client -> client.level.getBlockState(receiverPosition).isAir()), "A sign appeared while its location was visible");
                    context.runOnClient(client -> client.player.setYRot(180));
                    context.waitFor(client -> client.level.getBlockEntity(receiverPosition) instanceof SignBlockEntity sign && ((PhantomSignData) sign).multisolo$remote() != null, 300);
                    check(context.computeOnClient(client -> client.level.getBlockState(occupied).is(Blocks.STONE) && client.level.getBlockState(unsupported).isAir()), "Remote placement replaced a block or ignored support");
                    AtomicBoolean noLoot = new AtomicBoolean();
                    context.runOnClient(client -> client.getSingleplayerServer().execute(() -> {
                        var level = client.getSingleplayerServer().overworld(); var block = level.getBlockEntity(receiverPosition);
                        var params = new net.minecraft.world.level.storage.loot.LootParams.Builder(level)
                                .withParameter(net.minecraft.world.level.storage.loot.parameters.LootContextParams.ORIGIN, net.minecraft.world.phys.Vec3.atCenterOf(receiverPosition))
                                .withParameter(net.minecraft.world.level.storage.loot.parameters.LootContextParams.TOOL, net.minecraft.world.item.ItemStack.EMPTY)
                                .withOptionalParameter(net.minecraft.world.level.storage.loot.parameters.LootContextParams.BLOCK_ENTITY, block);
                        noLoot.set(level.getBlockState(receiverPosition).getDrops(params).isEmpty());
                    }));
                    context.waitFor(client -> noLoot.get(), 100);
                    teleport(context, receiverPosition.offset(0, 0, -3));
                    context.runOnClient(client -> { client.player.setYRot(0); client.player.setXRot(0); });
                    context.runOnClient(client -> {
                        var block = (SignBlockEntity) client.level.getBlockEntity(receiverPosition);
                        var original = block.getText(SignTextSlot.FRONT);
                        check(block.isWaxed() && original.hasMessage(false), "Temporary copy is not a real read-only vanilla sign");
                        var overlay = PhantomSignMenus.display(block, SignTextSlot.FRONT);
                        check(overlay.getMessages(false).getFirst().getString().equals(PhraseText.line(MessagePhrase.Line.BEWARE).getString()), "Preset text was not rendered on the real vanilla sign");
                        check(overlay.getMessages(false).stream().allMatch(line -> line.getStyle().getColor() == null), "Legacy author color overrode vanilla sign text");
                        check(overlay.getColor() == net.minecraft.world.item.DyeColor.BLACK && !overlay.hasGlowingText(), "Default vanilla sign color changed");
                        check(block.getText(SignTextSlot.FRONT).equals(original), "Reading remote text mutated the receiving block");
                        var dyed = original.withColor(net.minecraft.world.item.DyeColor.BLUE).withGlowingText(true);
                        block.setText(dyed, SignTextSlot.FRONT);
                        var dyedOverlay = PhantomSignMenus.display(block, SignTextSlot.FRONT);
                        check(dyedOverlay.getColor() == dyed.getColor() && dyedOverlay.hasGlowingText(), "Vanilla dye or glow was overridden");
                        check(block.getText(SignTextSlot.FRONT).equals(dyed), "Reading dyed text mutated the receiving block");
                        block.setText(original, SignTextSlot.FRONT);
                        PhantomSignMenus.openViewer(client, block, SignTextSlot.FRONT, remote);
                    });
                    context.takeScreenshot("connected-solitude-sign-votes");
                    context.clickScreenButton(context.computeOnClient(client -> net.minecraft.network.chat.Component.translatable("multisolo.vote.up", 0).getString()));
                    context.waitFor(client -> MultisoloClient.SOCIAL.view(remote).ownVote() == 1, 600);
                    context.runOnClient(client -> {
                        ((PhantomSignEditor) client.gui.screen()).multisolo$lines(new String[]{"HACK", "", "", ""});
                        client.gui.screen().onClose();
                    });
                    context.waitTicks(5);
                    AtomicBoolean unchanged = new AtomicBoolean();
                    context.runOnClient(client -> client.getSingleplayerServer().execute(() -> unchanged.set(
                            ((SignBlockEntity) client.getSingleplayerServer().overworld().getBlockEntity(receiverPosition)).getText(SignTextSlot.FRONT).getMessages(false).getFirst().getString().equals(PhraseText.line(MessagePhrase.Line.BEWARE).getString()))));
                    context.waitFor(client -> unchanged.get(), 100);
                    context.waitFor(client -> !remote.activeAt(System.currentTimeMillis()), 700);
                    context.waitTicks(10);
                    check(context.computeOnClient(client -> MultisoloClient.SOCIAL.view(remote).activeAt(System.currentTimeMillis()) && client.level.getBlockEntity(receiverPosition) instanceof SignBlockEntity), "Upvote did not extend presence beyond seven days");
                    context.runOnClient(client -> PhantomSignMenus.openViewer(client, (SignBlockEntity) client.level.getBlockEntity(receiverPosition), SignTextSlot.FRONT, remote));
                    context.clickScreenButton(context.computeOnClient(client -> net.minecraft.network.chat.Component.translatable("multisolo.vote.up", 1).getString() + " +"));
                    context.waitFor(client -> !MultisoloClient.SOCIAL.view(remote).activeAt(System.currentTimeMillis()) && client.gui.screen() == null, 600);
                    check(context.computeOnClient(client -> client.level.getBlockEntity(receiverPosition) instanceof SignBlockEntity), "Expired sign disappeared in view");
                    teleport(context, receiverPosition.offset(0, 0, -12));
                    context.runOnClient(client -> client.player.setYRot(180));
                    context.waitFor(client -> client.level.getBlockState(receiverPosition).isAir(), 300);
                    check(Files.exists(relayData.resolve(remote.id() + ".json")), "Expiration deleted the relay archive");
                }
                context.runOnClient(client -> MultisoloClient.SOCIAL.configure(false, endpoint));
                check(context.computeOnClient(client -> MultisoloClient.SOCIAL.messages().isEmpty()), "Disabled social retained visible remote messages");
            }
            echoes(context, signPosition);
            // Persist an expired marked copy and verify that chunk loading cleans only that copy.
            BlockPos cleanupPosition = signPosition.offset(-2, 0, 14);
            context.setScreen(() -> new MultisoloScreen(new TitleScreen()));
            AtomicBoolean expiredSaved = new AtomicBoolean();
            AtomicReference<Throwable> cleanupError = new AtomicReference<>();
            context.runOnClient(client -> client.getSingleplayerServer().execute(() -> {
                try {
                    var level = client.getSingleplayerServer().overworld();
                    level.setBlock(cleanupPosition.below(), Blocks.STONE.defaultBlockState(), 3);
                    level.setBlock(cleanupPosition, Blocks.AIR.defaultBlockState(), 3);
                    var scope = new CompatibilityKey(ExperienceCatalog.origin(), ExperienceWorlds.gameVersion(), Protocol.VERSION, "minecraft:overworld");
                    var message = new LocatedMessage(UUID.randomUUID(), "a".repeat(64), scope,
                            new MessagePosition(cleanupPosition.getX(), cleanupPosition.getY(), cleanupPosition.getZ()),
                            new PhantomSign(MessagePhrase.example(), PhantomSign.Face.FRONT, io.github.koka99cab.multisolo.fabric.SignPlacements.capture(Blocks.OAK_SIGN.defaultBlockState())), System.currentTimeMillis());
                    check(io.github.koka99cab.multisolo.fabric.RemoteSigns.place(level, new Protocol.MessageView(message, 0, 0, 0), "b".repeat(64)), "Could not prepare persisted copy");
                    var expired = new LocatedMessage(message.id(), message.authorId(), message.scope(), message.position(), message.sign(), System.currentTimeMillis() - LocatedMessage.LIFETIME_MILLIS);
                    ((PhantomSignData) level.getBlockEntity(cleanupPosition)).multisolo$remote(new io.github.koka99cab.multisolo.fabric.RemoteSignState(expired, "minecraft:air"));
                } catch (Throwable failure) { cleanupError.set(failure); }
                finally { expiredSaved.set(true); }
            }));
            context.waitFor(client -> expiredSaved.get(), 200);
            if (cleanupError.get() != null) throw new AssertionError(cleanupError.get());
            String folder = context.computeOnClient(client -> client.getSingleplayerServer()
                    .getWorldPath(LevelResource.ROOT).normalize().getFileName().toString());
            disconnect(context);
            context.runOnClient(client -> client.createWorldOpenFlows().openWorld(folder, () -> {}));
            context.waitFor(client -> client.player != null && client.gui.screen() == null, 2400);
            check(context.computeOnClient(client -> ExperienceWorlds.connectedSolitude(client.getSingleplayerServer())), "Saved experience did not reload");
            context.waitFor(client -> client.level.getBlockEntity(signPosition) instanceof SignBlockEntity, 600);
            check(context.computeOnClient(client -> client.level.getBlockState(cleanupPosition).isAir()), "Expired persisted copy survived chunk loading");
            context.runOnClient(client -> {
                var block = (SignBlockEntity) client.level.getBlockEntity(signPosition);
                var saved = ((PhantomSignData) block).multisolo$message(SignTextSlot.FRONT);
                check(saved != null && saved.sign().phrase().equals(MessagePhrase.example()), "Phantom metadata was not saved on the actual vanilla sign");
                check(block.getText(SignTextSlot.FRONT).hasMessage(false), "Vanilla sign text was lost after reloading");
            });
            disconnect(context);
            try (var ordinary = context.worldBuilder().create()) {
                BlockPos pos = context.computeOnClient(client -> client.player.blockPosition().offset(2, 0, 0));
                placeSign(context, pos);
                context.runOnClient(client -> {
                    var screen = new SignEditScreen((SignBlockEntity) client.level.getBlockEntity(pos), SignTextSlot.FRONT, false);
                    client.gui.setScreen(screen);
                    check(((PhantomSignEditor) screen).multisolo$session() == null, "Ordinary sign gained Multisolo controls");
                    screen.charTyped(new CharacterEvent('Z'));
                    check(((PhantomSignEditor) screen).multisolo$lines()[0].equals("Z"), "Ordinary vanilla sign input was intercepted");
                    screen.onClose();
                });
                ordinary.getServer().runOnServer(server -> {
                    check(!ExperienceWorlds.connectedSolitude(server), "Ordinary save was converted");
                    server.setDifficulty(Difficulty.HARD, true);
                    server.setDifficultyLocked(false);
                    check(server.getWorldData().getDifficulty() == Difficulty.HARD, "Ordinary difficulty was intercepted");
                    check(!server.getWorldData().isDifficultyLocked(), "Ordinary difficulty was locked");
                });
            }
        } catch (Exception exception) { throw new RuntimeException(exception); }
        finally {
            disconnect(context);
        }
    }
}
