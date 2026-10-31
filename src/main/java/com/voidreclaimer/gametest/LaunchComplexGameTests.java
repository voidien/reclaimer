package com.voidreclaimer.gametest;

import com.voidreclaimer.Config;
import com.voidreclaimer.VoidReclaimer;
import com.voidreclaimer.dyson.Dyson;
import com.voidreclaimer.dyson.DysonSphereData;
import com.voidreclaimer.launch.LaunchComplex;
import com.voidreclaimer.launch.LaunchControllerBlockEntity;
import com.voidreclaimer.launch.LaunchPad;
import com.voidreclaimer.registry.ModBlocks;
import com.voidreclaimer.registry.ModMaterials;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.neoforged.neoforge.items.IItemHandler;

/**
 * The launch complex: a Launch Controller facing south with a ring of Scaffold around a 15 x 15 pad. Each test has its
 * controller at chunk-local x 8 so the whole pad lies in one force-loaded chunk.
 */
@GameTestHolder(VoidReclaimer.MOD_ID)
@PrefixGameTestTemplate(false)
public final class LaunchComplexGameTests {
    private LaunchComplexGameTests() {}

    private static final int Y = 80;
    private static final Direction FACING = Direction.SOUTH;
    /** Create's own tick eats a tick or two around a speed change. */
    private static final int SLACK = 4;

    /** Create's periodic check resets the speed of a source-less block about every 60 ticks, which costs a tick each time. */
    private static int slack(int ticks) {
        return SLACK + ticks / 30;
    }

    private interface Test {
        void run(ServerLevel level, BlockPos pos);
    }

    private static void inArea(GameTestHelper helper, int x, Test test) {
        BlockPos pos = new BlockPos(x, Y, 0);
        NaniteGameTests.inLoadedArea(helper, pos, 2, () -> {
            ServerLevel level = TestPlayers.deepSpace(helper);
            clear(level, pos);
            test.run(level, pos);
        });
    }

    /** Empties the pad's footprint and the layer above it (the world persists between runs). */
    private static void clear(ServerLevel level, BlockPos pos) {
        for (int across = -LaunchPad.HALF; across <= LaunchPad.HALF; across++) {
            for (int forward = 0; forward < LaunchPad.SIZE; forward++) {
                BlockPos cell = LaunchPad.cell(pos, FACING, across, forward);
                level.setBlock(cell, Blocks.AIR.defaultBlockState(), 3);
                level.setBlock(cell.above(), Blocks.AIR.defaultBlockState(), 3);
            }
        }
    }

    private static LaunchControllerBlockEntity place(ServerLevel level, BlockPos pos) {
        level.setBlock(pos, ModBlocks.LAUNCH_CONTROLLER.get().defaultBlockState().setValue(BlockStateProperties.HORIZONTAL_FACING, FACING), 3);
        return (LaunchControllerBlockEntity) level.getBlockEntity(pos);
    }

    private static void ring(ServerLevel level, BlockPos pos, Block block) {
        for (BlockPos cell : LaunchPad.ring(pos, FACING)) {
            level.setBlock(cell, block.defaultBlockState(), 3);
        }
    }

    private static int padBlocks(ServerLevel level, BlockPos pos) {
        int count = 0;
        for (BlockPos cell : LaunchPad.interior(pos, FACING)) {
            if (level.getBlockState(cell).is(ModBlocks.LAUNCH_PAD.get())) {
                count++;
            }
        }
        return count;
    }

    /** A controller with a complete Scaffold ring, assembled. */
    private static LaunchControllerBlockEntity build(GameTestHelper helper, ServerLevel level, BlockPos pos) {
        LaunchControllerBlockEntity controller = place(level, pos);
        ring(level, pos, ModBlocks.SCAFFOLD.get());
        LaunchComplex.Result result = LaunchComplex.assemble(controller);
        helper.assertTrue(result == LaunchComplex.Result.ASSEMBLED, "assembly: " + result);
        controller.tick();
        return controller;
    }

    private static void spin(LaunchControllerBlockEntity controller, float rpm, int ticks) {
        for (int i = 0; i < ticks; i++) {
            controller.setSpeed(rpm);
            controller.tick();
        }
    }

