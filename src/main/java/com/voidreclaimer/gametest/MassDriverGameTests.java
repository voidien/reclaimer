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

    /** The three-block stack is superseded by the Launch Complex ({@link LaunchComplexGameTests}) and launches nothing. */
    @GameTest(template = "empty", timeoutTicks = 200)
    public static void theOldMassDriverStackIsInert(GameTestHelper helper) {
        inArea(helper, 87_000, (level, pos) -> {
            resetDyson(level);
            MassDriverBaseBlockEntity base = build(level, pos, true);
            chamber(level, pos).insertItem(0, new ItemStack(ModMaterials.DYSON_SHELL_TRUSS_MODULE.get(), 2), false);
            spin(base, Config.MASS_DRIVER_FAST_RPM.get(), 300);
            DysonSphereData data = DysonSphereData.of(level.getServer());
            helper.assertTrue(data.trusses() == 0 && data.satellites() == 0, "the old stack launched: " + data);
            helper.assertTrue(base.launches() == 0 && base.progress() == 0f, "the old stack wound up");
            helper.assertTrue(((PayloadChamberBlockEntity) level.getBlockEntity(pos.above())).inventory().getStackInSlot(0).getCount() == 2, "payloads used");
            helper.succeed();
        });
    }

    @GameTest(template = "empty", timeoutTicks = 200)
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
