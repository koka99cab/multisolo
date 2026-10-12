package io.github.koka99cab.multisolo.fabric.client.mixin;

import io.github.koka99cab.multisolo.core.message.LocatedMessage;
import io.github.koka99cab.multisolo.fabric.ExperienceWorlds;
import io.github.koka99cab.multisolo.fabric.client.PhantomSignEditor;
import io.github.koka99cab.multisolo.fabric.client.SignEditorSession;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractSignEditScreen;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.PreeditEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.entity.SignBlockEntity;
import net.minecraft.world.level.block.entity.SignTextSlot;
import org.joml.Vector2f;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Screen events cannot access the four private lines or suppress vanilla text input/packets. */
@Mixin(AbstractSignEditScreen.class)
public abstract class SignEditorMixin extends Screen implements PhantomSignEditor {
    @Shadow @Final protected SignBlockEntity sign;
    @Shadow @Final private SignTextSlot slot;
    @Shadow @Final private String[] messages;
    @Shadow @Final private int textColor;
    @Unique private SignEditorSession multisolo$session;
    @Unique private LocatedMessage multisolo$viewer;
    protected SignEditorMixin(Component title) { super(title); }
    public SignBlockEntity multisolo$sign() { return sign; }
    public SignTextSlot multisolo$slot() { return slot; }
    public String[] multisolo$lines() { return messages.clone(); }
    public void multisolo$lines(String[] lines) { System.arraycopy(lines, 0, messages, 0, 4); }
    public void multisolo$view(LocatedMessage message) { multisolo$viewer = message; }
    public SignEditorSession multisolo$session() { return multisolo$session; }
    public void multisolo$rebuild() { rebuildWidgets(); }
    @Inject(method = "init", at = @At("TAIL"))
    private void multisolo$init(CallbackInfo ci) {
        if (!ExperienceWorlds.canShare(minecraft.getSingleplayerServer())) return;
        if (multisolo$session == null) multisolo$session = new SignEditorSession(this, this, multisolo$viewer);
        multisolo$session.init();
    }
    @Inject(method = "keyPressed", at = @At("HEAD"), cancellable = true)
    private void multisolo$key(KeyEvent event, CallbackInfoReturnable<Boolean> ci) {
        if (multisolo$session != null && multisolo$session.active()) ci.setReturnValue(super.keyPressed(event));
    }
    @Inject(method = "charTyped", at = @At("HEAD"), cancellable = true)
    private void multisolo$char(CharacterEvent event, CallbackInfoReturnable<Boolean> ci) {
        if (multisolo$session != null && multisolo$session.active()) ci.setReturnValue(true);
    }
    @Inject(method = "preeditUpdated", at = @At("HEAD"), cancellable = true)
    private void multisolo$ime(PreeditEvent event, CallbackInfoReturnable<Boolean> ci) {
        if (multisolo$session != null && multisolo$session.active()) ci.setReturnValue(true);
    }
    @Inject(method = "extractSignText", at = @At("HEAD"), cancellable = true)
    private void multisolo$preview(GuiGraphicsExtractor graphics, Vector2f cursor, CallbackInfo ci) {
        if (multisolo$session != null && multisolo$session.active()) {
            multisolo$session.preview(graphics, textColor); ci.cancel();
        }
    }
    @Inject(method = "removed", at = @At("HEAD"), cancellable = true)
    private void multisolo$removed(CallbackInfo ci) {
        if (multisolo$session == null) return;
        if (multisolo$session.readOnly()) {
            minecraft.textInputManager().stopTextInput(this); ci.cancel();
        } else multisolo$session.closed();
    }
}
