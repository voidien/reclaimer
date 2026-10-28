package com.voidreclaimer.dust;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.voidreclaimer.Config;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.entity.player.Player;

/** The AI's internal dust reserve, one amount per kind. Spent by the Replicator (Phase 4). */
public record DustData(float metallic, float silicate, float ice, float carbon) {
    public static final DustData EMPTY = new DustData(0f, 0f, 0f, 0f);

    public static final Codec<DustData> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.FLOAT.optionalFieldOf("metallic", 0f).forGetter(DustData::metallic),
            Codec.FLOAT.optionalFieldOf("silicate", 0f).forGetter(DustData::silicate),
            Codec.FLOAT.optionalFieldOf("ice", 0f).forGetter(DustData::ice),
            Codec.FLOAT.optionalFieldOf("carbon", 0f).forGetter(DustData::carbon)
    ).apply(instance, DustData::new));

    public static final StreamCodec<ByteBuf, DustData> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.FLOAT, DustData::metallic,
            ByteBufCodecs.FLOAT, DustData::silicate,
            ByteBufCodecs.FLOAT, DustData::ice,
            ByteBufCodecs.FLOAT, DustData::carbon,
            DustData::new);

    /** Storage per kind. Phase 5a's storage parts raise it; for now it's the config starter capacity. */
    public static float capacity(Player player) {
        return Config.DUST_CAPACITY.get().floatValue();
    }

    public float get(DustKind kind) {
        return switch (kind) {
            case METALLIC -> metallic;
            case SILICATE -> silicate;
            case ICE -> ice;
            case CARBON -> carbon;
        };
    }

    public DustData with(DustKind kind, float amount) {
        return switch (kind) {
            case METALLIC -> new DustData(amount, silicate, ice, carbon);
            case SILICATE -> new DustData(metallic, amount, ice, carbon);
            case ICE -> new DustData(metallic, silicate, amount, carbon);
            case CARBON -> new DustData(metallic, silicate, ice, amount);
        };
    }

    /** Adds dust of one kind, never exceeding {@code capacity}. */
    public DustData add(DustKind kind, float amount, float capacity) {
        return with(kind, Math.min(capacity, get(kind) + amount));
    }
}
