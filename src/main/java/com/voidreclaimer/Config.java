package com.voidreclaimer;

import net.neoforged.neoforge.common.ModConfigSpec;

/**
 * Common config, read on both logical sides (the void boundary push runs client-side for the local player).
 */
public final class Config {
    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

    /** On/off switches for whole features, by name. Recipes read them through {@code machine.ConfigCondition}. */
    public static final java.util.Map<String, ModConfigSpec.BooleanValue> TOGGLES = new java.util.LinkedHashMap<>();

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

    public static final ModConfigSpec.DoubleValue THRUST_ENERGY_COST;
    public static final ModConfigSpec.DoubleValue BRAKE_ENERGY_COST;
    public static final ModConfigSpec.DoubleValue SPEED_CHECK_TOLERANCE;
    public static final ModConfigSpec.DoubleValue HOVER_WEIGHT_FACTOR;

    public static final ModConfigSpec.DoubleValue DUST_CAPACITY;
    public static final ModConfigSpec.DoubleValue DUST_TRICKLE;
    public static final ModConfigSpec.DoubleValue DUST_SCOOP;
    public static final ModConfigSpec.DoubleValue CLOUD_DRAG;
    public static final ModConfigSpec.DoubleValue CLOUD_SPEED_PENALTY;
    public static final ModConfigSpec.DoubleValue PULSE_ENERGY_COST;
    public static final ModConfigSpec.IntValue PULSE_COOLDOWN_SECONDS;
    public static final ModConfigSpec.DoubleValue PULSE_RANGE;
    public static final ModConfigSpec.DoubleValue HUM_RANGE;
    public static final ModConfigSpec.BooleanValue STARTER_DUST_ENABLED;
    public static final ModConfigSpec.DoubleValue STARTER_METALLIC;
    public static final ModConfigSpec.DoubleValue STARTER_SILICATE;
    public static final ModConfigSpec.DoubleValue STARTER_ICE;
    public static final ModConfigSpec.DoubleValue STARTER_CARBON;

    public static final ModConfigSpec.DoubleValue REPLICATOR_ENERGY;
    public static final ModConfigSpec.DoubleValue SALVAGE_REFUND;
    public static final ModConfigSpec.DoubleValue NANITE_ENERGY;

    public static final ModConfigSpec.BooleanValue WEAR_ENABLED;
    public static final ModConfigSpec.DoubleValue WEAR_PER_THRUST_MINUTE;
    public static final ModConfigSpec.DoubleValue WEAR_PER_TOOL_USE;
    public static final ModConfigSpec.DoubleValue REPAIR_COST_FRACTION;

    public static final ModConfigSpec.DoubleValue TETHER_LENGTH;
    public static final ModConfigSpec.DoubleValue RECALL_SPEED_FACTOR;
    public static final ModConfigSpec.DoubleValue RECALL_PULL_RANGE;
    public static final ModConfigSpec.DoubleValue RECALL_PULL_ENERGY;
    public static final ModConfigSpec.DoubleValue RECALL_TELEPORT_ENERGY;
    public static final ModConfigSpec.IntValue RECALL_TELEPORT_COOLDOWN;

    public static final ModConfigSpec.IntValue CORE_SUN_TICKS;

    /** Whether a feature toggle is on. Before the config has loaded (or for an unknown name) the feature counts as on. */
    public static boolean isEnabled(String key) {
        ModConfigSpec.BooleanValue value = TOGGLES.get(key);
        if (value == null) {
            return true;
        }
        try {
            return value.get();
        } catch (IllegalStateException e) {
            return value.getDefault();
        }
    }

    private static void toggle(String key, String comment) {
        TOGGLES.put(key, BUILDER.comment(comment, "Recipes read this when they load: restart or /reload after changing it.").define(key, true));
    }

