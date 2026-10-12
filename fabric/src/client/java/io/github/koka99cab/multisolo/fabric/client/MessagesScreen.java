package io.github.koka99cab.multisolo.fabric.client;

import io.github.koka99cab.multisolo.core.message.LocatedMessage;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

final class MessagesScreen extends Screen {
    private final Screen parent;
    private EditBox text;
    private int page;
    private String notice = "";
    MessagesScreen(Screen parent) { super(Component.translatable("multisolo.messages")); this.parent = parent; }
    private SocialController social() { return MultisoloClient.SOCIAL; }
    private int pageSize() { return Math.max(1, (height - 158) / 24); }

    @Override protected void init() {
        int x = width / 2 - 150;
        String draft = text == null ? "" : text.getValue();
        text = addRenderableWidget(new EditBox(font, x, 39, 300, 20, Component.translatable("multisolo.write")));
        text.setMaxLength(560);
        text.setValue(draft);
        text.setHint(Component.translatable("multisolo.write"));
        addRenderableWidget(Button.builder(Component.translatable("multisolo.publish"), button -> {
            try {
                social().publish(minecraft, text.getValue(), ok -> {
                    notice = ok ? "multisolo.sent" : social().errorKey();
                    if (ok) text.setValue("");
                    rebuildWidgets();
                });
            } catch (IllegalArgumentException exception) { notice = "multisolo.error.text"; }
        }).bounds(x, 63, 148, 20).build()).active = !social().busy();
        addRenderableWidget(Button.builder(Component.translatable("multisolo.refresh"), button ->
                social().refresh(minecraft, ok -> { notice = ok ? "" : social().errorKey(); rebuildWidgets(); }))
                .bounds(x + 152, 63, 148, 20).build()).active = !social().busy();
        var messages = social().messages();
        page = Math.min(page, Math.max(0, (messages.size() - 1) / pageSize()));
        for (int index = page * pageSize(); index < Math.min(messages.size(), (page + 1) * pageSize()); index++) {
            LocatedMessage message = messages.get(index);
            String summary = message.position().x() + ", " + message.position().y() + ", " + message.position().z() + " : " + message.text();
            summary = font.plainSubstrByWidth(summary, 282);
            addRenderableWidget(Button.builder(Component.literal(summary), button ->
                    minecraft.gui.setScreen(new MessageScreen(this, message)))
                    .bounds(x, 102 + (index % pageSize()) * 24, 300, 20).build());
        }
        addRenderableWidget(Button.builder(Component.literal("<"), button -> { page--; rebuildWidgets(); })
                .bounds(x, height - 52, 60, 20).build()).active = page > 0;
        addRenderableWidget(Button.builder(Component.literal(">"), button -> { page++; rebuildWidgets(); })
                .bounds(x + 240, height - 52, 60, 20).build()).active = (page + 1) * pageSize() < messages.size();
        addRenderableWidget(Button.builder(Component.translatable("gui.back"), button -> onClose())
                .bounds(x, height - 28, 300, 20).build());
    }

    @Override public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
        super.extractRenderState(graphics, mouseX, mouseY, delta);
        graphics.centeredText(font, title, width / 2, 12, 0xFFFFFFFF);
        String id = social().identityId();
        graphics.centeredText(font, Component.translatable("multisolo.identity", id.isEmpty() ? "—" : id.substring(0, 12)), width / 2, 25, 0xFFCCCCCC);
        String status = social().busy() ? "multisolo.loading" : notice;
        graphics.text(font, status.isEmpty() ? Component.translatable("multisolo.nearby", social().messages().size())
                : Component.translatable(status), width / 2 - 150, 88, 0xFFCCCCCC);
    }
    @Override public void onClose() { minecraft.gui.setScreen(parent); }
}
