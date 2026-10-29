package com.voidreclaimer;

import net.neoforged.neoforge.common.ModConfigSpec;

/** Client-only presentation settings (motion effects, sounds). Registered from {@link VoidReclaimerClient}. */
public final class ClientConfig {
    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

    public static final ModConfigSpec.BooleanValue SPECKS_ENABLED;
    public static final ModConfigSpec.DoubleValue SPECKS_DENSITY;
    public static final ModConfigSpec.BooleanValue SWAY_ENABLED;
    public static final ModConfigSpec.DoubleValue SWAY_STRENGTH;
    public static final ModConfigSpec.DoubleValue THRUSTER_VOLUME;
    public static final ModConfigSpec.BooleanValue CLOUD_SCANNER;
    public static final ModConfigSpec.IntValue BODY_MIN_LIGHT;

    static {
        BUILDER.comment("Sense of motion in deep space.").push("motion");
        SPECKS_ENABLED = BUILDER
                .comment("Faint space specks around the camera that stream past as you move (purely cosmetic, the same everywhere).")
                .define("specks", true);
        SPECKS_DENSITY = BUILDER.comment("Speck density multiplier.").defineInRange("specksDensity", 1.0, 0.1, 3.0);
        SWAY_ENABLED = BUILDER
                .comment("Slight camera tilt and lag when accelerating or braking. Some players find it uncomfortable.")
                .define("cameraSway", false);
        SWAY_STRENGTH = BUILDER.comment("Camera sway strength multiplier.").defineInRange("cameraSwayStrength", 1.0, 0.0, 3.0);
        BUILDER.pop();

        BUILDER.push("sensor");
        CLOUD_SCANNER = BUILDER
                .comment("Dust Sensor scanner view: outline nearby dust clouds in the world (toggle in game with the scanner key).")
                .define("cloudScanner", true);
        BUILDER.pop();

        BUILDER.push("body");
        BODY_MIN_LIGHT = BUILDER
                .comment("Lowest light level (0-15) a body is drawn with, so the dark textures read in deep space. 0 uses only the world light.")
                .defineInRange("minLight", 8, 0, 15);
        BUILDER.pop();

        BUILDER.push("sound");
        THRUSTER_VOLUME = BUILDER.comment("Volume of the thruster and dampener loops.").defineInRange("thrusterVolume", 1.0, 0.0, 2.0);
        BUILDER.pop();
    }

    public static final ModConfigSpec SPEC = BUILDER.build();

    private ClientConfig() {}
}
