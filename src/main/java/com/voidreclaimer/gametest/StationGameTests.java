package com.voidreclaimer.gametest;

import static com.voidreclaimer.gametest.AeronauticsGameTests.clearArea;
import static com.voidreclaimer.gametest.AeronauticsGameTests.command;

import com.voidreclaimer.registry.ModAttachments;
import com.voidreclaimer.registry.ModBlocks;
import com.voidreclaimer.space.ModDimensions;
import com.voidreclaimer.station.StationAssembly;
import com.voidreclaimer.station.StationCoreBlockEntity;
import com.voidreclaimer.station.StationList;
import com.voidreclaimer.station.Stations;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import org.joml.Quaterniond;
import org.joml.Vector3d;

/**
 * Stations: a Station Core assembles its hull into a Sable ship and sets it down again. Part of the Aeronautics suite
 * ({@code ./gradlew runGameTestAeronautics}); tests sit far apart along x in deep space.
 */
@GameTestHolder(AeronauticsGameTests.NAMESPACE)
@PrefixGameTestTemplate(false)
public final class StationGameTests {
    private StationGameTests() {}

    /** Sneak-use at the core: the connected blocks become one named station that moves as one; the list sees it. */
    @GameTest(template = "empty", timeoutTicks = 400)
    public static void aStationCoreAssemblesItsHullAndNamesIt(GameTestHelper helper) {
        BlockPos pos = new BlockPos(100_000, 200, 3_000);
        MinecraftServer server = helper.getLevel().getServer();
        ServerLevel space = server.getLevel(ModDimensions.DEEP_SPACE);
        space.setChunkForced(pos.getX() >> 4, pos.getZ() >> 4, true);
        helper.runAfterDelay(5, () -> {
            clearArea(server, space, pos);
            hull(server, space, pos);
            StationCoreBlockEntity core = (StationCoreBlockEntity) space.getBlockEntity(pos);
            core.setStationName("Outpost 7");
            StationAssembly.Result result = StationAssembly.assemble(core);
            helper.assertTrue(result == StationAssembly.Result.ASSEMBLED, "assembly: " + result);
            helper.assertTrue(space.getBlockState(pos).isAir(), "the core is still in the world");
            // The core now lives in the ship's plot; find it through the ship.
            helper.runAfterDelay(Stations.REFRESH_TICKS + 2, () -> {
                StationCoreBlockEntity moved = coreOf(space, "Outpost 7");
                helper.assertTrue(moved != null && moved.ship() != null, "no assembled core named Outpost 7");
                helper.assertTrue("Outpost 7".equals(moved.ship().getName()), "the ship is named " + moved.ship().getName());
                StationList list = space.getData(ModAttachments.STATIONS);
                StationList.Entry entry = list.stations().stream().filter(e -> e.name().equals("Outpost 7")).findFirst().orElse(null);
                helper.assertTrue(entry != null && entry.assembled(), "the station list does not show the station: " + list);
                helper.assertTrue(entry.position().distanceTo(Vec3.atCenterOf(pos)) < 2.0, "the station is listed at " + entry.position());
                command(server, space, pos, "sable remove @e[name=Outpost 7,limit=1]");
                helper.succeed();
            });
        });
    }

