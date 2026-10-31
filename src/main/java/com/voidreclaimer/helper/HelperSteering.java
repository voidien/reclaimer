package com.voidreclaimer.helper;

import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

/** Pure flight of a helper drone: a straight line to its target at a fixed speed, stopping on it. */
public final class HelperSteering {
    private HelperSteering() {}

    /** The position after one tick flying from {@code pos} towards {@code target} at {@code speed} blocks per tick. */
    public static Vec3 step(Vec3 pos, Vec3 target, double speed) {
        Vec3 to = target.subtract(pos);
        double distance = to.length();
        if (distance <= speed || distance < 1.0e-6) {
            return target;
        }
        return pos.add(to.scale(speed / distance));
    }

    /** Whether {@code pos} is within {@code reach} of {@code target}. */
    public static boolean arrived(Vec3 pos, Vec3 target, double reach) {
        return pos.distanceToSqr(target) <= reach * reach;
    }

    /** The yaw (degrees, Minecraft's convention) that faces from {@code from} towards {@code to}; unchanged when they coincide. */
    public static float yaw(Vec3 from, Vec3 to, float current) {
        double dx = to.x - from.x, dz = to.z - from.z;
        if (dx * dx + dz * dz < 1.0e-6) {
            return current;
        }
        return (float) (Mth.atan2(dz, dx) * (180.0 / Math.PI)) - 90f;
    }
}
