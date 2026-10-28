package com.voidreclaimer.nanite;

import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

/** Pure Nanite Swarm targeting: where the next block goes, and the adjustable build distance. */
public final class NaniteTargeting {
    public static final int MIN_RANGE = 2;
    public static final int MAX_RANGE = 6;
    public static final int DEFAULT_RANGE = 3;

    private NaniteTargeting() {}

    /** The block {@code range} blocks ahead of the eyes along the look direction. */
    public static BlockPos target(Vec3 eyes, Vec3 look, int range) {
        return BlockPos.containing(eyes.add(look.normalize().scale(clampRange(range))));
    }

    /** One step closer or farther per scroll notch, kept within {@link #MIN_RANGE}..{@link #MAX_RANGE}. */
    public static int adjustRange(int range, double scrollDelta) {
        if (scrollDelta == 0.0) {
            return clampRange(range);
        }
        return clampRange(range + (scrollDelta > 0 ? 1 : -1));
    }

    public static int clampRange(int range) {
        return Mth.clamp(range, MIN_RANGE, MAX_RANGE);
    }
}
