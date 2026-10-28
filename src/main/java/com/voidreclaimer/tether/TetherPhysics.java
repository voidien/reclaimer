package com.voidreclaimer.tether;

import net.minecraft.world.phys.Vec3;

/** Pure tether math: the leash and the recall winch. Independent of entities and config so it can be unit tested. */
public final class TetherPhysics {
    /** Inward pull per block past the leash length, in blocks/tick². */
    static final double LEASH_SPRING = 0.02;
    /** Cap on the leash's inward pull per tick. */
    static final double LEASH_MAX_PULL = 0.08;
    /** Recall slows down over the last stretch so it doesn't overshoot: target speed is distance × this. */
    static final double RECALL_BRAKE_FACTOR = 0.15;

    private TetherPhysics() {}

    /**
     * Leash: past {@code length} blocks from the anchor, outward motion is cancelled and a gentle spring pulls back.
     * Inside the leash, velocity is untouched.
     *
     * @param velocity current velocity
     * @param offset   position minus anchor
     * @param length   leash length
     */
    public static Vec3 leash(Vec3 velocity, Vec3 offset, double length) {
        double distance = offset.length();
        if (distance <= length || distance < 1.0E-6) {
            return velocity;
        }
        Vec3 out = offset.scale(1.0 / distance);
        double radial = velocity.dot(out);
        if (radial > 0.0) {
            velocity = velocity.subtract(out.scale(radial));
        }
        double pull = Math.min(LEASH_MAX_PULL, (distance - length) * LEASH_SPRING);
        return velocity.subtract(out.scale(pull));
    }

    /**
     * Recall winch: steers velocity toward {@code toAnchor} at up to {@code speed}, changing by at most
     * {@code acceleration} per tick, and slowing down on arrival.
     *
     * @param velocity     current velocity
     * @param toAnchor     anchor minus position
     * @param speed        top recall speed in blocks/tick
     * @param acceleration max velocity change per tick
     */
    public static Vec3 recall(Vec3 velocity, Vec3 toAnchor, double speed, double acceleration) {
        double distance = toAnchor.length();
        if (distance < 1.0E-6) {
            return Vec3.ZERO;
        }
        double targetSpeed = Math.min(speed, distance * RECALL_BRAKE_FACTOR);
        Vec3 desired = toAnchor.scale(targetSpeed / distance);
        Vec3 change = desired.subtract(velocity);
        double changeLength = change.length();
        if (changeLength > acceleration) {
            change = change.scale(acceleration / changeLength);
        }
        return velocity.add(change);
    }

    /** Recall mode for a hold: pull-flight within {@code pullRange}, otherwise teleport. */
    public static boolean usesTeleport(double distance, double pullRange) {
        return distance > pullRange;
    }
}
