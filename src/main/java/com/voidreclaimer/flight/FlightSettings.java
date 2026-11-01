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
 * @param aimSteering       at a Helm: the ship swings to where the pilot looks, not with the mouse's movement
 * @param rcsLock           at a Helm: the ship's thrusters stop its spin and hold its heading when no rotation key is held
 */
public record FlightSettings(boolean dampener, boolean creativeThrusters, boolean rcsLock, boolean aimSteering) {
    private static final boolean DEFAULT_DAMPENER = true;
    private static final boolean DEFAULT_RCS_LOCK = true;
    public static final FlightSettings DEFAULT = new FlightSettings(DEFAULT_DAMPENER, false, DEFAULT_RCS_LOCK, false);

    public static final Codec<FlightSettings> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.BOOL.optionalFieldOf("dampener", DEFAULT_DAMPENER).forGetter(FlightSettings::dampener),
            Codec.BOOL.optionalFieldOf("creative_thrusters", false).forGetter(FlightSettings::creativeThrusters),
            Codec.BOOL.optionalFieldOf("rcs_lock", DEFAULT_RCS_LOCK).forGetter(FlightSettings::rcsLock),
            Codec.BOOL.optionalFieldOf("aim_steering", false).forGetter(FlightSettings::aimSteering)
    ).apply(instance, FlightSettings::new));

    public static final StreamCodec<ByteBuf, FlightSettings> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.BOOL, FlightSettings::dampener,
            ByteBufCodecs.BOOL, FlightSettings::creativeThrusters,
            ByteBufCodecs.BOOL, FlightSettings::rcsLock,
            ByteBufCodecs.BOOL, FlightSettings::aimSteering,
            FlightSettings::new);

    public FlightSettings withDampener(boolean value) {
        return new FlightSettings(value, creativeThrusters, rcsLock, aimSteering);
    }

    public FlightSettings withCreativeThrusters(boolean value) {
        return new FlightSettings(dampener, value, rcsLock, aimSteering);
    }

    public FlightSettings withRcsLock(boolean value) {
        return new FlightSettings(dampener, creativeThrusters, value, aimSteering);
    }

    public FlightSettings withAimSteering(boolean value) {
        return new FlightSettings(dampener, creativeThrusters, rcsLock, value);
    }
}
