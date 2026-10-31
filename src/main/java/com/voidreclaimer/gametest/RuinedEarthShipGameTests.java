package com.voidreclaimer.gametest;

import static com.voidreclaimer.gametest.AeronauticsGameTests.LINEAR;
import static com.voidreclaimer.gametest.AeronauticsGameTests.clearArea;
import static com.voidreclaimer.gametest.AeronauticsGameTests.command;
import static com.voidreclaimer.gametest.AeronauticsGameTests.cross;
import static com.voidreclaimer.gametest.AeronauticsGameTests.nameShip;
import static com.voidreclaimer.gametest.AeronauticsGameTests.removeShip;
import static com.voidreclaimer.gametest.AeronauticsGameTests.shipPosition;
import static com.voidreclaimer.gametest.AeronauticsGameTests.shipSelector;

import com.voidreclaimer.earth.EarthWind;
import com.voidreclaimer.earth.RuinedEarth;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.regex.Matcher;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * Ships over ruined Earth, where Sable has gravity, air and (ours) wind: thrusters lift, Aeronautics propellers push,
 * and free hulls drift. Part of the Aeronautics suite ({@code ./gradlew runGameTestAeronautics}); the helpers are
 * {@link AeronauticsGameTests}'. Tests sit 200 blocks up, well above the ruined terrain, and far apart along x.
 */
@GameTestHolder(AeronauticsGameTests.NAMESPACE)
@PrefixGameTestTemplate(false)
public final class RuinedEarthShipGameTests {
    private static final int SETTLE_TICKS = 10;
    private static final int SAMPLE_TICKS = 40;
    /** A pushed ship travels at least this far in {@link #SAMPLE_TICKS}. */
    private static final double PUSHED = 3.0;
    /** A lifting ship rises at least this far in {@link #SAMPLE_TICKS}; an unlifted one falls about 20 blocks. */
    private static final double CLIMBED = 2.0;
    /** A drifting hull has at least this much horizontal speed (blocks per second) after {@link #DRIFT_TICKS}. */
    private static final double DRIFTING = 0.1;
    private static final int DRIFT_TICKS = 40;
    private static final AtomicBoolean EARLIER_RUNS_CLEARED = new AtomicBoolean();

    private RuinedEarthShipGameTests() {}

    /** A Chemical Thruster with its nozzle down lifts its ship against Earth's gravity: the ship climbs instead of falling. */
    @GameTest(template = "empty", timeoutTicks = 400)
    public static void aThrusterShipClimbsAgainstEarthGravity(GameTestHelper helper) {
        BlockPos pos = new BlockPos(95_000, 200, 3_000);
        MinecraftServer server = helper.getLevel().getServer();
        ServerLevel earth = earth(server);
        earth.setChunkForced(pos.getX() >> 4, pos.getZ() >> 4, true);
        helper.runAfterDelay(5, () -> {
            prepare(server, earth, pos);
            int x = pos.getX(), y = pos.getY(), z = pos.getZ();
            List<String> log = new ArrayList<>();
            // A symmetric plus round the thruster so the thrust line passes through the centre of mass (an uneven hull
            // tips over and flies sideways): redstone blocks north and south as the throttle, stone east and west, the
            // motor on the thruster's back (above), on the same axis.
            for (BlockPos arm : List.of(pos.east(), pos.west())) {
                log.addAll(command(server, earth, pos, "setblock " + arm.getX() + " " + arm.getY() + " " + arm.getZ() + " minecraft:stone"));
            }
            for (BlockPos arm : List.of(pos.north(), pos.south())) {
                log.addAll(command(server, earth, pos, "setblock " + arm.getX() + " " + arm.getY() + " " + arm.getZ() + " minecraft:redstone_block"));
            }
            log.addAll(command(server, earth, pos, "setblock " + x + " " + (y + 1) + " " + z + " create:creative_motor[facing=down]"));
            // A creative motor turns at 16 rpm until scrolled up; the thruster wants its full 256 to lift against gravity.
            log.addAll(command(server, earth, pos, "data merge block " + x + " " + (y + 1) + " " + z + " {ScrollValue:256}"));
            log.addAll(command(server, earth, pos, "setblock " + x + " " + y + " " + z + " void_reclaimer:chemical_thruster[facing=down]"));
            var thruster = (com.voidreclaimer.ship.ChemicalThrusterBlockEntity) earth.getBlockEntity(pos);
            log.add("filled " + thruster.tank().fill(new net.neoforged.neoforge.fluids.FluidStack(
                    com.voidreclaimer.registry.ModFluids.NEBULITE_PROPELLANT.source().get(), 4000),
                    net.neoforged.neoforge.fluids.capability.IFluidHandler.FluidAction.EXECUTE));
            helper.runAfterDelay(10, () -> {
                log.addAll(command(server, earth, pos, "sable assemble connected " + x + " " + y + " " + z));
                watch(helper, server, earth, pos, log, () -> thrusterState(earth, pos), (first, second, state) -> {
                    double climbed = second.y - first.y;
                    if (climbed < CLIMBED) {
                        helper.fail("the ship climbed " + String.format("%.2f", climbed) + " blocks, from " + first + " to " + second
                                + "; thruster " + state + "; setup: " + log);
                    } else {
                        helper.succeed();
                    }
                });
            });
        });
    }

