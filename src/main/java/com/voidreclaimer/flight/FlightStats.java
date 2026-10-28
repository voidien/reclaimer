package com.voidreclaimer.flight;

import com.voidreclaimer.Config;
import net.minecraft.world.entity.player.Player;

/**
 * Handling stats of the AI's body. Forces are in blocks/tick² per unit mass, so acceleration = force / mass.
 *
 * @param mass       total body mass
 * @param thrust     combined thruster force
 * @param brakeForce inertial dampener force
 * @param maxSpeed   top speed in blocks per tick
 */
public record FlightStats(double mass, double thrust, double brakeForce, double maxSpeed) {
    public static final double TICKS_PER_SECOND = 20.0;

    /** Current stats for a player. Phase 5 computes these from the assembled frame and parts. */
    public static FlightStats of(Player player) {
        return new FlightStats(
                Config.STARTER_MASS.get(),
                Config.STARTER_THRUST.get(),
                Config.STARTER_BRAKE_THRUST.get(),
                Config.STARTER_MAX_SPEED.get() / TICKS_PER_SECOND);
    }

    public double acceleration() {
        return thrust / mass;
    }

    public double brakeDeceleration() {
        return brakeForce / mass;
    }
}
