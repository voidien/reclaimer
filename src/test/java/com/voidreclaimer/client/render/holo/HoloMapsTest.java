package com.voidreclaimer.client.render.holo;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

/** The parts of a map's texture that need no client. */
class HoloMapsTest {
    @Test
    void aNewMapTakesTheSlotUnusedLongest() {
        assertEquals(1, HoloMaps.pick(new long[] {5, 2, 7}, 9));
    }

    @Test
    void aSlotInUseThisTickIsNotTaken() {
        assertEquals(1, HoloMaps.pick(new long[] {9, 2, 9}, 9));
    }

    @Test
    void withEverySlotInUseThereIsNone() {
        assertEquals(-1, HoloMaps.pick(new long[] {9, 9}, 9));
    }

    @Test
    void clearAirStaysClearOutsideACloud() {
        assertEquals(0, HoloMaps.over(0, 0));
    }

    @Test
    void aDenserCloudIsMoreOpaqueOverClearAir() {
        int thin = HoloMaps.over(0, 0.25) >>> 24;
        int thick = HoloMaps.over(0, 1) >>> 24;
        assertEquals(180, thick);
        assertEquals(45, thin);
    }

    @Test
    void aCloudTintsABlockAndKeepsItOpaque() {
        int red = 0xFF0000FF;
        int tinted = HoloMaps.over(red, 1);
        assertEquals(0xFF, tinted >>> 24);
        // ABGR: the red falls towards the cloud's, the blue rises towards it.
        assertEquals(0xFF * 75 / 255 + 0x66 * 180 / 255, tinted & 0xFF, 1);
    }
}
