package io.github.koka99cab.multisolo.fabric.client;

import io.github.koka99cab.multisolo.core.message.LocatedMessage;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

final class MessageScreen extends Screen {
    private final Screen parent;
    private final LocatedMessage message;
    private String status = "";
    MessageScreen(Screen parent, LocatedMessage message) { super(Component.translatable("multisolo.message")); this.parent = parent; this.message = message; }
    @Override protected void init() {
        var social = MultisoloClient.SOCIAL;
        if (message.authorId().equals(social.identityId())) {
            addRenderableWidget(Button.builder(Component.translatable("multisolo.delete"), button ->
                    social.delete(minecraft, message.id(), ok -> {
                        if (ok) onClose(); else status = social.errorKey();
                    })).bounds(width / 2 - 150, height - 52, 300, 20).build());
        }
        addRenderableWidget(Button.builder(Component.translatable("gui.back"), button -> onClose())
                .bounds(width / 2 - 150, height - 28, 300, 20).build());
    }
    @Override public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
        super.extractRenderState(graphics, mouseX, mouseY, delta);
        int x = width / 2 - 150;
        graphics.centeredText(font, title, width / 2, 16, 0xFFFFFFFF);
        graphics.text(font, Component.translatable("multisolo.identity", message.authorId().substring(0, 12)), x, 36, 0xFFCCCCCC);
        graphics.text(font, message.position().x() + ", " + message.position().y() + ", " + message.position().z(), x, 50, 0xFFC5D8CC);
        graphics.textWithWordWrap(font, Component.literal(message.text()), x, 70, 300, 0xFFFFFFFF);
        if (!status.isEmpty()) graphics.textWithWordWrap(font, Component.translatable(status), x, height - 76, 300, 0xFFFF7777);
    }
    @Override public void onClose() { minecraft.gui.setScreen(parent); }
}