    /** A level station set down returns its blocks to the world where it floats; a turned one comes down turned. */
    @GameTest(template = "empty", timeoutTicks = 400)
    public static void aStationSetsDownWhereItIsTurned(GameTestHelper helper) {
        BlockPos pos = new BlockPos(101_000, 200, 3_000);
        MinecraftServer server = helper.getLevel().getServer();
        ServerLevel space = server.getLevel(ModDimensions.DEEP_SPACE);
        space.setChunkForced(pos.getX() >> 4, pos.getZ() >> 4, true);
        helper.runAfterDelay(5, () -> {
            clearArea(server, space, pos);
            hull(server, space, pos);
            StationCoreBlockEntity core = (StationCoreBlockEntity) space.getBlockEntity(pos);
            core.setStationName("Turntable");
            helper.assertTrue(StationAssembly.assemble(core) == StationAssembly.Result.ASSEMBLED, "assembly failed");
            helper.runAfterDelay(5, () -> {
                StationCoreBlockEntity aboard = coreOf(space, "Turntable");
                helper.assertTrue(aboard != null && aboard.ship() != null, "no station");
                // Turn the station a quarter turn about Y (its +Z now points +X) and set it down.
                var body = dev.ryanhcode.sable.api.physics.handle.RigidBodyHandle.of(aboard.ship());
                Vector3d at = new Vector3d(aboard.ship().logicalPose().position());
                body.teleport(at, new Quaterniond().rotateY(Math.PI / 2));
                helper.runAfterDelay(5, () -> {
                    // A ship turns about its centre of mass, so the core comes back wherever it visibly is now, and the
                    // arm (two blocks +Z of the core in the ship) lands where the turned ship shows it: +X of the core.
                    var pose = aboard.ship().logicalPose();
                    BlockPos corePlot = aboard.getBlockPos();
                    BlockPos coreSpot = BlockPos.containing(pose.transformPosition(Vec3.atCenterOf(corePlot)));
                    BlockPos armSpot = BlockPos.containing(pose.transformPosition(Vec3.atCenterOf(corePlot.south(2))));
                    StationAssembly.Result result = StationAssembly.disassemble(aboard);
                    helper.assertTrue(result == StationAssembly.Result.DISASSEMBLED, "setting down: " + result);
                    helper.runAfterDelay(5, () -> {
                        boolean coreBack = space.getBlockEntity(coreSpot) instanceof StationCoreBlockEntity back && "Turntable".equals(back.stationName());
                        boolean armTurned = space.getBlockState(armSpot).is(Blocks.STONE) && armSpot.equals(coreSpot.east(2)) && space.getBlockState(coreSpot.south(2)).isAir();
                        helper.assertTrue(coreBack, "the core did not come back at " + coreSpot + ": " + space.getBlockState(coreSpot));
                        helper.assertTrue(armTurned, "the arm did not come down turned: arm at " + armSpot + " is " + space.getBlockState(armSpot) + ", core at " + coreSpot);
                        helper.assertTrue(coreOf(space, "Turntable") == null || coreOf(space, "Turntable").ship() == null, "the station still exists");
                        helper.succeed();
                    });
                });
            });
        });
    }

    /** A station cannot be set down into blocks that are in the way. */
    @GameTest(template = "empty", timeoutTicks = 400)
    public static void aStationWillNotSetDownOntoBlocks(GameTestHelper helper) {
        BlockPos pos = new BlockPos(102_000, 200, 3_000);
        MinecraftServer server = helper.getLevel().getServer();
        ServerLevel space = server.getLevel(ModDimensions.DEEP_SPACE);
        space.setChunkForced(pos.getX() >> 4, pos.getZ() >> 4, true);
        helper.runAfterDelay(5, () -> {
            clearArea(server, space, pos);
            hull(server, space, pos);
            StationCoreBlockEntity core = (StationCoreBlockEntity) space.getBlockEntity(pos);
            core.setStationName("Blocked");
            helper.assertTrue(StationAssembly.assemble(core) == StationAssembly.Result.ASSEMBLED, "assembly failed");
            helper.runAfterDelay(5, () -> {
                StationCoreBlockEntity aboard = coreOf(space, "Blocked");
                helper.assertTrue(aboard != null && aboard.ship() != null, "no station");
                // Something moved into the space the arm would land in.
                space.setBlock(pos.south(2), Blocks.OBSIDIAN.defaultBlockState(), 3);
                StationAssembly.Result result = StationAssembly.disassemble(aboard);
                boolean stillAStation = aboard.ship() != null && !aboard.isRemoved();
                command(server, space, pos, "sable remove @e[name=Blocked,limit=1]");
                helper.assertTrue(result == StationAssembly.Result.BLOCKED, "setting down onto obsidian: " + result);
                helper.assertTrue(stillAStation, "the station was taken apart anyway");
                helper.succeed();
            });
        });
    }

