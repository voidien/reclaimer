package com.voidreclaimer.api.ship;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

/**
 * What a fluid is worth in a Chemical Thruster. Set per fluid in the data map {@code void_reclaimer:thruster_fuel}
 * ({@code data/<ns>/data_maps/fluid/thruster_fuel.json}), so packs can add fuels.
 *
 * @param thrust multiplier on the thruster's force (1 = Nebulite Propellant)
 * @param burn   mB burned per tick at full throttle and full spin
 */
public record ThrusterFuel(float thrust, float burn) {
    public static final Codec<ThrusterFuel> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.floatRange(0f, 100f).fieldOf("thrust").forGetter(ThrusterFuel::thrust),
            Codec.floatRange(0f, 10_000f).fieldOf("burn").forGetter(ThrusterFuel::burn)
    ).apply(instance, ThrusterFuel::new));
}
