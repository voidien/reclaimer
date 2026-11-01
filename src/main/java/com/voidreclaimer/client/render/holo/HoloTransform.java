package com.voidreclaimer.client.render.holo;

import com.mojang.math.Axis;
import net.minecraft.core.Direction;
import org.joml.Quaternionf;
import org.joml.Quaternionfc;

/** The turn and size of a hologram (pure). */
public final class HoloTransform {
    /** Blocks per font pixel at scale 1: a full line of 48 characters is about 4.5 blocks wide. */
    public static final float PIXEL = 1f / 64f;

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

    /** A picture's size on one side, in blocks. */
    public static float size(int pixels, float scale) {
        return pixels * PIXEL * scale;
    }
}
