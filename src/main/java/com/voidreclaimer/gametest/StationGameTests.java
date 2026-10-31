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

    /** A Station Core with a stone arm reaching three blocks south. */
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
