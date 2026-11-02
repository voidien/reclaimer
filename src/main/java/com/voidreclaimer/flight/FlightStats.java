package com.voidreclaimer.flight;

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

    /** Current handling for a player, from the assembled body and any burn in progress (both sides; the client has the synced body). */
    public static FlightStats of(Player player) {
        FlightStats stats = com.voidreclaimer.body.Bodies.stats(player).flight();
        float factor = player.getData(com.voidreclaimer.registry.ModAttachments.AFTERBURNER).speedFactor();
        return factor > 1f ? new FlightStats(stats.mass, stats.thrust, stats.brakeForce, stats.maxSpeed * factor) : stats;
    }

    public double acceleration() {
        return thrust / mass;
    }

    public double brakeDeceleration() {
        return brakeForce / mass;
    }
}
