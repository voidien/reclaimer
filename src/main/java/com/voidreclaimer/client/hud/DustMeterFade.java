package com.voidreclaimer.client.hud;

/**
 * When the dust meter is bright and when it dims, kept free of rendering classes so it can be unit-tested. The meter stays
 * fully visible while dust changes, while the player is inside a cloud and while a screen is open; after
 * {@link #HOLD_TICKS} without any of that it fades to {@link #MIN_ALPHA} over {@link #FADE_TICKS}.
 */
public final class DustMeterFade {
    public static final long HOLD_TICKS = 200;
    public static final long FADE_TICKS = 40;
    public static final float MIN_ALPHA = 0.35f;

    private DustMeterFade() {}

    public static float alpha(long ticksSinceChange, boolean active) {
        if (active || ticksSinceChange <= HOLD_TICKS) {
            return 1f;
        }
        float t = Math.min(1f, (ticksSinceChange - HOLD_TICKS) / (float) FADE_TICKS);
        return 1f - (1f - MIN_ALPHA) * t;
    }
}