    private static IItemHandler items(ServerLevel level, BlockPos pos) {
        return level.getCapability(Capabilities.ItemHandler.BLOCK, pos, null);
    }

    private static void resetDyson(ServerLevel level) {
        Dyson.set(level.getServer(), DysonSphereData.createDefault());
    }

    @GameTest(template = "empty", timeoutTicks = 200)
    public static void aCompleteRingAssemblesAndFillsThePad(GameTestHelper helper) {
        inArea(helper, 89_000, (level, pos) -> {
            LaunchControllerBlockEntity controller = place(level, pos);
            ring(level, pos, ModBlocks.PORTAL_FRAME.get());
            helper.assertTrue(LaunchComplex.assemble(controller) == LaunchComplex.Result.ASSEMBLED, "a Portal Frame ring did not assemble");
            helper.assertTrue(controller.assembled(), "controller not marked assembled");
            helper.assertTrue(padBlocks(level, pos) == (LaunchPad.SIZE - 2) * (LaunchPad.SIZE - 2), "pad blocks: " + padBlocks(level, pos));
            helper.assertTrue(level.getBlockState(LaunchPad.centre(pos, FACING)).isSolid(), "the pad is not solid");
            helper.assertTrue(LaunchComplex.assemble(controller) == LaunchComplex.Result.ALREADY_ASSEMBLED, "assembled twice");

            helper.assertTrue(LaunchComplex.disassemble(controller) == LaunchComplex.Result.DISASSEMBLED, "did not set down");
            helper.assertTrue(!controller.assembled() && padBlocks(level, pos) == 0, "pad blocks left: " + padBlocks(level, pos));
            helper.assertTrue(LaunchComplex.disassemble(controller) == LaunchComplex.Result.NOT_ASSEMBLED, "set down twice");
            helper.succeed();
        });
    }

    @GameTest(template = "empty", timeoutTicks = 200)
    public static void aGapOrAnObstacleRefusesAssembly(GameTestHelper helper) {
        inArea(helper, 89_032, (level, pos) -> {
            LaunchControllerBlockEntity controller = place(level, pos);
            ring(level, pos, ModBlocks.SCAFFOLD.get());
            BlockPos corner = LaunchPad.cell(pos, FACING, LaunchPad.HALF, LaunchPad.SIZE - 1);
            level.setBlock(corner, Blocks.AIR.defaultBlockState(), 3);
            helper.assertTrue(LaunchComplex.assemble(controller) == LaunchComplex.Result.RING_INCOMPLETE, "assembled with a gap");
            level.setBlock(corner, Blocks.DIRT.defaultBlockState(), 3);
            helper.assertTrue(LaunchComplex.assemble(controller) == LaunchComplex.Result.RING_INCOMPLETE, "dirt counted as frame");
            level.setBlock(corner, ModBlocks.SCAFFOLD.get().defaultBlockState(), 3);

            BlockPos inside = LaunchPad.cell(pos, FACING, 2, 5);
            level.setBlock(inside, Blocks.STONE.defaultBlockState(), 3);
            helper.assertTrue(LaunchComplex.assemble(controller) == LaunchComplex.Result.BLOCKED, "assembled over a stone");
            helper.assertTrue(padBlocks(level, pos) == 0 && !controller.assembled(), "a refused assembly left pad blocks");
            level.setBlock(inside, Blocks.AIR.defaultBlockState(), 3);
            helper.assertTrue(LaunchComplex.assemble(controller) == LaunchComplex.Result.ASSEMBLED, "did not assemble once clear");
            LaunchComplex.disassemble(controller);
            helper.succeed();
        });
    }

    @GameTest(template = "empty", timeoutTicks = 200)
    public static void aBrokenRingSetsThePadDownAndAMissingPadBlockGrowsBack(GameTestHelper helper) {
        inArea(helper, 89_064, (level, pos) -> {
            LaunchControllerBlockEntity controller = build(helper, level, pos);
            BlockPos hole = LaunchPad.cell(pos, FACING, -3, 7);
            level.setBlock(hole, Blocks.AIR.defaultBlockState(), 3);
            spin(controller, 0, 25);
            helper.assertTrue(level.getBlockState(hole).is(ModBlocks.LAUNCH_PAD.get()), "the pad did not grow back");

            level.setBlock(LaunchPad.cell(pos, FACING, 0, LaunchPad.SIZE - 1), Blocks.AIR.defaultBlockState(), 3);
            spin(controller, 0, 25);
            helper.assertTrue(!controller.assembled(), "still assembled with a broken ring");
            helper.assertTrue(padBlocks(level, pos) == 0, "pad blocks left after the ring broke: " + padBlocks(level, pos));
            helper.succeed();
        });
    }

