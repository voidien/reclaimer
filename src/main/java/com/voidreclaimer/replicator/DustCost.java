package com.voidreclaimer.replicator;

import com.mojang.serialization.Codec;
import com.voidreclaimer.dust.DustData;
import io.netty.buffer.ByteBuf;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;

/**
 * An amount per dust kind id, used for recipe costs and refunds. In recipe JSON it is a plain object,
 * {@code {"void_reclaimer:metallic": 10}}; bare names ({@code "metallic"}) mean the built-in kinds.
 */
public record DustCost(Map<ResourceLocation, Float> amounts) {
    public static final DustCost NONE = new DustCost(Map.of());

    public static final Codec<DustCost> CODEC =
            DustData.CODEC.xmap(data -> new DustCost(data.amounts()), cost -> new DustData(cost.amounts()));
    public static final StreamCodec<ByteBuf, DustCost> STREAM_CODEC =
            DustData.STREAM_CODEC.map(data -> new DustCost(data.amounts()), cost -> new DustData(cost.amounts()));

    public DustCost {
        amounts = new DustData(amounts).amounts();
    }

    /** Shorthand for the four built-in kinds. */
    public static DustCost of(float metallic, float silicate, float ice, float carbon) {
        return new DustCost(DustData.of(metallic, silicate, ice, carbon).amounts());
    }

    public static Builder builder() {
        return new Builder();
    }

    public float get(ResourceLocation kind) {
        return amounts.getOrDefault(kind, 0f);
    }

    public boolean canAfford(DustData dust) {
        for (Map.Entry<ResourceLocation, Float> entry : amounts.entrySet()) {
            if (dust.get(entry.getKey()) + 1.0E-4f < entry.getValue()) {
                return false;
            }
        }
        return true;
    }

    /** {@code dust} minus this cost (clamped at zero). */
    public DustData takeFrom(DustData dust) {
        DustData result = dust;
        for (Map.Entry<ResourceLocation, Float> entry : amounts.entrySet()) {
            result = result.with(entry.getKey(), Math.max(0f, dust.get(entry.getKey()) - entry.getValue()));
        }
        return result;
    }

    /** {@code dust} plus this cost, never exceeding {@code capacity} per kind. */
    public DustData giveTo(DustData dust, float capacity) {
        DustData result = dust;
        for (Map.Entry<ResourceLocation, Float> entry : amounts.entrySet()) {
            result = result.add(entry.getKey(), entry.getValue(), capacity);
        }
        return result;
    }

    public DustCost scale(float factor) {
        Map<ResourceLocation, Float> scaled = new HashMap<>();
        amounts.forEach((kind, amount) -> scaled.put(kind, amount * factor));
        return new DustCost(scaled);
    }

    public float total() {
        float total = 0f;
        for (float amount : amounts.values()) {
            total += amount;
        }
        return total;
    }

    public boolean isFree() {
        return amounts.isEmpty();
    }

    /** Builds a cost one kind at a time, e.g. {@code DustCost.builder().add(DustKinds.METALLIC, 10).build()}. */
    public static final class Builder {
        private final Map<ResourceLocation, Float> amounts = new LinkedHashMap<>();

        public Builder add(ResourceLocation kind, float amount) {
            amounts.merge(kind, amount, Float::sum);
            return this;
        }

        public DustCost build() {
            return new DustCost(amounts);
        }
    }
}
