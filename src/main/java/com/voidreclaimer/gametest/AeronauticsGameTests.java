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
    private static void cross(MinecraftServer server, ServerLevel space, BlockPos pos, boolean powered, List<String> log) {
        for (BlockPos arm : List.of(pos.north(), pos.south(), pos.below())) {
            log.addAll(command(server, space, pos, "setblock " + arm.getX() + " " + arm.getY() + " " + arm.getZ() + " minecraft:stone"));
        }
        BlockPos top = pos.above();
        log.addAll(command(server, space, pos, "setblock " + top.getX() + " " + top.getY() + " " + top.getZ() + " "
                + (powered ? "minecraft:redstone_block" : "minecraft:stone")));
    }

    /** The same ship with an Ion Thruster, nozzle east: no motor, and a charged buffer instead of fuel. */
    private static void ionShip(GameTestHelper helper, BlockPos pos, boolean charged, java.util.function.Consumer<List<String>> then) {
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
            log.addAll(command(server, space, pos, "setblock " + pos.getX() + " " + pos.getY() + " " + pos.getZ() + " void_reclaimer:ion_thruster[facing=east]"));
            if (charged) {
                var thruster = (com.voidreclaimer.ship.IonThrusterBlockEntity) space.getBlockEntity(pos);
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
        ionShip(helper, pos, true, log -> watch(helper, server, space, pos, log, (first, second) -> {
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
        ionShip(helper, pos, false, log -> coasts(helper, server, space, pos, log));
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
    private static void clearArea(MinecraftServer server, ServerLevel space, BlockPos pos) {
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
    private static String shipSelector(BlockPos pos) {
        return "@e[name=vr_test_" + pos.getX() + ",limit=1]";
    }

    private static void nameShip(MinecraftServer server, ServerLevel space, BlockPos pos) {
        command(server, space, pos, "sable name set @n vr_test_" + pos.getX());
    }

    private static Vec3 shipPosition(MinecraftServer server, ServerLevel space, BlockPos pos) {
        return position(command(server, space, pos, "sable info " + shipSelector(pos)));
    }

    private static void removeShip(MinecraftServer server, ServerLevel space, BlockPos pos) {
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

    private static List<String> command(MinecraftServer server, ServerLevel level, BlockPos pos, String command) {
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