    /** An Aeronautics wooden propeller on a creative motor pushes its ship through Earth's air (it gives nothing in vacuum). */
    @GameTest(template = "empty", timeoutTicks = 400)
    public static void aPropellerPushesItsShipInEarthsAir(GameTestHelper helper) {
        BlockPos pos = new BlockPos(96_000, 200, 3_000);
        MinecraftServer server = helper.getLevel().getServer();
        ServerLevel earth = earth(server);
        for (int chunks = -8; chunks <= 8; chunks++) {
            earth.setChunkForced((pos.getX() >> 4) + chunks, pos.getZ() >> 4, true);
        }
        helper.runAfterDelay(5, () -> {
            prepare(server, earth, pos);
            int x = pos.getX(), y = pos.getY(), z = pos.getZ();
            List<String> log = new ArrayList<>();
            // The hull is a cross around the motor; the propeller sits on the motor's shaft with nothing round its blades.
            BlockPos motor = pos.west();
            cross(server, earth, motor, false, log);
            log.addAll(command(server, earth, pos, "setblock " + (x - 1) + " " + y + " " + z + " create:creative_motor[facing=east]"));
            // A wooden propeller's push is about its rpm (times the air pressure, a third of sea level's up here): the
            // motor's 16 rpm default would barely move the hull; 256 does.
            log.addAll(command(server, earth, pos, "data merge block " + (x - 1) + " " + y + " " + z + " {ScrollValue:256}"));
            log.addAll(command(server, earth, pos, "setblock " + x + " " + y + " " + z + " aeronautics:wooden_propeller[facing=east]"));
            helper.runAfterDelay(10, () -> {
                log.add("before assembly: " + kinetics(earth, List.of(pos.west(), pos)));
                log.addAll(command(server, earth, pos, "sable assemble connected " + x + " " + y + " " + z));
                watch(helper, server, earth, pos, log, () -> shipKinetics(earth, pos), (first, second, state) -> {
                    double along = Math.abs(second.x - first.x);
                    if (along < PUSHED) {
                        helper.fail("the propeller moved the ship " + String.format("%.2f", along) + " blocks along its axis, from " + first
                                + " to " + second + "; kinetics " + state + "; setup: " + log);
                    } else {
                        helper.succeed();
                    }
                });
            });
        });
    }

    /** A free hull over Earth is carried the way the wind blows (while it falls); nothing drifts in deep space. */
    @GameTest(template = "empty", timeoutTicks = 400)
    public static void aFreeHullDriftsWithTheWindOverEarth(GameTestHelper helper) {
        BlockPos pos = new BlockPos(97_000, 200, 3_000);
        MinecraftServer server = helper.getLevel().getServer();
        ServerLevel earth = earth(server);
        earth.setChunkForced(pos.getX() >> 4, pos.getZ() >> 4, true);
        helper.runAfterDelay(5, () -> {
            prepare(server, earth, pos);
            List<String> log = new ArrayList<>(command(server, earth, pos, "sable spawn platform 2 minecraft:stone"));
            helper.runAfterDelay(1, () -> {
                nameShip(server, earth, pos);
                helper.runAfterDelay(DRIFT_TICKS, () -> {
                    List<String> info = command(server, earth, pos, "sable info " + shipSelector(pos));
                    Vec3 velocity = linearVelocity(info);
                    Vec3 where = shipPosition(server, earth, pos);
                    removeShip(server, earth, pos);
                    if (velocity == null || where == null) {
                        helper.fail("lost the hull: " + info + "; setup: " + log);
                        return;
                    }
                    Vec3 wind = EarthWind.field(where.x, where.z, earth.getGameTime(), EarthWind.maxSpeedPerSecond());
                    Vec3 sideways = new Vec3(velocity.x, 0, velocity.z);
                    if (sideways.length() < DRIFTING || sideways.dot(wind) <= 0) {
                        helper.fail("the hull is not carried by the wind: moving " + sideways + " in wind " + wind + "; " + info);
                    } else {
                        helper.succeed();
                    }
                });
            });
        });
    }

