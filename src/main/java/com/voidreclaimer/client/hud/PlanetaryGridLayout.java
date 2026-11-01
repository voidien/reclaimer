package com.voidreclaimer.client.hud;

import com.voidreclaimer.earth.Sectors;

/** Where the Planetary Grid's map sits on the screen, pure: at the right edge, centred, north at the top. */
public final class PlanetaryGridLayout {
    public static final int CELL = 18;
    public static final int GAP = 1;
    /** The map's width and height. */
    public static final int SIZE = Sectors.GRID * CELL + (Sectors.GRID - 1) * GAP;
    public static final int MARGIN = 8;

    private PlanetaryGridLayout() {}

    public static int left(int screenWidth) {
        return screenWidth - SIZE - MARGIN;
    }

    public static int top(int screenHeight) {
        return (screenHeight - SIZE) / 2;
    }

    /** The left edge of a sector's cell. */
    public static int cellX(int sector, int screenWidth) {
        return left(screenWidth) + Sectors.column(sector) * (CELL + GAP);
    }

    /** The top edge of a sector's cell. */
    public static int cellY(int sector, int screenHeight) {
        return top(screenHeight) + Sectors.row(sector) * (CELL + GAP);
    }
}
