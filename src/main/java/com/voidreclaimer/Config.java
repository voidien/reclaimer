package com.voidreclaimer;

import net.neoforged.neoforge.common.ModConfigSpec;

/**
 * Common config, read on both logical sides (the void boundary push runs client-side for the local player).
 */
public final class Config {
    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

    public static final ModConfigSpec.IntValue SPAWN_X;
    public static final ModConfigSpec.IntValue SPAWN_Y;
    public static final ModConfigSpec.IntValue SPAWN_Z;

    public static final ModConfigSpec.DoubleValue BOUNDARY_MARGIN;
    public static final ModConfigSpec.DoubleValue BOUNDARY_PUSH;

    public static final ModConfigSpec.DoubleValue ENERGY_CAPACITY;
    public static final ModConfigSpec.DoubleValue ENERGY_IDLE_DRAIN;
    public static final ModConfigSpec.DoubleValue ENERGY_SUN_CHARGE;
    public static final ModConfigSpec.DoubleValue ENERGY_EMERGENCY_CHARGE;
    public static final ModConfigSpec.DoubleValue ENERGY_RESERVE_FRACTION;
    public static final ModConfigSpec.DoubleValue ENERGY_RESTART_FRACTION;

    static {
        BUILDER.comment("Where new and respawning players appear in void_reclaimer:deep_space.").push("spawn");
        SPAWN_X = BUILDER.defineInRange("x", 0, -29_000_000, 29_000_000);
        SPAWN_Y = BUILDER.defineInRange("y", 64, -2000, 2000);
        SPAWN_Z = BUILDER.defineInRange("z", 0, -29_000_000, 29_000_000);
        BUILDER.pop();

        BUILDER.comment("Soft push-back near the top and bottom of deep space.").push("boundary");
        BOUNDARY_MARGIN = BUILDER
                .comment("Distance in blocks from the build limit where the push-back starts.")
                .defineInRange("margin", 96.0, 8.0, 512.0);
        BOUNDARY_PUSH = BUILDER
                .comment("Acceleration in blocks/tick^2 at the build limit; it scales up linearly from 0 at the margin.")
                .defineInRange("push", 0.04, 0.001, 1.0);
        BUILDER.pop();

        BUILDER.comment("The AI's internal energy (shown in place of the hunger bar). Rates are per second.").push("energy");
        ENERGY_CAPACITY = BUILDER.comment("Base energy capacity.").defineInRange("capacity", 100.0, 1.0, 100_000.0);
        ENERGY_IDLE_DRAIN = BUILDER
                .comment("Passive drain while above the emergency reserve.")
                .defineInRange("idleDrain", 0.2, 0.0, 1000.0);
        ENERGY_SUN_CHARGE = BUILDER
                .comment("Charge under full, unobstructed sunlight. Deep space's distant sun gives about 2/3 of this.")
                .defineInRange("sunCharge", 1.5, 0.0, 1000.0);
        ENERGY_EMERGENCY_CHARGE = BUILDER
                .comment("Always-on trickle charge while below the emergency reserve, even in darkness.")
                .defineInRange("emergencyCharge", 0.1, 0.0, 1000.0);
        ENERGY_RESERVE_FRACTION = BUILDER
                .comment("Fraction of capacity below which idle drain stops and the emergency trickle starts.")
                .defineInRange("reserveFraction", 0.10, 0.0, 1.0);
        ENERGY_RESTART_FRACTION = BUILDER
                .comment("Fraction of capacity needed to leave low power mode after hitting zero.")
                .defineInRange("restartFraction", 0.05, 0.0, 1.0);
        BUILDER.pop();
    }

    public static final ModConfigSpec SPEC = BUILDER.build();

    private Config() {}
}
