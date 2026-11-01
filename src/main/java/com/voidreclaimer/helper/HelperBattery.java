package com.voidreclaimer.helper;

import net.minecraft.util.Mth;

/**
 * Pure battery math of a helper drone. A battery is a share of full, 0 to 1, so changing {@code helper.batteryCapacity}
 * never spoils a saved drone. Nothing here stops a drone: below {@code low} its speed and work fade towards {@code flat},
 * and a flat battery still runs at {@code flat}, because the only hazard in this game is time. The fade is continuous, so a
 * drone living on what the sun gives settles at a steady speed instead of stuttering between full and flat.
 */
public final class HelperBattery {
    private HelperBattery() {}

    /** The share of full speed and work a battery at {@code share} gives. */
    public static float power(float share, float low, float flat) {
        if (share >= low) {
            return 1f;
        }
        return flat + (1f - flat) * Math.max(0f, share) / low;
    }

    /** What the battery takes in per tick: {@code sun} is 0 to 1 sunlight, {@code atCore} whether a Station Core is in range. */
    public static float gain(float sun, boolean atCore, float sunCharge, float coreCharge) {
        return sun * sunCharge + (atCore ? coreCharge : 0f);
    }

    /** The share after one tick. */
    public static float next(float share, float gain, float drain, float capacity) {
        return Mth.clamp(share + (gain - drain) / capacity, 0f, 1f);
    }
}
