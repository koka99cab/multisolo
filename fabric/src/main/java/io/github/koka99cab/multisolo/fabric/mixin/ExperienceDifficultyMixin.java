package io.github.koka99cab.multisolo.fabric.mixin;

import io.github.koka99cab.multisolo.fabric.ExperienceWorlds;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.Difficulty;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** No Fabric event can veto difficulty setters, including ignoreLock=true commands. */
@Mixin(MinecraftServer.class)
public abstract class ExperienceDifficultyMixin {
    @Inject(method = "setDifficulty", at = @At("HEAD"), cancellable = true)
    private void multisolo$fixedDifficulty(Difficulty difficulty, boolean ignoreLock, CallbackInfo callback) {
        if (ExperienceWorlds.connectedSolitude((MinecraftServer) (Object) this) && difficulty != Difficulty.NORMAL) callback.cancel();
    }

    @Inject(method = "setDifficultyLocked", at = @At("HEAD"), cancellable = true)
    private void multisolo$keepLocked(boolean locked, CallbackInfo callback) {
        if (ExperienceWorlds.connectedSolitude((MinecraftServer) (Object) this) && !locked) callback.cancel();
    }
}
