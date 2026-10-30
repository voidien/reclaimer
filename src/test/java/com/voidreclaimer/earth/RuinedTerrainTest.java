package com.voidreclaimer.earth;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class RuinedTerrainTest {
    private static final long SEED = 123456789L;

    @Test
    void planetIsTheFourBySectorGrid() {
        assertTrue(RuinedTerrain.onPlanet(0, 0));
        assertTrue(RuinedTerrain.onPlanet(-512, -512));
        assertTrue(RuinedTerrain.onPlanet(511, 511));
        assertFalse(RuinedTerrain.onPlanet(512, 0));
        assertFalse(RuinedTerrain.onPlanet(0, -513));
    }

    @Test
    void sameSeedSameTerrainDifferentSeedDifferentTerrain() {
        boolean differs = false;
        for (int x = -400; x < 400; x += 7) {
            assertEquals(RuinedTerrain.surfaceY(SEED, x, 33), RuinedTerrain.surfaceY(SEED, x, 33));
            differs |= RuinedTerrain.surfaceY(SEED, x, 33) != RuinedTerrain.surfaceY(SEED + 1, x, 33);
        }
        assertTrue(differs);
    }

    @Test
    void landingAreaIsFlatAtBaseHeight() {
        assertEquals(RuinedTerrain.BASE_Y, RuinedTerrain.surfaceY(SEED, 0, 0));
        for (int x = -8; x <= 8; x++) {
            for (int z = -8; z <= 8; z++) {
                assertTrue(Math.abs(RuinedTerrain.surfaceY(SEED, x, z) - RuinedTerrain.BASE_Y) <= 2, x + "," + z);
            }
        }
    }

    @Test
    void cratersDigDeepAndHillsStayBounded() {
        int lowest = Integer.MAX_VALUE;
        int highest = Integer.MIN_VALUE;
        for (int x = -RuinedTerrain.HALF; x < RuinedTerrain.HALF; x += 3) {
            for (int z = -RuinedTerrain.HALF; z < RuinedTerrain.HALF; z += 3) {
                int y = RuinedTerrain.surfaceY(SEED, x, z);
                lowest = Math.min(lowest, y);
                highest = Math.max(highest, y);
            }
        }
        assertTrue(lowest <= RuinedTerrain.BASE_Y - 10, "no crater found, lowest " + lowest);
        assertTrue(highest <= RuinedTerrain.BASE_Y + 12, "hills too tall: " + highest);
        assertTrue(lowest >= RuinedTerrain.BASE_Y - 40, "craters too deep: " + lowest);
    }
}
