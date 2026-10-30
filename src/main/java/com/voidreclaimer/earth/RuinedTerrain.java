package com.voidreclaimer.earth;

import net.minecraft.util.Mth;

/**
 * The shape of the ruined planet: a 1024 x 1024 square (the 4 x 4 grid of 256-block Spire sectors) of low ashen hills
 * pocked with craters. Pure functions of the world seed and a column, so client, server and tests agree and no chunk
 * needs its neighbours.
 */
public final class RuinedTerrain {
    /** The planet covers x and z from -HALF to HALF - 1. */
    public static final int HALF = 512;
    public static final int BASE_Y = 64;
    private static final int HILL_AMPLITUDE = 9;
    private static final int HILL_SCALE = 56;
    private static final int RUBBLE_AMPLITUDE = 2;
    private static final int RUBBLE_SCALE = 9;
    /** Craters sit on a jittered grid of this cell size, at most one per cell. */
    private static final int CRATER_CELL = 80;
    private static final int CRATER_MIN_RADIUS = 9;
    private static final int CRATER_MAX_RADIUS = 26;
    private static final int CRATER_MAX_DEPTH = 14;
    /** Chance (out of 100) that a cell holds a crater. */
    private static final int CRATER_PERCENT = 55;
    /** Clear flat ground around the arrival point at the world origin. */
    public static final int LANDING_RADIUS = 40;

    private RuinedTerrain() {}

    public static boolean onPlanet(int x, int z) {
        return x >= -HALF && x < HALF && z >= -HALF && z < HALF;
    }

    /** The y of the topmost solid block of the column at (x, z). Only meaningful when {@link #onPlanet}. */
    public static int surfaceY(long seed, int x, int z) {
        double height = BASE_Y
                + (noise(seed, x, z, HILL_SCALE) - 0.5) * 2 * HILL_AMPLITUDE
                + (noise(seed ^ 0x9E3779B97F4A7C15L, x, z, RUBBLE_SCALE) - 0.5) * 2 * RUBBLE_AMPLITUDE;
        double flat = Mth.clamp(Math.sqrt((double) x * x + (double) z * z) / LANDING_RADIUS, 0.0, 1.0);
        height = BASE_Y + (height - BASE_Y) * flat;
        return (int) Math.round(height - craterDepth(seed, x, z) * flat);
    }

    /** How far the crater nearest to (x, z) digs it down, 0 outside every crater. */
    static double craterDepth(long seed, int x, int z) {
        int cellX = Math.floorDiv(x, CRATER_CELL);
        int cellZ = Math.floorDiv(z, CRATER_CELL);
        double deepest = 0;
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                long h = hash(seed + 0x51ED27L, cellX + dx, cellZ + dz);
                if (Math.floorMod(h, 100) >= CRATER_PERCENT) {
                    continue;
                }
                int radius = CRATER_MIN_RADIUS + (int) Math.floorMod(h >>> 8, CRATER_MAX_RADIUS - CRATER_MIN_RADIUS + 1);
                int centerX = (cellX + dx) * CRATER_CELL + CRATER_MAX_RADIUS + (int) Math.floorMod(h >>> 20, CRATER_CELL - 2 * CRATER_MAX_RADIUS);
                int centerZ = (cellZ + dz) * CRATER_CELL + CRATER_MAX_RADIUS + (int) Math.floorMod(h >>> 34, CRATER_CELL - 2 * CRATER_MAX_RADIUS);
                double distance = Math.sqrt((double) (x - centerX) * (x - centerX) + (double) (z - centerZ) * (z - centerZ));
                if (distance >= radius) {
                    continue;
                }
                double t = distance / radius;
                // A bowl with a slightly raised rim.
                double depth = CRATER_MAX_DEPTH * (radius / (double) CRATER_MAX_RADIUS) * (1 - t * t) - 1.5 * t * t * t;
                deepest = Math.max(deepest, depth);
            }
        }
        return deepest;
    }

    /** Smooth value noise in [0, 1) with the given feature size. */
    static double noise(long seed, int x, int z, int scale) {
        int cellX = Math.floorDiv(x, scale);
        int cellZ = Math.floorDiv(z, scale);
        double fx = smooth((x - cellX * scale) / (double) scale);
        double fz = smooth((z - cellZ * scale) / (double) scale);
        double top = Mth.lerp(fx, unit(seed, cellX, cellZ), unit(seed, cellX + 1, cellZ));
        double bottom = Mth.lerp(fx, unit(seed, cellX, cellZ + 1), unit(seed, cellX + 1, cellZ + 1));
        return Mth.lerp(fz, top, bottom);
    }

    private static double smooth(double t) {
        return t * t * (3 - 2 * t);
    }

    private static double unit(long seed, int x, int z) {
        return (hash(seed, x, z) >>> 11) / (double) (1L << 53);
    }

    private static long hash(long seed, int x, int z) {
        long h = seed ^ (x * 0x632BE59BD9B4E019L) ^ (z * 0x9E3779B97F4A7C15L);
        h = (h ^ (h >>> 30)) * 0xBF58476D1CE4E5B9L;
        h = (h ^ (h >>> 27)) * 0x94D049BB133111EBL;
        return h ^ (h >>> 31);
    }
}
