package com.voidreclaimer.client.camera;

import net.minecraft.world.phys.Vec3;

/**
 * The photo-mode free camera's position, angles, roll and field of view, kept free of rendering classes so they can be
 * unit-tested. Movement follows the view (forward, right and up of the camera); the camera cannot stray further than
 * {@link #MAX_RANGE} blocks from the body, since the world beyond is not loaded.
 */
public final class PhotoState {
    public static final double MAX_RANGE = 256.0;
    public static final float MIN_FOV = 5f;
    public static final float MAX_FOV = 110f;
    public static final float SENSITIVITY = 0.15f;

    private Vec3 position = Vec3.ZERO;
    private float yaw;
    private float pitch;
    private float roll;
    private float fov = 70f;

    public void start(Vec3 position, float yaw, float pitch, float fov) {
        this.position = position;
        this.yaw = yaw;
        this.pitch = Math.max(-90f, Math.min(90f, pitch));
        this.roll = 0f;
        this.fov = clampFov(fov);
    }

    public Vec3 position() {
        return position;
    }

    public float yaw() {
        return yaw;
    }

    public float pitch() {
        return pitch;
    }

    public float roll() {
        return roll;
    }

    public float fov() {
        return fov;
    }

    /** Turns the view by a mouse drag of {@code dx, dy} pixels. */
    public void look(double dx, double dy) {
        yaw = wrap(yaw + (float) dx * SENSITIVITY);
        pitch = Math.max(-90f, Math.min(90f, pitch + (float) dy * SENSITIVITY));
    }

    public void rollBy(float degrees) {
        roll = Math.max(-180f, Math.min(180f, roll + degrees));
    }

    public void resetRoll() {
        roll = 0f;
    }

    /** Changes the field of view by scroll steps: positive narrows it (zooms in). */
    public void zoom(double steps) {
        fov = clampFov((float) (fov * Math.pow(0.9, steps)));
    }

    /**
     * Moves along the view: {@code forward}, {@code right} and {@code up} are in -1..1 (key states) and {@code speed} is blocks per
     * tick. {@code center} is the body; the result stays within {@link #MAX_RANGE} of it.
     */
    public void move(double forward, double right, double up, double speed, Vec3 center) {
        double yawRad = Math.toRadians(yaw);
        double pitchRad = Math.toRadians(pitch);
        // Minecraft: yaw 0 looks along +Z, positive yaw turns toward -X.
        Vec3 look = new Vec3(-Math.sin(yawRad) * Math.cos(pitchRad), -Math.sin(pitchRad), Math.cos(yawRad) * Math.cos(pitchRad));
        Vec3 rightVec = new Vec3(-Math.cos(yawRad), 0, -Math.sin(yawRad));
        Vec3 step = look.scale(forward).add(rightVec.scale(right)).add(0, up, 0);
        Vec3 next = position.add(step.scale(speed));
        Vec3 offset = next.subtract(center);
        if (offset.length() > MAX_RANGE) {
            next = center.add(offset.normalize().scale(MAX_RANGE));
        }
        position = next;
    }

    private static float clampFov(float value) {
        return Math.max(MIN_FOV, Math.min(MAX_FOV, value));
    }

    private static float wrap(float degrees) {
        float wrapped = degrees % 360f;
        return wrapped > 180f ? wrapped - 360f : wrapped < -180f ? wrapped + 360f : wrapped;
    }
}
