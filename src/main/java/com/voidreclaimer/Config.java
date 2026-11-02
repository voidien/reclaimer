package com.voidreclaimer;

import com.voidreclaimer.link.ShellPrices;
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

    public static final ModConfigSpec.DoubleValue HELM_SPEED_CAP;
    public static final ModConfigSpec.DoubleValue CRASH_SAFE_SPEED;
    public static final ModConfigSpec.DoubleValue CRASH_AT_TOP_SPEED;
    public static final ModConfigSpec.BooleanValue CRASH_BREAKS_BLOCKS;
    public static final ModConfigSpec.IntValue CRASH_MOST_BLOCKS;
    public static final ModConfigSpec.DoubleValue BODY_TURN_FACTOR;
    public static final ModConfigSpec.DoubleValue BODY_TURN_SLOWEST;
    public static final ModConfigSpec.DoubleValue BODY_TURN_FASTEST;
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
    public static final ModConfigSpec.DoubleValue EARTH_WIND_SPEED;
    public static final ModConfigSpec.DoubleValue LINK_SEAT_RANGE;
    public static final ModConfigSpec.DoubleValue LINK_RELAY_RANGE;
    public static final ModConfigSpec.IntValue LINK_RELAY_FE_PER_TICK;
    public static final ModConfigSpec.IntValue LINK_RELAY_FE_BUFFER;
    public static final ModConfigSpec.DoubleValue LINK_DYSON_TIER_1_RANGE;
    public static final ModConfigSpec.DoubleValue LINK_DYSON_TIER_2_RANGE;
    public static final ModConfigSpec.DoubleValue LINK_DYSON_TIER_3_RANGE;
    public static final ModConfigSpec.DoubleValue LINK_DYSON_TIER_4_RANGE;
    public static final ModConfigSpec.DoubleValue LINK_STATIC_BAND;
    public static final ModConfigSpec.IntValue LINK_SIGNAL_GRACE;
    public static final ModConfigSpec.EnumValue<ShellPrices> LINK_SHELL_PRICES;
    public static final ModConfigSpec.DoubleValue LINK_STATION_RANGE;
    public static final ModConfigSpec.DoubleValue LINK_STATION_STATIC_BAND;
    public static final ModConfigSpec.DoubleValue LINK_SEAT_CHARGE;
    public static final ModConfigSpec.DoubleValue LINK_STATION_CHARGE;
    public static final ModConfigSpec.DoubleValue STATION_RANGE;
    public static final ModConfigSpec.DoubleValue HELPER_SPEED;
    public static final ModConfigSpec.IntValue HELPER_CARRY;
    public static final ModConfigSpec.DoubleValue HELPER_REACH;
    public static final ModConfigSpec.IntValue HELPER_PAUSE;
    public static final ModConfigSpec.IntValue HELPER_MINE_RADIUS;
    public static final ModConfigSpec.IntValue HELPER_DIG_TICKS;
    public static final ModConfigSpec.DoubleValue HELPER_TANK;
    public static final ModConfigSpec.DoubleValue HELPER_COLLECT_RATE;
    public static final ModConfigSpec.DoubleValue HELPER_OWNER_RANGE;
    public static final ModConfigSpec.IntValue HELPER_SEARCH_NODES;
    public static final ModConfigSpec.IntValue HELPER_BLOCKED_RETRY;
    public static final ModConfigSpec.IntValue STATION_REPLICATOR_SPEED;
    public static final ModConfigSpec.IntValue STATION_MAX_BLOCKS;
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

    /** The setting of a config choice, in lower case, or the empty string for an unknown name. Before the config has loaded, its default. */
    public static String choice(String key) {
        if (!"shellPrices".equals(key)) {
            return "";
        }
        ShellPrices value;
        try {
            value = LINK_SHELL_PRICES.get();
        } catch (IllegalStateException e) {
            value = LINK_SHELL_PRICES.getDefault();
        }
        return value.name().toLowerCase(java.util.Locale.ROOT);
    }

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
    public static final ModConfigSpec.IntValue SIPHON_FE_PER_TICK;
    public static final ModConfigSpec.IntValue SIPHON_FE_BUFFER;
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

    public static final ModConfigSpec.DoubleValue CHEMICAL_THRUST;
    public static final ModConfigSpec.DoubleValue THRUSTER_STRESS;
    public static final ModConfigSpec.IntValue THRUSTER_TANK;
    public static final ModConfigSpec.DoubleValue ION_THRUST;
    public static final ModConfigSpec.IntValue ION_FE_PER_TICK;
    public static final ModConfigSpec.IntValue ION_FE_BUFFER;
    public static final ModConfigSpec.DoubleValue RCS_THRUST;
    public static final ModConfigSpec.IntValue RCS_FE_PER_TICK;
    public static final ModConfigSpec.IntValue RCS_FE_BUFFER;

    public static final ModConfigSpec.IntValue HOLO_REFRESH_TICKS;
    public static final ModConfigSpec.IntValue HOLO_SOURCE_RANGE;
    public static final ModConfigSpec.IntValue HOLO_MAP_RADIUS;
    public static final ModConfigSpec.IntValue HOLO_MAP_REFRESH_TICKS;
    public static final ModConfigSpec.BooleanValue EARTH_ACID_DRIZZLE;
    public static final ModConfigSpec.DoubleValue EARTH_ACID_WEAR;
    public static final ModConfigSpec.IntValue SPIRE_PULSE_TICKS;
    public static final ModConfigSpec.IntValue SPIRE_CATALYSTS;
    public static final ModConfigSpec.IntValue SPIRE_WATER;
    public static final ModConfigSpec.IntValue SPIRE_SLURRY;
    public static final ModConfigSpec.IntValue SPIRE_TANK;
    public static final ModConfigSpec.IntValue SPIRE_CHARGE_TICKS;
    public static final ModConfigSpec.IntValue SPIRE_MIN_RPM;
    public static final ModConfigSpec.DoubleValue SPIRE_STRESS;

    public static final ModConfigSpec.LongValue DYSON_LATTICE_START;
    public static final ModConfigSpec.LongValue DYSON_SHELL_TOTAL;

    static {
        BUILDER.comment("Where new and respawning players appear in void_reclaimer:deep_space.").push("spawn");
        SPAWN_X = BUILDER.defineInRange("x", 0, -29_000_000, 29_000_000);
        SPAWN_Y = BUILDER.defineInRange("y", 64, -2000, 2000);
        SPAWN_Z = BUILDER.defineInRange("z", 0, -29_000_000, 29_000_000);
        BUILDER.pop();

        BUILDER.comment("How big bodies turn with weight. The view turns at once and the body follows.").push("bodyTurning");
        BODY_TURN_FACTOR = BUILDER
                .comment("Degrees a second for each unit of the body's turn rate per unit of its mass. Higher turns faster. At the default a Shuttle takes 1.5 s for a half turn, a Ship 4 s and a Titan 12 s.")
                .defineInRange("factor", com.voidreclaimer.body.BodyHeading.TURN_FACTOR, 1.0, 100_000.0);
        BODY_TURN_SLOWEST = BUILDER.comment("Degrees a second for a body with nothing to help it turn.")
                .defineInRange("slowest", com.voidreclaimer.body.BodyHeading.SLOWEST_TURN, 0.1, 720.0);
        BODY_TURN_FASTEST = BUILDER.comment("Degrees a second for the lightest big body.")
                .defineInRange("fastest", com.voidreclaimer.body.BodyHeading.FASTEST_TURN, 0.1, 720.0);
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
                .comment("Ruined Earth: weight the thrusters must cancel = the body's gravity times this (vanilla gravity is 0.08 blocks/tick²). "
                        + "Thrust over mass of the full presets is Drone 0.015, Mech 0.023, Ship 0.014, Shuttle 0.014, Titan 0.0044, so 0.12 (weight 0.0096) lets "
                        + "every player body hover and leaves the Titan too heavy.")
                .defineInRange("hoverWeightFactor", 0.12, 0.0, 4.0);
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

        BUILDER.comment("Helper drones.").push("helper");
        HELPER_SPEED = BUILDER.comment("Flight speed in blocks per tick.").defineInRange("speed", 0.35, 0.05, 2.0);
        HELPER_CARRY = BUILDER.comment("Items a drone carries per trip.").defineInRange("carry", 16, 1, 64);
        HELPER_REACH = BUILDER.comment("How close to its hover point a drone must be to load or unload.").defineInRange("reach", 1.5, 0.5, 4.0);
        HELPER_PAUSE = BUILDER.comment("Ticks a drone waits when there is nothing to take or no room to give.").defineInRange("pause", 40, 1, 1200);
        HELPER_MINE_RADIUS = BUILDER.comment("How far from its mine point a mining drone looks for asteroid rock, in blocks.").defineInRange("mineRadius", 8, 1, 32);
        HELPER_DIG_TICKS = BUILDER.comment("Ticks a mining drone digs one asteroid block.").defineInRange("digTicks", 40, 1, 1200);
        HELPER_TANK = BUILDER.comment("Dust a collecting drone holds before bringing it to its owner.").defineInRange("tank", 50.0, 0.1, 10000.0);
        HELPER_COLLECT_RATE = BUILDER.comment("A collecting drone gathers this many times what a still body would in the same cloud.").defineInRange("collectRate", 1.0, 0.01, 100.0);
        HELPER_OWNER_RANGE = BUILDER.comment("How far a collecting drone will fly to its owner, in blocks.").defineInRange("ownerRange", 96.0, 8.0, 512.0);
        HELPER_SEARCH_NODES = BUILDER.comment("Cells one search for a way round blocks may look at. More finds ways round bigger walls and costs more when a drone is stuck.").defineInRange("searchNodes", 2048, 64, 65536);
        HELPER_BLOCKED_RETRY = BUILDER.comment("Ticks a drone with no way to its target holds still before it looks again.").defineInRange("blockedRetry", 100, 10, 12000);
        BUILDER.pop();

        BUILDER.comment("Remote control. The ranges are first guesses, all placeholders.").push("link");
        LINK_SEAT_RANGE = BUILDER.comment("Blocks a seat reaches with no Dyson tier.").defineInRange("seatRange", 1024.0, 1.0, 100_000_000.0);
        LINK_RELAY_RANGE = BUILDER.comment("Blocks a relay reaches with no Dyson tier.").defineInRange("relayRange", 2048.0, 1.0, 100_000_000.0);
        LINK_RELAY_FE_PER_TICK = BUILDER.comment("Forge Energy a Relay uses each tick. Without it the relay goes dark.")
                .defineInRange("relayFePerTick", 20, 0, 1_000_000);
        LINK_RELAY_FE_BUFFER = BUILDER.comment("Forge Energy a Relay holds.").defineInRange("relayFeBuffer", 2000, 1, 100_000_000);
        LINK_DYSON_TIER_1_RANGE = BUILDER.comment("Blocks Dyson tier 1 adds to every seat and relay.").defineInRange("dysonTier1Range", 2048.0, 0.0, 100_000_000.0);
        LINK_DYSON_TIER_2_RANGE = BUILDER.comment("Blocks Dyson tier 2 adds on top of tier 1.").defineInRange("dysonTier2Range", 16_384.0, 0.0, 100_000_000.0);
        LINK_DYSON_TIER_3_RANGE = BUILDER.comment("Blocks Dyson tier 3 adds on top of tier 2.").defineInRange("dysonTier3Range", 262_144.0, 0.0, 100_000_000.0);
        LINK_DYSON_TIER_4_RANGE = BUILDER.comment("Blocks Dyson tier 4 adds on top of tier 3: near the whole galaxy.").defineInRange("dysonTier4Range", 20_000_000.0, 0.0, 100_000_000.0);
        LINK_STATIC_BAND = BUILDER.comment("The outer share of a range that carries static: strength falls from full to nothing across it.")
                .defineInRange("staticBand", 0.25, 0.0, 1.0);
        LINK_STATION_RANGE = BUILDER.comment("A seat at a station multiplies its range by this.").defineInRange("stationRange", 1.5, 1.0, 16.0);
        LINK_SHELL_PRICES = BUILDER.comment("How dear Remote Shells are: CHEAP (well under the parts inside), REDUCED (3/4 of them), PARTS (what they cost), PREMIUM (5/4).",
                "Recipes read this when they load: restart or /reload after changing it.").defineEnum("shellPrices", ShellPrices.PARTS);
        LINK_SIGNAL_GRACE = BUILDER.comment("Seconds a link stands after its signal is lost, with a countdown; a signal back in time keeps it.").defineInRange("signalGrace", 5, 0, 60);
        LINK_STATION_STATIC_BAND = BUILDER.comment("staticBand for a seat at a station, across its whole network, so static starts later.")
                .defineInRange("stationStaticBand", 0.1, 0.0, 1.0);
        LINK_SEAT_CHARGE = BUILDER.comment("Energy per second an Uplink Seat gives the main body while the mind is away.")
                .defineInRange("seatCharge", 0.5, 0.0, 1000.0);
        LINK_STATION_CHARGE = BUILDER.comment("A seat within station range of a Station Core charges this many times faster (1 = no bonus).")
                .defineInRange("stationCharge", 2.0, 1.0, 100.0);
        BUILDER.pop();

        BUILDER.comment("Crashes: a body flying into something.").push("crash");
        CRASH_SAFE_SPEED = BUILDER.comment("Blocks a second along what is hit. Under this nothing happens.").defineInRange("safeSpeed", 5.0, 0.0, 1000.0);
        CRASH_AT_TOP_SPEED = BUILDER.comment("The share of its health a body loses in a head-on hit at its top speed. It grows with the square of the speed over the safe one. At 1 such a hit destroys it.")
                .defineInRange("atTopSpeed", 1.0, 0.0, 100.0);
        CRASH_BREAKS_BLOCKS = BUILDER.comment("Whether a big body breaks the blocks it hits, the weak ones first.").define("breaksBlocks", true);
        CRASH_MOST_BLOCKS = BUILDER.comment("The most blocks one hit can break.").defineInRange("mostBlocks", 32, 0, 1024);
        BUILDER.pop();

        BUILDER.comment("Ships.").push("ship");
        HELM_SPEED_CAP = BUILDER.comment("Top speed of a ship flown from a Helm, in blocks a second. Keys stop adding speed here and the thrusters hold it down. A ship on redstone has no cap.")
                .defineInRange("helmSpeedCap", 100.0, 1.0, 10_000.0);
        BUILDER.pop();

        BUILDER.comment("Stations.").push("station");
        STATION_RANGE = BUILDER.comment("Within this many blocks of a Station Core the body counts as being at the station.")
                .defineInRange("range", 8.0, 1.0, 64.0);
        STATION_REPLICATOR_SPEED = BUILDER.comment("Replicator progress per tick while at a station (1 = no bonus).")
                .defineInRange("replicatorSpeed", 2, 1, 10);
        STATION_MAX_BLOCKS = BUILDER.comment("Most blocks a Station Core will assemble into one station.")
                .defineInRange("maxBlocks", 50_000, 2, 1_000_000);
        BUILDER.pop();

        BUILDER.comment("Ruined Earth.").push("earth");
        EARTH_WIND_SPEED = BUILDER
                .comment("Top speed of the wind over ruined Earth, in blocks per second; it drifts ships and the flying body (the dampener holds). 0 switches it off.")
                .defineInRange("windSpeed", 1.5, 0.0, 20.0);
        EARTH_ACID_DRIZZLE = BUILDER.comment("Acid Drizzle falls on ruined Earth outside restored sectors, and slowly wears body sections under the open sky.")
                .define("acidDrizzle", true);
        EARTH_ACID_WEAR = BUILDER.comment("Condition each section loses per minute in full Acid Drizzle (1.0 = all of it). Less as the planet heals. It never breaks a section.")
                .defineInRange("acidWearPerMinute", 0.003, 0.0, 1.0);
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
        toggle("bioSlurry", "The Bio-Nutrient Slurry recipe (mixed in a basin; the Terraforming Spire drinks it).");
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
        SIPHON_FE_PER_TICK = BUILDER.comment("Forge Energy per tick a Cloud Siphon with no shaft needs to run at full power (energy tops a shaft up to full and never costs more than that). 0 turns the energy input off.")
                .defineInRange("siphonFePerTick", 40, 0, 1_000_000);
        SIPHON_FE_BUFFER = BUILDER.comment("Forge Energy a Cloud Siphon can hold (100 ticks of full power by default).")
                .defineInRange("siphonFeBuffer", 4000, 0, 100_000_000);
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
        toggle("massDriver", "The Launch Complex launches payloads into the Dyson Sphere, and the Replicator can make its controller.");
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

        BUILDER.comment("Block ship thrusters.").push("ships");
        toggle("chemicalThruster", "The Chemical Thruster pushes ships.");
        CHEMICAL_THRUST = BUILDER.comment("Force of a Chemical Thruster at full throttle, full spin (64 RPM) and a fuel of thrust 1.")
                .defineInRange("chemicalThrust", 400.0, 0.0, 1_000_000.0);
        THRUSTER_STRESS = BUILDER.comment("Stress a Chemical Thruster needs, in SU per RPM.").defineInRange("thrusterStress", 8.0, 0.0, 1024.0);
        THRUSTER_TANK = BUILDER.comment("mB of fuel a Chemical Thruster holds.").defineInRange("thrusterTank", 4000, 1, 1_000_000);
        toggle("ionThruster", "The Ion Thruster pushes ships.");
        ION_THRUST = BUILDER.comment("Force of an Ion Thruster at full throttle.").defineInRange("ionThrust", 60.0, 0.0, 1_000_000.0);
        ION_FE_PER_TICK = BUILDER.comment("Forge Energy per tick an Ion Thruster uses at full throttle.").defineInRange("ionFePerTick", 80, 0, 1_000_000);
        ION_FE_BUFFER = BUILDER.comment("Forge Energy an Ion Thruster can hold.").defineInRange("ionFeBuffer", 8000, 1, 100_000_000);
        toggle("rcsPush", "A placed RCS Thruster pushes ships.");
        RCS_THRUST = BUILDER.comment("Force of an RCS Thruster at full throttle.").defineInRange("rcsThrust", 40.0, 0.0, 1_000_000.0);
        RCS_FE_PER_TICK = BUILDER.comment("Forge Energy per tick an RCS Thruster uses at full throttle.").defineInRange("rcsFePerTick", 20, 0, 1_000_000);
        RCS_FE_BUFFER = BUILDER.comment("Forge Energy an RCS Thruster can hold.").defineInRange("rcsFeBuffer", 2000, 1, 100_000_000);
        BUILDER.pop();

        BUILDER.comment("Holo Projector.").push("holo");
        toggle("holoProjector", "The Replicator can make the Holo Projector.");
        HOLO_REFRESH_TICKS = BUILDER.comment("Ticks between refreshes of a projector showing a readout.").defineInRange("refreshTicks", 10, 1, 1200);
        HOLO_SOURCE_RANGE = BUILDER.comment("How far a readout looks for the machine it shows, in blocks.").defineInRange("sourceRange", 8, 1, 32);
        HOLO_MAP_RADIUS = BUILDER.comment("Blocks from the middle of a map hologram to its edge.").defineInRange("mapRadius", 64, 16, 256);
        HOLO_MAP_REFRESH_TICKS = BUILDER.comment("Ticks between new pictures of a map hologram.").defineInRange("mapRefreshTicks", 100, 20, 1200);
        BUILDER.pop();

        BUILDER.comment("Terraforming Spire.").push("terraform");
        SPIRE_PULSE_TICKS = BUILDER.comment("Ticks the Genesis Pulse takes to sweep from the tower to the far edge of its sector.")
                .defineInRange("pulseTicks", 300, 1, 24_000);
        SPIRE_CATALYSTS = BUILDER.comment("Items from #void_reclaimer:spire/catalysts one pulse burns.").defineInRange("catalysts", 1, 0, 64);
        SPIRE_WATER = BUILDER.comment("Millibuckets of Purified Water one pulse burns.").defineInRange("water", 8_000, 0, 1_000_000);
        SPIRE_SLURRY = BUILDER.comment("Millibuckets of Bio-Nutrient Slurry one pulse burns.").defineInRange("slurry", 4_000, 0, 1_000_000);
        SPIRE_TANK = BUILDER.comment("Size of each of the Spire's two tanks, in millibuckets.").defineInRange("tank", 16_000, 1, 1_000_000);
        SPIRE_CHARGE_TICKS = BUILDER.comment("Ticks a pulse charges for at 64 RPM, shorter when faster.").defineInRange("chargeTicks", 400, 1, 24_000);
        SPIRE_MIN_RPM = BUILDER.comment("Slowest shaft that charges a pulse.").defineInRange("minRpm", 32, 0, 256);
        SPIRE_STRESS = BUILDER.comment("Stress the Spire needs, in SU per RPM.").defineInRange("stress", 32.0, 0.0, 1024.0);
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
