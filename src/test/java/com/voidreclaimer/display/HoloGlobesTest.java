package com.voidreclaimer.display;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;

class HoloGlobesTest {
    private static final HoloGlobes.Progress PROGRESS = new HoloGlobes.Progress(12, 34, 3, 0.25f, 0b101, 1);

    @Test
    void theDysonCaptionShowsItsCounters() {
        assertEquals(List.of("DYSON SPHERE  TIER 3", "Satellites 12", "Trusses 34  Shell 25%"), HoloGlobes.caption(HoloGlobes.DYSON, PROGRESS));
    }

    @Test
    void theEarthCaptionCountsRestoredSectors() {
        assertEquals(List.of("RUINED EARTH", "Restored 2 / 16"), HoloGlobes.caption(HoloGlobes.EARTH, PROGRESS));
    }

    @Test
    void sectorBitsAreReadByIndex() {
        assertTrue(PROGRESS.isRestored(0));
        assertFalse(PROGRESS.isRestored(1));
        assertTrue(PROGRESS.isRestored(2));
        assertEquals(com.voidreclaimer.earth.Sectors.COUNT, new HoloGlobes.Progress(0, 0, 0, 0, -1, 0).restoredCount(), "bits past the grid do not count");
    }
}
