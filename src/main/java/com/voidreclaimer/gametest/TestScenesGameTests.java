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
