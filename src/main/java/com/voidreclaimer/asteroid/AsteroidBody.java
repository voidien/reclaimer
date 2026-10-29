package com.voidreclaimer.asteroid;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

/**
 * One asteroid: a lumpy ellipsoid. Pure geometry, so it can be tested without a world.
 *
 * @param center    middle of the body
 * @param rx        half-width along x before the surface noise (also {@code ry}, {@code rz})
 * @param kind      id of the dust kind it is made of (and so which block it is built from)
 * @param noiseSeed seed of its surface noise
 * @param size      size class
 */
public record AsteroidBody(Vec3 center, double rx, double ry, double rz, ResourceLocation kind, long noiseSeed, Size size) {
    public enum Size { SMALL, LARGE, GIANT }

    /** Largest relative bump of the surface (outer limit is {@code 1 + AMPLITUDE}), and the first octave's share of it. */
    private static final double AMPLITUDE_1 = 0.22;
    private static final double AMPLITUDE_2 = 0.10;
    public static final double OUTER = 1.0 + AMPLITUDE_1 + AMPLITUDE_2;
    private static final double INNER = 1.0 - AMPLITUDE_1 - AMPLITUDE_2;
    private static final double OUTER_SQ = OUTER * OUTER;
    private static final double INNER_SQ = INNER * INNER;

    /** Farthest the body can reach from its center along any axis. */
    public double maxExtent() {
        return Math.max(rx, Math.max(ry, rz)) * OUTER;
    }

    /** Whether the block whose center is at (x, y, z) belongs to this body. */
    public boolean contains(double x, double y, double z) {
        double dx = (x - center.x) / rx;
        double dy = (y - center.y) / ry;
        double dz = (z - center.z) / rz;
        double distanceSq = dx * dx + dy * dy + dz * dz;
        if (distanceSq >= OUTER_SQ) {
            return false;
        }
        if (distanceSq <= INNER_SQ) {
            return true;
        }
        double coarse = Math.max(6.0, Math.min(rx, Math.min(ry, rz)) / 2.2);
        double fine = coarse / 2.6;
        double surface = 1.0
                + AMPLITUDE_1 * noise(noiseSeed, (x - center.x) / coarse, (y - center.y) / coarse, (z - center.z) / coarse)
                + AMPLITUDE_2 * noise(noiseSeed ^ 0x5DEECE66DL, (x - center.x) / fine, (y - center.y) / fine, (z - center.z) / fine);
        return distanceSq < surface * surface;
    }

    /** Smooth value noise in -1..1. */
    static double noise(long seed, double x, double y, double z) {
        int x0 = Mth.floor(x);
        int y0 = Mth.floor(y);
        int z0 = Mth.floor(z);
        double fx = smooth(x - x0);
        double fy = smooth(y - y0);
        double fz = smooth(z - z0);
        double c00 = Mth.lerp(fx, lattice(seed, x0, y0, z0), lattice(seed, x0 + 1, y0, z0));
        double c10 = Mth.lerp(fx, lattice(seed, x0, y0 + 1, z0), lattice(seed, x0 + 1, y0 + 1, z0));
        double c01 = Mth.lerp(fx, lattice(seed, x0, y0, z0 + 1), lattice(seed, x0 + 1, y0, z0 + 1));
        double c11 = Mth.lerp(fx, lattice(seed, x0, y0 + 1, z0 + 1), lattice(seed, x0 + 1, y0 + 1, z0 + 1));
        return Mth.lerp(fz, Mth.lerp(fy, c00, c10), Mth.lerp(fy, c01, c11));
    }

    private static double smooth(double t) {
        return t * t * (3.0 - 2.0 * t);
    }

    private static double lattice(long seed, int x, int y, int z) {
        long h = seed ^ (x * 0x9E3779B97F4A7C15L) ^ (y * 0xC2B2AE3D27D4EB4FL) ^ (z * 0x165667B19E3779F9L);
        h ^= h >>> 32;
        h *= 0xD6E8FEB86659FD93L;
        h ^= h >>> 32;
        return (h & 0xFFFFFFL) / (double) 0x7FFFFF - 1.0;
    }
}
