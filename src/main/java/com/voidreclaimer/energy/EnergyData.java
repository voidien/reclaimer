package com.voidreclaimer.energy;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.voidreclaimer.Config;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

/**
 * Immutable snapshot of a player's energy.
 *
 * @param energy   current energy, 0..capacity
 * @param capacity maximum energy (base config value for now; the modular body sets it from Phase 5 on)
 * @param lowPower true after energy hit zero, until it recovers to the restart threshold
 */
public record EnergyData(float energy, float capacity, boolean lowPower) {
    /** Missing fields load as a full charge at the configured capacity. */
    public static final Codec<EnergyData> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.FLOAT.optionalFieldOf("energy").forGetter(data -> java.util.Optional.of(data.energy())),
            Codec.FLOAT.optionalFieldOf("capacity").forGetter(data -> java.util.Optional.of(data.capacity())),
            Codec.BOOL.optionalFieldOf("low_power", false).forGetter(EnergyData::lowPower)
    ).apply(instance, (energy, capacity, lowPower) -> {
        float cap = capacity.orElseGet(() -> Config.ENERGY_CAPACITY.get().floatValue());
        return new EnergyData(energy.orElse(cap), cap, lowPower);
    }));

    public static final StreamCodec<ByteBuf, EnergyData> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.FLOAT, EnergyData::energy,
            ByteBufCodecs.FLOAT, EnergyData::capacity,
            ByteBufCodecs.BOOL, EnergyData::lowPower,
            EnergyData::new);

    public static EnergyData createDefault() {
        float capacity = Config.ENERGY_CAPACITY.get().floatValue();
        return new EnergyData(capacity, capacity, false);
    }

    public float fraction() {
        return capacity <= 0 ? 0 : Math.min(1f, energy / capacity);
    }

    /** Number of half-icons (0..20) the HUD shows; used to decide when a resync is visible. */
    public int halfIcons() {
        return (int) Math.ceil(fraction() * 20f);
    }
}
