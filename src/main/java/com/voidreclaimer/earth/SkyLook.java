package com.voidreclaimer.earth;

/**
 * What one {@link SkyEra} looks like: pure data so it can be tested and tuned in one place. Colors are 0xRRGGBB.
 *
 * @param zenith      sky color straight up
 * @param horizon     sky color at and below the horizon
 * @param fog         terrain fog color
 * @param fogFar      distance (blocks) at which terrain fog is fully opaque
 * @param sunHalfSize half the sun disc's edge in sky units (vanilla's sun is 15)
 * @param sunColor    sun tint, 0xRRGGBB
 * @param sunAlpha    sun opacity, 0 to 1
 * @param clouds      whether vanilla clouds are drawn
 */
public record SkyLook(int zenith, int horizon, int fog, float fogFar, float sunHalfSize, int sunColor, float sunAlpha, boolean clouds) {
    private static final SkyLook RUINED = new SkyLook(0x3A2A22, 0x7A5A3C, 0x6B5338, 90f, 17f, 0xFF7340, 0.75f, false);
    private static final SkyLook HEALING = new SkyLook(0x5A7A94, 0xB9A98A, 0xA8A08A, 220f, 13f, 0xFFE6B3, 0.9f, true);
    private static final SkyLook RESTORED = new SkyLook(0x4A86E8, 0xBFD8F5, 0xC0D8FF, 512f, 12f, 0xFFFFFF, 1f, true);

    public static SkyLook of(SkyEra era) {
        return switch (era) {
            case RUINED -> RUINED;
            case HEALING -> HEALING;
            case RESTORED -> RESTORED;
        };
    }

    /** Linear blend of two colors, {@code t} 0 for {@code from} and 1 for {@code to}. */
    public static int mix(int from, int to, float t) {
        int r = Math.round(((from >> 16) & 0xFF) * (1 - t) + ((to >> 16) & 0xFF) * t);
        int g = Math.round(((from >> 8) & 0xFF) * (1 - t) + ((to >> 8) & 0xFF) * t);
        int b = Math.round((from & 0xFF) * (1 - t) + (to & 0xFF) * t);
        return (r << 16) | (g << 8) | b;
    }
}
