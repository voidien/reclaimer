package com.voidreclaimer.ship;

/**
 * Mouse movement at a Helm, in degrees of view, raw from the client. It is not the pilot's view: a seat turns with its ship, so
 * reading the view would feed the ship's own turn back into the steering.
 *
 * @param right degrees to the right
 * @param up    degrees up
 */
public record PilotMouse(float right, float up) {
    public static final PilotMouse NONE = new PilotMouse(0f, 0f);
    /** The most one packet may add: a client can send anything, and a real flick stays well under this. */
    private static final float MOST = 90f;

    /** This movement with {@code right} and {@code up} more, which arrive between Helm ticks. */
    public PilotMouse plus(float moreRight, float moreUp) {
        return new PilotMouse(right + clamp(moreRight), up + clamp(moreUp));
    }

    private static float clamp(float value) {
        return Float.isFinite(value) ? Math.max(-MOST, Math.min(MOST, value)) : 0f;
    }
}
