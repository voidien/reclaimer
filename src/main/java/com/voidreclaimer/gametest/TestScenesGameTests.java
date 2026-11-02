package com.voidreclaimer.gametest;

import com.voidreclaimer.VoidReclaimer;
import com.voidreclaimer.debug.GateTimer;
import com.voidreclaimer.debug.Speedway;
import com.voidreclaimer.debug.TestScenes;
import com.voidreclaimer.helper.HelperDrone;
import com.voidreclaimer.launch.LaunchControllerBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/** The test scenes really build what they promise, also when built twice. */
@GameTestHolder(VoidReclaimer.MOD_ID)
@PrefixGameTestTemplate(false)
public final class TestScenesGameTests {
    private TestScenesGameTests() {}

    /** A parked shell and another player's body are in the way like any block, and the extension point hears about the hit. */
    @GameTest(template = "empty", timeoutTicks = 200)
    public static void aBigBodyStopsAtOtherBodies(GameTestHelper helper) {
        BlockPos origin = new BlockPos(95_200, 300, 0);
        NaniteGameTests.inEntityTickingArea(helper, origin, 2, () -> {
            ServerLevel space = TestPlayers.deepSpace(helper);
            var player = TestPlayers.survivalInDeepSpace(helper, origin.getX() + 0.5, origin.getY(), origin.getZ() + 0.5);
            com.voidreclaimer.body.Bodies.store(player, com.voidreclaimer.body.Bodies.preset(com.voidreclaimer.body.BuiltinBodies.SHUTTLE));
            player.setYRot(0f);
            player.setXRot(0f);
            var ahead = new net.minecraft.world.phys.Vec3(0.0, 0.0, 2.0);
            helper.assertTrue(com.voidreclaimer.body.BoxCollision.resolve(player, ahead) == ahead, "something is in the way already");
            // A Drone shell ahead of the nose, whose own vanilla box is all that is in the way.
            var shell = com.voidreclaimer.link.BodyHost.spawn(space, player.position().add(0.0, 1.5, 9.0), com.voidreclaimer.link.HostRole.SHELL,
                    com.voidreclaimer.body.Bodies.preset(com.voidreclaimer.body.BuiltinBodies.DRONE), player.getUUID());
            TestEventListeners.COLLISIONS.remove(player.getUUID());
            double allowed = com.voidreclaimer.body.BoxCollision.resolve(player, ahead).z;
            helper.assertTrue(allowed < 2.0, "the Shuttle went through a shell");
            Double hit = TestEventListeners.COLLISIONS.get(player.getUUID());
            helper.assertTrue(hit != null && Math.abs(hit - 2.0) < 1e-6, "a head-on hit at 2.0 a tick lost " + hit);
            shell.discard();
            TestPlayers.remove(player);
            helper.succeed();
        });
    }

    /** A Titan shell, and a body ahead of its nose looking back at it, further from the shell's feet than its vanilla box reaches. */
    private static void flyAtATitansNose(GameTestHelper helper, BlockPos origin, net.minecraft.resources.ResourceKey<com.voidreclaimer.api.body.Chassis> flyer) {
        NaniteGameTests.inEntityTickingArea(helper, origin, 2, () -> {
            ServerLevel space = TestPlayers.deepSpace(helper);
            var player = TestPlayers.survivalInDeepSpace(helper, origin.getX() + 0.5, origin.getY() + 13.0, origin.getZ() + 44.5);
            com.voidreclaimer.body.Bodies.store(player, com.voidreclaimer.body.Bodies.preset(flyer));
            player.setYRot(180f);
            player.setXRot(0f);
            var ahead = new net.minecraft.world.phys.Vec3(0.0, 0.0, -2.0);
            var shell = com.voidreclaimer.link.BodyHost.spawn(space, origin.getBottomCenter(), com.voidreclaimer.link.HostRole.SHELL,
                    com.voidreclaimer.body.Bodies.preset(com.voidreclaimer.body.BuiltinBodies.TITAN), player.getUUID());
            shell.setYRot(0f);
            shell.setXRot(0f);
            double start = player.getZ();
            for (int tick = 0; tick < 12 && com.voidreclaimer.body.BoxCollision.resolve(player, ahead).z <= -1.9; tick++) {
                player.setPos(player.position().add(ahead));
            }
            double stopped = player.getZ();
            helper.assertTrue(start - stopped < 22.0, "it flew " + (start - stopped) + " blocks, into the saucer");
            helper.assertTrue(com.voidreclaimer.body.BoxCollision.resolve(player, ahead).z > -1.9, "nothing stopped it");
            helper.assertTrue(stopped - origin.getZ() > 20.0, "it stopped at " + (stopped - origin.getZ()) + " blocks from the Titan's feet, not at its nose");
            shell.discard();
            TestPlayers.remove(player);
            helper.succeed();
        });
    }