    public static final ModConfigSpec.DoubleValue ELECTROLYZER_STRESS;
    public static final ModConfigSpec.DoubleValue SPINNER_STRESS;
    public static final ModConfigSpec.DoubleValue SIPHON_STRESS;
    public static final ModConfigSpec.DoubleValue SIPHON_RATE;
    public static final ModConfigSpec.DoubleValue SIPHON_MIN_DENSITY;
    public static final ModConfigSpec.IntValue MACHINE_MIN_RPM;
    public static final ModConfigSpec.DoubleValue MASS_DRIVER_STRESS;
    public static final ModConfigSpec.IntValue MASS_DRIVER_MIN_RPM;
    public static final ModConfigSpec.IntValue MASS_DRIVER_FAST_RPM;
    public static final ModConfigSpec.IntValue MASS_DRIVER_SLOW_TICKS;
    public static final ModConfigSpec.IntValue MASS_DRIVER_FAST_TICKS;
    public static final ModConfigSpec.IntValue RECEIVER_RPM;
    public static final ModConfigSpec.DoubleValue RECEIVER_MAX_STRESS;
    public static final ModConfigSpec.IntValue MACHINE_CYCLE_TICKS;
    public static final ModConfigSpec.IntValue ELECTROLYZER_TANK;
    public static final ModConfigSpec.IntValue ELECTROLYZER_WATER;
    public static final ModConfigSpec.IntValue ELECTROLYZER_OXYGEN;
    public static final ModConfigSpec.IntValue ELECTROLYZER_HYDROGEN;
    public static final ModConfigSpec.IntValue SPINNER_THREADS;
    public static final ModConfigSpec.IntValue CASTING_AMOUNT;

    public static final ModConfigSpec.DoubleValue DRILL_ENERGY;
    public static final ModConfigSpec.DoubleValue DRILL_DUST_YIELD;
    public static final ModConfigSpec.BooleanValue COLLECTOR_ENABLED;
    public static final ModConfigSpec.DoubleValue COLLECTOR_YIELD;

    public static final ModConfigSpec.IntValue SPIRE_RADIUS;
    public static final ModConfigSpec.IntValue SPIRE_PULSE_TICKS;

    public static final ModConfigSpec.LongValue DYSON_LATTICE_START;
    public static final ModConfigSpec.LongValue DYSON_SHELL_TOTAL;

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
        ENERGY_CAPACITY = BUILDER
                .comment("Energy capacity before the body is applied (the installed core sets the real capacity).")
                .defineInRange("capacity", 100.0, 1.0, 100_000.0);
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

        BUILDER.comment("Thruster flight. Handling (mass, thrust, top speed) comes from the body's parts.").push("flight");
        THRUST_ENERGY_COST = BUILDER.comment("Energy per second while any thruster fires.").defineInRange("thrustEnergyCost", 2.0, 0.0, 1000.0);
        BRAKE_ENERGY_COST = BUILDER.comment("Energy per second while the dampener is braking.").defineInRange("brakeEnergyCost", 1.0, 0.0, 1000.0);
        SPEED_CHECK_TOLERANCE = BUILDER
                .comment("Server speed check: movement over one second may exceed top speed by this factor before it is corrected.")
                .defineInRange("speedCheckTolerance", 1.5, 1.0, 10.0);
        HOVER_WEIGHT_FACTOR = BUILDER
                .comment("Ruined Earth: weight the thrusters must cancel = the body's gravity times this. Vanilla gravity is 0.08 blocks/tick²; a starter Drone's thrust is about 0.075.")
                .defineInRange("hoverWeightFactor", 0.6, 0.0, 4.0);
        BUILDER.pop();

        BUILDER.comment("Dust a new body starts with: enough for the Dust Sensor, Scaffold, Thruster Mk I and Rotor Arm. Granted once, when a player first wakes up.").push("starter");
        STARTER_DUST_ENABLED = BUILDER.define("enabled", true);
        STARTER_METALLIC = BUILDER.defineInRange("metallic", 25.0, 0.0, 1_000_000.0);
        STARTER_SILICATE = BUILDER.defineInRange("silicate", 30.0, 0.0, 1_000_000.0);
        STARTER_ICE = BUILDER.defineInRange("ice", 0.0, 0.0, 1_000_000.0);
        STARTER_CARBON = BUILDER.defineInRange("carbon", 5.0, 0.0, 1_000_000.0);
        BUILDER.pop();

        BUILDER.comment("Cosmic dust. Cloud placement and shape are fixed by the world seed and not configurable.").push("dust");
        DUST_CAPACITY = BUILDER.comment("Starter storage per dust kind.").defineInRange("capacity", 200.0, 1.0, 1_000_000.0);
        DUST_TRICKLE = BUILDER
                .comment("Dust per second collected at full density while not moving.")
                .defineInRange("trickle", 0.1, 0.0, 1000.0);
        DUST_SCOOP = BUILDER
                .comment("Dust per block travelled at full density (flying through a cloud scoops dust).")
                .defineInRange("scoop", 0.1, 0.0, 1000.0);
        CLOUD_DRAG = BUILDER
                .comment("Fraction of velocity lost per tick at full density (applies with the dampener off too).")
                .defineInRange("drag", 0.04, 0.0, 1.0);
        CLOUD_SPEED_PENALTY = BUILDER
                .comment("Top speed reduction at full density (0.6 = 60% slower in the densest core).")
                .defineInRange("speedPenalty", 0.6, 0.0, 0.95);
        BUILDER.pop();

