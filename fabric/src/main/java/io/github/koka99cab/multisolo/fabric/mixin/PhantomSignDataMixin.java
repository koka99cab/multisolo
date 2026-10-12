package io.github.koka99cab.multisolo.fabric.mixin;

import io.github.koka99cab.multisolo.core.message.LocatedMessage;
import io.github.koka99cab.multisolo.fabric.PhantomSignData;
import io.github.koka99cab.multisolo.fabric.RemoteSignState;
import io.github.koka99cab.multisolo.network.Protocol;
import net.minecraft.world.level.block.entity.SignBlockEntity;
import net.minecraft.world.level.block.entity.SignTextSlot;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Fabric has no event for persisting extra data on an existing vanilla sign. */
@Mixin(SignBlockEntity.class)
public abstract class PhantomSignDataMixin implements PhantomSignData {
    @Unique private LocatedMessage multisolo$front;
    @Unique private LocatedMessage multisolo$back;
    @Unique private RemoteSignState multisolo$remote;
    @Override public RemoteSignState multisolo$remote() { return multisolo$remote; }
    @Override public void multisolo$remote(RemoteSignState remote) {
        multisolo$remote = remote;
        ((SignBlockEntity) (Object) this).setChanged();
    }
    @Override public LocatedMessage multisolo$message(SignTextSlot slot) {
        return slot == SignTextSlot.FRONT ? multisolo$front : multisolo$back;
    }
    @Override public void multisolo$message(SignTextSlot slot, LocatedMessage message) {
        if (slot == SignTextSlot.FRONT) multisolo$front = message; else multisolo$back = message;
        ((SignBlockEntity) (Object) this).setChanged();
    }
    @Inject(method = "saveAdditional", at = @At("TAIL"))
    private void multisolo$save(ValueOutput output, CallbackInfo ci) {
        if (multisolo$front != null) output.putString("multisolo_front_v2", Protocol.JSON.toJson(multisolo$front));
        if (multisolo$back != null) output.putString("multisolo_back_v2", Protocol.JSON.toJson(multisolo$back));
        if (multisolo$remote != null) output.putString("multisolo_remote_v2", Protocol.JSON.toJson(multisolo$remote));
    }
    @Inject(method = "loadAdditional", at = @At("TAIL"))
    private void multisolo$load(ValueInput input, CallbackInfo ci) {
        multisolo$front = multisolo$read(input.getStringOr("multisolo_front_v2", ""));
        multisolo$back = multisolo$read(input.getStringOr("multisolo_back_v2", ""));
        String remote = input.getStringOr("multisolo_remote_v2", "");
        multisolo$remote = null;
        if (!remote.isEmpty() && remote.length() <= Protocol.MAX_BODY_BYTES) {
            try { multisolo$remote = Protocol.JSON.fromJson(remote, RemoteSignState.class); }
            catch (RuntimeException invalid) { /* Never infer ownership from malformed metadata. */ }
        }
    }
    @Unique private static LocatedMessage multisolo$read(String json) {
        if (json.isEmpty() || json.length() > Protocol.MAX_BODY_BYTES) return null;
        try { return Protocol.JSON.fromJson(json, LocatedMessage.class); }
        catch (RuntimeException invalid) { return null; }
    }
}
