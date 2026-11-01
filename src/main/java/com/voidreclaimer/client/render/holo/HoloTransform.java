package com.voidreclaimer.client.render.holo;

import com.mojang.math.Axis;
import net.minecraft.core.Direction;
import org.joml.Quaternionf;
import org.joml.Quaternionfc;

/** The turn and size of a hologram (pure). */
public final class HoloTransform {
    /** Blocks per font pixel at scale 1: a full line of 48 characters is about 4.5 blocks wide. */
    public static final float PIXEL = 1f / 64f;
    /** Blocks along the longest side of a model at scale 1. */
    public static final float MODEL_SIZE = 1.5f;
    private static final float SPIN_PER_TICK = 1f;

    private HoloTransform() {}

    /**
     * The local turn that makes a picture face the camera. {@code parent} is the rotation already on the pose stack (none in
     * the world, the ship's on a ship), so it is undone first and the billboard still faces the viewer on a moving ship.
     */
    public static Quaternionf billboard(Quaternionfc parent, Quaternionfc camera) {
        return new Quaternionf(parent).conjugate().mul(camera);
    }

    /** The local turn of a picture fixed to the block: its front looks along {@code facing}. */
    public static Quaternionf fixed(Direction facing) {
        return Axis.YP.rotationDegrees(-facing.toYRot());
    }

    /** How much to grow a model and where its middle is, in its own blocks. */
    public record Fit(float scale, float centerX, float centerY, float centerZ) {}

    /** Fits a box (min x, y, z, then max x, y, z) so that its longest side is {@code size}; an empty box stays as it is. */
    public static Fit fit(float[] box, float size) {
        float longest = Math.max(box[3] - box[0], Math.max(box[4] - box[1], box[5] - box[2]));
        if (longest <= 0) {
            return new Fit(1f, 0f, 0f, 0f);
        }
        return new Fit(size / longest, (box[0] + box[3]) / 2, (box[1] + box[4]) / 2, (box[2] + box[5]) / 2);
    }

    /** How far a model has turned, in degrees: it turns slowly so that every side shows. */
    public static float spin(float time) {
        return time * SPIN_PER_TICK % 360f;
    }

    /** A picture's size on one side, in blocks. */
    public static float size(int pixels, float scale) {
        return pixels * PIXEL * scale;
    }
}
