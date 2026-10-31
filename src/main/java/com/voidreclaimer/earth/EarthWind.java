package com.voidreclaimer.earth;

import com.voidreclaimer.Config;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * The wind over ruined Earth: a slow, slowly shifting horizontal flow that drifts ships and the flying body (the
 * dampener holds against it). Pure apart from {@link #at}, which reads the dimension, the game time and the config.
 * The field is a few sines over position and time, so it is the same on the client and the server, needs no state, and
 * swings round in about a minute; there is no weather in it (visual weather is Act III's own).
 */
public final class EarthWind {
    /** Blocks across one swirl of the wind. */
    static final double CELL = 96.0;
    /** Ticks for the wind to swing round once. */
    static final double PERIOD = 1200.0;
    /** The sum of two unit waves ranges over [-2, 2] on each axis: this brings the strongest gust to the top speed. */
    private static final double NORMALISE = 1.0 / (2.0 * Math.sqrt(2.0));

    private EarthWind() {}

    /**
     * The wind at ({@code x}, {@code z}) at game time {@code time}: horizontal, never faster than {@code maxSpeed}
     * (whichever unit the caller uses, blocks per tick or per second). Zero when {@code maxSpeed} is zero.
     */
    public static Vec3 field(double x, double z, long time, double maxSpeed) {
        if (maxSpeed <= 0.0) {
            return Vec3.ZERO;
        }
        double t = time / PERIOD * (2.0 * Math.PI);
        double east = Math.sin(x / CELL + t) + Math.cos(z / CELL * 0.7 - t * 0.6);
        double south = Math.cos(x / CELL * 0.8 - t * 0.9) + Math.sin(z / CELL + t * 0.4);
        return new Vec3(east, 0.0, south).scale(maxSpeed * NORMALISE);
    }

    /** The wind on the body at {@code pos} in {@code level}, in blocks per tick: zero anywhere but ruined Earth. */
    public static Vec3 at(Level level, Vec3 pos) {
        if (level.dimension() != RuinedEarth.DIMENSION) {
            return Vec3.ZERO;
        }
        return field(pos.x, pos.z, level.getGameTime(), Config.EARTH_WIND_SPEED.get() / 20.0);
    }

    /** The wind's top speed in blocks per second from the config, for ships (Sable's velocities are per second). */
    public static double maxSpeedPerSecond() {
        return Config.EARTH_WIND_SPEED.get();
    }
}
