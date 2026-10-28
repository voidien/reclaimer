package com.voidreclaimer.replicator;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.voidreclaimer.dust.DustData;
import com.voidreclaimer.dust.DustKind;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

/** An amount of each dust kind, used for recipe costs and refunds. */
public record DustCost(float metallic, float silicate, float ice, float carbon) {
    public static final DustCost NONE = new DustCost(0f, 0f, 0f, 0f);

    public static final Codec<DustCost> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.FLOAT.optionalFieldOf("metallic", 0f).forGetter(DustCost::metallic),
            Codec.FLOAT.optionalFieldOf("silicate", 0f).forGetter(DustCost::silicate),
            Codec.FLOAT.optionalFieldOf("ice", 0f).forGetter(DustCost::ice),
            Codec.FLOAT.optionalFieldOf("carbon", 0f).forGetter(DustCost::carbon)
    ).apply(instance, DustCost::new));

    public static final StreamCodec<ByteBuf, DustCost> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.FLOAT, DustCost::metallic,
            ByteBufCodecs.FLOAT, DustCost::silicate,
            ByteBufCodecs.FLOAT, DustCost::ice,
            ByteBufCodecs.FLOAT, DustCost::carbon,
            DustCost::new);

    public float get(DustKind kind) {
        return switch (kind) {
            case METALLIC -> metallic;
            case SILICATE -> silicate;
            case ICE -> ice;
            case CARBON -> carbon;
        };
    }

    public boolean canAfford(DustData dust) {
        for (DustKind kind : DustKind.values()) {
            if (dust.get(kind) + 1.0E-4f < get(kind)) {
                return false;
            }
        }
        return true;
    }

    /** {@code dust} minus this cost (clamped at zero). */
    public DustData takeFrom(DustData dust) {
        DustData result = dust;
        for (DustKind kind : DustKind.values()) {
            result = result.with(kind, Math.max(0f, dust.get(kind) - get(kind)));
        }
        return result;
    }

    /** {@code dust} plus this cost, never exceeding {@code capacity} per kind. */
    public DustData giveTo(DustData dust, float capacity) {
        DustData result = dust;
        for (DustKind kind : DustKind.values()) {
            result = result.add(kind, get(kind), capacity);
        }
        return result;
    }

    public DustCost scale(float factor) {
        return new DustCost(metallic * factor, silicate * factor, ice * factor, carbon * factor);
    }

    public boolean isFree() {
        return metallic <= 0f && silicate <= 0f && ice <= 0f && carbon <= 0f;
    }
}
