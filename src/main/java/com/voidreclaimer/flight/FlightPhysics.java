package com.voidreclaimer.flight;

import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

/** Pure thruster-flight math, independent of entities and config so it can be unit tested. */
public final class FlightPhysics {
    private FlightPhysics() {}

    /**
     * One tick of thruster physics.
     *
     * @param velocity  current velocity in blocks/tick
     * @param thrust    un-normalized thrust direction (zero when no key is held)
     * @param powered   false in low power mode: no thrust, no dampener, pure drift
     * @param dampener  inertial dampener enabled
     * @param stats     body stats
     * @return the new velocity, never faster than {@link FlightStats#maxSpeed()}
     */
    public static Vec3 step(Vec3 velocity, Vec3 thrust, boolean powered, boolean dampener, FlightStats stats) {
        if (powered) {
            if (thrust.lengthSqr() > 1.0E-6) {
                velocity = velocity.add(thrust.normalize().scale(stats.acceleration()));
            } else if (dampener) {
                velocity = brake(velocity, stats.brakeDeceleration());
            }
        }
        return capSpeed(velocity, stats.maxSpeed());
    }

    /**
     * View-relative thrust direction: forward/back along the look vector, strafe along the view's right vector
     * (horizontal, since there is no roll) and up/down along the view's up vector.
     */
    public static Vec3 thrustDirection(float xRot, float yRot, FlightInput input) {
        Vec3 forward = viewVector(xRot, yRot);
        Vec3 up = viewVector(xRot - 90f, yRot);
        Vec3 right = forward.cross(up);
        return forward.scale(input.forwardAxis())
                .add(right.scale(input.strafeAxis()))
                .add(up.scale(input.verticalAxis()));
    }

    /** Same as {@code Entity.calculateViewVector}: yaw 0 looks toward +Z, positive pitch looks down. */
    public static Vec3 viewVector(float xRot, float yRot) {
        float pitch = xRot * Mth.DEG_TO_RAD;
        float yaw = -yRot * Mth.DEG_TO_RAD;
        float cosYaw = Mth.cos(yaw);
        float sinYaw = Mth.sin(yaw);
        float cosPitch = Mth.cos(pitch);
        float sinPitch = Mth.sin(pitch);
        return new Vec3(sinYaw * cosPitch, -sinPitch, cosYaw * cosPitch);
    }

    public static Vec3 brake(Vec3 velocity, double deceleration) {
        double speed = velocity.length();
        if (speed <= deceleration) {
            return Vec3.ZERO;
        }
        return velocity.scale((speed - deceleration) / speed);
    }

    public static Vec3 capSpeed(Vec3 velocity, double maxSpeed) {
        if (velocity.lengthSqr() > maxSpeed * maxSpeed) {
            return velocity.normalize().scale(maxSpeed);
        }
        return velocity;
    }

    /** Whether moving from {@code start} to {@code end} over {@code ticks} ticks breaks the speed limit. */
    public static boolean exceedsSpeedLimit(Vec3 start, Vec3 end, int ticks, double maxSpeed, double tolerance, double slack) {
        return start.distanceTo(end) > maxSpeed * ticks * tolerance + slack;
    }
}
