package com.voidreclaimer.body.assembly;

/**
 * The assembly view's camera, pure: an orbit round the body with yaw and pitch in degrees, a zoom on top of the scale that
 * fits the body in the view, and a focus point in the body's own space that the view eases towards.
 */
public final class AssemblyCamera {
    /** Degrees per GUI pixel dragged. */
    public static final float SENSITIVITY = 0.6f;
    public static final float PITCH_LIMIT = 89f;
    public static final float MIN_ZOOM = 0.6f;
    /** Far enough to fill the view with one Mech hand on the Titan-sized bodies. */
    public static final float MAX_ZOOM = 16f;
    /** Zoom factor per scroll notch. */
    public static final float ZOOM_STEP = 1.15f;
    /** How quickly the view reaches its focus: the share of the way left that is covered per second. */
    public static final float EASE_PER_SECOND = 0.995f;
    /** Close enough to count as there, so the body can hold still once it has spread. */
    private static final float SETTLED = 0.002f;

    private static final int MAX_GRID_STEP = 1024;

    private float yaw;
    private float pitch;
    private float zoom = 1f;
    private float explode;
    private boolean exploded;
    private final float[] focus = new float[3];
    private final float[] target = new float[3];

    public AssemblyCamera(float yaw, float pitch) {
        this.yaw = yaw;
        this.pitch = clampPitch(pitch);
    }

    private static float clampPitch(float pitch) {
        return Math.max(-PITCH_LIMIT, Math.min(PITCH_LIMIT, pitch));
    }

    /** The grid's spacing in blocks: the smallest power of two that leaves at least {@code minPixels} between lines. */
    public static int gridStep(double pixelsPerBlock, int minPixels) {
        int step = 1;
        while (step * pixelsPerBlock < minPixels && step < MAX_GRID_STEP) {
            step *= 2;
        }
        return step;
    }

    /** Turns the view by a drag of {@code dx}, {@code dy} GUI pixels. */
    public void drag(double dx, double dy) {
        yaw = (float) ((yaw + dx * SENSITIVITY) % 360.0);
        pitch = clampPitch((float) (pitch - dy * SENSITIVITY));
    }

    /** Zooms by {@code notches} of the scroll wheel; up is in. */
    public void scroll(double notches) {
        zoom = Math.max(MIN_ZOOM, Math.min(MAX_ZOOM, (float) (zoom * Math.pow(ZOOM_STEP, notches))));
    }

    /** Eases towards this point in the body's space. */
    public void focusOn(float x, float y, float z) {
        target[0] = x;
        target[1] = y;
        target[2] = z;
    }

    /** Eases back to the body's centre. */
    public void focusCentre() {
        focusOn(0f, 0f, 0f);
    }

    /** Moves the focus along for {@code seconds} of real time; independent of the frame rate. */
    public void advance(float seconds) {
        float left = (float) Math.pow(1f - EASE_PER_SECOND, Math.max(0f, seconds));
        for (int i = 0; i < 3; i++) {
            focus[i] = target[i] + (focus[i] - target[i]) * left;
        }
        float wanted = exploded ? 1f : 0f;
        explode = wanted + (explode - wanted) * left;
        if (Math.abs(explode - wanted) < SETTLED) {
            explode = wanted;
        }
    }

    /** Spreads the body's sections apart, or brings them back. */
    public void toggleExploded() {
        exploded = !exploded;
    }

    public boolean exploded() {
        return exploded;
    }

    /** How far apart the sections are now: 0 together, 1 fully spread. */
    public float explode() {
        return explode;
    }

    /**
     * How far a section moves from where it sits, in the model's pixels, at full spread: along the line from its anchor (the joint it
     * hangs from, or the body's middle) to its own pivot, so parts leave the way they attach.
     */
    public static float[] spread(float[] pivot, float[] anchor, float amount) {
        return new float[] {(pivot[0] - anchor[0]) * amount, (pivot[1] - anchor[1]) * amount, (pivot[2] - anchor[2]) * amount};
    }

    public float yaw() {
        return yaw;
    }

    public float pitch() {
        return pitch;
    }

    public float zoom() {
        return zoom;
    }

    public float focusX() {
        return focus[0];
    }

    public float focusY() {
        return focus[1];
    }

    public float focusZ() {
        return focus[2];
    }
}
