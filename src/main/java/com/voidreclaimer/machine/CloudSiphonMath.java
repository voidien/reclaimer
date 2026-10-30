package com.voidreclaimer.machine;

/** Config-free math of the Cloud Siphon, so it can be unit-tested. */
public final class CloudSiphonMath {
    private CloudSiphonMath() {}

    /**
     * Raw items per second: nothing in thin dust (below {@code minDensity}) or without power, otherwise
     * {@code baseRate} at full density and power 1, in proportion to both.
     */
    public static float itemsPerSecond(double density, float power, double minDensity, double baseRate) {
        if (density < minDensity || density <= 0.0 || power <= 0f) {
            return 0f;
        }
        return (float) (density * power * baseRate);
    }

    /**
     * What stored Forge Energy adds to the shaft's {@code power} this tick: it tops the siphon up towards full power (1.0)
     * and only takes the energy that top-up needs, so a shaft that already gives full power uses none.
     *
     * @param power       the shaft's power (after any addon event)
     * @param stored      FE in the buffer
     * @param feForFull   FE per tick a siphon with no shaft at all needs to run at full power; 0 turns the energy input off
     * @return the power to run at and the FE to take from the buffer
     */
    public static EnergyUse topUp(float power, int stored, int feForFull) {
        if (feForFull <= 0 || stored <= 0 || power >= 1f) {
            return new EnergyUse(Math.max(0f, power), 0);
        }
        int wanted = (int) Math.ceil((1f - Math.max(0f, power)) * feForFull);
        int used = Math.min(stored, wanted);
        return new EnergyUse(Math.min(1f, Math.max(0f, power) + used / (float) feForFull), used);
    }

    /** Power to run at, and the energy that cost. */
    public record EnergyUse(float power, int drained) {}
}