        BUILDER.comment("Dust Sensor.").push("sensor");
        PULSE_ENERGY_COST = BUILDER.comment("Energy per sensor pulse.").defineInRange("pulseEnergyCost", 5.0, 0.0, 1000.0);
        PULSE_COOLDOWN_SECONDS = BUILDER.comment("Seconds between pulses.").defineInRange("pulseCooldown", 10, 0, 600);
        PULSE_RANGE = BUILDER.comment("How far a pulse can detect a cloud, in blocks.").defineInRange("pulseRange", 2048.0, 64.0, 16384.0);
        HUM_RANGE = BUILDER.comment("Range of the directional hum, in blocks from a cloud's edge.").defineInRange("humRange", 512.0, 16.0, 4096.0);
        BUILDER.pop();

        BUILDER.comment("Replicator and tools. Recipe costs and times are data-driven (datapack recipes).").push("replicator");
        REPLICATOR_ENERGY = BUILDER.comment("Energy per second while the Replicator is working.").defineInRange("energy", 1.5, 0.0, 1000.0);
        SALVAGE_REFUND = BUILDER
                .comment("Fraction of a block's Replicator dust cost refunded when the Salvage Tool dismantles it.")
                .defineInRange("salvageRefund", 0.5, 0.0, 1.0);
        NANITE_ENERGY = BUILDER.comment("Energy per block built by the Nanite Swarm.").defineInRange("naniteEnergy", 1.0, 0.0, 1000.0);
        BUILDER.pop();

        BUILDER.comment("Tether Anchor: leash and recall.").push("tether");
        TETHER_LENGTH = BUILDER.comment("How far you can drift from a clipped anchor, in blocks.").defineInRange("length", 16.0, 2.0, 256.0);
        RECALL_SPEED_FACTOR = BUILDER
                .comment("Recall pull speed as a multiple of top speed.")
                .defineInRange("recallSpeedFactor", 2.0, 1.0, 10.0);
        RECALL_PULL_RANGE = BUILDER
                .comment("Within this distance recall pulls you in; farther away it teleports.")
                .defineInRange("recallPullRange", 256.0, 8.0, 100_000.0);
        RECALL_PULL_ENERGY = BUILDER.comment("Energy per second while the recall pulls.").defineInRange("recallPullEnergy", 3.0, 0.0, 1000.0);
        RECALL_TELEPORT_ENERGY = BUILDER.comment("Energy for a long-range recall teleport.").defineInRange("recallTeleportEnergy", 25.0, 0.0, 1000.0);
        RECALL_TELEPORT_COOLDOWN = BUILDER.comment("Seconds between recall teleports.").defineInRange("recallTeleportCooldown", 60, 0, 3600);
        BUILDER.pop();

        BUILDER.comment("Body section wear: light and repairable; sections never break. No wear in creative.").push("wear");
        WEAR_ENABLED = BUILDER.comment("Whether sections wear at all.").define("enabled", true);
        WEAR_PER_THRUST_MINUTE = BUILDER.comment("Condition lost per minute of thrust by each section carrying a thruster (1.0 = all of it).")
                .defineInRange("perThrustMinute", 0.012, 0.0, 1.0);
        WEAR_PER_TOOL_USE = BUILDER.comment("Condition lost per tool module use by the section holding the tool.")
                .defineInRange("perToolUse", 0.002, 0.0, 1.0);
        REPAIR_COST_FRACTION = BUILDER.comment("Repairing a section from 0% costs this fraction of its recipe's dust (less when less worn).")
                .defineInRange("repairCostFraction", 0.2, 0.0, 10.0);
        BUILDER.pop();

        BUILDER.comment("Backup Cores.").push("backupCore");
        CORE_SUN_TICKS = BUILDER
                .comment("Ticks of full sunlight per respawn charge (deep space's distant sun is about 2/3 as fast).")
                .defineInRange("sunTicksPerCharge", 12_000, 20, 1_000_000);
        BUILDER.pop();