    @GameTest(template = "empty", timeoutTicks = 200)
    public static void breakingTheControllerSetsThePadDown(GameTestHelper helper) {
        inArea(helper, 89_096, (level, pos) -> {
            LaunchControllerBlockEntity controller = build(helper, level, pos);
            controller.inventory().setStackInSlot(0, new ItemStack(ModMaterials.DYSON_SHELL_TRUSS_MODULE.get(), 3));
            level.destroyBlock(pos, true);
            helper.assertTrue(padBlocks(level, pos) == 0, "pad blocks left after the controller broke: " + padBlocks(level, pos));
            helper.assertTrue(!level.getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class, new net.minecraft.world.phys.AABB(pos).inflate(2),
                    item -> item.getItem().is(ModMaterials.DYSON_SHELL_TRUSS_MODULE.get())).isEmpty(), "the payloads were not dropped");
            level.getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class, new net.minecraft.world.phys.AABB(pos).inflate(2)).forEach(net.minecraft.world.entity.Entity::discard);
            helper.succeed();
        });
    }

    @GameTest(template = "empty", timeoutTicks = 200)
    public static void theComplexLaunchesPayloadsIntoTheDysonSphere(GameTestHelper helper) {
        inArea(helper, 89_128, (level, pos) -> {
            resetDyson(level);
            LaunchControllerBlockEntity controller = build(helper, level, pos);
            IItemHandler items = items(level, pos);
            helper.assertTrue(items != null, "no item handler on the controller");
            helper.assertTrue(items.insertItem(0, new ItemStack(ModMaterials.DYSON_SWARM_SATELLITE_PAYLOAD.get(), 2), false).isEmpty(), "satellites refused");
            helper.assertTrue(items.insertItem(1, new ItemStack(ModMaterials.DYSON_SHELL_TRUSS_MODULE.get(), 1), false).isEmpty(), "truss refused");

            int slow = Config.MASS_DRIVER_SLOW_TICKS.get();
            spin(controller, Config.MASS_DRIVER_MIN_RPM.get(), slow - 5);
            helper.assertTrue(DysonSphereData.of(level.getServer()).satellites() == 0, "launched before the wind-up ended");
            spin(controller, Config.MASS_DRIVER_MIN_RPM.get(), 5 + slack(slow));
            DysonSphereData data = DysonSphereData.of(level.getServer());
            helper.assertTrue(data.satellites() == 1 && data.trusses() == 0, "first launch: " + data);
            spin(controller, Config.MASS_DRIVER_MIN_RPM.get(), slow + slack(slow));
            data = DysonSphereData.of(level.getServer());
            helper.assertTrue(data.satellites() == 2, "second launch: " + data);
            spin(controller, Config.MASS_DRIVER_MIN_RPM.get(), slow + slack(slow));
            data = DysonSphereData.of(level.getServer());
            helper.assertTrue(data.satellites() == 2 && data.trusses() == 1, "truss launch: " + data);
            helper.assertTrue(controller.launches() == 3, "launch count " + controller.launches());
            resetDyson(level);
            LaunchComplex.disassemble(controller);
            helper.succeed();
        });
    }

    @GameTest(template = "empty", timeoutTicks = 200)
    public static void fasterRotationLaunchesFasterAndSlowRotationNever(GameTestHelper helper) {
        inArea(helper, 89_160, (level, pos) -> {
            resetDyson(level);
            LaunchControllerBlockEntity controller = build(helper, level, pos);
            items(level, pos).insertItem(0, new ItemStack(ModMaterials.DYSON_SHELL_TRUSS_MODULE.get(), 4), false);

            spin(controller, Config.MASS_DRIVER_MIN_RPM.get() - 1, 800);
            helper.assertTrue(DysonSphereData.of(level.getServer()).trusses() == 0, "launched below the minimum speed");

            int fast = Config.MASS_DRIVER_FAST_TICKS.get();
            spin(controller, Config.MASS_DRIVER_FAST_RPM.get(), fast + SLACK);
            helper.assertTrue(DysonSphereData.of(level.getServer()).trusses() == 1, "fastest speed should launch every " + fast + " ticks");
            spin(controller, Config.MASS_DRIVER_FAST_RPM.get(), 2 * (fast + SLACK));
            helper.assertTrue(DysonSphereData.of(level.getServer()).trusses() == 3, "two more launches expected, have " + DysonSphereData.of(level.getServer()).trusses());
            resetDyson(level);
            LaunchComplex.disassemble(controller);
            helper.succeed();
        });
    }

    @GameTest(template = "empty", timeoutTicks = 200)
    public static void anUnassembledOrSwitchedOffComplexDoesNothing(GameTestHelper helper) {
        inArea(helper, 89_192, (level, pos) -> {
            resetDyson(level);
            LaunchControllerBlockEntity controller = place(level, pos);
            ring(level, pos, ModBlocks.SCAFFOLD.get());
            items(level, pos).insertItem(0, new ItemStack(ModMaterials.DYSON_SHELL_TRUSS_MODULE.get(), 1), false);
            spin(controller, Config.MASS_DRIVER_FAST_RPM.get(), 300);
            helper.assertTrue(DysonSphereData.of(level.getServer()).trusses() == 0, "fired before assembly");

            helper.assertTrue(LaunchComplex.assemble(controller) == LaunchComplex.Result.ASSEMBLED, "did not assemble");
            var toggle = Config.TOGGLES.get("massDriver");
            try {
                toggle.set(false);
                spin(controller, Config.MASS_DRIVER_FAST_RPM.get(), 300);
                helper.assertTrue(DysonSphereData.of(level.getServer()).trusses() == 0, "fired while switched off");
            } finally {
                toggle.set(true);
            }
            spin(controller, Config.MASS_DRIVER_FAST_RPM.get(), Config.MASS_DRIVER_FAST_TICKS.get() + SLACK);
            helper.assertTrue(DysonSphereData.of(level.getServer()).trusses() == 1, "did not fire once assembled and switched on");
            resetDyson(level);
            LaunchComplex.disassemble(controller);
            helper.succeed();
        });
    }

    @GameTest(template = "empty", timeoutTicks = 200)
    public static void theControllerTakesOnlyPayloadsAndGivesNothingBack(GameTestHelper helper) {
        inArea(helper, 89_224, (level, pos) -> {
            LaunchControllerBlockEntity controller = build(helper, level, pos);
            IItemHandler items = items(level, pos);
            helper.assertTrue(!items.insertItem(0, new ItemStack(Items.DIRT), false).isEmpty(), "dirt accepted");
            helper.assertTrue(items.insertItem(0, new ItemStack(ModMaterials.DYSON_SHELL_TRUSS_MODULE.get(), 16), false).isEmpty(), "16 trusses refused");
            helper.assertTrue(items.insertItem(0, new ItemStack(ModMaterials.DYSON_SHELL_TRUSS_MODULE.get(), 1), false).getCount() == 1, "a 17th truss fitted the slot");
            helper.assertTrue(items.extractItem(0, 1, false).isEmpty(), "automation extracted a payload");
            helper.assertTrue(controller.inventory().getStackInSlot(0).getCount() == 16, "payloads lost");
            controller.inventory().setStackInSlot(0, ItemStack.EMPTY);
            LaunchComplex.disassemble(controller);
            helper.succeed();
        });
    }

    @GameTest(template = "empty", timeoutTicks = 200)
    public static void launchEventCanChangeTheCounts(GameTestHelper helper) {
        inArea(helper, TestEventListeners.FIVE_TRUSS_DRIVER_X, (level, pos) -> {
            resetDyson(level);
            LaunchControllerBlockEntity controller = build(helper, level, pos);
            items(level, pos).insertItem(0, new ItemStack(ModMaterials.DYSON_SWARM_SATELLITE_PAYLOAD.get(), 1), false);
            spin(controller, Config.MASS_DRIVER_FAST_RPM.get(), Config.MASS_DRIVER_FAST_TICKS.get() + SLACK);
            DysonSphereData data = DysonSphereData.of(level.getServer());
            helper.assertTrue(data.satellites() == 0 && data.trusses() == 5, "listener should turn the satellite into 5 trusses: " + data);
            resetDyson(level);
            LaunchComplex.disassemble(controller);
            helper.succeed();
        });
    }

    @GameTest(template = "empty", timeoutTicks = 200)
    public static void cancelledLaunchStillUsesThePayload(GameTestHelper helper) {
        inArea(helper, TestEventListeners.CANCELLED_DRIVER_X, (level, pos) -> {
            resetDyson(level);
            LaunchControllerBlockEntity controller = build(helper, level, pos);
            items(level, pos).insertItem(0, new ItemStack(ModMaterials.DYSON_SHELL_TRUSS_MODULE.get(), 1), false);
            spin(controller, Config.MASS_DRIVER_FAST_RPM.get(), Config.MASS_DRIVER_FAST_TICKS.get() + SLACK);
            helper.assertTrue(DysonSphereData.of(level.getServer()).trusses() == 0, "a cancelled launch still counted");
            helper.assertTrue(controller.inventory().getStackInSlot(0).isEmpty(), "payload kept");
            helper.assertTrue(controller.launches() == 1, "a cancelled launch is still a launch: " + controller.launches());
            LaunchComplex.disassemble(controller);
            helper.succeed();
        });
    }

    /** The renderer poses the model from the synced wind-up and launch time. */
    @GameTest(template = "empty", timeoutTicks = 200)
    public static void aLaunchIsSyncedForTheRenderer(GameTestHelper helper) {
        inArea(helper, 89_352, (level, pos) -> {
            resetDyson(level);
            LaunchControllerBlockEntity controller = build(helper, level, pos);
            helper.assertTrue(controller.lastLaunch() < 0, "a new controller remembers a launch");
            items(level, pos).insertItem(0, new ItemStack(ModMaterials.DYSON_SHELL_TRUSS_MODULE.get(), 1), false);
            int fast = Config.MASS_DRIVER_FAST_TICKS.get();
            spin(controller, Config.MASS_DRIVER_FAST_RPM.get(), fast / 2);
            net.minecraft.nbt.CompoundTag packet = controller.getUpdateTag(level.registryAccess());
            helper.assertTrue(packet.getFloat("progress") > 0.2f && packet.getFloat("progress") < 0.8f, "the client packet lacks the wind-up: " + packet);
            helper.assertTrue(!packet.contains("inventory"), "the client packet carries the inventory");
            spin(controller, Config.MASS_DRIVER_FAST_RPM.get(), fast / 2 + SLACK);
            helper.assertTrue(controller.lastLaunch() == level.getGameTime(), "launch time " + controller.lastLaunch() + " vs " + level.getGameTime());
            helper.assertTrue(controller.getUpdateTag(level.registryAccess()).getLong("lastLaunch") == level.getGameTime(), "the client packet lacks the launch time");
            resetDyson(level);
            LaunchComplex.disassemble(controller);
            helper.succeed();
        });
    }

    @GameTest(template = "empty", timeoutTicks = 200)
    public static void payloadTagsDecideWhatLaunches(GameTestHelper helper) {
        // The GameTest datapack adds an amethyst shard to #void_reclaimer:dyson/satellite_payloads: no code names it.
        inArea(helper, 89_320, (level, pos) -> {
            resetDyson(level);
            LaunchControllerBlockEntity controller = build(helper, level, pos);
            helper.assertTrue(items(level, pos).insertItem(0, new ItemStack(Items.AMETHYST_SHARD, 1), false).isEmpty(), "tagged item refused");
            spin(controller, Config.MASS_DRIVER_FAST_RPM.get(), Config.MASS_DRIVER_FAST_TICKS.get() + SLACK);
            helper.assertTrue(DysonSphereData.of(level.getServer()).satellites() == 1, "tagged item did not count as a satellite");
            resetDyson(level);
            LaunchComplex.disassemble(controller);
            helper.succeed();
        });
    }
}
