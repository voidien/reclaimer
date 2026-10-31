package com.voidreclaimer.gametest;

import com.voidreclaimer.VoidReclaimer;
import com.voidreclaimer.space.ModDimensions;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.minecraft.commands.CommandSource;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * Create Aeronautics / Sable inside deep space. Separate from the main GameTests: run with
 * {@code ./gradlew runGameTestAeronautics} (its own namespace {@value #NAMESPACE}), so an upstream problem shows up
 * as its own CI job.
 *
 * <p>Sub-levels are spawned and read through Sable's own commands. The world is cleared of ships once per run: a thruster
 * ship saved by an earlier run keeps flying when the world loads.
 */
@GameTestHolder(AeronauticsGameTests.NAMESPACE)
@PrefixGameTestTemplate(false)
public final class AeronauticsGameTests {
    public static final String NAMESPACE = VoidReclaimer.MOD_ID + "_aero";

    private static final Pattern POSITION = Pattern.compile("Position: (-?[\\d.]+) (-?[\\d.]+) (-?[\\d.]+)");
    private static final int WATCH_TICKS = 100;
    /** A sub-level that sits still reads within this many blocks of where it started. */
    private static final double STILL = 1.0;
    private static final Pattern ANGULAR = Pattern.compile("Angular Velocity: (\\S+) (\\S+) (\\S+)");
    private static final double TURNING = 0.05;
    /** A held ship is this slow (blocks per second) and this still (radians per second) after the holds have worked. */
    private static final double STOPPED = 0.6;
    /** How long the holds get to stop the shoved ship: 8 s. */
    private static final int HOLD_TICKS = 160;
    private static final double STOPPED_SPIN = 0.15;
    private static final int SETTLE_TICKS = 10;
    private static final int SAMPLE_TICKS = 40;
    /** A pushed ship travels at least this far in {@link #SAMPLE_TICKS}. */
    private static final double PUSHED = 3.0;
    private static final AtomicBoolean EARLIER_RUNS_CLEARED = new AtomicBoolean();

    private AeronauticsGameTests() {}

    /** The shipped dimension_physics file keeps a platform from falling in deep space. */
    @GameTest(template = "empty", timeoutTicks = 300)
    public static void platformStaysPutInDeepSpace(GameTestHelper helper) {
        BlockPos pos = new BlockPos(78_000, 200, 3_000);
        MinecraftServer server = helper.getLevel().getServer();
        ServerLevel space = server.getLevel(ModDimensions.DEEP_SPACE);
        space.setChunkForced(pos.getX() >> 4, pos.getZ() >> 4, true);
        helper.runAfterDelay(5, () -> {
            clearArea(server, space, pos);
            List<String> spawn = command(server, space, pos, "sable spawn platform 2 minecraft:stone");
            helper.runAfterDelay(1, () -> {
                nameShip(server, space, pos);
                helper.runAfterDelay(WATCH_TICKS, () -> finish(helper, server, space, pos, spawn));
            });
        });
    }

    /** Aeronautics propellers need air: in vacuum a ship with a running propeller must not move. */
    @GameTest(template = "empty", timeoutTicks = 300)
    public static void propellerGivesNoThrustInVacuum(GameTestHelper helper) {
        BlockPos pos = new BlockPos(79_000, 200, 3_000);
        MinecraftServer server = helper.getLevel().getServer();
        ServerLevel space = server.getLevel(ModDimensions.DEEP_SPACE);
        space.setChunkForced(pos.getX() >> 4, pos.getZ() >> 4, true);
        helper.runAfterDelay(5, () -> {
            clearArea(server, space, pos);
            int x = pos.getX(), y = pos.getY(), z = pos.getZ();
            List<String> log = new ArrayList<>();
            log.addAll(command(server, space, pos, "fill " + (x - 1) + " " + y + " " + (z - 1) + " " + (x + 1) + " " + y + " " + (z + 1) + " minecraft:stone"));
            log.addAll(command(server, space, pos, "setblock " + x + " " + (y + 1) + " " + z + " create:creative_motor[facing=up]"));
            log.addAll(command(server, space, pos, "setblock " + x + " " + (y + 2) + " " + z + " aeronautics:andesite_propeller[facing=up]"));
            helper.runAfterDelay(10, () -> {
                log.addAll(command(server, space, pos, "sable assemble connected " + x + " " + y + " " + z));
                coasts(helper, server, space, pos, log);
            });
        });
    }

    /**
     * Names the new ship, then calls {@code then} with its positions {@value #SETTLE_TICKS} and {@value #SETTLE_TICKS} +
     * {@value #SAMPLE_TICKS} ticks later, and removes it.
     */
    private static void watch(GameTestHelper helper, MinecraftServer server, ServerLevel space, BlockPos pos, List<String> log,
                              java.util.function.BiConsumer<Vec3, Vec3> then) {
        helper.runAfterDelay(1, () -> {
            nameShip(server, space, pos);
            helper.runAfterDelay(SETTLE_TICKS, () -> {
                Vec3 first = shipPosition(server, space, pos);
                helper.runAfterDelay(SAMPLE_TICKS, () -> {
                    Vec3 second = shipPosition(server, space, pos);
                    removeShip(server, space, pos);
                    if (first == null || second == null) {
                        helper.fail("lost the ship of " + pos + ": " + first + ", " + second + "; setup: " + log);
                        return;
                    }
                    then.accept(first, second);
                });
            });
        });
    }

    private static void coasts(GameTestHelper helper, MinecraftServer server, ServerLevel space, BlockPos pos, List<String> log) {
        watch(helper, server, space, pos, log, (first, second) -> {
            if (first.distanceTo(second) > STILL) {
                helper.fail("something moved the ship from " + first + " to " + second + "; setup: " + log);
            } else {
                helper.succeed();
            }
        });
    }

    /**
     * A small balanced ship: a Chemical Thruster with its nozzle east, so it pushes west, a creative motor turning its back,
     * and a redstone block as the throttle. Then it is assembled into a Sable sub-level.
     */
    private static void thrusterShip(GameTestHelper helper, BlockPos pos, boolean fuelled, boolean powered, java.util.function.Consumer<List<String>> then) {
        MinecraftServer server = helper.getLevel().getServer();
        ServerLevel space = server.getLevel(ModDimensions.DEEP_SPACE);
        space.setChunkForced(pos.getX() >> 4, pos.getZ() >> 4, true);
        helper.runAfterDelay(5, () -> {
            clearArea(server, space, pos);
            int x = pos.getX(), y = pos.getY(), z = pos.getZ();
            List<String> log = new ArrayList<>();
            cross(server, space, pos, powered, log);
            log.addAll(command(server, space, pos, "setblock " + (x - 1) + " " + y + " " + z + " create:creative_motor[facing=east]"));
            log.addAll(command(server, space, pos, "setblock " + x + " " + y + " " + z + " void_reclaimer:chemical_thruster[facing=east]"));
            if (fuelled) {
                var thruster = (com.voidreclaimer.ship.ChemicalThrusterBlockEntity) space.getBlockEntity(pos);
                int filled = thruster.tank().fill(new net.neoforged.neoforge.fluids.FluidStack(
                        com.voidreclaimer.registry.ModFluids.NEBULITE_PROPELLANT.source().get(), 4000),
                        net.neoforged.neoforge.fluids.capability.IFluidHandler.FluidAction.EXECUTE);
                log.add("filled " + filled);
            }
            helper.runAfterDelay(10, () -> {
                log.addAll(command(server, space, pos, "sable assemble connected " + x + " " + y + " " + z));
                then.accept(log);
            });
        });
    }

    /** A cross of blocks around a thruster, redstone on top if it is powered: the force goes close to the centre of mass. */
    static void cross(MinecraftServer server, ServerLevel space, BlockPos pos, boolean powered, List<String> log) {
        for (BlockPos arm : List.of(pos.north(), pos.south(), pos.below())) {
            log.addAll(command(server, space, pos, "setblock " + arm.getX() + " " + arm.getY() + " " + arm.getZ() + " minecraft:stone"));
        }
        BlockPos top = pos.above();
        log.addAll(command(server, space, pos, "setblock " + top.getX() + " " + top.getY() + " " + top.getZ() + " "
                + (powered ? "minecraft:redstone_block" : "minecraft:stone")));
    }

    /** The same ship with an electric thruster, nozzle east: no motor, and a charged buffer instead of fuel. */
    private static void electricShip(GameTestHelper helper, BlockPos pos, String thrusterBlock, boolean charged, java.util.function.Consumer<List<String>> then) {
        MinecraftServer server = helper.getLevel().getServer();
        ServerLevel space = server.getLevel(ModDimensions.DEEP_SPACE);
        space.setChunkForced(pos.getX() >> 4, pos.getZ() >> 4, true);
        for (int chunks = 1; chunks <= 8; chunks++) {
            space.setChunkForced((pos.getX() >> 4) - chunks, pos.getZ() >> 4, true);
        }
        helper.runAfterDelay(5, () -> {
            clearArea(server, space, pos);
            List<String> log = new ArrayList<>();
            cross(server, space, pos, true, log);
            log.addAll(command(server, space, pos, "setblock " + pos.getX() + " " + pos.getY() + " " + pos.getZ() + " " + thrusterBlock + "[facing=east]"));
            if (charged) {
                var thruster = (com.voidreclaimer.ship.ElectricThrusterBlockEntity) space.getBlockEntity(pos);
                log.add("charged " + thruster.energy().receiveEnergy(8000, false));
            }
            helper.runAfterDelay(10, () -> {
                log.addAll(command(server, space, pos, "sable assemble connected " + pos.getX() + " " + pos.getY() + " " + pos.getZ()));
                then.accept(log);
            });
        });
    }

    @GameTest(template = "empty", timeoutTicks = 400)
    public static void anIonThrusterPushesItsShip(GameTestHelper helper) {
        BlockPos pos = new BlockPos(83_000, 200, 3_000);
        MinecraftServer server = helper.getLevel().getServer();
        ServerLevel space = server.getLevel(ModDimensions.DEEP_SPACE);
        electricShip(helper, pos, "void_reclaimer:ion_thruster", true, log -> watch(helper, server, space, pos, log, (first, second) -> {
            double west = first.x - second.x;
            if (west < PUSHED) {
                helper.fail("the ship moved " + String.format("%.2f", west) + " blocks west, from " + first + " to " + second + "; setup: " + log);
            } else {
                helper.succeed();
            }
        }));
    }

    @GameTest(template = "empty", timeoutTicks = 400)
    public static void anIonThrusterWithoutEnergyDoesNotPush(GameTestHelper helper) {
        BlockPos pos = new BlockPos(84_000, 200, 3_000);
        MinecraftServer server = helper.getLevel().getServer();
        ServerLevel space = server.getLevel(ModDimensions.DEEP_SPACE);
        electricShip(helper, pos, "void_reclaimer:ion_thruster", false, log -> coasts(helper, server, space, pos, log));
    }

    @GameTest(template = "empty", timeoutTicks = 400)
    public static void anRcsThrusterPushesItsShip(GameTestHelper helper) {
        BlockPos pos = new BlockPos(85_000, 200, 3_000);
        MinecraftServer server = helper.getLevel().getServer();
        ServerLevel space = server.getLevel(ModDimensions.DEEP_SPACE);
        electricShip(helper, pos, "void_reclaimer:rcs_thruster", true, log -> watch(helper, server, space, pos, log, (first, second) -> {
            double west = first.x - second.x;
            if (west < PUSHED) {
                helper.fail("the ship moved " + String.format("%.2f", west) + " blocks west, from " + first + " to " + second + "; setup: " + log);
            } else {
                helper.succeed();
            }
        }));
    }

    /** What RCS is for: a jet at the end of a long ship, firing across it, starts the ship turning. */
    @GameTest(template = "empty", timeoutTicks = 400)
    public static void anOffCentreRcsThrusterTurnsItsShip(GameTestHelper helper) {
        BlockPos pos = new BlockPos(86_000, 200, 3_000);
        MinecraftServer server = helper.getLevel().getServer();
        ServerLevel space = server.getLevel(ModDimensions.DEEP_SPACE);
        space.setChunkForced(pos.getX() >> 4, pos.getZ() >> 4, true);
        helper.runAfterDelay(5, () -> {
            clearArea(server, space, pos);
            List<String> log = new ArrayList<>();
            int x = pos.getX(), y = pos.getY(), z = pos.getZ();
            log.addAll(command(server, space, pos, "fill " + (x + 1) + " " + y + " " + z + " " + (x + 8) + " " + y + " " + z + " minecraft:stone"));
            log.addAll(command(server, space, pos, "setblock " + x + " " + y + " " + z + " void_reclaimer:rcs_thruster[facing=up]"));
            log.addAll(command(server, space, pos, "setblock " + (x - 1) + " " + y + " " + z + " minecraft:redstone_block"));
            var thruster = (com.voidreclaimer.ship.ElectricThrusterBlockEntity) space.getBlockEntity(pos);
            log.add("charged " + thruster.energy().receiveEnergy(2000, false));
            helper.runAfterDelay(10, () -> {
                log.addAll(command(server, space, pos, "sable assemble connected " + x + " " + y + " " + z));
                helper.runAfterDelay(1, () -> {
                    nameShip(server, space, pos);
                    helper.runAfterDelay(SAMPLE_TICKS, () -> {
                        List<String> info = command(server, space, pos, "sable info " + shipSelector(pos));
                        double spin = angularSpeed(info);
                        removeShip(server, space, pos);
                        if (spin < TURNING) {
                            helper.fail("the ship did not turn: angular speed " + spin + "; info: " + info);
                        } else {
                            helper.succeed();
                        }
                    });
                });
            });
        });
    }

    private static double angularSpeed(List<String> info) {
        for (String line : info) {
            Matcher m = ANGULAR.matcher(line);
            if (m.find()) {
                return Math.sqrt(Math.pow(Double.parseDouble(m.group(1)), 2) + Math.pow(Double.parseDouble(m.group(2)), 2)
                        + Math.pow(Double.parseDouble(m.group(3)), 2));
            }
        }
        return 0.0;
    }

    /**
     * A ship with a Helm facing north, a Chemical Thruster at the stern (nozzle south, so it pushes north) and an RCS jet at
     * the bow pushing east (yaw). No redstone anywhere: only the pilot's keys open throttles.
     */
    private static void helmShip(GameTestHelper helper, BlockPos pos, java.util.function.Consumer<List<String>> then) {
        MinecraftServer server = helper.getLevel().getServer();
        ServerLevel space = server.getLevel(ModDimensions.DEEP_SPACE);
        space.setChunkForced(pos.getX() >> 4, pos.getZ() >> 4, true);
        for (int chunks = 1; chunks <= 8; chunks++) {
            space.setChunkForced(pos.getX() >> 4, (pos.getZ() >> 4) - chunks, true);
        }
        helper.runAfterDelay(5, () -> {
            clearArea(server, space, pos);
            int x = pos.getX(), y = pos.getY(), z = pos.getZ();
            List<String> log = new ArrayList<>();
            // Keel from the bow (z-3) to the stern (z+2); the Helm in the middle, on top of the keel.
            // Keel from the bow (z-3) to the stern; the Helm on top in the middle; the motor on the keel feeds the
            // stern thruster from its back (a south-facing nozzle takes its shaft from the north).
            log.addAll(command(server, space, pos, "fill " + x + " " + y + " " + (z - 3) + " " + x + " " + y + " " + (z + 1) + " minecraft:stone"));
            log.addAll(command(server, space, pos, "setblock " + x + " " + (y + 1) + " " + z + " void_reclaimer:helm[facing=north]"));
            log.addAll(command(server, space, pos, "setblock " + x + " " + y + " " + (z + 2) + " create:creative_motor[facing=south]"));
            log.addAll(command(server, space, pos, "setblock " + x + " " + y + " " + (z + 3) + " void_reclaimer:chemical_thruster[facing=south]"));
            log.addAll(command(server, space, pos, "setblock " + x + " " + y + " " + (z - 4) + " void_reclaimer:rcs_thruster[facing=west]"));
            var main = (com.voidreclaimer.ship.ChemicalThrusterBlockEntity) space.getBlockEntity(new BlockPos(x, y, z + 3));
            main.tank().fill(new net.neoforged.neoforge.fluids.FluidStack(com.voidreclaimer.registry.ModFluids.NEBULITE_PROPELLANT.source().get(), 4000),
                    net.neoforged.neoforge.fluids.capability.IFluidHandler.FluidAction.EXECUTE);
            var jet = (com.voidreclaimer.ship.ElectricThrusterBlockEntity) space.getBlockEntity(new BlockPos(x, y, z - 4));
            jet.energy().receiveEnergy(2000, false);
            helper.runAfterDelay(10, () -> {
                log.addAll(command(server, space, pos, "sable assemble connected " + x + " " + y + " " + z));
                then.accept(log);
            });
        });
    }

    /**
     * Seats a fresh survival player at the Helm of this test's own ship (named after {@code pos}) and gives them the keys
     * held. After assembly the Helm lives in the ship's plot, so it is found by scanning that ship's block entities; the
     * pilot stands where the Helm is in the world.
     */
    private static ServerPlayer pilot(GameTestHelper helper, ServerLevel space, BlockPos pos, com.voidreclaimer.ship.PilotInput keys) {
        dev.ryanhcode.sable.sublevel.ServerSubLevel ship = null;
        for (var sub : dev.ryanhcode.sable.api.sublevel.SubLevelContainer.getContainer(space).getAllSubLevels()) {
            if (("vr_test_" + pos.getX()).equals(sub.getName())) {
                ship = sub;
            }
        }
        helper.assertTrue(ship != null, "no ship named vr_test_" + pos.getX());
        BlockPos helmPos = null;
        for (BlockPos p : com.voidreclaimer.ship.Ships.blocksOf(space, ship)) {
            if (space.getBlockEntity(p) instanceof com.voidreclaimer.ship.HelmBlockEntity) {
                helmPos = p;
            }
        }
        helper.assertTrue(helmPos != null, "no Helm on ship vr_test_" + pos.getX());
        var helm = (com.voidreclaimer.ship.HelmBlockEntity) space.getBlockEntity(helmPos);
        Vec3 where = ship.logicalPose().transformPosition(Vec3.atBottomCenterOf(helmPos));
        ServerPlayer player = TestPlayers.survivalInDeepSpace(helper, where.x, where.y + 1, where.z);
        ServerPlayer current = space.getServer().getPlayerList().getPlayer(player.getUUID());
        player = current != null ? current : player;
        helper.assertTrue(helm.seat(player), "the pilot could not sit at " + helmPos + " (player at " + player.position() + ")");
        // A cross-dimension teleport can hand the server a fresh ServerPlayer object for the same player: compare by id.
        if (helm.pilot() == null || !helm.pilot().getUUID().equals(player.getUUID())) {
            var seats = space.getEntities(com.voidreclaimer.registry.ModEntities.HELM_SEAT.get(), e -> true).stream()
                    .map(e -> e.position() + "->" + ((com.voidreclaimer.ship.HelmSeat) e).helmPos() + " carrying " + e.getPassengers()).toList();
            var vehicle = player.getVehicle();
            helper.fail("the Helm does not see its pilot: " + helm.pilot() + "; entityTicking " + space.isPositionEntityTicking(BlockPos.containing(where))
                    + ", chunk " + space.getChunkSource().hasChunk(BlockPos.containing(where).getX() >> 4, BlockPos.containing(where).getZ() >> 4)
                    + ", byUuid " + (vehicle == null ? "none" : space.getEntity(vehicle.getUUID())) + ", removed " + (vehicle != null && vehicle.isRemoved())
                    + "; helm at " + helmPos + " in world at " + where + ", ship " + ship
                    + " plot " + ship.getPlot().getChunkMin() + ".." + ship.getPlot().getChunkMax() + ", player at " + player.position()
                    + " riding " + player.getVehicle() + ", seats " + seats);
        }
        player.setData(com.voidreclaimer.registry.ModAttachments.PILOT_INPUT, keys);
        return player;
    }

    /** W at the Helm opens the stern thruster: the ship moves the way the chair faces (north, -Z). */
    @GameTest(template = "empty", timeoutTicks = 400)
    public static void aSeatedPilotPressingForwardMovesTheShipAhead(GameTestHelper helper) {
        BlockPos pos = new BlockPos(87_000, 200, 3_000);
        MinecraftServer server = helper.getLevel().getServer();
        ServerLevel space = server.getLevel(ModDimensions.DEEP_SPACE);
        helmShip(helper, pos, log -> helper.runAfterDelay(1, () -> {
            nameShip(server, space, pos);
            ServerPlayer player = pilot(helper, space, pos, com.voidreclaimer.ship.PilotInput.of(
                    true, false, false, false, false, false, false, false, false, false, false, false));
            helper.runAfterDelay(SETTLE_TICKS, () -> {
                Vec3 first = shipPosition(server, space, pos);
                helper.runAfterDelay(SAMPLE_TICKS, () -> {
                    Vec3 second = shipPosition(server, space, pos);
                    removeShip(server, space, pos);
                    TestPlayers.remove(player);
                    if (first == null || second == null) {
                        helper.fail("lost the ship: " + first + ", " + second + "; setup: " + log);
                        return;
                    }
                    double north = first.z - second.z;
                    if (north < PUSHED) {
                        helper.fail("the ship moved " + String.format("%.2f", north) + " blocks north, from " + first + " to " + second + "; setup: " + log);
                    } else {
                        helper.succeed();
                    }
                });
            });
        }));
    }

    /**
     * The Helm's readout, which the pilot HUD shows, follows the ship: moving ahead reads as forward speed, heading north
     * with the nose lifted by the low engine. This also proves the seat rides along: the Helm only reads its ship while
     * it sees its pilot, and the ship has moved on by the time the readout is taken.
     */
    @GameTest(template = "empty", timeoutTicks = 400)
    public static void theHelmReadoutFollowsTheShip(GameTestHelper helper) {
        BlockPos pos = new BlockPos(91_000, 200, 3_000);
        MinecraftServer server = helper.getLevel().getServer();
        ServerLevel space = server.getLevel(ModDimensions.DEEP_SPACE);
        helmShip(helper, pos, log -> helper.runAfterDelay(1, () -> {
            nameShip(server, space, pos);
            ServerPlayer player = pilot(helper, space, pos, com.voidreclaimer.ship.PilotInput.of(
                    true, false, false, false, false, false, false, false, false, false, false, false));
            helper.runAfterDelay(SETTLE_TICKS + SAMPLE_TICKS, () -> {
                com.voidreclaimer.ship.HelmBlockEntity helm = helmOf(space, pos);
                com.voidreclaimer.ship.HelmBlockEntity.Readout readout = helm == null ? null : helm.readout();
                String seen = helm == null ? "no Helm" : "pilot " + helm.pilot() + ", ship " + com.voidreclaimer.ship.Ships.shipAt(space, helm.getBlockPos())
                        + ", thrusters " + com.voidreclaimer.ship.Ships.thrustersOf(space, shipNamed(space, pos)).size() + ", helm at " + helm.getBlockPos()
                        + ", player at " + player.position() + " riding " + player.getVehicle() + " in " + player.level().dimension().location()
                        + ", seats " + space.getEntities(com.voidreclaimer.registry.ModEntities.HELM_SEAT.get(), e -> true).stream()
                                .map(e -> e.position() + "->" + ((com.voidreclaimer.ship.HelmSeat) e).helmPos() + " carrying " + e.getPassengers()).toList();
                List<String> info = command(server, space, pos, "sable info " + shipSelector(pos));
                removeShip(server, space, pos);
                TestPlayers.remove(player);
                if (readout == null) {
                    helper.fail("lost the Helm; setup: " + log);
                    return;
                }
                double surge = readout.velocity()[0];
                double heading = readout.attitude().heading();
                if (!readout.onShip() || readout.thrusters() == 0) {
                    helper.fail("the Helm does not see its ship: " + seen + "; " + readout);
                } else if (surge < 0.3 || Math.abs(readout.speed() - linearSpeed(info)) > 0.2 * readout.speed() + 0.05) {
                    helper.fail("the readout's speed does not follow the ship: surge " + surge + ", speed " + readout.speed() + "; " + info);
                } else if (Math.min(heading, 360 - heading) > 15 || Math.abs(readout.attitude().roll()) > 15 || readout.attitude().pitch() < 5) {
                    // The stern engine sits below the centre of mass (the Helm is on top), so the push lifts the nose:
                    // the attitude must show that pitch, and no roll or turn.
                    helper.fail("the readout's attitude is off for a ship pushed north by a low stern engine: " + readout.attitude());
                } else {
                    helper.succeed();
                }
            });
        }));
    }

    /**
     * When the ship is removed under a seated pilot, the pilot is left standing in the world where the ship was, not
     * carried off to the Helm's plot position (which once dragged a player twenty million blocks away and dropped the
     * connection).
     */
    @GameTest(template = "empty", timeoutTicks = 400)
    public static void removingTheShipUnseatsThePilotInTheWorld(GameTestHelper helper) {
        BlockPos pos = new BlockPos(92_000, 200, 3_000);
        MinecraftServer server = helper.getLevel().getServer();
        ServerLevel space = server.getLevel(ModDimensions.DEEP_SPACE);
        helmShip(helper, pos, log -> helper.runAfterDelay(1, () -> {
            nameShip(server, space, pos);
            ServerPlayer player = pilot(helper, space, pos, com.voidreclaimer.ship.PilotInput.NONE);
            helper.runAfterDelay(5, () -> {
                removeShip(server, space, pos);
                // A real player ticks and so keeps the chunks around them entity-ticking, which is what lets the seat
                // tick and notice; a test player only ticks when told to.
                tickPlayer(helper, server, player.getUUID(), UNSEAT_TICKS, () -> {
                    ServerPlayer now = server.getPlayerList().getPlayer(player.getUUID());
                    Vec3 at = now == null ? null : now.position();
                    boolean riding = now != null && now.getVehicle() != null;
                    String why = "";
                    if (riding && now.getVehicle() instanceof com.voidreclaimer.ship.HelmSeat seat) {
                        BlockPos helmPos = seat.helmPos();
                        why = "; seat removed " + seat.isRemoved() + " at " + seat.position() + ", helm " + helmPos + " entity " + space.getBlockEntity(helmPos)
                                + ", shipAt(server) " + com.voidreclaimer.ship.Ships.shipAt(space, helmPos) + ", shipAt(level) " + com.voidreclaimer.ship.Ships.shipAt((net.minecraft.world.level.Level) space, helmPos)
                                + ", inPlots " + com.voidreclaimer.ship.Ships.inPlots(space, helmPos) + ", worldSpot " + com.voidreclaimer.ship.HelmSeat.worldSpot(space, helmPos)
                                + ", seat chunk entityTicking " + space.isPositionEntityTicking(seat.blockPosition()) + ", helm chunk loaded " + space.hasChunk(helmPos.getX() >> 4, helmPos.getZ() >> 4)
                                + ", helm chunk entityTicking " + space.isPositionEntityTicking(helmPos);
                    }
                    TestPlayers.remove(player);
                    if (now == null || riding || at.distanceTo(Vec3.atCenterOf(pos)) > 16) {
                        helper.fail("the pilot was not left in the world: at " + at + ", riding " + riding + why + "; setup: " + log);
                    } else {
                        helper.succeed();
                    }
                });
            });
        }));
    }

    /** Ticks per the player's turn to be left alone after the ship is gone: the chunk has to become entity-ticking first. */
    private static final int UNSEAT_TICKS = 40;

    /** Runs the player's own tick once per game tick for {@code ticks} ticks, then {@code then}. */
    private static void tickPlayer(GameTestHelper helper, MinecraftServer server, java.util.UUID player, int ticks, Runnable then) {
        ServerPlayer now = server.getPlayerList().getPlayer(player);
        if (now != null) {
            now.doTick();
        }
        if (ticks <= 0) {
            then.run();
        } else {
            helper.runAfterDelay(1, () -> tickPlayer(helper, server, player, ticks - 1, then));
        }
    }

    /** The Helm on the test ship at {@code pos}, or null. */
    private static com.voidreclaimer.ship.HelmBlockEntity helmOf(ServerLevel space, BlockPos pos) {
        var ship = shipNamed(space, pos);
        if (ship == null) {
            return null;
        }
        for (BlockPos p : com.voidreclaimer.ship.Ships.blocksOf(space, ship)) {
            if (space.getBlockEntity(p) instanceof com.voidreclaimer.ship.HelmBlockEntity helm) {
                return helm;
            }
        }
        return null;
    }

    /** The arrow keys open the bow jet: the ship starts turning. No key, no turn and no move. */
    @GameTest(template = "empty", timeoutTicks = 400)
    public static void aSeatedPilotYawsWithTheArrowKeysAndNothingHappensWithoutKeys(GameTestHelper helper) {
        BlockPos pos = new BlockPos(88_000, 200, 3_000);
        MinecraftServer server = helper.getLevel().getServer();
        ServerLevel space = server.getLevel(ModDimensions.DEEP_SPACE);
        helmShip(helper, pos, log -> helper.runAfterDelay(1, () -> {
            nameShip(server, space, pos);
            ServerPlayer player = pilot(helper, space, pos, com.voidreclaimer.ship.PilotInput.NONE);
            helper.runAfterDelay(SAMPLE_TICKS, () -> {
                List<String> idle = command(server, space, pos, "sable info " + shipSelector(pos));
                double idleSpin = angularSpeed(idle);
                Vec3 idlePos = position(idle);
                // Now hold yaw right: the jet at the bow pushes east, ahead of the centre of mass, which turns the nose
                // right (torque about -Y); the allocator refuses to open it for yaw left.
                player.setData(com.voidreclaimer.registry.ModAttachments.PILOT_INPUT,
                        com.voidreclaimer.ship.PilotInput.of(false, false, false, false, false, false, false, false, false, false, false, true));
                helper.runAfterDelay(SAMPLE_TICKS, () -> {
                    List<String> info = command(server, space, pos, "sable info " + shipSelector(pos));
                    double spin = angularSpeed(info);
                    removeShip(server, space, pos);
                    TestPlayers.remove(player);
                    if (idleSpin > TURNING || idlePos != null && idlePos.distanceTo(Vec3.atCenterOf(pos)) > 2.0) {
                        helper.fail("the ship moved with no keys held: spin " + idleSpin + ", at " + idlePos + "; setup: " + log);
                    } else if (spin < TURNING) {
                        helper.fail("yaw did not turn the ship: angular speed " + spin + "; info: " + info);
                    } else {
                        helper.succeed();
                    }
                });
            });
        }));
    }

    /**
     * The holds. A ship built like the Helm ship but with thrusters both ways (a bow thruster pushing back, a jet each side
     * at the bow) is given a shove and a spin after assembly; with the dampener and the RCS lock on, the seated pilot's
     * Helm brings it to a stop without any key held. With both off it keeps drifting and spinning.
     */
    private static void holdShip(GameTestHelper helper, BlockPos pos, boolean holdsOn, java.util.function.Consumer<List<String>> then) {
        MinecraftServer server = helper.getLevel().getServer();
        ServerLevel space = server.getLevel(ModDimensions.DEEP_SPACE);
        space.setChunkForced(pos.getX() >> 4, pos.getZ() >> 4, true);
        for (int chunks = 1; chunks <= 8; chunks++) {
            space.setChunkForced(pos.getX() >> 4, (pos.getZ() >> 4) - chunks, true);
            space.setChunkForced(pos.getX() >> 4, (pos.getZ() >> 4) + chunks, true);
        }
        helper.runAfterDelay(5, () -> {
            clearArea(server, space, pos);
            int x = pos.getX(), y = pos.getY(), z = pos.getZ();
            List<String> log = new ArrayList<>();
            log.addAll(command(server, space, pos, "fill " + x + " " + y + " " + (z - 3) + " " + x + " " + y + " " + (z + 1) + " minecraft:stone"));
            log.addAll(command(server, space, pos, "setblock " + x + " " + (y + 1) + " " + z + " void_reclaimer:helm[facing=north]"));
            // A keel block under the Helm balances it, so the centre of mass stays in the thrusters' plane and their
            // pushes make no roll or pitch, which this ship has no jets to hold.
            log.addAll(command(server, space, pos, "setblock " + x + " " + (y - 1) + " " + z + " minecraft:stone"));
            // Stern engine (pushes north) and a bow engine (pushes south), both electric so no motors are needed.
            log.addAll(command(server, space, pos, "setblock " + x + " " + y + " " + (z + 2) + " void_reclaimer:ion_thruster[facing=south]"));
            log.addAll(command(server, space, pos, "setblock " + x + " " + y + " " + (z - 4) + " void_reclaimer:ion_thruster[facing=north]"));
            // Side jets at both ends, pushing east and west: a bow and a stern jet pushing the same way brake sideways
            // drift, pushing opposite ways they yaw, so each hold can be served without feeding the other.
            log.addAll(command(server, space, pos, "setblock " + (x + 1) + " " + y + " " + (z - 3) + " void_reclaimer:rcs_thruster[facing=east]"));
            log.addAll(command(server, space, pos, "setblock " + (x - 1) + " " + y + " " + (z - 3) + " void_reclaimer:rcs_thruster[facing=west]"));
            log.addAll(command(server, space, pos, "setblock " + (x + 1) + " " + y + " " + (z + 1) + " void_reclaimer:rcs_thruster[facing=east]"));
            log.addAll(command(server, space, pos, "setblock " + (x - 1) + " " + y + " " + (z + 1) + " void_reclaimer:rcs_thruster[facing=west]"));
            // Roll jets: a pair pushing down on one side and up on the other rolls the ship without pushing it, one pair
            // each way. A tumbling body trades yaw for roll, so the lock needs roll authority to settle the spin.
            log.addAll(command(server, space, pos, "setblock " + (x + 1) + " " + y + " " + (z - 1) + " void_reclaimer:rcs_thruster[facing=up]"));
            log.addAll(command(server, space, pos, "setblock " + (x - 1) + " " + y + " " + (z - 1) + " void_reclaimer:rcs_thruster[facing=down]"));
            log.addAll(command(server, space, pos, "setblock " + (x + 1) + " " + y + " " + (z - 2) + " void_reclaimer:rcs_thruster[facing=down]"));
            log.addAll(command(server, space, pos, "setblock " + (x - 1) + " " + y + " " + (z - 2) + " void_reclaimer:rcs_thruster[facing=up]"));
            for (BlockPos t : List.of(new BlockPos(x, y, z + 2), new BlockPos(x, y, z - 4), new BlockPos(x + 1, y, z - 3),
                    new BlockPos(x - 1, y, z - 3), new BlockPos(x + 1, y, z + 1), new BlockPos(x - 1, y, z + 1),
                    new BlockPos(x + 1, y, z - 1), new BlockPos(x - 1, y, z - 1), new BlockPos(x + 1, y, z - 2), new BlockPos(x - 1, y, z - 2))) {
                ((com.voidreclaimer.ship.ElectricThrusterBlockEntity) space.getBlockEntity(t)).energy().receiveEnergy(8000, false);
            }
            helper.runAfterDelay(10, () -> {
                log.addAll(command(server, space, pos, "sable assemble connected " + x + " " + y + " " + z));
                helper.runAfterDelay(1, () -> {
                    nameShip(server, space, pos);
                    ServerPlayer player = pilot(helper, space, pos, com.voidreclaimer.ship.PilotInput.NONE);
                    player.setData(com.voidreclaimer.registry.ModAttachments.FLIGHT_SETTINGS,
                            com.voidreclaimer.flight.FlightSettings.DEFAULT.withDampener(holdsOn).withRcsLock(holdsOn));
                    // The shove and the spin, straight into the physics body.
                    var ship = shipNamed(space, pos);
                    var body = dev.ryanhcode.sable.api.physics.handle.RigidBodyHandle.of(ship);
                    body.addLinearAndAngularVelocity(new org.joml.Vector3d(0, 0, -3), new org.joml.Vector3d(0, 1.0, 0));
                    log.add("shoved");
                    helper.runAfterDelay(HOLD_TICKS, () -> {
                        List<String> info = command(server, space, pos, "sable info " + shipSelector(pos));
                        removeShip(server, space, pos);
                        TestPlayers.remove(player);
                        log.addAll(info);
                        then.accept(log);
                    });
                });
            });
        });
    }

    private static dev.ryanhcode.sable.sublevel.ServerSubLevel shipNamed(ServerLevel space, BlockPos pos) {
        for (var sub : dev.ryanhcode.sable.api.sublevel.SubLevelContainer.getContainer(space).getAllSubLevels()) {
            if (("vr_test_" + pos.getX()).equals(sub.getName())) {
                return sub;
            }
        }
        return null;
    }

    static final Pattern LINEAR = Pattern.compile("Linear Velocity: (\\S+) (\\S+) (\\S+)");

    static double linearSpeed(List<String> info) {
        for (String line : info) {
            Matcher m = LINEAR.matcher(line);
            if (m.find()) {
                return Math.sqrt(Math.pow(Double.parseDouble(m.group(1)), 2) + Math.pow(Double.parseDouble(m.group(2)), 2)
                        + Math.pow(Double.parseDouble(m.group(3)), 2));
            }
        }
        return Double.NaN;
    }

    @GameTest(template = "empty", timeoutTicks = 600)
    public static void theHoldsStopAShovedAndSpinningShip(GameTestHelper helper) {
        BlockPos pos = new BlockPos(89_000, 200, 3_000);
        holdShip(helper, pos, true, log -> {
            double speed = linearSpeed(log), spin = angularSpeed(log);
            if (Double.isNaN(speed)) {
                helper.fail("lost the ship: " + log);
            } else if (speed > STOPPED || spin > STOPPED_SPIN) {
                helper.fail("the holds did not stop the ship: speed " + speed + ", spin " + spin + "; " + log);
            } else {
                helper.succeed();
            }
        });
    }

    @GameTest(template = "empty", timeoutTicks = 600)
    public static void withTheHoldsOffAShovedShipKeepsGoing(GameTestHelper helper) {
        BlockPos pos = new BlockPos(90_000, 200, 3_000);
        holdShip(helper, pos, false, log -> {
            double speed = linearSpeed(log), spin = angularSpeed(log);
            if (Double.isNaN(speed)) {
                helper.fail("lost the ship: " + log);
            } else if (speed < 1.0 || spin < 0.3) {
                helper.fail("something slowed the ship with the holds off: speed " + speed + ", spin " + spin + "; " + log);
            } else {
                helper.succeed();
            }
        });
    }

    /** A fuelled, turning, open thruster pushes its ship away from the nozzle, through vacuum. */
    @GameTest(template = "empty", timeoutTicks = 400)
    public static void aChemicalThrusterPushesItsShip(GameTestHelper helper) {
        BlockPos pos = new BlockPos(80_000, 200, 3_000);
        MinecraftServer server = helper.getLevel().getServer();
        ServerLevel space = server.getLevel(ModDimensions.DEEP_SPACE);
        // It leaves its own chunk within a second; a ship in unloaded chunks cannot be read.
        for (int chunks = 1; chunks <= 8; chunks++) {
            space.setChunkForced((pos.getX() >> 4) - chunks, pos.getZ() >> 4, true);
        }
        thrusterShip(helper, pos, true, true, log -> watch(helper, server, space, pos, log, (first, second) -> {
            // Watched early and from rest: the ship is never perfectly balanced, so it soon starts to turn with its thrust.
            double west = first.x - second.x;
            if (west < PUSHED) {
                helper.fail("the ship moved " + String.format("%.2f", west) + " blocks west, from " + first + " to " + second + "; setup: " + log);
            } else {
                helper.succeed();
            }
        }));
    }

    /**
     * The Create Propulsion spike: its thrusters scale their push by Sable's air pressure (full at none, a fifth at sea
     * level), and deep space has none, so a powered Creative Thruster pushes a ship through vacuum with no bridge of ours.
     */
    @GameTest(template = "empty", timeoutTicks = 400)
    public static void aCreatePropulsionThrusterPushesItsShipInVacuum(GameTestHelper helper) {
        BlockPos pos = new BlockPos(93_000, 200, 3_000);
        MinecraftServer server = helper.getLevel().getServer();
        ServerLevel space = server.getLevel(ModDimensions.DEEP_SPACE);
        for (int chunks = 1; chunks <= 8; chunks++) {
            space.setChunkForced((pos.getX() >> 4) - chunks, pos.getZ() >> 4, true);
            space.setChunkForced((pos.getX() >> 4) + chunks, pos.getZ() >> 4, true);
        }
        space.setChunkForced(pos.getX() >> 4, pos.getZ() >> 4, true);
        helper.runAfterDelay(5, () -> {
            clearArea(server, space, pos);
            int x = pos.getX(), y = pos.getY(), z = pos.getZ();
            List<String> log = new ArrayList<>();
            cross(server, space, pos, true, log);
            log.addAll(command(server, space, pos, "setblock " + x + " " + y + " " + z + " createpropulsion:creative_thruster[facing=east]"));
            helper.runAfterDelay(10, () -> {
                log.addAll(command(server, space, pos, "sable assemble connected " + x + " " + y + " " + z));
                watch(helper, server, space, pos, log, (first, second) -> {
                    double along = Math.abs(first.x - second.x);
                    if (along < PUSHED) {
                        helper.fail("Propulsion's thruster moved the ship " + String.format("%.2f", along) + " blocks along its axis, from "
                                + first + " to " + second + "; setup: " + log);
                    } else {
                        helper.succeed();
                    }
                });
            });
        });
    }

    @GameTest(template = "empty", timeoutTicks = 400)
    public static void aThrusterWithoutFuelDoesNotPush(GameTestHelper helper) {
        stays(helper, new BlockPos(81_000, 200, 3_000), false, true);
    }

    @GameTest(template = "empty", timeoutTicks = 400)
    public static void aThrusterWithoutRedstoneDoesNotPush(GameTestHelper helper) {
        stays(helper, new BlockPos(82_000, 200, 3_000), true, false);
    }

    private static void stays(GameTestHelper helper, BlockPos pos, boolean fuelled, boolean powered) {
        MinecraftServer server = helper.getLevel().getServer();
        ServerLevel space = server.getLevel(ModDimensions.DEEP_SPACE);
        thrusterShip(helper, pos, fuelled, powered, log -> coasts(helper, server, space, pos, log));
    }

    /** Deep space has asteroids now, and a fresh world may put one where a test wants its sub-level. */
    static void clearArea(MinecraftServer server, ServerLevel space, BlockPos pos) {
        space.getChunk(pos);
        removeShipsFromEarlierRuns(server, space, pos);
        command(server, space, pos, "fill " + (pos.getX() - 10) + " " + (pos.getY() - 10) + " " + (pos.getZ() - 10) + " "
                + (pos.getX() + 10) + " " + (pos.getY() + 10) + " " + (pos.getZ() + 10) + " minecraft:air");
    }

    /** Sable answers a bad selector with a message, not an exception, and a remove that does nothing poisons later runs. */
    private static void remove(MinecraftServer server, ServerLevel space, BlockPos pos, String selector) {
        List<String> out = command(server, space, pos, "sable remove " + selector);
        if (out.stream().anyMatch(line -> line.contains("Invalid"))) {
            throw new IllegalStateException("sable remove " + selector + ": " + out);
        }
    }

    /**
     * Tests run side by side, so each names its ship and reads and removes it by that name: selectors by position reach
     * other tests' ships. The name comes from the test's own coordinates.
     */
    static String shipSelector(BlockPos pos) {
        return "@e[name=vr_test_" + pos.getX() + ",limit=1]";
    }

    static void nameShip(MinecraftServer server, ServerLevel space, BlockPos pos) {
        command(server, space, pos, "sable name set @n vr_test_" + pos.getX());
    }

    static Vec3 shipPosition(MinecraftServer server, ServerLevel space, BlockPos pos) {
        return position(command(server, space, pos, "sable info " + shipSelector(pos)));
    }

    static void removeShip(MinecraftServer server, ServerLevel space, BlockPos pos) {
        remove(server, space, pos, shipSelector(pos));
    }

    /**
     * Once per run, before any test has a ship: clears every loaded ship in the dimension. A thruster ship saved by an
     * earlier run keeps firing and flying when the world loads, far beyond its own test's area, into the other tests' ships.
     */
    private static void removeShipsFromEarlierRuns(MinecraftServer server, ServerLevel space, BlockPos pos) {
        if (EARLIER_RUNS_CLEARED.compareAndSet(false, true)) {
            remove(server, space, pos, "@e");
        }
    }

    private static void finish(GameTestHelper helper, MinecraftServer server, ServerLevel space, BlockPos start, List<String> setup) {
        Vec3 now = shipPosition(server, space, start);
        removeShip(server, space, start);
        if (now == null) {
            helper.fail("lost the ship near " + start + "; setup: " + setup);
            return;
        }
        double drift = now.distanceTo(Vec3.atCenterOf(start));
        if (drift > STILL) {
            helper.fail("sub-level moved " + String.format("%.2f", drift) + " blocks from " + start + " to " + now);
            return;
        }
        helper.succeed();
    }

    private static Vec3 position(List<String> info) {
        for (String line : info) {
            Matcher m = POSITION.matcher(line);
            if (m.find()) {
                return new Vec3(Double.parseDouble(m.group(1)), Double.parseDouble(m.group(2)), Double.parseDouble(m.group(3)));
            }
        }
        return null;
    }

    static List<String> command(MinecraftServer server, ServerLevel level, BlockPos pos, String command) {
        List<String> out = new ArrayList<>();
        CommandSource sink = new CommandSource() {
            @Override
            public void sendSystemMessage(Component message) {
                out.add(message.getString());
            }

            @Override
            public boolean acceptsSuccess() {
                return true;
            }

            @Override
            public boolean acceptsFailure() {
                return true;
            }

            @Override
            public boolean shouldInformAdmins() {
                return false;
            }
        };
        CommandSourceStack source = server.createCommandSourceStack()
                .withSource(sink)
                .withLevel(level)
                .withPosition(Vec3.atCenterOf(pos))
                .withPermission(4);
        server.getCommands().performPrefixedCommand(source, command);
        return out;
    }
}
