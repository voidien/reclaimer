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
}
