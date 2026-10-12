package io.github.koka99cab.multisolo.fabric.client;

import io.github.koka99cab.multisolo.fabric.ExperienceWorlds;
import java.util.UUID;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.Difficulty;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.LevelSettings;
import net.minecraft.world.level.WorldDataConfiguration;
import net.minecraft.world.level.levelgen.WorldOptions;
import net.minecraft.world.level.levelgen.presets.WorldPresets;

final class MultisoloScreen extends Screen {
    private final Screen parent;
    private EditBox endpoint;
    private String localError = "";
    MultisoloScreen(Screen parent) { super(Component.literal("Multisolo — Connected Solitude")); this.parent = parent; }
    private SocialController social() { return MultisoloClient.SOCIAL; }

    @Override protected void init() {
        int x = width / 2 - 150;
        endpoint = addRenderableWidget(new EditBox(font, x, 65, 300, 20, Component.translatable("multisolo.relay")));
        endpoint.setMaxLength(512);
        endpoint.setValue(social().endpoint());
        addRenderableWidget(Button.builder(Component.translatable(social().enabled() ? "multisolo.disable" : "multisolo.enable"), button -> {
            try {
                social().configure(!social().enabled(), social().enabled() ? social().endpoint() : endpoint.getValue());
                minecraft.gui.setScreen(new MultisoloScreen(parent));
            } catch (Exception exception) {
                localError = "multisolo.error.endpoint";
                button.setMessage(Component.translatable(social().enabled() ? "multisolo.disable" : "multisolo.enable"));
            }
        }).bounds(x, 89, 300, 20).tooltip(net.minecraft.client.gui.components.Tooltip.create(Component.translatable("multisolo.message.consent"))).build());
        addRenderableWidget(Button.builder(Component.translatable(social().echoesEnabled() ? "multisolo.echo.disable" : "multisolo.echo.enable"), button -> {
            try {
                social().configureEchoes(!social().echoesEnabled(), endpoint.getValue());
                minecraft.gui.setScreen(new MultisoloScreen(parent));
            } catch (Exception exception) { localError = "multisolo.error.endpoint"; }
        }).bounds(x, 113, 300, 20).tooltip(net.minecraft.client.gui.components.Tooltip.create(Component.translatable("multisolo.echo.consent"))).build());
        Button create = addRenderableWidget(Button.builder(Component.translatable("multisolo.create"), button -> createWorld())
                .bounds(x, height - 76, 300, 20).build());
        create.active = minecraft.level == null && ExperienceWorlds.cleanProfile();
        Button messages = addRenderableWidget(Button.builder(Component.translatable("multisolo.messages"),
                button -> minecraft.gui.setScreen(new MessagesScreen(this))).bounds(x, height - 52, 300, 20).build());
        messages.active = social().enabled() && ExperienceWorlds.canShare(minecraft.getSingleplayerServer());
        addRenderableWidget(Button.builder(Component.translatable("gui.back"), button -> onClose()).bounds(x, height - 28, 300, 20).build());
    }

    private void createWorld() {
        String id = "Multisolo-" + UUID.randomUUID();
        try {
            ExperienceWorlds.markNewWorld(minecraft.getLevelSource().getBaseDir().resolve(id));
            LevelSettings rules = new LevelSettings("Connected Solitude", GameType.SURVIVAL,
                    new LevelSettings.DifficultySettings(Difficulty.NORMAL, false, true), false, WorldDataConfiguration.DEFAULT);
            minecraft.createWorldOpenFlows().createFreshLevel(id, rules, new WorldOptions(0L, true, false),
                    WorldPresets::createNormalWorldDimensions, this);
        } catch (Exception exception) { localError = "multisolo.error.create"; }
    }

    @Override public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
        super.extractRenderState(graphics, mouseX, mouseY, delta);
        graphics.centeredText(font, title, width / 2, 16, 0xFFFFFFFF);
        graphics.centeredText(font, Component.translatable("multisolo.rules"), width / 2, 32, 0xFFC5D8CC);
        graphics.text(font, Component.translatable("multisolo.relay"), width / 2 - 150, 52, 0xFFFFFFFF);
        String error = localError.isEmpty() ? social().errorKey() : localError;
        if (error.isEmpty()) error = MultisoloClient.ECHOES.errorKey();
        if (!error.isEmpty()) graphics.textWithWordWrap(font, Component.translatable(error), width / 2 - 150, 138, 300, 0xFFFF7777);
        else if (!ExperienceWorlds.cleanProfile()) graphics.textWithWordWrap(font, Component.translatable("multisolo.error.profile"), width / 2 - 150, 138, 300, 0xFFFFBB77);
        else graphics.textWithWordWrap(font, Component.translatable("multisolo.consent"), width / 2 - 150, 138, 300, 0xFFCCCCCC);
    }
    @Override public void onClose() { minecraft.gui.setScreen(parent); }
}