    @GameTest(template = "empty", timeoutTicks = 200)
    public static void aShuttleStopsAtATitansNoseFarFromItsVanillaBox(GameTestHelper helper) {
        flyAtATitansNose(helper, new BlockPos(95_600, 300, 0), com.voidreclaimer.body.BuiltinBodies.SHUTTLE);
    }

    @GameTest(template = "empty", timeoutTicks = 200)
    public static void aDroneStopsAtATitansNose(GameTestHelper helper) {
        flyAtATitansNose(helper, new BlockPos(96_000, 300, 0), com.voidreclaimer.body.BuiltinBodies.DRONE);
    }

    /** The view jumps a quarter turn: a Titan's nose follows slowly and thrust goes with it, and a Mech is there at once. */
    @GameTest(template = "empty", timeoutTicks = 200)
    public static void aBigBodyTurnsWithWeightAndThrustFollowsTheNose(GameTestHelper helper) {
        BlockPos origin = new BlockPos(94_800, 300, 0);
        NaniteGameTests.inLoadedArea(helper, origin, 2, () -> {
            var player = TestPlayers.survivalInDeepSpace(helper, origin.getX() + 0.5, origin.getY(), origin.getZ() + 0.5);
            com.voidreclaimer.body.Bodies.store(player, com.voidreclaimer.body.Bodies.preset(com.voidreclaimer.body.BuiltinBodies.TITAN));
            player.setYRot(0f);
            player.setXRot(0f);
            com.voidreclaimer.body.BodyTurning.turn(player);
            player.setYRot(90f);
            for (int tick = 0; tick < 10; tick++) {
                player.tickCount++;
                com.voidreclaimer.body.BodyTurning.turn(player);
            }
            float turned = com.voidreclaimer.body.BodyTurning.of(player).yaw();
            helper.assertTrue(turned > 1f && turned < 30f, "half a second after a quarter turn the Titan has turned " + turned);
            var input = com.voidreclaimer.flight.FlightInput.of(true, false, false, false, false, false);
            var thrust = com.voidreclaimer.flight.ThrusterFlight.step(player, net.minecraft.world.phys.Vec3.ZERO, input);
            helper.assertTrue(thrust.z > Math.abs(thrust.x), "thrust went along the view, not the nose: " + thrust);

            com.voidreclaimer.body.Bodies.store(player, com.voidreclaimer.body.Bodies.preset(com.voidreclaimer.body.BuiltinBodies.MECH));
            player.tickCount++;
            com.voidreclaimer.body.BodyTurning.turn(player);
            helper.assertTrue(com.voidreclaimer.body.BodyTurning.of(player).yaw() == 90f, "a Mech lags its view");
            TestPlayers.remove(player);
            helper.succeed();
        });
    }

    private static double halfTurn(GameTestHelper helper, net.minecraft.resources.ResourceKey<com.voidreclaimer.api.body.Chassis> chassis) {
        var body = com.voidreclaimer.body.Bodies.preview(com.voidreclaimer.body.Bodies.preset(chassis), helper.getLevel().registryAccess());
        double rate = com.voidreclaimer.body.BodyHeading.degreesPerTick(body.stats(),
                com.voidreclaimer.body.BodyHeading.TURN_FACTOR, com.voidreclaimer.body.BodyHeading.SLOWEST_TURN, com.voidreclaimer.body.BodyHeading.FASTEST_TURN);
        return 180.0 / (rate * com.voidreclaimer.flight.FlightStats.TICKS_PER_SECOND);
    }

