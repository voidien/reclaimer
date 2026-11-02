package com.voidreclaimer.space;

import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;
import org.joml.Vector3f;

/** Where deep space's fixed sun is: one place for the sky to draw it and for weather to shine from it, so what you see is what you shelter from. */
public final class DeepSpaceSun {
    public static final float TILT_X_DEGREES = 12f;
    public static final float TILT_Z_DEGREES = -8f;

    private DeepSpaceSun() {}

    /** The way to the sun, as a unit vector: straight up, with the sky's two tilts. */
    public static Vec3 direction() {
        Vector3f up = new Quaternionf().rotateX((float) Math.toRadians(TILT_X_DEGREES)).rotateZ((float) Math.toRadians(TILT_Z_DEGREES))
                .transform(new Vector3f(0f, 1f, 0f));
        return new Vec3(up.x, up.y, up.z);
    }
}
