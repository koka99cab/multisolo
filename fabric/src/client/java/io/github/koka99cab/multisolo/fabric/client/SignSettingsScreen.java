package io.github.koka99cab.multisolo.fabric.client;

import io.github.koka99cab.multisolo.core.message.SignColor;
import io.github.koka99cab.multisolo.core.message.MessagePhrase;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

final class SignSettingsScreen extends Screen {
    private final Screen parent;
    private EditBox hex;
    private Button save;
    private String error = "";
    SignSettingsScreen(Screen parent) { super(Component.translatable("multisolo.sign.settings")); this.parent = parent; }
    @Override protected void init() {
        int x = width / 2 - 150;
        hex = addRenderableWidget(new EditBox(font, x, 60, 300, 20, Component.translatable("multisolo.sign.color")));
        hex.setMaxLength(7);
        hex.setValue(MultisoloClient.SOCIAL.signColor().hex());
        save = addRenderableWidget(Button.builder(Component.translatable("multisolo.sign.save"), button -> {
            try { MultisoloClient.SOCIAL.saveSignColor(hex.getValue()); onClose(); }
            catch (Exception exception) { error = "multisolo.error.color"; }
        }).bounds(x, height - 52, 300, 20).build());
        hex.setResponder(value -> { save.active = valid(); error = ""; });
        addRenderableWidget(Button.builder(Component.translatable("gui.back"), button -> onClose())
                .bounds(x, height - 28, 300, 20).build());
    }
    private boolean valid() { try { SignColor.parse(hex.getValue()); return true; } catch (IllegalArgumentException exception) { return false; } }
    @Override public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
        super.extractRenderState(graphics, mouseX, mouseY, delta);
        int x = width / 2 - 150;
        graphics.centeredText(font, title, width / 2, 16, 0xFFFFFFFF);
        graphics.text(font, Component.translatable("multisolo.sign.color"), x, 44, 0xFFFFFFFF);
        var preview = PhraseText.line(MessagePhrase.Line.BEWARE).append(" ").append(PhraseText.line(MessagePhrase.Line.LAVA));
        if (valid()) preview.withStyle(style -> style.withColor(SignColor.parse(hex.getValue()).rgb()));
        graphics.textWithWordWrap(font, preview, x, 96, 300, 0xFFFFFFFF);
        graphics.textWithWordWrap(font, Component.translatable("multisolo.sign.local"), x, 124, 300, 0xFFCCCCCC);
        if (!valid() || !error.isEmpty()) graphics.textWithWordWrap(font, Component.translatable("multisolo.error.color"), x, height - 76, 300, 0xFFFF7777);
    }
    @Override public void onClose() { minecraft.gui.setScreen(parent); }
}
