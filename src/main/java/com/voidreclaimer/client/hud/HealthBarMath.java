package com.voidreclaimer.client.hud;

import java.util.ArrayList;
import java.util.List;

/**
 * Sizes of the single health bar, kept free of rendering classes so they can be unit-tested. A bigger body gets a longer
 * and taller bar (on a log scale, so the Drone's 20 health and the Titan's hundreds are both readable) and one notch per
 * {@link #SEGMENT} health, so a Titan's bar looks like a long armoured plate and a Drone's like a thin strip.
 */
public final class HealthBarMath {
    /** Health of the starter body (ten hearts), the smallest bar. */
    public static final float BASE = 20f;
    /** Max health at which the bar reaches its full size. */
    public static final float HUGE = 1000f;
    public static final int MIN_WIDTH = 60;
    /** The width of the hotbar. */
    public static final int MAX_WIDTH = 182;
    public static final int MIN_HEIGHT = 5;
    public static final int MAX_HEIGHT = 9;
    /** Health between two notches. */
    public static final float SEGMENT = 100f;
    /** Notches closer than this many pixels are left out, so a huge bar doesn't turn into a comb. */
    public static final int MIN_NOTCH_GAP = 3;

    private HealthBarMath() {}

    /** 0 for the starter body up to 1 for a huge one, on a log scale. */
    public static float size(float maxHealth) {
        if (maxHealth <= BASE) {
            return 0f;
        }
        float t = (float) (Math.log(maxHealth / BASE) / Math.log(HUGE / BASE));
        return Math.min(1f, t);
    }

    public static int width(float maxHealth) {
        return MIN_WIDTH + Math.round((MAX_WIDTH - MIN_WIDTH) * size(maxHealth));
    }

    public static int height(float maxHealth) {
        return MIN_HEIGHT + Math.round((MAX_HEIGHT - MIN_HEIGHT) * size(maxHealth));
    }

    public static int segments(float maxHealth) {
        return Math.max(1, (int) Math.ceil(maxHealth / SEGMENT));
    }

    /** Pixel offsets (from the bar's left edge) of the notches between segments; empty when they would be too tight. */
    public static List<Integer> notches(float maxHealth, int width) {
        int segments = segments(maxHealth);
        List<Integer> xs = new ArrayList<>();
        if (segments < 2 || (float) width / segments < MIN_NOTCH_GAP) {
            return xs;
        }
        for (int i = 1; i < segments; i++) {
            xs.add(Math.round((float) width * i * SEGMENT / maxHealth));
        }
        xs.removeIf(x -> x <= 0 || x >= width);
        return xs;
    }

    /** Width of the filled part: proportional, and at least one pixel while any health is left. */
    public static int fill(float health, float maxHealth, int width) {
        if (health <= 0f || maxHealth <= 0f) {
            return 0;
        }
        return Math.max(1, Math.min(width, Math.round(width * health / maxHealth)));
    }
}