    /** The owner's numbers for a half turn, from the bodies as built, so a change to a section or a mass can't drift from them unseen. */
    @GameTest(template = "empty")
    public static void theBuiltInBodiesTakeTheOwnersTimeToTurn(GameTestHelper helper) {
        double shuttle = halfTurn(helper, com.voidreclaimer.body.BuiltinBodies.SHUTTLE);
        double ship = halfTurn(helper, com.voidreclaimer.body.BuiltinBodies.SHIP);
        double titan = halfTurn(helper, com.voidreclaimer.body.BuiltinBodies.TITAN);
        helper.assertTrue(Math.abs(shuttle - 1.5) < 0.1, "Shuttle " + shuttle);
        helper.assertTrue(Math.abs(ship - 4.0) < 0.1, "Ship " + ship);
        helper.assertTrue(Math.abs(titan - 12.0) < 0.2, "Titan " + titan);
        helper.succeed();
    }

    /** A body moved in one step is pointing at its view, not swinging round from where it was. */
    @GameTest(template = "empty", timeoutTicks = 200)
    public static void aTeleportedBigBodyPointsAtItsView(GameTestHelper helper) {
        BlockPos origin = new BlockPos(94_600, 300, 0);
        NaniteGameTests.inLoadedArea(helper, origin, 2, () -> {
            var player = TestPlayers.survivalInDeepSpace(helper, origin.getX() + 0.5, origin.getY(), origin.getZ() + 0.5);
            com.voidreclaimer.body.Bodies.store(player, com.voidreclaimer.body.Bodies.preset(com.voidreclaimer.body.BuiltinBodies.TITAN));
            player.setYRot(0f);
            player.setXRot(0f);
            com.voidreclaimer.body.BodyTurning.turn(player);
            player.setYRot(120f);
            player.setPos(player.position().add(0.0, 0.0, 200.0));
            player.tickCount++;
            com.voidreclaimer.body.BodyTurning.turn(player);
            float yaw = com.voidreclaimer.body.BodyTurning.of(player).yaw();
            helper.assertTrue(yaw == 120f, "the body still swings from its old heading: " + yaw);
            player.setYRot(0f);
            player.setPos(player.position().add(0.0, 0.0, 2.0));
            player.tickCount++;
            com.voidreclaimer.body.BodyTurning.turn(player);
            float flying = com.voidreclaimer.body.BodyTurning.of(player).yaw();
            helper.assertTrue(flying > 100f, "flying on is not a teleport, yet the heading went to " + flying);
            TestPlayers.remove(player);
            helper.succeed();
        });
    }

    /** A Titan's nose stops at a wall its vanilla box is nowhere near; the same flight is free for a Drone, and past a section that has gone. */
    @GameTest(template = "empty", timeoutTicks = 200)
    public static void aBigBodyStopsAtAWallItsVanillaBoxWouldMiss(GameTestHelper helper) {
        BlockPos origin = new BlockPos(94_400, 300, 0);
        NaniteGameTests.inLoadedArea(helper, origin, 3, () -> {
            ServerLevel space = TestPlayers.deepSpace(helper);
            var player = TestPlayers.survivalInDeepSpace(helper, origin.getX() + 0.5, origin.getY(), origin.getZ() + 0.5);
            com.voidreclaimer.body.Bodies.store(player, com.voidreclaimer.body.Bodies.preset(com.voidreclaimer.body.BuiltinBodies.TITAN));
            // Yaw 0 looks along +Z, and a Titan's saucer reaches about 30 blocks that way.
            player.setYRot(0f);
            player.setXRot(0f);
            BlockPos wall = origin.offset(0, 13, 31);
            for (int x = -3; x <= 3; x++) {
                for (int y = -3; y <= 3; y++) {
                    space.setBlock(wall.offset(x, y, 0), Blocks.STONE.defaultBlockState(), net.minecraft.world.level.block.Block.UPDATE_CLIENTS);
                }
            }
            net.minecraft.world.phys.Vec3 ahead = new net.minecraft.world.phys.Vec3(0.0, 0.0, 2.0);
            double allowed = com.voidreclaimer.body.BoxCollision.resolve(player, ahead).z;
            helper.assertTrue(allowed >= 0.0 && allowed < 2.0, "the Titan's saucer moved " + allowed + " of 2 into a wall in front of it");
            var sideways = new net.minecraft.world.phys.Vec3(2.0, 0.0, 0.0);
            helper.assertTrue(com.voidreclaimer.body.BoxCollision.resolve(player, sideways) == sideways, "a wall ahead stopped a move along it");
            com.voidreclaimer.body.Bodies.store(player, com.voidreclaimer.body.Bodies.preset(com.voidreclaimer.body.BuiltinBodies.DRONE));
            helper.assertTrue(com.voidreclaimer.body.BoxCollision.resolve(player, ahead) == ahead, "a Drone has no boxes, so nothing should change for it");
            TestPlayers.remove(player);
            helper.succeed();
        });
    }

