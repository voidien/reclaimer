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
