package io.github.koka99cab.multisolo.fabric.client;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.fabricmc.fabric.api.client.screen.v1.Screens;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.PauseScreen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.network.chat.Component;

public final class MultisoloClient implements ClientModInitializer {
    public static final SocialController SOCIAL = new SocialController();
    public static final RemoteSignController REMOTE_SIGNS = new RemoteSignController();
    public static final EchoController ECHOES = new EchoController();
    @Override public void onInitializeClient() {
        net.fabricmc.fabric.api.client.rendering.v1.level.LevelExtractionEvents.END_EXTRACTION.register(ECHOES::extract);
        ScreenEvents.AFTER_INIT.register((client, screen, width, height) -> {
            if (screen instanceof TitleScreen || screen instanceof PauseScreen) {
                Screens.getWidgets(screen).add(Button.builder(Component.literal("Multisolo"),
                        button -> client.gui.setScreen(new MultisoloScreen(screen)))
                        .bounds(8, 8, 90, 20).build());
            }
        });
        ClientTickEvents.END_CLIENT_TICK.register(client -> { SOCIAL.checkWorld(client); REMOTE_SIGNS.tick(client); ECHOES.tick(client); });
    }
}
