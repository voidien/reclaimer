package com.voidreclaimer.earth;

/**
 * How much of the ruined atmosphere is left, pure. Smog is the sky's fog ({@link SkyLook#blend}); Acid Drizzle falls
 * everywhere but over restored ground, thinner with every sector restored.
 */
public final class EarthAtmosphere {
    private EarthAtmosphere() {}

    /** Acid Drizzle at the column (x, z), 0 to 1: none in a restored sector, otherwise what the planet has left. */
    public static float drizzle(SectorMap map, int x, int z) {
        return map.isRestored(Sectors.at(x, z)) ? 0f : 1f - map.fraction();
    }

    /** Condition one second outdoors takes from each section, given the config's rate per minute. */
    public static float wearPerSecond(float strength, double perMinute) {
        return (float) (strength * perMinute / 60.0);
    }

    /** Drops to draw in one client tick, at most {@code max}; a strength above 0 always draws one. */
    public static int drops(float strength, int max) {
        return strength <= 0f ? 0 : Math.max(1, Math.round(strength * max));
    }
}