        BUILDER.comment("Materials from the four nebulae and the Create chains that process them. Switch off what you don't want.").push("materials");
        toggle("replicatorRawItems", "The Replicator can turn dust into the raw nebula items (Raw Ice Shards, Carbon Flakes, Silicon Grit, Metallic Grains).");
        toggle("iceChain", "Create recipes of the Ice nebula chain (cryo-ice, purified water, propellant, coolant).");
        toggle("carbonChain", "Create recipes of the Carbon nebula chain (powder, paste, composite plate, slurry, tiles, graphite).");
        toggle("siliconChain", "Create recipes of the Silicon nebula chain (silicon dust, liquid silicon, doped ingot, solar cell, sensor unit).");
        toggle("metallicChain", "Create recipes of the Metallic nebula chain (nodules, micro-alloy sheets).");
        toggle("solarSail", "The High-Output Solar Sail Sequenced Assembly recipe.");
        toggle("frame", "The Zero-G Precision Frame Sequenced Assembly recipe (the base of the payloads and machinery below).");
        toggle("dysonPayloads", "The Dyson Swarm Satellite Payload and Dyson Shell Truss Module recipes.");
        toggle("rcsThruster", "The RCS Thruster recipe.");
        toggle("reactionWheel", "The Reaction Wheel Gyroscope recipe.");
        toggle("catalystCore", "The Atmospheric Catalyst Core recipe.");
        BUILDER.pop();

        BUILDER.comment("Custom Create machines. All amounts are placeholders until the design settles them.").push("machines");
        toggle("electrolyzer", "The Electrolyzer block (water into oxygen and hydrogen) works and the Replicator can make it.");
        toggle("spinner", "The Spinner block (graphite into carbon thread) works and the Replicator can make it.");
        toggle("cloudSiphon", "The Cloud Siphon block (raw items out of a dense dust cloud) works and the Replicator can make it.");
        toggle("fanFreezing", "Encased Fans blowing through powder snow freeze items (Water Bucket into Packed Ice).");
        toggle("spoutCasting", "Spouts can cast Liquid Silicon into a Quartz Wafer on an empty Depot.");
        ELECTROLYZER_STRESS = BUILDER.comment("Stress the Electrolyzer needs, in SU per RPM.").defineInRange("electrolyzerStress", 4.0, 0.0, 1024.0);
        SPINNER_STRESS = BUILDER.comment("Stress the Spinner needs, in SU per RPM.").defineInRange("spinnerStress", 2.0, 0.0, 1024.0);
        SIPHON_STRESS = BUILDER.comment("Stress the Cloud Siphon needs, in SU per RPM.").defineInRange("siphonStress", 4.0, 0.0, 1024.0);
        SIPHON_RATE = BUILDER.comment("Raw items per second a Cloud Siphon makes at full cloud density and 64 RPM.").defineInRange("siphonRate", 0.5, 0.0, 1000.0);
        SIPHON_MIN_DENSITY = BUILDER.comment("Cloud density below which a Cloud Siphon makes nothing (thin dust).").defineInRange("siphonMinDensity", 0.4, 0.0, 1.0);
        MACHINE_MIN_RPM = BUILDER.comment("Slowest rotation at which the Electrolyzer and the Spinner work.").defineInRange("minRpm", 16, 1, 256);
        MACHINE_CYCLE_TICKS = BUILDER.comment("Ticks per cycle at 64 RPM; faster rotation shortens it in proportion.").defineInRange("cycleTicks", 60, 1, 24000);
        ELECTROLYZER_TANK = BUILDER.comment("Capacity in mB of each Electrolyzer tank.").defineInRange("electrolyzerTank", 4000, 250, 100000);
        ELECTROLYZER_WATER = BUILDER.comment("Water in mB used per Electrolyzer cycle.").defineInRange("electrolyzerWater", 250, 1, 10000);
        ELECTROLYZER_OXYGEN = BUILDER.comment("Oxygen Gas in mB made per Electrolyzer cycle.").defineInRange("electrolyzerOxygen", 125, 0, 10000);
        ELECTROLYZER_HYDROGEN = BUILDER.comment("Hydrogen Gas in mB made per Electrolyzer cycle.").defineInRange("electrolyzerHydrogen", 250, 0, 10000);
        SPINNER_THREADS = BUILDER.comment("Carbon Thread made from one High-Purity Graphite.").defineInRange("spinnerThreads", 2, 1, 64);
        CASTING_AMOUNT = BUILDER.comment("Liquid Silicon in mB cast into one Quartz Wafer.").defineInRange("castingAmount", 250, 1, 10000);
        BUILDER.pop();

