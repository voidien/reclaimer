package com.voidreclaimer.client.render;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.voidreclaimer.client.render.body.BodyLight;
import org.junit.jupiter.api.Test;

class BodyLightTest {
    private static int pack(int block, int sky) {
        return block << 4 | sky << 20;
    }

    @Test
    void darkLightIsLiftedToTheMinimum() {
        assertEquals(pack(8, 8), BodyLight.atLeast(pack(0, 0), 8));
        assertEquals(pack(8, 10), BodyLight.atLeast(pack(0, 10), 8));
    }

    @Test
    void brighterLightIsNeverDarkened() {
        assertEquals(pack(15, 12), BodyLight.atLeast(pack(15, 12), 8));
        assertEquals(pack(12, 15), BodyLight.atLeast(pack(12, 15), 0));
    }

    @Test
    void zeroMinimumLeavesLightAlone() {
        assertEquals(pack(0, 0), BodyLight.atLeast(pack(0, 0), 0));
    }

    @Test
    void minimumIsClampedToValidLevels() {
        assertEquals(pack(15, 15), BodyLight.atLeast(pack(0, 0), 99));
        assertEquals(pack(3, 4), BodyLight.atLeast(pack(3, 4), -5));
    }
}
