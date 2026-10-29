package com.voidreclaimer.gametest;

import com.voidreclaimer.Config;
import com.voidreclaimer.VoidReclaimer;
import com.voidreclaimer.dyson.Dyson;
import com.voidreclaimer.dyson.DysonSphereData;
import com.voidreclaimer.machine.MassDriverBaseBlockEntity;
import com.voidreclaimer.machine.PayloadChamberBlockEntity;
import com.voidreclaimer.machine.QuantumReceiverBlockEntity;
import com.voidreclaimer.registry.ModBlocks;
import com.voidreclaimer.registry.ModMaterials;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.neoforged.neoforge.items.IItemHandler;

/** Phase 9: the Kinetic Mass Driver (launch rate, payload handling, Dyson hand-off, event) and the Quantum Receiver. */
@GameTestHolder(VoidReclaimer.MOD_ID)
@PrefixGameTestTemplate(false)
public final class MassDriverGameTests {
    private MassDriverGameTests() {}

    private static final BlockPos AREA = new BlockPos(87_000, 80, 0);
    /** Create's own tick eats a tick or two around a speed change. */
    private static final int SLACK = 4;

    /** Create's periodic check resets the speed of a source-less block about every 60 ticks, which costs a tick each time. */
    private static int slack(int ticks) {
        return SLACK + ticks / 30;
    }

    private interface Test {
        void run(ServerLevel level, BlockPos pos);
    }

    /** Areas 100 and 120 blocks along X are the ones {@link TestEventListeners} reacts to; the rest are spaced 20 apart. */
    private static void inArea(GameTestHelper helper, int x, Test test) {
        BlockPos pos = new BlockPos(x, AREA.getY(), AREA.getZ());
        NaniteGameTests.inLoadedArea(helper, pos, 6, () -> test.run(TestPlayers.deepSpace(helper), pos));
    }

    private static void resetDyson(ServerLevel level) {
        Dyson.set(level.getServer(), DysonSphereData.createDefault());
    }

    private static MassDriverBaseBlockEntity build(ServerLevel level, BlockPos pos, boolean nozzle) {
        level.setBlock(pos, ModBlocks.KINETIC_DRIVE_BASE.get().defaultBlockState(), 3);
        level.setBlock(pos.above(), ModBlocks.PAYLOAD_CHAMBER.get().defaultBlockState(), 3);
        level.setBlock(pos.above(2), nozzle ? ModBlocks.MAGNETIC_BARREL_NOZZLE.get().defaultBlockState() : net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(), 3);
        MassDriverBaseBlockEntity base = (MassDriverBaseBlockEntity) level.getBlockEntity(pos);
        base.tick();
        return base;
    }

    private static void spin(MassDriverBaseBlockEntity base, float rpm, int ticks) {
        for (int i = 0; i < ticks; i++) {
            base.setSpeed(rpm);
            base.tick();
        }
    }

    private static IItemHandler chamber(ServerLevel level, BlockPos base) {
        return level.getCapability(Capabilities.ItemHandler.BLOCK, base.above(), null);
    }

    @GameTest(template = "empty", timeoutTicks = 100)
    public static void massDriverLaunchesPayloadsIntoTheDysonSphere(GameTestHelper helper) {
        inArea(helper, 87_000, (level, pos) -> {
            resetDyson(level);
            MassDriverBaseBlockEntity base = build(level, pos, true);
            IItemHandler items = chamber(level, pos);
            helper.assertTrue(items != null, "no item handler on the chamber");
            helper.assertTrue(items.insertItem(0, new ItemStack(ModMaterials.DYSON_SWARM_SATELLITE_PAYLOAD.get(), 2), false).isEmpty(), "satellites refused");
            helper.assertTrue(items.insertItem(1, new ItemStack(ModMaterials.DYSON_SHELL_TRUSS_MODULE.get(), 1), false).isEmpty(), "truss refused");

            int slow = Config.MASS_DRIVER_SLOW_TICKS.get();
            spin(base, Config.MASS_DRIVER_MIN_RPM.get(), slow - 5);
            helper.assertTrue(DysonSphereData.of(level.getServer()).satellites() == 0, "launched before the wind-up ended");
            spin(base, Config.MASS_DRIVER_MIN_RPM.get(), 5 + slack(slow));
            DysonSphereData data = DysonSphereData.of(level.getServer());
            helper.assertTrue(data.satellites() == 1 && data.trusses() == 0, "first launch: " + data);
            spin(base, Config.MASS_DRIVER_MIN_RPM.get(), slow + slack(slow));
            data = DysonSphereData.of(level.getServer());
            helper.assertTrue(data.satellites() == 2, "second launch: " + data);
            spin(base, Config.MASS_DRIVER_MIN_RPM.get(), slow + slack(slow));
            data = DysonSphereData.of(level.getServer());
            helper.assertTrue(data.satellites() == 2 && data.trusses() == 1, "truss launch: " + data);
            helper.assertTrue(base.launches() == 3, "launch count " + base.launches());
            resetDyson(level);
            helper.succeed();
        });
    }

