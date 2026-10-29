package com.voidreclaimer.client.render.body;

/** Lifts the light a body is drawn with to a minimum, so the dark art textures stay readable in deep space. */
public final class BodyLight {
    private BodyLight() {}

    /** Packed light as vanilla stores it: block light in bits 4-7, sky light in bits 20-23. */
    public static int atLeast(int packedLight, int minimum) {
        int level = Math.max(0, Math.min(15, minimum));
        int block = Math.max(packedLight >> 4 & 15, level);
        int sky = Math.max(packedLight >> 20 & 15, level);
        return block << 4 | sky << 20;
    }
}
