package com.voidreclaimer.dust;

import com.mojang.serialization.Codec;
import io.netty.buffer.ByteBuf;
import java.util.function.IntFunction;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.ByIdMap;
import net.minecraft.util.StringRepresentable;

/** What a cloud is made of. Each large cloud has one dominant kind. */
public enum DustKind implements StringRepresentable {
    METALLIC("metallic", 0xB8C2CC, 0.30),
    SILICATE("silicate", 0xD9B38C, 0.35),
    ICE("ice", 0x9FE3FF, 0.20),
    CARBON("carbon", 0x6E6A80, 0.15);

    public static final Codec<DustKind> CODEC = StringRepresentable.fromEnum(DustKind::values);
    private static final IntFunction<DustKind> BY_ID = ByIdMap.continuous(Enum::ordinal, values(), ByIdMap.OutOfBoundsStrategy.ZERO);
    public static final StreamCodec<ByteBuf, DustKind> STREAM_CODEC = ByteBufCodecs.idMapper(BY_ID, Enum::ordinal);

    private final String name;
    private final int color;
    private final double weight;

    DustKind(String name, int color, double weight) {
        this.name = name;
        this.color = color;
        this.weight = weight;
    }

    @Override
    public String getSerializedName() {
        return name;
    }

    /** RGB color used for HUD meters and labels. */
    public int color() {
        return color;
    }

    /** Relative chance that a generated cloud is of this kind. */
    double weight() {
        return weight;
    }

    public Component displayName() {
        return Component.translatable("dust.void_reclaimer." + name);
    }

    /** Picks a kind from a uniform value in [0, 1) according to the weights. */
    static DustKind pick(double roll) {
        double total = 0;
        for (DustKind kind : values()) {
            total += kind.weight;
        }
        double target = roll * total;
        for (DustKind kind : values()) {
            target -= kind.weight;
            if (target < 0) {
                return kind;
            }
        }
        return CARBON;
    }

    public static DustKind byName(String name) {
        for (DustKind kind : values()) {
            if (kind.name.equals(name)) {
                return kind;
            }
        }
        return null;
    }
}
