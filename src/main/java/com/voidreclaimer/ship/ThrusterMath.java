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

    /**
     * What an Ion Thruster draws and pushes this tick: the throttle sets both, and a buffer that cannot cover the draw gives
     * a proportional push instead of none.
     */
    public static IonDraw ionDraw(double baseForce, int fePerTick, double throttle, int stored) {
        int wanted = (int) Math.ceil(fePerTick * Math.max(0.0, Math.min(1.0, throttle)));
        if (wanted <= 0 || stored <= 0) {
            return new IonDraw(0.0, 0);
        }
        int used = Math.min(stored, wanted);
        return new IonDraw(baseForce * throttle * used / (double) wanted, used);
    }

    /** The force to push with and the Forge Energy that cost. */
    public record IonDraw(double force, int energy) {}

    /** mB burned this tick: the fuel's rate, scaled the same way, so a thruster that pushes less burns less. */
    public static double burn(double fuelBurn, double throttle, double spin) {
        return fuelBurn * throttle * Math.min(1.0, Math.max(0.0, spin));
    }
}
