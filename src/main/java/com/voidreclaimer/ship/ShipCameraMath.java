package com.voidreclaimer.ship;

import org.joml.Quaterniondc;
import org.joml.Quaternionf;
import org.joml.Vector3d;
import org.joml.Vector3f;

/** The camera roll that keeps a pilot's view upright relative to their ship. Pure, so the sign can be unit-tested. */
public final class ShipCameraMath {
    /** When the ship's up is this close to the line of sight there is no roll to speak of; the last one is kept. */
    static final double DEGENERATE = 0.05;

    private ShipCameraMath() {}

    /** Minecraft's yaw for looking along {@code direction}: 0 south, 90 west. */
    public static float yaw(Vector3d direction) {
        return (float) Math.toDegrees(Math.atan2(-direction.x, direction.z));
    }

    /** Minecraft's pitch for looking along {@code direction}, positive down. Straight up or down is the limit of yaw and pitch. */
    public static float pitch(Vector3d direction) {
        return (float) -Math.toDegrees(Math.atan2(direction.y, Math.hypot(direction.x, direction.z)));
    }

    /**
     * The roll, in degrees, that turns a camera looking along Minecraft's {@code yaw} and {@code pitch} so that its up
     * matches the up of a ship turned by {@code orientation} (ship frame to world, the ship's own up being +Y), as far as
     * the line of sight allows. Positive tips the camera's up toward its right, as vanilla's roll does. When the ship's up
     * lies along the line of sight {@code fallback} is returned.
     */
    public static float roll(Quaterniondc orientation, float yaw, float pitch, float fallback) {
        Vector3d shipUp = orientation.transform(new Vector3d(0, 1, 0));
        // Vanilla's camera: rotationYXZ(pi - yaw, -pitch, -roll), with up (0, 1, 0) and left (-1, 0, 0) in its own frame.
        Quaternionf view = new Quaternionf().rotationYXZ((float) Math.PI - (float) Math.toRadians(yaw), -(float) Math.toRadians(pitch), 0f);
        Vector3f up = view.transform(new Vector3f(0, 1, 0));
        Vector3f right = view.transform(new Vector3f(1, 0, 0));
        double toRight = shipUp.dot(right.x, right.y, right.z);
        double toUp = shipUp.dot(up.x, up.y, up.z);
        if (toRight * toRight + toUp * toUp < DEGENERATE * DEGENERATE) {
            return fallback;
        }
        return (float) Math.toDegrees(Math.atan2(toRight, toUp));
    }
}
