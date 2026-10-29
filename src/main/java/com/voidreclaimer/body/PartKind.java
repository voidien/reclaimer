package com.voidreclaimer.body;

import com.mojang.serialization.Codec;
import io.netty.buffer.ByteBuf;
import java.util.function.IntFunction;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.ByIdMap;
import net.minecraft.util.StringRepresentable;

/** The slot a part goes in. The frame decides how many slots of each other kind the body has. */
public enum PartKind implements StringRepresentable {
    FRAME("frame"),
    CORE("core"),
    THRUSTER("thruster"),
    STORAGE("storage"),
    SENSOR("sensor"),
    TOOL("tool"),
    COSMETIC("cosmetic");

    public static final Codec<PartKind> CODEC = StringRepresentable.fromEnum(PartKind::values);
    private static final IntFunction<PartKind> BY_ID = ByIdMap.continuous(Enum::ordinal, values(), ByIdMap.OutOfBoundsStrategy.ZERO);
    public static final StreamCodec<ByteBuf, PartKind> STREAM_CODEC = ByteBufCodecs.idMapper(BY_ID, Enum::ordinal);

    private final String name;

    PartKind(String name) {
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
