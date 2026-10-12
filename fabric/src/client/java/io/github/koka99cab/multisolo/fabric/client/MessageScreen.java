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
        var view = social.view(message);
        boolean canVote = !message.authorId().equals(social.identityId()) && !social.busy();
        addRenderableWidget(Button.builder(Component.translatable("multisolo.vote.up", view.upvotes()), button ->
                vote(view.ownVote() == 1 ? 0 : 1)).bounds(width / 2 - 150, height - 100, 148, 20).build()).active = canVote;
        addRenderableWidget(Button.builder(Component.translatable("multisolo.vote.down", view.downvotes()), button ->
                vote(view.ownVote() == -1 ? 0 : -1)).bounds(width / 2 + 2, height - 100, 148, 20).build()).active = canVote;
        if (message.authorId().equals(social.identityId())) {
            addRenderableWidget(Button.builder(Component.translatable("multisolo.delete"), button ->
                    social.delete(minecraft, message.id(), ok -> {
                        if (ok) onClose(); else status = social.errorKey();
                    })).bounds(width / 2 - 150, height - 52, 300, 20).build());
        }
        addRenderableWidget(Button.builder(Component.translatable("gui.back"), button -> onClose())
                .bounds(width / 2 - 150, height - 28, 300, 20).build());
    }
    private void vote(int value) {
        MultisoloClient.SOCIAL.vote(minecraft, message.id(), value, ok -> {
            status = ok ? "" : MultisoloClient.SOCIAL.errorKey(); rebuildWidgets();
        });
    }
    @Override public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
        super.extractRenderState(graphics, mouseX, mouseY, delta);
        int x = width / 2 - 150;
        graphics.centeredText(font, title, width / 2, 16, 0xFFFFFFFF);
        graphics.text(font, Component.translatable("multisolo.identity", message.authorId().substring(0, 12)), x, 36, 0xFFCCCCCC);
        graphics.text(font, message.position().x() + ", " + message.position().y() + ", " + message.position().z(), x, 50, 0xFFC5D8CC);
        for (int i = 0; i < 4; i++) graphics.text(font, PhraseText.line(message.sign().phrase().lines().get(i)), x, 70 + i * 12, 0xFFFFFFFF);
        var view = MultisoloClient.SOCIAL.view(message);
        graphics.text(font, Component.translatable("multisolo.vote.current", view.ownVote()), x, height - 76, 0xFFCCCCCC);
        if (!status.isEmpty()) graphics.textWithWordWrap(font, Component.translatable(status), x, height - 124, 300, 0xFFFF7777);
    }
    @Override public void onClose() { minecraft.gui.setScreen(parent); }
}
