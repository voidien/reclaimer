package com.voidreclaimer.starship;

import net.minecraft.world.phys.Vec3;

/**
 * How the Test Starship drifts and turns, as a pure function of the level's game time, so that the server and every
 * client put it in the same place without a stream of movement packets. It circles its anchor slowly, bobs up and
 * down, and turns once a minute or so.
 */
public final class StarshipDrift {
    /** Horizontal radius of the drift circle, in blocks. */
    public static final double RADIUS = 3.0;
    /** Up and down bob, in blocks either way. */
    public static final double BOB = 1.0;
    /** Ticks for one trip round the circle. */
    public static final double CIRCLE_TICKS = 1200.0;
    /** Ticks for one bob up and down. */
    public static final double BOB_TICKS = 300.0;
    /** Degrees the ship turns each tick (one turn in 72 s). */
    public static final float TURN_PER_TICK = 0.25f;

    private StarshipDrift() {}

    /** Offset from the anchor at {@code time} (game ticks, fractional for render frames); {@code phase} in [0, 1). */
    public static Vec3 offset(double time, double phase) {
        double circle = 2 * Math.PI * (time / CIRCLE_TICKS + phase);
        double bob = 2 * Math.PI * (time / BOB_TICKS + phase);
        return new Vec3(RADIUS * Math.sin(circle), BOB * Math.sin(bob), RADIUS * Math.cos(circle));
    }

    /** Yaw in degrees, [0, 360). */
    public static float yaw(double time, double phase) {
        double degrees = time * TURN_PER_TICK + phase * 360.0;
        return (float) (degrees - 360.0 * Math.floor(degrees / 360.0));
    }
}
