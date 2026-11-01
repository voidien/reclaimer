package com.voidreclaimer.earth;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class SkyLookBlendTest {
    @Test
    void theThreeErasAreTheStopsOfTheBlend() {
        assertEquals(SkyLook.of(SkyEra.RUINED), SkyLook.blend(0f));
        assertEquals(SkyLook.of(SkyEra.HEALING), SkyLook.blend(0.5f));
        assertEquals(SkyLook.of(SkyEra.RESTORED), SkyLook.blend(1f));
    }

    @Test
    void everyRestoredSectorThinsTheSmog() {
        float last = SkyLook.blend(0f).fogFar();
        for (int count = 1; count <= Sectors.COUNT; count++) {
            float fogFar = SkyLook.blend(count / (float) Sectors.COUNT).fogFar();
            assertTrue(fogFar > last, "sector " + count + ": " + fogFar + " after " + last);
            last = fogFar;
        }
    }

    @Test
    void cloudsComeBackPartWayToHealing() {
        assertFalse(SkyLook.blend(1 / 16f).clouds());
        assertTrue(SkyLook.blend(5 / 16f).clouds());
    }
}
