package com.voidreclaimer.launch;

/**
 * Pure code animation of the launch complex's model, a placeholder until the owner animates it: the gun tower leans
 * back as the wind-up builds and snaps forward with a recoil when it fires, the two dishes turn slowly in opposite
 * directions, and the cranes sway. Angles are in degrees, time in ticks.
 */
public final class LaunchAnimation {
    /** How far the gun tower leans at a full wind-up. */
    public static final float WINDUP_DEGREES = -12f;
    /** The recoil kick right after a launch, decaying over {@link #RECOIL_TICKS}. */
    public static final float RECOIL_DEGREES = 6f;
    public static final int RECOIL_TICKS = 20;
    public static final float DISH_A_DEGREES_PER_TICK = 0.6f;
    public static final float DISH_B_DEGREES_PER_TICK = -0.45f;
    public static final float CRANE_SWAY_DEGREES = 4f;
    public static final float CRANE_PERIOD_TICKS = 200f;

    private LaunchAnimation() {}

    /** Bone rotations: the gun about X, the dishes about Y, the cranes about Y. */
    public record Pose(float gunPitch, float dishA, float dishB, float crane, float crane2) {}

    /**
     * The gun's lean for a wind-up {@code progress} (0 to 1) and {@code ticksSinceLaunch} (negative when it has never
     * fired): the lean eases in with the wind-up, and a launch adds a kick that fades out.
     */
    public static float gunPitch(float progress, float ticksSinceLaunch) {
        float p = Math.max(0f, Math.min(1f, progress));
        float smooth = p * p * (3f - 2f * p);
        float pitch = WINDUP_DEGREES * smooth;
        if (ticksSinceLaunch >= 0f && ticksSinceLaunch < RECOIL_TICKS) {
            float k = 1f - ticksSinceLaunch / RECOIL_TICKS;
            pitch += RECOIL_DEGREES * k * k;
        }
        return pitch;
    }

    /** A dish's heading at {@code time}, kept within 0 to 360. */
    public static float dish(float time, float degreesPerTick) {
        float angle = (time * degreesPerTick) % 360f;
        return angle < 0f ? angle + 360f : angle;
    }

    /** The cranes' sway at {@code time}; the second crane is half a period behind. */
    public static float crane(float time, float phaseTicks) {
        return CRANE_SWAY_DEGREES * (float) Math.sin((time + phaseTicks) / CRANE_PERIOD_TICKS * 2.0 * Math.PI);
    }

    public static Pose at(float progress, float ticksSinceLaunch, float time) {
        return new Pose(gunPitch(progress, ticksSinceLaunch), dish(time, DISH_A_DEGREES_PER_TICK), dish(time, DISH_B_DEGREES_PER_TICK),
                crane(time, 0f), crane(time, CRANE_PERIOD_TICKS / 2f));
    }
}