    @GameTest(template = "empty", timeoutTicks = 100)
    public static void fasterRotationLaunchesFasterAndSlowRotationNever(GameTestHelper helper) {
        inArea(helper, 87_020, (level, pos) -> {
            resetDyson(level);
            MassDriverBaseBlockEntity base = build(level, pos, true);
            chamber(level, pos).insertItem(0, new ItemStack(ModMaterials.DYSON_SHELL_TRUSS_MODULE.get(), 4), false);

            spin(base, Config.MASS_DRIVER_MIN_RPM.get() - 1, 800);
            helper.assertTrue(DysonSphereData.of(level.getServer()).trusses() == 0, "launched below the minimum speed");

            int fast = Config.MASS_DRIVER_FAST_TICKS.get();
            spin(base, Config.MASS_DRIVER_FAST_RPM.get(), fast + SLACK);
            helper.assertTrue(DysonSphereData.of(level.getServer()).trusses() == 1, "fastest speed should launch every " + fast + " ticks");
            spin(base, Config.MASS_DRIVER_FAST_RPM.get(), 2 * (fast + SLACK));
            helper.assertTrue(DysonSphereData.of(level.getServer()).trusses() == 3, "two more launches expected, have " + DysonSphereData.of(level.getServer()).trusses());
            resetDyson(level);
            helper.succeed();
        });
    }

    @GameTest(template = "empty", timeoutTicks = 100)
    public static void incompleteOrSwitchedOffDriverDoesNothing(GameTestHelper helper) {
        inArea(helper, 87_040, (level, pos) -> {
            resetDyson(level);
            MassDriverBaseBlockEntity base = build(level, pos, false);
            chamber(level, pos).insertItem(0, new ItemStack(ModMaterials.DYSON_SHELL_TRUSS_MODULE.get(), 1), false);
            spin(base, Config.MASS_DRIVER_FAST_RPM.get(), 300);
            helper.assertTrue(DysonSphereData.of(level.getServer()).trusses() == 0, "fired without a nozzle");

            level.setBlock(pos.above(2), ModBlocks.MAGNETIC_BARREL_NOZZLE.get().defaultBlockState(), 3);
            var toggle = Config.TOGGLES.get("massDriver");
            try {
                toggle.set(false);
                spin(base, Config.MASS_DRIVER_FAST_RPM.get(), 300);
                helper.assertTrue(DysonSphereData.of(level.getServer()).trusses() == 0, "fired while switched off");
            } finally {
                toggle.set(true);
            }
            spin(base, Config.MASS_DRIVER_FAST_RPM.get(), Config.MASS_DRIVER_FAST_TICKS.get() + SLACK);
            helper.assertTrue(DysonSphereData.of(level.getServer()).trusses() == 1, "did not fire once complete and switched on");
            resetDyson(level);
            helper.succeed();
        });
    }

    @GameTest(template = "empty", timeoutTicks = 60)
    public static void chamberTakesOnlyPayloadsAndGivesNothingBack(GameTestHelper helper) {
        inArea(helper, 87_060, (level, pos) -> {
            build(level, pos, true);
            IItemHandler items = chamber(level, pos);
            helper.assertTrue(!items.insertItem(0, new ItemStack(Items.DIRT), false).isEmpty(), "dirt accepted");
            helper.assertTrue(items.insertItem(0, new ItemStack(ModMaterials.DYSON_SHELL_TRUSS_MODULE.get(), 16), false).isEmpty(), "16 trusses refused");
            helper.assertTrue(items.insertItem(0, new ItemStack(ModMaterials.DYSON_SHELL_TRUSS_MODULE.get(), 1), false).getCount() == 1, "a 17th truss fitted the slot");
            helper.assertTrue(items.extractItem(0, 1, false).isEmpty(), "automation extracted a payload");
            helper.assertTrue(((PayloadChamberBlockEntity) level.getBlockEntity(pos.above())).inventory().getStackInSlot(0).getCount() == 16, "payloads lost");
            helper.succeed();
        });
    }

