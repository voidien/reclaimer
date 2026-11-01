package com.voidreclaimer.client.render.holo;

import com.voidreclaimer.VoidReclaimer;
import net.minecraft.resources.ResourceLocation;

/** The placeholder hologram look, until the owner's art: one cyan, a slow flicker, a scrolling scanline texture (pure). */
public final class HoloStyle {
    public static final int CYAN = 0x66E6FF;
    /** Multiplies a model's own texture, so its shapes still read through the tint. */
    public static final int MODEL_TINT = 0xB0F0FF;
    public static final ResourceLocation PANEL = VoidReclaimer.id("textures/holo/panel.png");
    public static final ResourceLocation GLOW = VoidReclaimer.id("textures/holo/glow.png");
    /** Strength of the panel behind the text, and of the text showing through what stands in front of it. */
    public static final float PANEL_ALPHA = 0.35f;
    public static final float SEE_THROUGH_ALPHA = 0.12f;
    public static final float GLOW_ALPHA = 0.3f;
    public static final float MODEL_ALPHA = 0.7f;
    /** A second pass in plain cyan over a model, so that a dark texture still reads as light. */
    public static final float WASH_ALPHA = 0.35f;
    /** The panel's texture repeats every this many font pixels. */
    public static final float SCANLINE_PIXELS = 16f;
    private static final float SCROLL_PER_TICK = 0.01f;

    private HoloStyle() {}

    /** Brightness from 0.85 to 1 over time; {@code seed} keeps two projectors from flickering together. */
    public static float flicker(float time, long seed) {
        float wave = (float) Math.sin(time * 0.31f + (seed & 0xFF)) * (float) Math.sin(time * 0.073f + ((seed >> 8) & 0xFF));
        return 0.925f + 0.075f * wave;
    }

    /** How far the scanlines have moved, in texture repeats, from 0 up to 1. */
    public static float scroll(float time) {
        return (time * SCROLL_PER_TICK) % 1f;
    }

    /** A colour 0xRRGGBB with an alpha from 0 to 1, as the font wants it. */
    public static int argb(int rgb, float alpha) {
        int a = Math.max(0, Math.min(255, Math.round(alpha * 255)));
        return a << 24 | rgb & 0xFFFFFF;
    }
}
