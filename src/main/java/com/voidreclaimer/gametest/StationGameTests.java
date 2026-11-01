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

    /**
     * A clamp asks every ship what sits in its front cell, in that ship's own coordinates. From far away those coordinates run
     * on into another ship's plot, and the block found there is not the far ship's: the far ship must not count as a visitor.
     */
    @GameTest(template = "empty", timeoutTicks = 300)
    public static void aClampIgnoresAFarShipWhoseCoordinatesLandInAnotherShip(GameTestHelper helper) {
        BlockPos farPos = new BlockPos(104_000, 200, 3_000);
        BlockPos otherPos = new BlockPos(104_300, 200, 3_000);
        String farName = "vr_test_" + farPos.getX();
        String otherName = "vr_test_" + otherPos.getX();
        MinecraftServer server = helper.getLevel().getServer();
        ServerLevel space = server.getLevel(ModDimensions.DEEP_SPACE);
        space.setChunkForced(farPos.getX() >> 4, farPos.getZ() >> 4, true);
        space.setChunkForced(otherPos.getX() >> 4, otherPos.getZ() >> 4, true);
        helper.runAfterDelay(5, () -> {
            for (BlockPos pos : List.of(farPos, otherPos)) {
                clearArea(server, space, pos);
                command(server, space, pos, "fill " + pos.getX() + " " + pos.getY() + " " + pos.getZ() + " " + pos.getX() + " " + pos.getY() + " " + (pos.getZ() + 2) + " minecraft:stone");
                command(server, space, pos, "sable assemble connected " + pos.getX() + " " + pos.getY() + " " + pos.getZ());
            }
            helper.runAfterDelay(2, () -> {
                AeronauticsGameTests.nameShip(server, space, farPos.south());
                AeronauticsGameTests.nameShip(server, space, otherPos.south());
                var far = shipNamed(space, farName);
                var other = shipNamed(space, otherName);
                helper.assertTrue(far != null && other != null, "the two ships were not built");
                BlockPos inOther = StationAssembly.blocksOf(space, other).get(0);
                BlockPos front = BlockPos.containing(far.logicalPose().transformPosition(net.minecraft.world.phys.Vec3.atCenterOf(inOther)));
                BlockPos corePos = front.north(3);
                space.setChunkForced(front.getX() >> 4, front.getZ() >> 4, true);
                space.setChunkForced(corePos.getX() >> 4, corePos.getZ() >> 4, true);
                helper.runAfterDelay(5, () -> {
                    int x = corePos.getX(), y = corePos.getY(), z = corePos.getZ();
                    command(server, space, corePos, "setblock " + x + " " + y + " " + z + " void_reclaimer:station_core[facing=north]");
                    command(server, space, corePos, "setblock " + x + " " + y + " " + (z + 1) + " minecraft:stone");
                    command(server, space, corePos, "setblock " + x + " " + y + " " + (z + 2) + " void_reclaimer:docking_clamp[facing=south]");
                    StationCoreBlockEntity core = (StationCoreBlockEntity) space.getBlockEntity(corePos);
                    core.setStationName("FarDock");
                    helper.assertTrue(StationAssembly.assemble(core) == StationAssembly.Result.ASSEMBLED, "station assembly failed");
                    helper.runAfterDelay(5, () -> {
                        var clamp = clampOf(space, "FarDock");
                        helper.assertTrue(clamp != null, "no clamp aboard the station");
                        var station = com.voidreclaimer.ship.Ships.shipAt(space, clamp.getBlockPos());
                        var point = station.logicalPose().transformPosition(net.minecraft.world.phys.Vec3.atCenterOf(clamp.front()));
                        BlockPos seen = BlockPos.containing(far.logicalPose().transformPositionInverse(point));
                        boolean landsOnOther = com.voidreclaimer.ship.Ships.shipAt(space, seen) == other && space.getBlockState(seen).is(Blocks.STONE);
                        var status = clamp.dock();
                        boolean stayed = shipNamed(space, farName) != null;
                        remove(server, space, farPos, "FarDock");
                        remove(server, space, farPos, farName);
                        remove(server, space, otherPos, otherName);
                        helper.assertTrue(landsOnOther, "the far ship's coordinates do not land on the other ship, so this checks nothing: " + seen);
                        helper.assertTrue(stayed, "the clamp pulled in a ship from far away");
                        helper.assertTrue(status == com.voidreclaimer.station.DockingClampBlockEntity.Status.NO_VISITOR, "status " + status);
                        helper.succeed();
                    });
                });
            });
        });
    }

    /**
     * A seat aboard a ship keeps the ship loaded while it is in use, so a shell run from a station does not cost the station, and a
     * wake lands by the ship in the world, not at the seat's position in the ship's own coordinates. The ship lets go after a wake.
     */
    @GameTest(template = "empty", timeoutTicks = 1500)
    public static void aSeatAboardAShipHoldsItAndWakesYouByIt(GameTestHelper helper) {
        BlockPos pos = new BlockPos(105_400, 200, 3_000);
        String name = "vr_test_" + pos.getX();
        MinecraftServer server = helper.getLevel().getServer();
        ServerLevel space = server.getLevel(ModDimensions.DEEP_SPACE);
        int chunkX = pos.getX() >> 4, chunkZ = pos.getZ() >> 4;
        space.setChunkForced(chunkX, chunkZ, true);
        helper.runAfterDelay(5, () -> {
            clearArea(server, space, pos);
            command(server, space, pos, "fill " + pos.getX() + " " + pos.getY() + " " + pos.getZ() + " " + pos.getX() + " " + pos.getY() + " " + (pos.getZ() + 2) + " minecraft:iron_block");
            command(server, space, pos, "sable assemble connected " + pos.getX() + " " + pos.getY() + " " + pos.getZ());
            helper.runAfterDelay(2, () -> {
                AeronauticsGameTests.nameShip(server, space, pos.south());
                var ship = shipNamed(space, name);
                helper.assertTrue(ship != null, "the ship was not built");
                BlockPos seatPos = StationAssembly.blocksOf(space, ship).get(0).above();
                var player = TestPlayers.survivalInDeepSpace(helper, pos.getX() + 0.5, pos.getY() + 3, pos.getZ() + 0.5);
                // The shell is far off, so that the player's own presence cannot be what keeps the ship loaded.
                var far = new Vec3(pos.getX() + 2_000.5, pos.getY(), pos.getZ() + 2_000.5);
                int farX = (int) far.x >> 4, farZ = (int) far.z >> 4;
                space.setChunkForced(farX, farZ, true);
                space.setBlock(seatPos, ModBlocks.UPLINK_SEAT.get().defaultBlockState(), 3);
                var seat = (com.voidreclaimer.link.UplinkSeatBlockEntity) space.getBlockEntity(seatPos);
                var shell = com.voidreclaimer.link.BodyHost.spawn(space, far, com.voidreclaimer.link.HostRole.SHELL,
                        com.voidreclaimer.body.Bodies.preset(com.voidreclaimer.body.BuiltinBodies.DRONE), player.getUUID());
                var id = shell.getUUID();
                helper.runAfterDelay(5, () -> {
                    seat.connect(player, id);
                    helper.runAfterDelay(10, () -> {
                        var state = com.voidreclaimer.link.Links.state(player);
                        helper.assertTrue(state.linked(), "the seat made no link");
                        // The ship has not moved, so this stands in for it having moved: the seat must put the place right.
                        var link = state.link().orElseThrow();
                        var wrong = new com.voidreclaimer.link.LinkState.Place(link.from().dimension(), far, 0f, 0f);
                        player.setData(com.voidreclaimer.registry.ModAttachments.LINK, state.withLink(
                                new com.voidreclaimer.link.LinkState.Link(link.shell(), link.epoch(), link.home(), wrong, link.seat())));
                        space.setChunkForced(chunkX, chunkZ, false);
                        helper.runAfterDelay(100, () -> {
                            helper.assertTrue(shipNamed(space, name) != null, "the seat did not hold its ship");
                            helper.assertTrue(com.voidreclaimer.link.Links.wake(player) == com.voidreclaimer.link.Links.Result.WOKEN, "no wake");
                            Vec3 woke = player.position();
                            space.getEntity(id).discard();
                            space.setChunkForced(farX, farZ, false);
                            helper.assertTrue(woke.distanceTo(Vec3.atCenterOf(pos)) < 16.0, "woke away from the ship: " + woke);
                            // A player beside a ship keeps it loaded, so only with them gone does this show the seat let go.
                            player.teleportTo(far.x, far.y, far.z);
                            whenShip(helper, space, name, false, 100, () -> {
                                TestPlayers.remove(player);
                                space.setChunkForced(chunkX, chunkZ, true);
                                whenShip(helper, space, name, true, 80, () -> {
                                    remove(server, space, pos, name);
                                    helper.succeed();
                                });
                            });
                        });
                    });
                });
            });
        });
    }

    /**
     * A Backup Core aboard a station is listed by the ship's own coordinates. While the ship is unloaded nothing can be read
     * there, which says nothing about the core: it stays on the list, and works again when the ship comes back.
     */
    @GameTest(template = "empty", timeoutTicks = 900)
    public static void aBackupCoreStaysListedWhileItsShipIsUnloaded(GameTestHelper helper) {
        BlockPos pos = new BlockPos(105_000, 200, 3_000);
        String name = "vr_test_" + pos.getX();
        MinecraftServer server = helper.getLevel().getServer();
        ServerLevel space = server.getLevel(ModDimensions.DEEP_SPACE);
        int chunkX = pos.getX() >> 4, chunkZ = pos.getZ() >> 4;
        space.setChunkForced(chunkX, chunkZ, true);
        helper.runAfterDelay(5, () -> {
            clearArea(server, space, pos);
            command(server, space, pos, "fill " + pos.getX() + " " + pos.getY() + " " + pos.getZ() + " " + pos.getX() + " " + pos.getY() + " " + (pos.getZ() + 2) + " minecraft:stone");
            command(server, space, pos, "sable assemble connected " + pos.getX() + " " + pos.getY() + " " + pos.getZ());
            helper.runAfterDelay(2, () -> {
                AeronauticsGameTests.nameShip(server, space, pos.south());
                var ship = shipNamed(space, name);
                helper.assertTrue(ship != null, "the ship was not built");
                BlockPos corePos = StationAssembly.blocksOf(space, ship).get(0).above();
                var player = TestPlayers.survivalInDeepSpace(helper, pos.getX() + 0.5, pos.getY(), pos.getZ() + 2_000.5);
                var state = ModBlocks.BACKUP_CORE.get().defaultBlockState();
                space.setBlock(corePos, state, 3);
                state.getBlock().setPlacedBy(space, corePos, state, player, new net.minecraft.world.item.ItemStack(com.voidreclaimer.registry.ModItems.BACKUP_CORE.get()));
                ((com.voidreclaimer.core.BackupCoreBlockEntity) space.getBlockEntity(corePos)).setCharges(2);
                var where = net.minecraft.core.GlobalPos.of(space.dimension(), corePos);
                space.setChunkForced(chunkX, chunkZ, false);
                whenShip(helper, space, name, false, 80, () -> {
                    com.voidreclaimer.core.BackupCores.refresh(player);
                    boolean listed = com.voidreclaimer.core.BackupCores.list(player).get(where).filter(entry -> entry.charges() == 2).isPresent();
                    space.setChunkForced(chunkX, chunkZ, true);
                    whenShip(helper, space, name, true, 80, () -> {
                        com.voidreclaimer.core.BackupCores.refresh(player);
                        com.voidreclaimer.core.BackupCores.select(player, java.util.Optional.of(where));
                        var respawn = com.voidreclaimer.core.BackupCores.respawnAtSelectedCore(player);
                        var core = space.getBlockEntity(corePos) instanceof com.voidreclaimer.core.BackupCoreBlockEntity back ? back : null;
                        TestPlayers.remove(player);
                        remove(server, space, pos, name);
                        helper.assertTrue(listed, "the core was forgotten while its ship was unloaded");
                        helper.assertTrue(core != null, "the core did not come back with its ship");
                        helper.assertTrue(respawn != null, "no respawn at the core once its ship was back");
                        helper.assertTrue(core.charges() == 1, "the respawn did not spend a charge: " + core.charges());
                        helper.assertTrue(respawn.pos().distanceTo(Vec3.atCenterOf(pos)) < 16.0, "respawned away from the ship: " + respawn.pos());
                        helper.succeed();
                    });
                });
            });
        });
    }

    /** Runs {@code then} once the named ship is loaded or not, as asked, looking every 5 ticks. */
    static void whenShip(GameTestHelper helper, ServerLevel space, String name, boolean loaded, int looks, Runnable then) {
        if ((shipNamed(space, name) != null) == loaded) {
            then.run();
        } else if (looks <= 0) {
            helper.fail("the ship never " + (loaded ? "came back" : "unloaded"));
        } else {
            helper.runAfterDelay(5, () -> whenShip(helper, space, name, loaded, looks - 1, then));
        }
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

    @GameTest(template = "empty", timeoutTicks = 200)
    public static void theShipOverlayReadsAShipsBoxAndMassFromSable(GameTestHelper helper) {
        var scene = com.voidreclaimer.debug.TestScenes.Scene.STATION;
        BlockPos pos = com.voidreclaimer.debug.TestScenes.origin(scene);
        MinecraftServer server = helper.getLevel().getServer();
        ServerLevel space = server.getLevel(ModDimensions.DEEP_SPACE);
        space.setChunkForced(pos.getX() >> 4, pos.getZ() >> 4, true);
        helper.runAfterDelay(5, () -> {
            com.voidreclaimer.debug.TestScenes.build(space, scene, null);
            var shapes = new com.voidreclaimer.debug.DebugShapes();
            var facts = new java.util.ArrayList<String>();
            com.voidreclaimer.debug.ShipOverlay.add(space, net.minecraft.world.phys.Vec3.atCenterOf(pos), 64.0, shapes, facts);
            helper.assertTrue(facts.stream().anyMatch(fact -> fact.startsWith("Ship Test Station: 47 blocks") && !fact.contains("mass 0,")),
                    "no ship, the wrong size (a 9 x 5 hull, the core and a Helm), or no mass: " + facts);
            helper.assertTrue(shapes.lines().size() >= 12 + 3, "a box and a centre of mass: " + shapes.lines().size());
            var box = shapes.lines().stream().filter(line -> line.colour() == com.voidreclaimer.debug.ShipOverlay.BOX).toList();
            helper.assertTrue(box.stream().allMatch(line -> line.from().distanceTo(net.minecraft.world.phys.Vec3.atCenterOf(pos)) < 32.0),
                    "the box is not where the ship is in the world: " + box.get(0));
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
