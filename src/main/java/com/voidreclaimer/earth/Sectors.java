package com.voidreclaimer.earth;

/**
 * The planet's sectors, pure: a {@value #GRID} x {@value #GRID} grid of {@value #SIZE}-block squares over the whole of
 * {@link RuinedTerrain}'s planet. A sector's number is {@code row * GRID + column}, with column 0 at the west edge and row 0
 * at the north edge, so the number reads like the map on a Planetary Grid.
 */
public final class Sectors {
    public static final int GRID = 4;
    public static final int COUNT = GRID * GRID;
    public static final int SIZE = 2 * RuinedTerrain.HALF / GRID;
    /** Not on the planet. */
    public static final int NONE = -1;

    private Sectors() {}

    /** The sector holding the column at (x, z), or {@link #NONE} off the planet. */
    public static int at(int x, int z) {
        if (!RuinedTerrain.onPlanet(x, z)) {
            return NONE;
        }
        return Math.floorDiv(z + RuinedTerrain.HALF, SIZE) * GRID + Math.floorDiv(x + RuinedTerrain.HALF, SIZE);
    }

    public static int column(int sector) {
        return sector % GRID;
    }

    public static int row(int sector) {
        return sector / GRID;
    }

    /** The lowest x inside the sector; the highest is {@code minX + SIZE - 1}. */
    public static int minX(int sector) {
        return column(sector) * SIZE - RuinedTerrain.HALF;
    }

    /** The lowest z inside the sector; the highest is {@code minZ + SIZE - 1}. */
    public static int minZ(int sector) {
        return row(sector) * SIZE - RuinedTerrain.HALF;
    }

    public static boolean contains(int sector, int x, int z) {
        return at(x, z) == sector;
    }

    /** How far (in the larger of x and z) the sector's furthest column lies from (x, z). */
    public static int reach(int sector, int x, int z) {
        int east = minX(sector) + SIZE - 1 - x;
        int west = x - minX(sector);
        int south = minZ(sector) + SIZE - 1 - z;
        int north = z - minZ(sector);
        return Math.max(0, Math.max(Math.max(east, west), Math.max(south, north)));
    }
}
