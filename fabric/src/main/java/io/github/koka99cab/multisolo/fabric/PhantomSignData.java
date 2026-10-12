package io.github.koka99cab.multisolo.fabric;

import io.github.koka99cab.multisolo.core.message.LocatedMessage;
import net.minecraft.world.level.block.entity.SignTextSlot;

/** Metadata attached to a real vanilla sign, independently for each face. */
public interface PhantomSignData {
    LocatedMessage multisolo$message(SignTextSlot slot);
    void multisolo$message(SignTextSlot slot, LocatedMessage message);
    RemoteSignState multisolo$remote();
    void multisolo$remote(RemoteSignState remote);
}
