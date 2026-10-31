package com.voidreclaimer.ship;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import org.joml.Quaterniondc;
import org.joml.Vector3d;
import org.joml.Vector3dc;

/**
 * Which way a ship points, in degrees, as a pilot reads it: {@code pitch} is the nose above the horizon (negative below),
 * {@code roll} is the ship's up tilted toward the pilot's right (negative left), {@code heading} is a compass bearing of
 * the nose (north 0, east 90, south 180, west 270). Pure, for the HUD and the Helm's readout.
 */
public record Attitude(double pitch, double roll, double heading) {
    public static final Attitude LEVEL = new Attitude(0, 0, 0);
    public static final StreamCodec<ByteBuf, Attitude> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.FLOAT, a -> (float) a.pitch,
            ByteBufCodecs.FLOAT, a -> (float) a.roll,
            ByteBufCodecs.FLOAT, a -> (float) a.heading,
            (pitch, roll, heading) -> new Attitude(pitch, roll, heading));

    /**
     * The attitude of a ship turned by {@code orientation} (ship frame to world), whose Helm's axes in the ship frame are
     * {@code forward}, {@code right} and {@code up} (see {@code HelmBlockEntity.axes}).
     */
    public static Attitude of(Quaterniondc orientation, Vector3dc forward, Vector3dc right, Vector3dc up) {
        Vector3d f = orientation.transform(new Vector3d(forward));
        Vector3d r = orientation.transform(new Vector3d(right));
        Vector3d u = orientation.transform(new Vector3d(up));
        double pitch = Math.toDegrees(Math.asin(Math.max(-1.0, Math.min(1.0, f.y))));
        // Rolling right tips the ship's up toward its right and its right toward the ground.
        double roll = Math.toDegrees(Math.atan2(-r.y, u.y));
        // Minecraft's yaw: south 0, west 90, north 180, east -90; a compass bearing is that plus 180.
        double heading = (Math.toDegrees(Math.atan2(-f.x, f.z)) + 180.0) % 360.0;
        if (heading < 0) {
            heading += 360.0;
        }
        return new Attitude(pitch, roll, heading);
    }
}