    /** The overlay reads the same boxes collision will, so a stone in the saucer has to show. */
    @GameTest(template = "empty", timeoutTicks = 200)
    public static void theBoxOverlayShowsABigBodyAndWhatItTouches(GameTestHelper helper) {
        BlockPos origin = new BlockPos(93_800, 300, 0);
        NaniteGameTests.inEntityTickingArea(helper, origin, 2, () -> {
            ServerLevel space = TestPlayers.deepSpace(helper);
            var player = TestPlayers.survivalInDeepSpace(helper, origin.getX() + 0.5, origin.getY(), origin.getZ() + 0.5);
            com.voidreclaimer.body.Bodies.store(player, com.voidreclaimer.body.Bodies.preset(com.voidreclaimer.body.BuiltinBodies.TITAN));
            space.getChunkSource().move(player);
            var shapes = new com.voidreclaimer.debug.DebugShapes();
            var facts = new java.util.ArrayList<String>();
            com.voidreclaimer.debug.BoxOverlay.add(space, player.position(), 8.0, shapes, facts);
            helper.assertTrue(facts.stream().anyMatch(fact -> fact.contains("19 boxes, 0 in a block")), "free: " + facts);
            helper.assertTrue(shapes.lines().stream().anyMatch(line -> line.colour() == com.voidreclaimer.debug.BoxOverlay.FREE), "no green box drawn");
            var ahead = com.voidreclaimer.body.BodyBoxes.place(com.voidreclaimer.body.Bodies.view(player).chassis(), socket -> true, player.position(), player.getYRot(), player.getXRot(), 1.0).get(0).middle();
            BlockPos stone = BlockPos.containing(ahead.x(), ahead.y(), ahead.z());
            space.setBlock(stone, Blocks.STONE.defaultBlockState(), net.minecraft.world.level.block.Block.UPDATE_CLIENTS);
            var touching = new java.util.ArrayList<String>();
            var red = new com.voidreclaimer.debug.DebugShapes();
            com.voidreclaimer.debug.BoxOverlay.add(space, player.position(), 8.0, red, touching);
            space.setBlock(stone, Blocks.AIR.defaultBlockState(), net.minecraft.world.level.block.Block.UPDATE_CLIENTS);
            TestPlayers.remove(player);
            helper.assertTrue(touching.stream().anyMatch(fact -> fact.contains("in a block") && !fact.contains(" 0 in a block")), "a stone in the body was not seen: " + touching);
            helper.assertTrue(red.lines().stream().anyMatch(line -> line.colour() == com.voidreclaimer.debug.BoxOverlay.TOUCHING), "no red box drawn");
            helper.succeed();
        });
    }

