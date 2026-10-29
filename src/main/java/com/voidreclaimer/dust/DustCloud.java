package com.voidreclaimer.dust;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;

/**
 * One cosmic dust cloud: a roughly spherical region whose density falls off from the core to the edge, with
 * internal structure from noise.
 *
 * @param center      cloud center
 * @param radius      distance from the center where density reaches zero
 * @param kind        id of the {@link com.voidreclaimer.api.dust.DustKind} it is made of
 * @param intensity   peak density multiplier, 0..1
 * @param noiseOffset per-cloud offset into the shared noise field, so clouds don't share structure
 */
public record DustCloud(Vec3 center, double radius, ResourceLocation kind, double intensity, Vec3 noiseOffset) {
    /** Distance from {@code pos} to the cloud's surface; negative when inside. */
    public double surfaceDistance(Vec3 pos) {
        return pos.distanceTo(center) - radius;
    }

    public boolean contains(Vec3 pos) {
        return pos.distanceToSqr(center) < radius * radius;
    }
}
