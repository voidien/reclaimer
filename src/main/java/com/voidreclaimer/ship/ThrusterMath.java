package com.voidreclaimer.ship;

/** Config-free thruster math, so it can be unit-tested. */
public final class ThrusterMath {
    public static final int MAX_SIGNAL = 15;

    private ThrusterMath() {}

    /** Redstone signal to throttle, 0 to 1. */
    public static double throttle(int signal) {
        return Math.max(0, Math.min(MAX_SIGNAL, signal)) / (double) MAX_SIGNAL;
    }

    /** Force of a Chemical Thruster: its base force, scaled by the fuel, the throttle and how well the shaft spins (capped at 1). */
    public static double chemicalForce(double baseForce, double fuelThrust, double throttle, double spin) {
        return baseForce * fuelThrust * throttle * Math.min(1.0, Math.max(0.0, spin));
    }

    /** mB burned this tick: the fuel's rate, scaled the same way, so a thruster that pushes less burns less. */
    public static double burn(double fuelBurn, double throttle, double spin) {
        return fuelBurn * throttle * Math.min(1.0, Math.max(0.0, spin));
    }
}
