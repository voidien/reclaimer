package com.voidreclaimer.earth;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class SectorsTest {
    @Test
    void theGridCoversThePlanetAndNothingElse() {
        int[] columns = new int[Sectors.COUNT];
        for (int x = -RuinedTerrain.HALF; x < RuinedTerrain.HALF; x += 16) {
            for (int z = -RuinedTerrain.HALF; z < RuinedTerrain.HALF; z += 16) {
                int sector = Sectors.at(x, z);
                assertTrue(sector >= 0 && sector < Sectors.COUNT, x + ", " + z);
                columns[sector]++;
            }
        }
        for (int count : columns) {
            assertEquals(16 * 16, count);
        }
        assertEquals(Sectors.NONE, Sectors.at(RuinedTerrain.HALF, 0));
        assertEquals(Sectors.NONE, Sectors.at(0, -RuinedTerrain.HALF - 1));
    }

    @Test
    void sectorsReadLikeAMapFromTheNorthWest() {
        assertEquals(0, Sectors.at(-512, -512));
        assertEquals(3, Sectors.at(511, -512));
        assertEquals(12, Sectors.at(-512, 511));
        assertEquals(15, Sectors.at(511, 511));
        assertEquals(5, Sectors.at(-1, -1));
        assertEquals(10, Sectors.at(0, 0));
    }

    @Test
    void aSectorsBoundsAreItsOwnEdges() {
        for (int sector = 0; sector < Sectors.COUNT; sector++) {
            int minX = Sectors.minX(sector);
            int minZ = Sectors.minZ(sector);
            assertEquals(sector, Sectors.at(minX, minZ));
            assertEquals(sector, Sectors.at(minX + Sectors.SIZE - 1, minZ + Sectors.SIZE - 1));
            assertFalse(Sectors.contains(sector, minX - 1, minZ));
            assertFalse(Sectors.contains(sector, minX, minZ + Sectors.SIZE));
        }
    }

    @Test
    void reachIsTheDistanceToTheFurthestCorner() {
        // Sector 10 covers x and z 0 to 255.
        assertEquals(128, Sectors.reach(10, 127, 127));
        assertEquals(255, Sectors.reach(10, 0, 0));
        assertEquals(255, Sectors.reach(10, 255, 255));
        assertEquals(205, Sectors.reach(10, 50, 200));
    }
}
