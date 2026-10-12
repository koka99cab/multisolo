package io.github.koka99cab.multisolo.fabric.client;

import io.github.koka99cab.multisolo.core.message.LocatedMessage;
import io.github.koka99cab.multisolo.core.message.MessagePhrase;
import io.github.koka99cab.multisolo.core.message.PhantomSign;
import io.github.koka99cab.multisolo.fabric.ExperienceWorlds;
import io.github.koka99cab.multisolo.fabric.PhantomSignData;
import java.util.Arrays;
import java.util.List;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.fabricmc.fabric.api.client.screen.v1.Screens;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.entity.SignBlockEntity;

/** Adds controls to the original vanilla screen; no separate composition screen. */
public final class SignEditorSession {
    private final Screen screen;
    private final PhantomSignEditor editor;
    private final LocatedMessage viewed;
    private LocatedMessage owned;
    private final MessagePhrase.Line[] lines = MessagePhrase.example().lines().toArray(MessagePhrase.Line[]::new);
    private String[] vanillaDraft;
    private boolean active;
    private boolean published;
    private String notice = "";
    private Button done;
    private Button mode;
    private Button upvote;
    private Button downvote;
    private final Button[] choices = new Button[4];
    private final Button[] previous = new Button[4];
    public SignEditorSession(Screen screen, PhantomSignEditor editor, LocatedMessage viewed) {
        this.screen = screen; this.editor = editor; this.viewed = viewed;
        vanillaDraft = editor.multisolo$lines();
        owned = ((PhantomSignData) editor.multisolo$sign()).multisolo$message(editor.multisolo$slot());
        LocatedMessage existing = viewed != null ? viewed : owned;
        if (existing != null) { active = true; existing.sign().phrase().lines().toArray(lines); }
        ScreenEvents.afterExtract(screen).register((s, graphics, x, y, delta) -> renderNotice(graphics));
        ScreenEvents.afterTick(screen).register(s -> update());
    }
    private Minecraft client() { return Minecraft.getInstance(); }
    private SocialController social() { return MultisoloClient.SOCIAL; }
    public boolean active() { return active; }
    public boolean readOnly() { return viewed != null; }
    public void init() {
        var widgets = Screens.getWidgets(screen);
        widgets.removeIf(widget -> widget instanceof Button button && button.getMessage().getString().equals(Component.translatable("gui.done").getString()));
        mode = Button.builder(Component.empty(), button -> {
            if (!active) { vanillaDraft = editor.multisolo$lines(); active = true; }
            else if (owned != null) {
                social().delete(client(), owned.id(), ok -> {
                    if (ok) { SocialController.forgetLocal(client(), owned); owned = null; active = false; editor.multisolo$lines(vanillaDraft); }
                    else notice = social().errorKey();
                    update();
                });
            } else { active = false; editor.multisolo$lines(vanillaDraft); }
            update();
        }).bounds(screen.width / 2 - 150, screen.height - 76, 300, 20).build();
        widgets.add(mode);
        int right = screen.width / 2 + 62;
        int available = Math.min(150, screen.width - right - 8);
        for (int i = 0; i < 4; i++) {
            final int row = i;
            previous[i] = Button.builder(Component.literal("<"), button -> cycle(row, -1))
                    .bounds(right, 64 + i * 24, 18, 20).build();
            widgets.add(previous[i]);
            choices[i] = Button.builder(Component.empty(), button -> cycle(row, 1))
                    .bounds(right + 20, 64 + i * 24, available - 20, 20).build();
            widgets.add(choices[i]);
        }
        if (readOnly()) {
            var lifetime = Tooltip.create(Component.translatable("multisolo.vote.lifetime",
                    io.github.koka99cab.multisolo.core.message.MessageLifetime.BASE_DAYS,
                    io.github.koka99cab.multisolo.core.message.MessageLifetime.MIN_DAYS,
                    io.github.koka99cab.multisolo.core.message.MessageLifetime.MAX_DAYS));
            upvote = Button.builder(Component.empty(), button -> vote(1)).bounds(screen.width / 2 - 150, 64, 96, 20).tooltip(lifetime).build();
            downvote = Button.builder(Component.empty(), button -> vote(-1)).bounds(screen.width / 2 - 150, 88, 96, 20).tooltip(lifetime).build();
            widgets.add(upvote); widgets.add(downvote);
        }
        done = Button.builder(Component.empty(), button -> {
            if (!active || readOnly()) screen.onClose(); else publish();
        }).bounds(screen.width / 2 - 150, screen.height - 28, 300, 20).build();
        widgets.add(done);
        update();
    }
    private void cycle(int row, int direction) {
        if (!active || readOnly()) return;
        var values = MessagePhrase.Line.values();
        lines[row] = values[Math.floorMod(lines[row].ordinal() + direction, values.length)];
        update();
    }
    private void update() {
        if (mode == null) return;
        if (readOnly() && (!social().enabled() || !social().view(viewed).activeAt(System.currentTimeMillis()) || !ExperienceWorlds.canShare(client().getSingleplayerServer()))) {
            screen.onClose(); return;
        }
        mode.setMessage(Component.translatable(active ? "multisolo.sign.on" : "multisolo.sign.off"));
        mode.active = !readOnly() && !social().busy();
        if (readOnly()) {
            var view = social().view(viewed);
            upvote.setMessage(Component.translatable("multisolo.vote.up", view.upvotes()).append(view.ownVote() == 1 ? " +" : ""));
            downvote.setMessage(Component.translatable("multisolo.vote.down", view.downvotes()).append(view.ownVote() == -1 ? " +" : ""));
            upvote.active = downvote.active = !social().busy() && !viewed.authorId().equals(social().identityId());
        }
        for (int i = 0; i < 4; i++) {
            choices[i].setVisible(active);
            previous[i].setVisible(active);
            previous[i].active = active && !readOnly() && !social().busy();
            choices[i].active = active && !readOnly() && !social().busy();
            choices[i].setMessage(lines[i] == MessagePhrase.Line.EMPTY ? Component.literal("—") : PhraseText.line(lines[i]));
        }
        done.setMessage(Component.translatable(readOnly() || !active ? "gui.done" : "multisolo.sign.publish"));
        done.active = !active || readOnly() || (social().enabled() && !social().busy() && Arrays.stream(lines).anyMatch(line -> line != MessagePhrase.Line.EMPTY));
    }
    public void preview(GuiGraphicsExtractor graphics, int vanillaTextColor) {
        var font = client().font;
        int lineHeight = editor.multisolo$sign().getTextLineHeight();
        for (int i = 0; i < 4; i++) {
            var text = PhraseText.line(lines[i]).getVisualOrderText();
            // Vanilla sign text has no GUI drop shadow; centeredText enables it.
            graphics.text(font, text, -font.width(text) / 2, i * lineHeight - 2 * lineHeight,
                    0xFF000000 | vanillaTextColor, false);
        }
    }
    private void renderNotice(GuiGraphicsExtractor graphics) {
        if (!active) return;
        Component text = Component.translatable(notice.isEmpty() ? (readOnly() ? "multisolo.sign.viewer" : "multisolo.sign.hint") : notice);
        graphics.textWithWordWrap(client().font, text, screen.width / 2 - 150, screen.height - 52, 300, 0xFFCCCCCC);
    }
    private void vote(int direction) {
        if (!readOnly() || social().busy() || viewed.authorId().equals(social().identityId())) return;
        int value = social().view(viewed).ownVote() == direction ? 0 : direction;
        social().vote(client(), viewed.id(), value, ok -> {
            notice = ok ? "multisolo.vote.saved" : social().errorKey();
            if (client().gui.screen() == screen) editor.multisolo$rebuild();
        });
    }
    private void publish() {
        if (social().busy()) return;
        var sign = new PhantomSign(new MessagePhrase(List.of(lines)), PhantomSign.Face.valueOf(editor.multisolo$slot().name()),
                io.github.koka99cab.multisolo.fabric.SignPlacements.capture(editor.multisolo$sign().getBlockState()));
        social().publishSign(client(), editor.multisolo$sign(), sign, ok -> {
            if (!ok) { notice = social().errorKey(); return; }
            LocatedMessage message = social().lastPublished();
            var server = client().getSingleplayerServer();
            var dimension = client().level == null ? null : client().level.dimension();
            var position = editor.multisolo$sign().getBlockPos();
            var slot = editor.multisolo$slot();
            if (server != null && dimension != null) server.execute(() -> {
                if (!ExperienceWorlds.canShare(server)) return;
                var level = server.getLevel(dimension);
                if (level != null && level.getBlockEntity(position) instanceof SignBlockEntity block) {
                    ((PhantomSignData) block).multisolo$message(slot, message);
                    level.sendBlockUpdated(position, block.getBlockState(), block.getBlockState(), 3);
                }
            });
            if (client().gui.screen() == screen) {
                published = true;
                editor.multisolo$lines(Arrays.stream(lines).map(value -> PhraseText.line(value).getString()).toArray(String[]::new));
                screen.onClose();
            }
        });
    }
    public void closed() {
        if (active && !published) editor.multisolo$lines(vanillaDraft);
    }
}