    @GameTest(template = "empty", timeoutTicks = 200)
    public static void aFlightThroughTheSpeedwayIsTimed(GameTestHelper helper) {
        BlockPos origin = TestScenes.origin(TestScenes.Scene.SPEEDWAY);
        NaniteGameTests.inLoadedArea(helper, origin, 2, () -> {
            ServerLevel space = TestPlayers.deepSpace(helper);
            TestScenes.build(space, TestScenes.Scene.SPEEDWAY, null);
            TestScenes.build(space, TestScenes.Scene.SPEEDWAY, null);
            GateTimer.Gate first = TestScenes.speedwayGates(origin).get(0);
            BlockPos centre = BlockPos.containing(first.x(), first.y(), first.z());
            helper.assertTrue(space.getBlockState(centre.offset(1, TestScenes.GATE_HALF + 1, 0)).is(Blocks.LIME_CONCRETE), "no green frame on the first gate");
            helper.assertTrue(space.getBlockState(centre).isAir(), "the first gate has no hole");
            var player = TestPlayers.survivalInDeepSpace(helper, first.x(), first.y(), first.z() - 1);
            player.doTick();
            player.setPos(first.x(), first.y(), first.z() + 1);
            player.doTick();
            helper.assertTrue(Speedway.run(player) != null && Speedway.run(player).running(), "flying through the first gate did not start the clock");
            Speedway.close();
            helper.assertTrue(Speedway.run(player) == null, "closing the course left a run behind");
            TestPlayers.remove(player);
            helper.succeed();
        });
    }

    @GameTest(template = "empty", timeoutTicks = 200)
    public static void theArenaIsAFloorWithWallsAndLampsAtEveryEighthBlock(GameTestHelper helper) {
        // A small one: a full arena is 64 chunks of 254 sections each, which the test server need not hold.
        BlockPos origin = new BlockPos(93_500, 300, 0);
        NaniteGameTests.inLoadedArea(helper, origin, 4, () -> {
            ServerLevel space = TestPlayers.deepSpace(helper);
            TestScenes.arena(space, origin, 8, 6);
            helper.assertTrue(space.getBlockState(origin).is(Blocks.SEA_LANTERN), "no lamp at the centre");
            helper.assertTrue(space.getBlockState(origin.offset(1, 0, 1)).is(Blocks.WHITE_CONCRETE), "the floor is not pale");
            helper.assertTrue(space.getBlockState(origin.offset(1, 3, 8)).is(Blocks.WHITE_CONCRETE), "no back wall");
            helper.assertTrue(space.getBlockState(origin.offset(-8, 3, 1)).is(Blocks.WHITE_CONCRETE), "no side wall");
            helper.succeed();
        });
    }

    @GameTest(template = "empty", timeoutTicks = 400)
    public static void theLaunchSceneIsAssembledAndLoadedEvenBuiltTwice(GameTestHelper helper) {
        BlockPos origin = TestScenes.origin(TestScenes.Scene.LAUNCH);
        NaniteGameTests.inEntityTickingArea(helper, origin, 2, () -> {
            ServerLevel space = TestPlayers.deepSpace(helper);
            TestScenes.build(space, TestScenes.Scene.LAUNCH, null);
            TestScenes.build(space, TestScenes.Scene.LAUNCH, null);
            helper.assertTrue(space.getBlockEntity(origin) instanceof LaunchControllerBlockEntity controller && controller.assembled(),
                    "the complex is not assembled");
            helper.succeed();
        });
    }

    @GameTest(template = "empty", timeoutTicks = 400)
    public static void theDroneSceneHasOneFerryAndOneMinerEvenBuiltTwice(GameTestHelper helper) {
        BlockPos origin = TestScenes.origin(TestScenes.Scene.DRONES);
        NaniteGameTests.inEntityTickingArea(helper, origin, 2, () -> {
            ServerLevel space = TestPlayers.deepSpace(helper);
            TestScenes.build(space, TestScenes.Scene.DRONES, null);
            TestScenes.build(space, TestScenes.Scene.DRONES, null);
            var drones = space.getEntitiesOfClass(HelperDrone.class, new AABB(origin).inflate(32));
            helper.assertTrue(drones.size() == 2, "drones: " + drones.size());
            helper.assertTrue(drones.stream().filter(d -> d.links().mining()).count() == 1, "no single miner");
            drones.forEach(HelperDrone::discard);
            helper.succeed();
        });
    }
}
