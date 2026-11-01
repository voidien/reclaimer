package com.voidreclaimer.client.hud;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.voidreclaimer.earth.Sectors;
import org.junit.jupiter.api.Test;

class PlanetaryGridLayoutTest {
    private static final int WIDTH = 427;
    private static final int HEIGHT = 240;

    @Test
    void theMapFitsTheSmallestScreenAtTheRightEdge() {
        assertEquals(WIDTH - PlanetaryGridLayout.MARGIN, PlanetaryGridLayout.left(WIDTH) + PlanetaryGridLayout.SIZE);
        assertTrue(PlanetaryGridLayout.top(HEIGHT) > 0);
        assertTrue(PlanetaryGridLayout.top(HEIGHT) + PlanetaryGridLayout.SIZE < HEIGHT);
    }

    @Test
    void northIsUpAndWestIsLeft() {
        assertEquals(PlanetaryGridLayout.left(WIDTH), PlanetaryGridLayout.cellX(Sectors.at(-512, -512), WIDTH));
        assertEquals(PlanetaryGridLayout.top(HEIGHT), PlanetaryGridLayout.cellY(Sectors.at(-512, -512), HEIGHT));
        assertTrue(PlanetaryGridLayout.cellX(Sectors.at(300, 0), WIDTH) > PlanetaryGridLayout.cellX(Sectors.at(-300, 0), WIDTH));
        assertTrue(PlanetaryGridLayout.cellY(Sectors.at(0, 300), HEIGHT) > PlanetaryGridLayout.cellY(Sectors.at(0, -300), HEIGHT));
    }

    @Test
    void cellsDoNotTouch() {
        int step = PlanetaryGridLayout.cellX(1, WIDTH) - PlanetaryGridLayout.cellX(0, WIDTH);
        assertEquals(PlanetaryGridLayout.CELL + PlanetaryGridLayout.GAP, step);
        assertEquals(PlanetaryGridLayout.SIZE, PlanetaryGridLayout.cellX(Sectors.COUNT - 1, WIDTH) + PlanetaryGridLayout.CELL - PlanetaryGridLayout.left(WIDTH));
    }
}
