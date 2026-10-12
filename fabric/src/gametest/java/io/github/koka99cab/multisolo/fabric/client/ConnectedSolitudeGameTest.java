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
            check(legacySettings.messagesEnabled() && legacySettings.relay().equals("http://127.0.0.1:8787"), "Removing color reset existing consent or relay preferences");
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
            try (RelayServer relay = new RelayServer(new InetSocketAddress("127.0.0.1", 0), Files.createTempDirectory("multisolo-relay-test"))) {
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
                    BlockPos receiverPosition = signPosition.offset(1, 0, 0);
                    placeSign(context, receiverPosition);
                    var remote = other.publish(UUID.randomUUID(), scope,
                            new MessagePosition(receiverPosition.getX(), receiverPosition.getY(), receiverPosition.getZ()),
                            new PhantomSign(MessagePhrase.example(), new SignColor(0xFE1288), PhantomSign.Face.FRONT));
                    AtomicBoolean read = new AtomicBoolean();
                    context.runOnClient(client -> MultisoloClient.SOCIAL.refresh(client, read::set));
                    context.waitFor(client -> read.get(), 600);
                    context.runOnClient(client -> {
                        var block = (SignBlockEntity) client.level.getBlockEntity(receiverPosition);
                        var original = block.getText(SignTextSlot.FRONT);
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
                            !((SignBlockEntity) client.getSingleplayerServer().overworld().getBlockEntity(receiverPosition)).getText(SignTextSlot.FRONT).hasMessage(false))));
                    context.waitFor(client -> unchanged.get(), 100);
                }
                context.runOnClient(client -> MultisoloClient.SOCIAL.configure(false, endpoint));
                check(context.computeOnClient(client -> MultisoloClient.SOCIAL.messages().isEmpty()), "Disabled social retained visible remote messages");
            }
            String folder = context.computeOnClient(client -> client.getSingleplayerServer()
                    .getWorldPath(LevelResource.ROOT).normalize().getFileName().toString());
            disconnect(context);
            context.runOnClient(client -> client.createWorldOpenFlows().openWorld(folder, () -> {}));
            context.waitFor(client -> client.player != null && client.gui.screen() == null, 2400);
            check(context.computeOnClient(client -> ExperienceWorlds.connectedSolitude(client.getSingleplayerServer())), "Saved experience did not reload");
            context.waitFor(client -> client.level.getBlockEntity(signPosition) instanceof SignBlockEntity, 600);
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
