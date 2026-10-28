package com.voidreclaimer.flight;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

/**
 * Per-player flight toggles.
 *
 * @param dampener          inertial dampener: brake to a stop when no thrust key is held
 * @param creativeThrusters creative mode only: use thruster flight instead of vanilla creative flight
 */
public record FlightSettings(boolean dampener, boolean creativeThrusters) {
    private static final boolean DEFAULT_DAMPENER = true;
    public static final FlightSettings DEFAULT = new FlightSettings(DEFAULT_DAMPENER, false);

    public static final Codec<FlightSettings> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.BOOL.optionalFieldOf("dampener", DEFAULT_DAMPENER).forGetter(FlightSettings::dampener),
            Codec.BOOL.optionalFieldOf("creative_thrusters", false).forGetter(FlightSettings::creativeThrusters)
    ).apply(instance, FlightSettings::new));

    public static final StreamCodec<ByteBuf, FlightSettings> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.BOOL, FlightSettings::dampener,
            ByteBufCodecs.BOOL, FlightSettings::creativeThrusters,
            FlightSettings::new);

    public FlightSettings withDampener(boolean value) {
        return new FlightSettings(value, creativeThrusters);
    }

    public FlightSettings withCreativeThrusters(boolean value) {
        return new FlightSettings(dampener, value);
    }
}