    /** The Create speed of the kinetic blocks at {@code positions}, for a failure message. */
    private static String kinetics(ServerLevel level, List<BlockPos> positions) {
        StringBuilder out = new StringBuilder();
        for (BlockPos p : positions) {
            if (level.getBlockEntity(p) instanceof com.simibubi.create.content.kinetics.base.KineticBlockEntity kinetic) {
                out.append(level.getBlockState(p).getBlock().getClass().getSimpleName()).append('@').append(p).append(" speed ").append(kinetic.getSpeed());
                try {
                    var active = kinetic.getClass().getMethod("isActive");
                    out.append(" active ").append(active.invoke(kinetic));
                } catch (ReflectiveOperationException ignored) {
                    // not a propeller
                }
                out.append("; ");
            }
        }
        return out.toString();
    }

    /** The kinetic blocks of the test ship named after {@code pos}, with their speeds. */
    private static String shipKinetics(ServerLevel earth, BlockPos pos) {
        for (var ship : dev.ryanhcode.sable.api.sublevel.SubLevelContainer.getContainer(earth).getAllSubLevels()) {
            if (("vr_test_" + pos.getX()).equals(ship.getName())) {
                List<BlockPos> blocks = new ArrayList<>();
                com.voidreclaimer.ship.Ships.blocksOf(earth, ship).forEach(blocks::add);
                return kinetics(earth, blocks);
            }
        }
        return "no ship";
    }

    /** What the test ship's Chemical Thruster reports: rpm, throttle, force and fuel, for a failure message. */
    private static String thrusterState(ServerLevel earth, BlockPos pos) {
        for (var ship : dev.ryanhcode.sable.api.sublevel.SubLevelContainer.getContainer(earth).getAllSubLevels()) {
            if (!("vr_test_" + pos.getX()).equals(ship.getName())) {
                continue;
            }
            for (BlockPos p : com.voidreclaimer.ship.Ships.blocksOf(earth, ship)) {
                if (earth.getBlockEntity(p) instanceof com.voidreclaimer.ship.ChemicalThrusterBlockEntity thruster) {
                    return "rpm " + thruster.getSpeed() + ", throttle " + thruster.throttle() + ", force " + thruster.force()
                            + ", fuel " + thruster.tank().getFluidAmount() + ", at " + p;
                }
            }
            return "no thruster on the ship";
        }
        return "no ship";
    }

    private static ServerLevel earth(MinecraftServer server) {
        ServerLevel earth = server.getLevel(RuinedEarth.DIMENSION);
        if (earth == null) {
            throw new IllegalStateException("ruined_earth is not loaded");
        }
        return earth;
    }

    /** Clears the area, and once per run every ship left over Earth by an earlier run. */
    private static void prepare(MinecraftServer server, ServerLevel earth, BlockPos pos) {
        if (EARLIER_RUNS_CLEARED.compareAndSet(false, true)) {
            command(server, earth, pos, "sable remove @e");
        }
        clearArea(server, earth, pos);
    }

    @FunctionalInterface
    private interface Watched {
        void accept(Vec3 first, Vec3 second, String state);
    }

    /**
     * Names the ship, reads where it is after settling and again {@link #SAMPLE_TICKS} later, reads {@code state} (for
     * the failure message) while the ship still exists, and removes it.
     */
    private static void watch(GameTestHelper helper, MinecraftServer server, ServerLevel earth, BlockPos pos, List<String> log,
                              java.util.function.Supplier<String> state, Watched then) {
        helper.runAfterDelay(1, () -> {
            nameShip(server, earth, pos);
            helper.runAfterDelay(SETTLE_TICKS, () -> {
                Vec3 first = shipPosition(server, earth, pos);
                helper.runAfterDelay(SAMPLE_TICKS, () -> {
                    Vec3 second = shipPosition(server, earth, pos);
                    String seen = state.get() + "; " + command(server, earth, pos, "sable info " + shipSelector(pos)).stream()
                            .filter(line -> line.contains("Mass")).toList();
                    removeShip(server, earth, pos);
                    if (first == null || second == null) {
                        helper.fail("lost the ship of " + pos + ": " + first + ", " + second + "; setup: " + log);
                        return;
                    }
                    then.accept(first, second, seen);
                });
            });
        });
    }

    private static Vec3 linearVelocity(List<String> info) {
        for (String line : info) {
            Matcher m = LINEAR.matcher(line);
            if (m.find()) {
                return new Vec3(Double.parseDouble(m.group(1)), Double.parseDouble(m.group(2)), Double.parseDouble(m.group(3)));
            }
        }
        return null;
    }
}
