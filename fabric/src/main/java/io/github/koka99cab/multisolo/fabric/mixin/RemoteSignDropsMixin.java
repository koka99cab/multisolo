package io.github.koka99cab.multisolo.fabric.mixin;

import io.github.koka99cab.multisolo.fabric.PhantomSignData;
import java.util.List;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** No bundled Fabric event can suppress every loot path of marked vanilla block entities. */
@Mixin(BlockBehaviour.class)
public abstract class RemoteSignDropsMixin {
    @Inject(method = "getDrops", at = @At("HEAD"), cancellable = true)
    private void multisolo$noRemoteLoot(BlockState state, LootParams.Builder params, CallbackInfoReturnable<List<ItemStack>> ci) {
        var block = params.getOptionalParameter(LootContextParams.BLOCK_ENTITY);
        if (block instanceof PhantomSignData data && data.multisolo$remote() != null) ci.setReturnValue(List.of());
    }
}
