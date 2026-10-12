package io.github.koka99cab.multisolo.fabric.client.mixin;

import io.github.koka99cab.multisolo.fabric.client.PhantomSignMenus;
import net.minecraft.client.renderer.blockentity.AbstractSignRenderer;
import net.minecraft.client.renderer.blockentity.state.SignRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.world.level.block.entity.SignBlockEntity;
import net.minecraft.world.level.block.entity.SignTextSlot;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** No Fabric event exposes sign text extraction. Only the render state is changed, never the block/save. */
@Mixin(AbstractSignRenderer.class)
public abstract class SignRendererMixin {
    @Inject(method = "extractRenderState(Lnet/minecraft/world/level/block/entity/SignBlockEntity;Lnet/minecraft/client/renderer/blockentity/state/SignRenderState;FLnet/minecraft/world/phys/Vec3;Lnet/minecraft/client/renderer/feature/ModelFeatureRenderer$CrumblingOverlay;)V", at = @At("TAIL"))
    private void multisolo$text(SignBlockEntity sign, SignRenderState state, float delta, Vec3 camera,
                               ModelFeatureRenderer.CrumblingOverlay overlay, CallbackInfo ci) {
        state.frontText = PhantomSignMenus.display(sign, SignTextSlot.FRONT);
        state.backText = PhantomSignMenus.display(sign, SignTextSlot.BACK);
    }
}
