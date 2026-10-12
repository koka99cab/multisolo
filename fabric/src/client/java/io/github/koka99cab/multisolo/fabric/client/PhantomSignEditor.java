package io.github.koka99cab.multisolo.fabric.client;

import io.github.koka99cab.multisolo.core.message.LocatedMessage;
import net.minecraft.world.level.block.entity.SignBlockEntity;
import net.minecraft.world.level.block.entity.SignTextSlot;

/** Narrow bridge to the vanilla sign screen's private editing state. */
public interface PhantomSignEditor {
    SignBlockEntity multisolo$sign();
    SignTextSlot multisolo$slot();
    String[] multisolo$lines();
    void multisolo$lines(String[] lines);
    void multisolo$view(LocatedMessage message);
    SignEditorSession multisolo$session();
    void multisolo$rebuild();
}
