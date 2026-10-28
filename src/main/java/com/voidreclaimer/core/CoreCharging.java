package com.voidreclaimer.core;

import com.voidreclaimer.replicator.DustCost;

/** Pure Backup Core charge rules. */
public final class CoreCharging {
    public static final int MAX_CHARGES = 3;
    /** Dust for one instant charge. */
    public static final DustCost CHARGE_COST = new DustCost(10, 10, 0, 0);

    private CoreCharging() {}

    public static final int FULL_SKY_LIGHT = 15;

    /**
     * Sunlight trickle for one update. Progress counts light-ticks (ticks × sky light level), so dim light charges
     * proportionally slower without rounding drift.
     *
     * @param charges  current charges
     * @param progress accumulated light-ticks toward the next charge
     * @param ticks    ticks since the last update
     * @param skyLight effective sky light 0..15 (deep space's distant sun gives 10)
     * @param needed   ticks per charge at full light
     */
    public static State sunTick(int charges, int progress, int ticks, int skyLight, int needed) {
        if (charges >= MAX_CHARGES) {
            return new State(MAX_CHARGES, 0);
        }
        long next = (long) progress + (long) ticks * Math.max(0, Math.min(FULL_SKY_LIGHT, skyLight));
        long neededLight = (long) needed * FULL_SKY_LIGHT;
        if (next >= neededLight) {
            return new State(charges + 1, (int) Math.min(next - neededLight, neededLight - 1));
        }
        return new State(charges, (int) next);
    }

    /** Fraction 0..1 of the way to the next charge. */
    public static float fraction(int progress, int needed) {
        return Math.min(1f, progress / (float) ((long) needed * FULL_SKY_LIGHT));
    }

    /** @param progress light-ticks toward the next charge */
    public record State(int charges, int progress) {}
}
