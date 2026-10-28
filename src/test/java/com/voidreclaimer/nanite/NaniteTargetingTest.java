package com.voidreclaimer.nanite;

import static org.junit.jupiter.api.Assertions.assertEquals;

import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.Test;

class NaniteTargetingTest {
    @Test
    void targetIsRangeBlocksAlongTheLook() {
        Vec3 eyes = new Vec3(0.5, 64.5, 0.5);
        assertEquals(new BlockPos(0, 64, 3), NaniteTargeting.target(eyes, new Vec3(0, 0, 1), 3));
        assertEquals(new BlockPos(-6, 64, 0), NaniteTargeting.target(eyes, new Vec3(-2, 0, 0), 6), "look vector is normalized");
        assertEquals(new BlockPos(0, 66, 0), NaniteTargeting.target(eyes, new Vec3(0, 1, 0), 1), "range is clamped to the minimum");
    }

    @Test
    void scrollAdjustsRangeWithinBounds() {
        assertEquals(4, NaniteTargeting.adjustRange(3, 1.0));
        assertEquals(2, NaniteTargeting.adjustRange(3, -0.5));
        assertEquals(NaniteTargeting.MAX_RANGE, NaniteTargeting.adjustRange(NaniteTargeting.MAX_RANGE, 1.0));
        assertEquals(NaniteTargeting.MIN_RANGE, NaniteTargeting.adjustRange(NaniteTargeting.MIN_RANGE, -1.0));
        assertEquals(3, NaniteTargeting.adjustRange(3, 0.0));
    }
}