        BUILDER.comment("Dyson launch and power: the Kinetic Mass Driver and the Sub-Light Quantum Receiver. Placeholder numbers.").push("dysonMachines");
        toggle("massDriver", "The Kinetic Mass Driver launches payloads into the Dyson Sphere, and the Replicator can make its blocks.");
        toggle("quantumReceiver", "The Sub-Light Quantum Receiver turns launched trusses into stress, and its Sequenced Assembly recipe exists.");
        MASS_DRIVER_STRESS = BUILDER.comment("Stress the Kinetic Drive Base needs, in SU per RPM.").defineInRange("massDriverStress", 32.0, 0.0, 4096.0);
        MASS_DRIVER_MIN_RPM = BUILDER.comment("Slowest rotation at which the Mass Driver launches.").defineInRange("massDriverMinRpm", 64, 1, 256);
        MASS_DRIVER_FAST_RPM = BUILDER.comment("Rotation at which the Mass Driver launches at its fastest.").defineInRange("massDriverFastRpm", 256, 1, 256);
        MASS_DRIVER_SLOW_TICKS = BUILDER.comment("Ticks between launches at the minimum speed (400 = 20 s).").defineInRange("massDriverSlowTicks", 400, 1, 24000);
        MASS_DRIVER_FAST_TICKS = BUILDER.comment("Ticks between launches at the fastest speed (60 = 3 s).").defineInRange("massDriverFastTicks", 60, 1, 24000);
        RECEIVER_RPM = BUILDER.comment("Rotation speed the Quantum Receiver outputs.").defineInRange("receiverRpm", 128, 1, 256);
        RECEIVER_MAX_STRESS = BUILDER.comment("Stress in SU the Receiver gives once the shell is complete; before that it is proportional to the trusses launched.")
                .defineInRange("receiverMaxStress", 262_144.0, 0.0, 100_000_000.0);
        BUILDER.pop();

        BUILDER.comment("The Cloud Collector module: raw items out of dust clouds while flying.").push("collector");
        COLLECTOR_ENABLED = BUILDER.comment("Whether installed collector modules produce items.").define("enabled", true);
        COLLECTOR_YIELD = BUILDER.comment("Multiplier on every collector module's yield (items per dust gathered).")
                .defineInRange("yield", 1.0, 0.0, 100.0);
        BUILDER.pop();

        BUILDER.comment("Mining asteroids with the Mechanical Drill and the Matter Harvester.").push("asteroids");
        DRILL_ENERGY = BUILDER.comment("Energy per asteroid block broken by either tool.").defineInRange("drillEnergy", 0.5, 0.0, 100.0);
        DRILL_DUST_YIELD = BUILDER.comment("Dust refunded per block by the Mechanical Drill, of the block's own kind.")
                .defineInRange("drillDustYield", 1.0, 0.0, 1000.0);
        BUILDER.pop();

        BUILDER.comment("Terraforming Spire.").push("terraform");
        SPIRE_RADIUS = BUILDER.comment("Blocks from the Spire to the edge of its square area. 128 makes 16 Spires tile the planet.")
                .defineInRange("spireRadius", 128, 1, 256);
        SPIRE_PULSE_TICKS = BUILDER.comment("Ticks the Genesis Pulse takes to sweep from the Spire to the edge of its area.")
                .defineInRange("pulseTicks", 300, 1, 24_000);
        BUILDER.pop();

        BUILDER.comment("Dyson Sphere.").push("dyson");
        DYSON_LATTICE_START = BUILDER.comment("Shell truss modules launched before the structural lattice (skybox tier 3) appears.")
                .defineInRange("latticeStart", com.voidreclaimer.dyson.DysonTiers.DEFAULT_LATTICE_START, 1L, 1_000_000_000L);
        DYSON_SHELL_TOTAL = BUILDER.comment("Shell truss modules that complete the sphere (skybox tier 4). Never below latticeStart.")
                .defineInRange("shellTotal", com.voidreclaimer.dyson.DysonTiers.DEFAULT_SHELL_TOTAL, 1L, 1_000_000_000L);
        BUILDER.pop();
    }

    public static final ModConfigSpec SPEC = BUILDER.build();

    private Config() {}
}
