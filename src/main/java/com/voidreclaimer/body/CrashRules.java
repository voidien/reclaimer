package com.voidreclaimer.body;

/**
 * How hard a crash is, in blocks a second along what was hit. Pure, so it can be unit tested. The owner's numbers: under the safe speed
 * nothing happens, and above it things grow with the square of the speed over it, as energy does, so a clip is a scratch and a head-on hit
 * at top speed is the end of a body.
 */
public final class CrashRules {
    /** The speed where a block of hardness 1 starts to go, for a body of {@link #PLAIN_MASS}, over the safe speed. */
    private static final double BREAKING_SPEED = 10.0;
    private static final double PLAIN_MASS = 10.0;

    private CrashRules() {}

    /** The share of a body's health a hit takes: 0 to the safe speed, then growing to {@code atTop} at the body's top speed, and on past it. */
    public static double healthShare(double speed, double topSpeed, double safe, double atTop) {
        if (speed <= safe) {
            return 0.0;
        }
        // A body that can barely move isn't made of glass: it goes by the safe speed.
        double over = (speed - safe) / Math.max(topSpeed - safe, safe);
        return over * over * atTop;
    }

    /** The hardest block a hit breaks. Mass counts for less than speed: its square root. */
    public static double hardness(double speed, double mass, double safe) {
        if (speed <= safe) {
            return -1.0;
        }
        double over = (speed - safe) / BREAKING_SPEED;
        return over * over * heft(mass);
    }

    /** How many blocks a hit breaks at most, however many it reaches. */
    public static int blocks(double speed, double mass, double safe, int most) {
        return speed <= safe ? 0 : Math.min(most, (int) Math.ceil((speed - safe) * heft(mass)));
    }

    private static double heft(double mass) {
        return Math.sqrt(Math.max(mass, 0.0) / PLAIN_MASS);
    }
}