    /**
     * A ship whose end sits in the cell in front of a powered Docking Clamp is docked (its blocks join the station,
     * the visiting ship is gone); cutting the power releases it as its own ship again, by its own name.
     */
    @GameTest(template = "empty", timeoutTicks = 600)
    public static void aPoweredClampDocksATouchingShipAndReleasesItWhenUnpowered(GameTestHelper helper) {
        BlockPos pos = new BlockPos(103_000, 200, 3_000);
        MinecraftServer server = helper.getLevel().getServer();
        ServerLevel space = server.getLevel(ModDimensions.DEEP_SPACE);
        space.setChunkForced(pos.getX() >> 4, pos.getZ() >> 4, true);
        helper.runAfterDelay(5, () -> {
            clearArea(server, space, pos);
            int x = pos.getX(), y = pos.getY(), z = pos.getZ();
            // The visitor first: a bar of stone three blocks long, two cells clear of the clamp's face; it gets its
            // name while it is the only ship here.
            command(server, space, pos, "fill " + x + " " + y + " " + (z + 5) + " " + x + " " + y + " " + (z + 7) + " minecraft:stone");
            command(server, space, pos, "sable assemble connected " + x + " " + y + " " + (z + 5));
            helper.runAfterDelay(2, () -> {
                AeronauticsGameTests.nameShip(server, space, pos.south(6));
                // The station: core, one stone, the clamp facing south at the end.
                command(server, space, pos, "setblock " + x + " " + y + " " + z + " void_reclaimer:station_core[facing=north]");
                command(server, space, pos, "setblock " + x + " " + y + " " + (z + 1) + " minecraft:stone");
                command(server, space, pos, "setblock " + x + " " + y + " " + (z + 2) + " void_reclaimer:docking_clamp[facing=south]");
                StationCoreBlockEntity core = (StationCoreBlockEntity) space.getBlockEntity(pos);
                core.setStationName("Dock");
                helper.assertTrue(StationAssembly.assemble(core) == StationAssembly.Result.ASSEMBLED, "station assembly failed");
                helper.runAfterDelay(5, () -> {
                    var visitor = shipNamed(space, "vr_test_" + pos.getX());
                    helper.assertTrue(visitor != null, "no visitor ship");
                    // Slide the visitor north so its first block sits in the clamp's front cell (z + 3), just short of touching.
                    var body = dev.ryanhcode.sable.api.physics.handle.RigidBodyHandle.of(visitor);
                    Vector3d at = new Vector3d(visitor.logicalPose().position()).add(0, 0, -1.9);
                    body.teleport(at, new Quaterniond(visitor.logicalPose().orientation()));
                    helper.runAfterDelay(5, () -> {
                        var clamp = clampOf(space, "Dock");
                        helper.assertTrue(clamp != null, "no clamp aboard the station");
                        BlockPos clampPos = clamp.getBlockPos();
                        space.setBlock(clampPos.east(), Blocks.REDSTONE_BLOCK.defaultBlockState(), 3);
                        helper.runAfterDelay(15, () -> {
                            var afterDock = clampOf(space, "Dock");
                            boolean docked = afterDock != null && afterDock.isDocked();
                            boolean visitorGone = shipNamed(space, "vr_test_" + pos.getX()) == null;
                            boolean joined = afterDock != null && space.getBlockState(afterDock.front()).is(Blocks.STONE);
                            helper.assertTrue(docked, "the clamp did not dock: " + (afterDock == null ? "no clamp" : afterDock.status()));
                            helper.assertTrue(visitorGone, "the visiting ship still exists on its own");
                            helper.assertTrue(joined, "the visitor's block did not join the station in front of the clamp");
                            helper.assertTrue(("vr_test_" + pos.getX()).equals(afterDock.dockedName()), "docked name " + afterDock.dockedName());
                            space.setBlock(afterDock.getBlockPos().east(), Blocks.AIR.defaultBlockState(), 3);
                            helper.runAfterDelay(15, () -> {
                                var afterRelease = clampOf(space, "Dock");
                                var released = shipNamed(space, "vr_test_" + pos.getX());
                                boolean freed = afterRelease != null && !afterRelease.isDocked() && space.getBlockState(afterRelease.front()).isAir();
                                int stones = released == null ? 0 : (int) StationAssembly.blocksOf(space, released).stream()
                                        .filter(p -> space.getBlockState(p).is(Blocks.STONE)).count();
                                remove(server, space, pos, "Dock");
                                remove(server, space, pos, "vr_test_" + pos.getX());
                                helper.assertTrue(freed, "the clamp did not let go: " + (afterRelease == null ? "no clamp" : afterRelease.status()));
                                helper.assertTrue(released != null && stones == 3, "the released ship is not the three-stone bar: " + stones);
                                helper.succeed();
                            });
                        });
                    });
                });
            });
        });
    }

