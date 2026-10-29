package com.voidreclaimer.dust;

import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

/**
 * The visible outline of a dust cloud for the scanner view: its true, noisy edge as a star-shaped mesh.
 *
 * <p>Rays leave the cloud center on a latitude/longitude grid. Along each ray the edge is the outermost point where
 * density still reaches {@link #EDGE_DENSITY}, found by stepping inward from the cloud radius and refining the crossing
 * by bisection. The grid rows and columns double as the wireframe. Pure and deterministic, so it can run on a worker
 * thread and be unit tested.
 */
public final class CloudShape {
    /** Density at the drawn edge: the faint fringe where dust starts to be collectable. */
    public static final double EDGE_DENSITY = 0.05;
    /** Latitude bands (rows of the grid go from pole to pole, {@code LAT_BANDS + 1} rows). */
    public static final int LAT_BANDS = 16;
    /** Longitude segments around each ring. */
    public static final int LON_SEGMENTS = 32;
    private static final int RAY_STEPS = 64;
    private static final int BISECTIONS = 8;

    private final Vec3 center;
    private final float[][] radii;

    private CloudShape(Vec3 center, float[][] radii) {
        this.center = center;
        this.radii = radii;
    }

    public static CloudShape compute(DustField field, DustCloud cloud) {
        float[][] radii = new float[LAT_BANDS + 1][LON_SEGMENTS];
        for (int lat = 0; lat <= LAT_BANDS; lat++) {
            for (int lon = 0; lon < LON_SEGMENTS; lon++) {
                radii[lat][lon] = (float) edgeAlong(field, cloud, direction(lat, lon));
            }
        }
        return new CloudShape(cloud.center(), radii);
    }

    /** Unit direction of a grid vertex: latitude 0 is the top pole, {@link #LAT_BANDS} the bottom. */
    public static Vec3 direction(int lat, int lon) {
        double theta = Math.PI * lat / LAT_BANDS;
        double phi = Mth.TWO_PI * lon / LON_SEGMENTS;
        double ring = Math.sin(theta);
        return new Vec3(ring * Math.cos(phi), Math.cos(theta), ring * Math.sin(phi));
    }

    /** Distance from the center to the outermost point along {@code dir} with density at or above the edge level. */
    static double edgeAlong(DustField field, DustCloud cloud, Vec3 dir) {
        double step = cloud.radius() / RAY_STEPS;
        double outside = cloud.radius();
        for (int i = RAY_STEPS - 1; i >= 0; i--) {
            double r = i * step;
            if (field.density(cloud, cloud.center().add(dir.scale(r))) >= EDGE_DENSITY) {
                double inside = r;
                for (int b = 0; b < BISECTIONS; b++) {
                    double mid = (inside + outside) / 2;
                    if (field.density(cloud, cloud.center().add(dir.scale(mid))) >= EDGE_DENSITY) {
                        inside = mid;
                    } else {
                        outside = mid;
                    }
                }
                return inside;
            }
            outside = r;
        }
        return 0.0;
    }

    public Vec3 center() {
        return center;
    }

    public float radius(int lat, int lon) {
        return radii[lat][Math.floorMod(lon, LON_SEGMENTS)];
    }

    /** World position of a grid vertex. */
    public Vec3 vertex(int lat, int lon) {
        return center.add(direction(lat, Math.floorMod(lon, LON_SEGMENTS)).scale(radius(lat, lon)));
    }
}
