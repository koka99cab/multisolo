package io.github.koka99cab.multisolo.fabric.client.mixin;

import io.github.koka99cab.multisolo.fabric.client.PhantomSignMenus;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** No installed Fabric event handles a local read-only menu before vanilla block use. */
@Mixin(Minecraft.class)
public abstract class SignInteractionMixin {
    @Inject(method = "startUseItem", at = @At("HEAD"), cancellable = true)
    private void multisolo$read(CallbackInfo ci) {
        if (PhantomSignMenus.tryOpen((Minecraft) (Object) this)) ci.cancel();
    }
}