    @GameTest(template = "empty", timeoutTicks = 100)
    public static void launchEventCanChangeTheCounts(GameTestHelper helper) {
        inArea(helper, TestEventListeners.FIVE_TRUSS_DRIVER_X, (level, pos) -> {
            resetDyson(level);
            MassDriverBaseBlockEntity base = build(level, pos, true);
            chamber(level, pos).insertItem(0, new ItemStack(ModMaterials.DYSON_SWARM_SATELLITE_PAYLOAD.get(), 1), false);
            spin(base, Config.MASS_DRIVER_FAST_RPM.get(), Config.MASS_DRIVER_FAST_TICKS.get() + SLACK);
            DysonSphereData data = DysonSphereData.of(level.getServer());
            helper.assertTrue(data.satellites() == 0 && data.trusses() == 5, "listener should turn the satellite into 5 trusses: " + data);
            resetDyson(level);
            helper.succeed();
        });
    }

    @GameTest(template = "empty", timeoutTicks = 100)
    public static void cancelledLaunchStillUsesThePayload(GameTestHelper helper) {
        inArea(helper, TestEventListeners.CANCELLED_DRIVER_X, (level, pos) -> {
            resetDyson(level);
            MassDriverBaseBlockEntity base = build(level, pos, true);
            chamber(level, pos).insertItem(0, new ItemStack(ModMaterials.DYSON_SHELL_TRUSS_MODULE.get(), 1), false);
            spin(base, Config.MASS_DRIVER_FAST_RPM.get(), Config.MASS_DRIVER_FAST_TICKS.get() + SLACK);
            helper.assertTrue(DysonSphereData.of(level.getServer()).trusses() == 0, "a cancelled launch still counted");
            helper.assertTrue(((PayloadChamberBlockEntity) level.getBlockEntity(pos.above())).inventory().getStackInSlot(0).isEmpty(), "payload kept");
            helper.succeed();
        });
    }

    @GameTest(template = "empty", timeoutTicks = 100)
    public static void payloadTagsDecideWhatLaunches(GameTestHelper helper) {
        // The GameTest datapack adds an amethyst shard to #void_reclaimer:dyson/satellite_payloads: no code names it.
        inArea(helper, 87_080, (level, pos) -> {
            resetDyson(level);
            MassDriverBaseBlockEntity base = build(level, pos, true);
            helper.assertTrue(chamber(level, pos).insertItem(0, new ItemStack(Items.AMETHYST_SHARD, 1), false).isEmpty(), "tagged item refused");
            spin(base, Config.MASS_DRIVER_FAST_RPM.get(), Config.MASS_DRIVER_FAST_TICKS.get() + SLACK);
            helper.assertTrue(DysonSphereData.of(level.getServer()).satellites() == 1, "tagged item did not count as a satellite");
            resetDyson(level);
            helper.succeed();
        });
    }

    @GameTest(template = "empty", timeoutTicks = 60)
    public static void receiverOutputGrowsWithTrussesLaunched(GameTestHelper helper) {
        inArea(helper, 87_140, (level, pos) -> {
            level.setBlock(pos, ModBlocks.QUANTUM_RECEIVER.get().defaultBlockState(), 3);
            QuantumReceiverBlockEntity receiver = (QuantumReceiverBlockEntity) level.getBlockEntity(pos);
            Dyson.set(level.getServer(), DysonSphereData.createDefault());
            helper.assertTrue(receiver.getGeneratedSpeed() == 0f, "turned with no trusses launched");

            long total = Config.DYSON_SHELL_TOTAL.get();
            Dyson.set(level.getServer(), DysonSphereData.createDefault().withTrusses(total / 2));
            helper.assertTrue(receiver.getGeneratedSpeed() == Config.RECEIVER_RPM.get(), "speed " + receiver.getGeneratedSpeed());
            float half = receiver.calculateAddedStressCapacity() * Config.RECEIVER_RPM.get();
            helper.assertTrue(Math.abs(half - Config.RECEIVER_MAX_STRESS.get() / 2) < 2f, "half shell gave " + half);

            Dyson.set(level.getServer(), DysonSphereData.createDefault().withTrusses(total * 2));
            float full = receiver.calculateAddedStressCapacity() * Config.RECEIVER_RPM.get();
            helper.assertTrue(Math.abs(full - Config.RECEIVER_MAX_STRESS.get()) < 2f, "full shell gave " + full);

            var toggle = Config.TOGGLES.get("quantumReceiver");
            try {
                toggle.set(false);
                helper.assertTrue(receiver.getGeneratedSpeed() == 0f, "turned while switched off");
            } finally {
                toggle.set(true);
            }
            resetDyson(level);
            helper.succeed();
        });
    }
}
