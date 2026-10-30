package com.voidreclaimer.core;

/** Pure helpers for the respawn cards on the death screen, kept free of rendering classes so they can be unit-tested. */
public final class RespawnCards {
    public static final char FULL = '●';
    public static final char EMPTY = '○';

    private RespawnCards() {}

    /** Charge pips such as "●●○": {@code charges} full pips out of {@code max}. Out-of-range values are clamped. */
    public static String pips(int charges, int max) {
        int total = Math.max(0, max);
        int full = Math.max(0, Math.min(total, charges));
        return String.valueOf(FULL).repeat(full) + String.valueOf(EMPTY).repeat(total - full);
    }

    /** Whole blocks between the death spot and a core, for "123 m away". */
    public static int blocks(double dx, double dy, double dz) {
        return (int) Math.round(Math.sqrt(dx * dx + dy * dy + dz * dz));
    }
}
