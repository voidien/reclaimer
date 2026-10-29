package com.voidreclaimer.machine;

/** Config-free math of the kinetic machines, so it can be unit-tested. */
public final class MachineMath {
    /** Rotation speed at which a cycle takes exactly the configured number of ticks. */
    public static final float REFERENCE_RPM = 64f;

    private MachineMath() {}

    /** Whether a machine turning at {@code rpm} (either direction) works at all. */
    public static boolean runs(float rpm, int minRpm) {
        return Math.abs(rpm) >= minRpm;
    }

    /** How much of a cycle one tick completes: 1 at {@link #REFERENCE_RPM}, proportional to speed, 0 below the minimum. */
    public static float progressPerTick(float rpm, int minRpm) {
        return runs(rpm, minRpm) ? Math.abs(rpm) / REFERENCE_RPM : 0f;
    }

    /** Ticks one cycle takes at {@code rpm}, rounded up; -1 when the machine doesn't run. */
    public static int cycleTicks(float rpm, int minRpm, int baseTicks) {
        float perTick = progressPerTick(rpm, minRpm);
        return perTick <= 0f ? -1 : (int) Math.ceil(baseTicks / perTick);
    }
}
