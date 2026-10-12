package io.github.koka99cab.multisolo.fabric.client;

import io.github.koka99cab.multisolo.core.experience.CompatibilityKey;
import io.github.koka99cab.multisolo.core.message.MessagePosition;
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
    @Override public void runTest(ClientGameTestContext context) {
        try {
            var invalidSettings = Files.createTempFile("multisolo-settings-test", ".json");
            Files.writeString(invalidSettings, "null");
            try {
                SocialSettings.load(invalidSettings);
                throw new AssertionError("Null preferences must fail closed");
            } catch (java.io.IOException expected) { check(Files.readString(invalidSettings).equals("null"), "Invalid preferences were rewritten"); }
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

            try (RelayServer relay = new RelayServer(new InetSocketAddress("127.0.0.1", 0), Files.createTempDirectory("multisolo-relay-test"))) {
                relay.start();
                String endpoint = "http://127.0.0.1:" + relay.port();
                AtomicBoolean completed = new AtomicBoolean();
                context.runOnClient(client -> {
                    MultisoloClient.SOCIAL.configure(true, endpoint);
                    MultisoloClient.SOCIAL.publish(client, "Connected Solitude — seed 0", ok -> {
                        check(ok, "In-game message publication failed"); completed.set(true);
                    });
                });
                context.waitFor(client -> completed.get(), 600);
                var located = context.computeOnClient(client -> MultisoloClient.SOCIAL.messages().getFirst());
                try (RelayClient other = new RelayClient(URI.create(endpoint), NetworkIdentity.generate())) {
                    check(other.nearby(new CompatibilityKey(ExperienceCatalog.origin(), ExperienceWorlds.gameVersion(), 1,
                            "minecraft:overworld"), new MessagePosition(located.position().x(), located.position().y(), located.position().z())).size() == 1,
                            "A second identity cannot read the in-game message");
                }
                context.setScreen(() -> new MessagesScreen(new MultisoloScreen(new TitleScreen())));
                context.takeScreenshot("connected-solitude-messages");
                context.runOnClient(client -> MultisoloClient.SOCIAL.configure(false, endpoint));
                check(context.computeOnClient(client -> MultisoloClient.SOCIAL.messages().isEmpty()), "Disabled social retained visible remote messages");
            }
            String folder = context.computeOnClient(client -> client.getSingleplayerServer()
                    .getWorldPath(LevelResource.ROOT).normalize().getFileName().toString());
            disconnect(context);
            context.runOnClient(client -> client.createWorldOpenFlows().openWorld(folder, () -> {}));
            context.waitFor(client -> client.player != null && client.gui.screen() == null, 2400);
            check(context.computeOnClient(client -> ExperienceWorlds.connectedSolitude(client.getSingleplayerServer())), "Saved experience did not reload");
            disconnect(context);
            try (var ordinary = context.worldBuilder().create()) {
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
