package io.github.koka99cab.multisolo.fabric.client.mixin;

import io.github.koka99cab.multisolo.fabric.client.EchoAvatarRenderState;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Fabric extraction events can append states, but cannot change vanilla model opacity per state. */
@Mixin(LivingEntityRenderer.class)
public abstract class EchoOpacityMixin {
    @Inject(method = "getRenderType", at = @At("HEAD"), cancellable = true)
    private void multisolo$translucent(LivingEntityRenderState state, boolean visible, boolean transparent, boolean glowing,
                                     CallbackInfoReturnable<RenderType> ci) {
        if (state instanceof EchoAvatarRenderState echo) ci.setReturnValue(RenderTypes.entityTranslucentCull(echo.skin.body().texturePath()));
    }
    @Inject(method = "getModelTint", at = @At("HEAD"), cancellable = true)
    private void multisolo$fade(LivingEntityRenderState state, CallbackInfoReturnable<Integer> ci) {
        if (state instanceof EchoAvatarRenderState echo) ci.setReturnValue(echo.tint);
    }
}
