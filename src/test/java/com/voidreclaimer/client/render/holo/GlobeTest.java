package com.voidreclaimer.client.render.holo;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.voidreclaimer.earth.Sectors;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

/** The globes' look and the sphere under them, without a client. */
class GlobeTest {
    @Test
    void everySphereCornerIsOnTheSphere() {
        List<SphereMesh.Cell> cells = SphereMesh.of(6, 12);
        assertEquals(72, cells.size());
        for (SphereMesh.Cell cell : cells) {
            float[] c = cell.corners();
            for (int i = 0; i < 12; i += 3) {
                assertEquals(1f, Math.sqrt(c[i] * c[i] + c[i + 1] * c[i + 1] + c[i + 2] * c[i + 2]), 1e-5);
            }
        }
    }

    @Test
    void theTopOfTheSphereIsUp() {
        float[] top = SphereMesh.of(6, 12).get(0).corners();
        assertEquals(1f, top[1], 1e-5);
    }

    @Test
    void everySectorHasTheSameShareOfTheGlobe() {
        int[] cells = new int[Sectors.COUNT];
        for (int row = 0; row < GlobeLook.EARTH_ROWS; row++) {
            for (int col = 0; col < GlobeLook.EARTH_COLS; col++) {
                cells[GlobeLook.sector(row, col)]++;
            }
        }
        for (int count : cells) {
            assertEquals(GlobeLook.EARTH_ROWS * GlobeLook.EARTH_COLS / Sectors.COUNT, count);
        }
    }

    @Test
    void sectorsCountAlongXThenZ() {
        assertEquals(0, GlobeLook.sector(0, 0));
        assertEquals(3, GlobeLook.sector(0, GlobeLook.EARTH_COLS - 1));
        assertEquals(12, GlobeLook.sector(GlobeLook.EARTH_ROWS - 1, 0));
        assertEquals(15, GlobeLook.sector(GlobeLook.EARTH_ROWS - 1, GlobeLook.EARTH_COLS - 1));
    }

    @Test
    void aRestoredSectorLooksDifferent() {
        for (int sector = 0; sector < Sectors.COUNT; sector++) {
            assertTrue(GlobeLook.earthColor(sector, true) != GlobeLook.earthColor(sector, false));
        }
    }

    @Test
    void neighbouringSectorsCanBeToldApart() {
        assertTrue(GlobeLook.earthColor(0, false) != GlobeLook.earthColor(1, false));
        assertTrue(GlobeLook.earthColor(0, false) != GlobeLook.earthColor(4, false));
    }

    @Test
    void theShellFillsWithItsProgress() {
        assertEquals(0, standing(0f));
        assertEquals(GlobeLook.SHELL_ROWS * GlobeLook.SHELL_COLS, standing(1f));
        int half = standing(0.5f);
        int total = GlobeLook.SHELL_ROWS * GlobeLook.SHELL_COLS;
        assertTrue(Math.abs(half - total / 2) < total / 10, "half stands, not " + half + " of " + total);
        assertTrue(standing(0.2f) < standing(0.4f));
    }

    private static int standing(float progress) {
        int count = 0;
        for (int row = 0; row < GlobeLook.SHELL_ROWS; row++) {
            for (int col = 0; col < GlobeLook.SHELL_COLS; col++) {
                if (GlobeLook.panelStands(row, col, progress)) {
                    count++;
                }
            }
        }
        return count;
    }

    @Test
    void theSwarmIsCappedAndGrowsWithItsCount() {
        assertEquals(0, GlobeLook.satellitesShown(0));
        assertEquals(7, GlobeLook.satellitesShown(7));
        assertEquals(GlobeLook.MAX_SATELLITES, GlobeLook.satellitesShown(1L << 40));
    }

    @Test
    void satellitesKeepToTheirOrbitsAndDoNotStack() {
        Set<String> places = new HashSet<>();
        for (int i = 0; i < 50; i++) {
            float[] at = GlobeLook.satellite(i, 40f);
            float radius = (float) Math.sqrt(at[0] * at[0] + at[1] * at[1] + at[2] * at[2]);
            assertTrue(radius >= 1.19f && radius <= 1.51f, "radius " + radius);
            assertEquals(radius, length(GlobeLook.satellite(i, 900f)), 1e-3, "an orbit is a circle");
            places.add(Math.round(at[0] * 100) + "," + Math.round(at[1] * 100));
        }
        assertTrue(places.size() > 45, "satellites stack up: " + places.size());
        assertFalse(GlobeLook.satellite(3, 0f)[0] == GlobeLook.satellite(3, 300f)[0], "a satellite moves");
    }

    private static float length(float[] v) {
        return (float) Math.sqrt(v[0] * v[0] + v[1] * v[1] + v[2] * v[2]);
    }
}
