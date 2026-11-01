package com.voidreclaimer.earth;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class SectorMapTest {
    @Test
    void restoringASectorIsCountedOnce() {
        SectorMap map = SectorMap.NONE.with(3).with(3).with(15);
        assertTrue(map.isRestored(3) && map.isRestored(15));
        assertFalse(map.isRestored(4));
        assertEquals(2, map.count());
        assertEquals(2 / 16f, map.fraction());
    }

    @Test
    void offThePlanetIsNeverRestored() {
        assertSame(SectorMap.NONE, SectorMap.NONE.with(Sectors.NONE));
        assertSame(SectorMap.NONE, SectorMap.NONE.with(Sectors.COUNT));
        assertFalse(SectorMap.NONE.isRestored(Sectors.NONE));
    }

    @Test
    void aSavedValueCannotHoldMoreThanTheGrid() {
        assertEquals(16, new SectorMap(-1).count());
    }

    @Test
    void theEraFollowsTheCount() {
        assertEquals(SkyEra.RUINED, SkyEra.forSectors(0));
        assertEquals(SkyEra.HEALING, SkyEra.forSectors(1));
        assertEquals(SkyEra.HEALING, SkyEra.forSectors(Sectors.COUNT - 1));
        assertEquals(SkyEra.RESTORED, SkyEra.forSectors(Sectors.COUNT));
    }
}
