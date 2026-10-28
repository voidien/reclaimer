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

    public static final ModConfigSpec.DoubleValue STARTER_MASS;
    public static final ModConfigSpec.DoubleValue STARTER_THRUST;
    public static final ModConfigSpec.DoubleValue STARTER_BRAKE_THRUST;
    public static final ModConfigSpec.DoubleValue STARTER_MAX_SPEED;
    public static final ModConfigSpec.DoubleValue THRUST_ENERGY_COST;
    public static final ModConfigSpec.DoubleValue BRAKE_ENERGY_COST;
    public static final ModConfigSpec.DoubleValue SPEED_CHECK_TOLERANCE;

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

        BUILDER.comment("Thruster flight. Acceleration is thrust / mass, in blocks per tick per tick.",
                "Phase 5's modular body replaces these starter drone stats with values from its parts.").push("flight");
        STARTER_MASS = BUILDER.comment("Starter drone mass.").defineInRange("starterMass", 1.0, 0.01, 10_000.0);
        STARTER_THRUST = BUILDER.comment("Starter drone thruster force.").defineInRange("starterThrust", 0.03, 0.0, 10.0);
        STARTER_BRAKE_THRUST = BUILDER
                .comment("Starter drone inertial dampener force (braking when no thrust key is held).")
                .defineInRange("starterBrakeThrust", 0.04, 0.0, 10.0);
        STARTER_MAX_SPEED = BUILDER.comment("Starter drone top speed in blocks per second.").defineInRange("starterMaxSpeed", 10.0, 0.1, 100.0);
        THRUST_ENERGY_COST = BUILDER.comment("Energy per second while any thruster fires.").defineInRange("thrustEnergyCost", 2.0, 0.0, 1000.0);
        BRAKE_ENERGY_COST = BUILDER.comment("Energy per second while the dampener is braking.").defineInRange("brakeEnergyCost", 1.0, 0.0, 1000.0);
        SPEED_CHECK_TOLERANCE = BUILDER
                .comment("Server speed check: movement over one second may exceed top speed by this factor before it is corrected.")
                .defineInRange("speedCheckTolerance", 1.5, 1.0, 10.0);
        BUILDER.pop();
    }

    public static final ModConfigSpec SPEC = BUILDER.build();

    private Config() {}
}
