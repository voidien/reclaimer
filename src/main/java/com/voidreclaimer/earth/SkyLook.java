package com.voidreclaimer.earth;

import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;

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

    /**
     * The look of a planet {@code restored} of the way healed, 0 to 1: ruined at 0, healing at one half, restored at 1,
     * and a straight line between them, so every restored sector moves the sky and thins the smog.
     */
    public static SkyLook blend(float restored) {
        float t = Mth.clamp(restored, 0f, 1f) * 2f;
        return t <= 1f ? between(RUINED, HEALING, t) : between(HEALING, RESTORED, t - 1f);
    }

    /** What {@code level} shows: the blend for its sectors, or an era a debug command set by hand. */
    public static SkyLook in(Level level) {
        SkyEra era = SkyEra.of(level);
        SectorMap map = SectorMap.of(level);
        return era == SkyEra.forSectors(map.count()) ? blend(map.fraction()) : of(era);
    }

    private static SkyLook between(SkyLook from, SkyLook to, float t) {
        if (t <= 0f) {
            return from;
        }
        if (t >= 1f) {
            return to;
        }
        return new SkyLook(mix(from.zenith, to.zenith, t), mix(from.horizon, to.horizon, t), mix(from.fog, to.fog, t),
                Mth.lerp(t, from.fogFar, to.fogFar), Mth.lerp(t, from.sunHalfSize, to.sunHalfSize), mix(from.sunColor, to.sunColor, t),
                Mth.lerp(t, from.sunAlpha, to.sunAlpha), t < 0.5f ? from.clouds : to.clouds);
    }

    /** Linear blend of two colors, {@code t} 0 for {@code from} and 1 for {@code to}. */
    public static int mix(int from, int to, float t) {
        int r = Math.round(((from >> 16) & 0xFF) * (1 - t) + ((to >> 16) & 0xFF) * t);
        int g = Math.round(((from >> 8) & 0xFF) * (1 - t) + ((to >> 8) & 0xFF) * t);
        int b = Math.round((from & 0xFF) * (1 - t) + (to & 0xFF) * t);
        return (r << 16) | (g << 8) | b;
    }
}
