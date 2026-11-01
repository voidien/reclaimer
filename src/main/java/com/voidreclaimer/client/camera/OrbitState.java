package com.voidreclaimer.client.camera;

/**
 * The orbit camera's angles and zoom, kept free of rendering classes so they can be unit-tested. While the orbit key is held,
 * mouse movement turns the camera around the body; scrolling zooms. After release the camera eases back behind the body.
 * Angles are offsets (degrees) from where the body looks, so the body itself never turns.
 */
public final class OrbitState {
    public static final float MAX_PITCH = 89f;
    public static final float MIN_ZOOM = 0.25f;
    public static final float MAX_ZOOM = 6f;
    /** Share of the remaining offset removed each tick after release. */
    public static final float RETURN = 0.25f;
    /** Degrees per unit of mouse movement. */
    public static final float SENSITIVITY = 0.15f;

    private float yaw;
    private float pitch;
    private float zoom = 1f;
    private float oldYaw;
    private float oldPitch;

    public float yaw(float partialTick) {
        return oldYaw + (yaw - oldYaw) * partialTick;
    }

    public float pitch(float partialTick) {
        return oldPitch + (pitch - oldPitch) * partialTick;
    }

    public float zoom() {
        return zoom;
    }

    /** True while the camera is away from its default place (so the body camera code can skip all work otherwise). */
    public boolean offset() {
        return Math.abs(yaw) > 1.0E-3f || Math.abs(pitch) > 1.0E-3f;
    }

    /** One client tick: {@code dx, dy} are the mouse movement since the last tick, used only while {@code held}. */
    public void tick(boolean held, double dx, double dy) {
        oldYaw = yaw;
        oldPitch = pitch;
        if (held) {
            yaw = wrap(yaw + (float) dx * SENSITIVITY);
            // Wrapping past 180 must not make the blend go the long way round.
            oldYaw = nearTo(oldYaw, yaw);
            pitch = Math.max(-MAX_PITCH, Math.min(MAX_PITCH, pitch + (float) dy * SENSITIVITY));
        } else {
            // Take the short way back round, and keep old and new on the same side so the blend never spins.
            yaw = wrap(yaw);
            oldYaw = nearTo(oldYaw, yaw);
            yaw -= yaw * RETURN;
            pitch -= pitch * RETURN;
            if (Math.abs(yaw) < 0.05f) {
                yaw = 0f;
            }
            if (Math.abs(pitch) < 0.05f) {
                pitch = 0f;
            }
        }
    }

    /** Scroll steps: positive zooms in. Each step changes the distance by 15%. */
    public void scroll(double steps) {
        zoom = Math.max(MIN_ZOOM, Math.min(MAX_ZOOM, (float) (zoom * Math.pow(0.85, steps))));
    }

    public void reset() {
        yaw = pitch = oldYaw = oldPitch = 0f;
        zoom = 1f;
    }

    private static float wrap(float degrees) {
        float wrapped = degrees % 360f;
        return wrapped > 180f ? wrapped - 360f : wrapped < -180f ? wrapped + 360f : wrapped;
    }

    private static float nearTo(float value, float reference) {
        float v = value;
        while (v - reference > 180f) {
            v -= 360f;
        }
        while (v - reference < -180f) {
            v += 360f;
        }
        return v;
    }
}
