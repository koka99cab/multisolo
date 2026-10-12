package io.github.koka99cab.multisolo.fabric;

import io.github.koka99cab.multisolo.core.message.SignPlacement;
import java.util.HashMap;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.SignBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;

public final class SignPlacements {
    private SignPlacements() {}
    public static SignPlacement capture(BlockState state) {
        if (!(state.getBlock() instanceof SignBlock)) throw new IllegalArgumentException("Expected a sign");
        var properties = new HashMap<String, String>();
        state.getValues().forEach(value -> properties.put(value.property().getName(), value.valueName()));
        // Never copy water into another player's world.
        properties.put("waterlogged", "false");
        return new SignPlacement(BuiltInRegistries.BLOCK.getKey(state.getBlock()).toString(), properties);
    }
    public static BlockState state(SignPlacement placement) {
        var block = BuiltInRegistries.BLOCK.getValue(Identifier.parse(placement.block()));
        if (!(block instanceof SignBlock)) throw new IllegalArgumentException("Unavailable sign block");
        BlockState state = block.defaultBlockState();
        for (var entry : placement.properties().entrySet())
            state = property(state, block.getStateDefinition().getProperty(entry.getKey()), entry.getValue());
        return state;
    }
    private static <T extends Comparable<T>> BlockState property(BlockState state, Property<T> property, String value) {
        if (property == null) throw new IllegalArgumentException("Unknown sign property");
        return state.setValue(property, property.getValue(value).orElseThrow());
    }
}
