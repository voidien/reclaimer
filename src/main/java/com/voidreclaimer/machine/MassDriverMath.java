package com.voidreclaimer.machine;

/** Config-free math of the Kinetic Mass Driver and the Quantum Receiver, so it can be unit-tested. */
public final class MassDriverMath {
    private MassDriverMath() {}

    /**
     * Ticks between two launches at {@code rpm}: {@code slowTicks} at the minimum speed, falling linearly to
     * {@code fastTicks} at {@code fastRpm} and beyond. -1 when the drive turns too slowly to launch.
     */
    public static int launchTicks(float rpm, int minRpm, int fastRpm, int slowTicks, int fastTicks) {
        float speed = Math.abs(rpm);
        if (speed < minRpm) {
            return -1;
        }
        if (fastRpm <= minRpm || speed >= fastRpm) {
            return Math.min(slowTicks, fastTicks);
        }
        float t = (speed - minRpm) / (fastRpm - minRpm);
        return Math.round(slowTicks + (fastTicks - slowTicks) * t);
    }

    /** How much of a launch one tick completes at the given speed; 0 when the drive is too slow. */
    public static float progressPerTick(float rpm, int minRpm, int fastRpm, int slowTicks, int fastTicks) {
        int ticks = launchTicks(rpm, minRpm, fastRpm, slowTicks, fastTicks);
        return ticks <= 0 ? 0f : 1f / ticks;
    }

    /** Fraction (0-1) of the Receiver's full output: linear in the trusses launched, full at the completed shell. */
    public static float receiverFraction(long trusses, long shellTotal) {
        if (shellTotal <= 0 || trusses <= 0) {
            return 0f;
        }
        return (float) Math.min(1.0, (double) trusses / shellTotal);
    }

    /** Stress capacity per RPM so that {@code rpm} gives {@code maxStress} SU at the full fraction. */
    public static float receiverCapacityPerRpm(long trusses, long shellTotal, double maxStress, int rpm) {
        return rpm <= 0 ? 0f : (float) (maxStress / rpm * receiverFraction(trusses, shellTotal));
    }
}
