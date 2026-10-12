package io.github.koka99cab.multisolo.fabric.client;

import io.github.koka99cab.multisolo.core.message.LocatedMessage;
import io.github.koka99cab.multisolo.fabric.ExperienceWorlds;
import io.github.koka99cab.multisolo.fabric.PhantomSignData;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.AbstractSignEditScreen;
import net.minecraft.client.gui.screens.inventory.SignEditScreen;
import net.minecraft.client.gui.screens.inventory.HangingSignEditScreen;
import net.minecraft.world.level.block.entity.HangingSignBlockEntity;
import net.minecraft.world.level.block.entity.SignBlockEntity;
import net.minecraft.world.level.block.entity.SignText;
import net.minecraft.world.level.block.entity.SignTextSlot;
import net.minecraft.world.phys.BlockHitResult;

public final class PhantomSignMenus {
    private PhantomSignMenus() {}
    public static LocatedMessage message(SignBlockEntity sign, SignTextSlot slot) {
        var client = Minecraft.getInstance();
        if (client.level == null || sign.getLevel() != client.level || !ExperienceWorlds.canShare(client.getSingleplayerServer())) return null;
        LocatedMessage own = ((PhantomSignData) sign).multisolo$message(slot);
        if (own != null && matches(own, sign, slot)) return own;
        var remote = ((PhantomSignData) sign).multisolo$remote();
        if (!MultisoloClient.SOCIAL.enabled() || remote == null || !remote.activeAt(System.currentTimeMillis())) return null;
        return matches(remote.message(), sign, slot) ? remote.message() : null;
    }
    private static boolean matches(LocatedMessage message, SignBlockEntity sign, SignTextSlot slot) {
        var pos = sign.getBlockPos();
        return message.position().x() == pos.getX() && message.position().y() == pos.getY() && message.position().z() == pos.getZ()
                && message.scope().minecraftVersion().equals(ExperienceWorlds.gameVersion())
                && message.scope().dimension().equals(Minecraft.getInstance().level.dimension().identifier().toString())
                && message.scope().experience().equals(io.github.koka99cab.multisolo.fabric.ExperienceCatalog.origin())
                && message.scope().protocolVersion() == io.github.koka99cab.multisolo.network.Protocol.VERSION
                && message.sign().face().name().equals(slot.name());
    }
    public static SignText display(SignBlockEntity sign, SignTextSlot slot) {
        LocatedMessage message = message(sign, slot);
        if (message == null) return sign.getText(slot);
        var original = sign.getText(slot);
        var lines = message.sign().phrase().lines().stream().map(value ->
                (net.minecraft.network.chat.Component) PhraseText.line(value)).toList();
        return new SignText(lines, lines, original.getColor(), original.hasGlowingText());
    }
    /** Uses a real block hit and opens the vanilla sign menu without sending an edit packet. */
    public static boolean tryOpen(Minecraft client) {
        if (client.player == null || client.level == null || client.gui.screen() != null || !client.player.isShiftKeyDown()
                || !client.player.getMainHandItem().isEmpty() || !client.player.getOffhandItem().isEmpty()
                || !(client.hitResult instanceof BlockHitResult hit)
                || !(client.level.getBlockEntity(hit.getBlockPos()) instanceof SignBlockEntity sign)) return false;
        var slot = sign.getSlotPlayerIsFacing(client.player);
        var message = message(sign, slot);
        if (message == null) return false;
        openViewer(client, sign, slot, message);
        return true;
    }
    public static void openViewer(Minecraft client, SignBlockEntity sign, SignTextSlot slot, LocatedMessage message) {
        AbstractSignEditScreen screen = sign instanceof HangingSignBlockEntity
                ? new HangingSignEditScreen(sign, slot, client.isTextFilteringEnabled())
                : new SignEditScreen(sign, slot, client.isTextFilteringEnabled());
        ((PhantomSignEditor) screen).multisolo$view(message);
        client.gui.setScreen(screen);
    }
}
