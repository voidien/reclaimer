package com.voidreclaimer.api.body;

import com.mojang.serialization.Codec;
import io.netty.buffer.ByteBuf;
import java.util.function.IntFunction;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.ByIdMap;
import net.minecraft.util.StringRepresentable;

/**
 * The mounts a body section offers, and the kind of module that goes in each. A fixed set by design: each kind has
 * its own rules in {@code BodyStats} (one core, thrusters add up, ...). New behaviour comes from new modules of these
 * kinds (for tools, a {@link ToolModuleType}).
 */
public enum ModuleKind implements StringRepresentable {
    CORE("core"),
    THRUSTER("thruster"),
    STORAGE("storage"),
    SENSOR("sensor"),
    TOOL("tool"),
    COSMETIC("cosmetic");

    public static final Codec<ModuleKind> CODEC = StringRepresentable.fromEnum(ModuleKind::values);
    private static final IntFunction<ModuleKind> BY_ID = ByIdMap.continuous(Enum::ordinal, values(), ByIdMap.OutOfBoundsStrategy.ZERO);
    public static final StreamCodec<ByteBuf, ModuleKind> STREAM_CODEC = ByteBufCodecs.idMapper(BY_ID, Enum::ordinal);

    private final String name;

    ModuleKind(String name) {
        this.name = name;
    }

    public Component displayName() {
        return Component.translatable("body.void_reclaimer.kind." + name);
    }

    @Override
    public String getSerializedName() {
        return name;
    }
}