    /** The Docking Clamp aboard the station named {@code name}, or null. */
    static com.voidreclaimer.station.DockingClampBlockEntity clampOf(ServerLevel space, String name) {
        for (var ship : dev.ryanhcode.sable.api.sublevel.SubLevelContainer.getContainer(space).getAllSubLevels()) {
            if (ship.isRemoved() || !name.equals(ship.getName())) {
                continue;
            }
            for (BlockPos p : com.voidreclaimer.ship.Ships.blocksOf(space, ship)) {
                if (space.getBlockEntity(p) instanceof com.voidreclaimer.station.DockingClampBlockEntity clamp) {
                    return clamp;
                }
            }
        }
        return null;
    }

    static dev.ryanhcode.sable.sublevel.ServerSubLevel shipNamed(ServerLevel space, String name) {
        for (var ship : dev.ryanhcode.sable.api.sublevel.SubLevelContainer.getContainer(space).getAllSubLevels()) {
            if (!ship.isRemoved() && name.equals(ship.getName())) {
                return ship;
            }
        }
        return null;
    }

    /** A Station Core with a stone arm reaching three blocks south. */
    @GameTest(template = "empty", timeoutTicks = 200)
    public static void theStationSceneAssemblesOnceEvenBuiltTwice(GameTestHelper helper) {
        var scene = com.voidreclaimer.debug.TestScenes.Scene.STATION;
        BlockPos pos = com.voidreclaimer.debug.TestScenes.origin(scene);
        MinecraftServer server = helper.getLevel().getServer();
        ServerLevel space = server.getLevel(ModDimensions.DEEP_SPACE);
        space.setChunkForced(pos.getX() >> 4, pos.getZ() >> 4, true);
        helper.runAfterDelay(5, () -> {
            // The world persists between local runs, so count against the first build, not against zero.
            com.voidreclaimer.debug.TestScenes.build(space, scene, null);
            long first = testStations(space);
            helper.assertTrue(first >= 1, "no station was built");
            com.voidreclaimer.debug.TestScenes.build(space, scene, null);
            helper.assertTrue(testStations(space) == first, "building again made another station: " + testStations(space));
            helper.assertTrue(coreOf(space, "Test Station") != null, "the core is not aboard");
            helper.succeed();
        });
    }

    private static long testStations(ServerLevel space) {
        return dev.ryanhcode.sable.api.sublevel.SubLevelContainer.getContainer(space).getAllSubLevels().stream()
                .filter(ship -> !ship.isRemoved() && "Test Station".equals(ship.getName())).count();
    }

    private static void hull(MinecraftServer server, ServerLevel space, BlockPos pos) {
        int x = pos.getX(), y = pos.getY(), z = pos.getZ();
        command(server, space, pos, "setblock " + x + " " + y + " " + z + " void_reclaimer:station_core[facing=north]");
        command(server, space, pos, "fill " + x + " " + y + " " + (z + 1) + " " + x + " " + y + " " + (z + 3) + " minecraft:stone");
    }

    /** The assembled Station Core named {@code name}, found through its ship. */
    static StationCoreBlockEntity coreOf(ServerLevel space, String name) {
        for (var ship : dev.ryanhcode.sable.api.sublevel.SubLevelContainer.getContainer(space).getAllSubLevels()) {
            if (ship.isRemoved() || !name.equals(ship.getName())) {
                continue;
            }
            for (BlockPos p : com.voidreclaimer.ship.Ships.blocksOf(space, ship)) {
                if (space.getBlockEntity(p) instanceof StationCoreBlockEntity core) {
                    return core;
                }
            }
        }
        return null;
    }

    static List<String> remove(MinecraftServer server, ServerLevel space, BlockPos pos, String name) {
        return command(server, space, pos, "sable remove @e[name=" + name + ",limit=1]");
    }
}
