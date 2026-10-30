package com.voidreclaimer.gametest;

import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import com.voidreclaimer.Config;
import com.voidreclaimer.VoidReclaimer;
import com.voidreclaimer.api.event.CollectorPowerEvent;
import com.voidreclaimer.dust.DustCloud;
import com.voidreclaimer.dust.DustField;
import com.voidreclaimer.dust.RawItems;
import com.voidreclaimer.machine.CloudSiphonBlockEntity;
import com.voidreclaimer.registry.ModBlocks;
import java.util.function.Consumer;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.energy.IEnergyStorage;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.neoforged.neoforge.items.IItemHandler;

/** The Cloud Siphon block: raw items out of a dense cloud, turned by a shaft or powered by an addon. */
@GameTestHolder(VoidReclaimer.MOD_ID)
@PrefixGameTestTemplate(false)
public final class CloudSiphonGameTests {
    private static final int TICKS = 400;

    private CloudSiphonGameTests() {}

    private interface Test {
        void run(ServerLevel level, BlockPos pos, Item raw);
    }

    /** A cloud whose heart is dense, searched outward from the origin. */
    private static DustCloud denseCloud(GameTestHelper helper) {
        DustField field = DustGameTests.field(helper);
        for (int r = 0; r < 40; r++) {
            for (int x = -r; x <= r; x++) {
                for (int z = -r; z <= r; z++) {
                    if (Math.max(Math.abs(x), Math.abs(z)) != r) {
                        continue;
                    }
                    for (int y = -1; y <= 0; y++) {
                        var cloud = field.cloudInCell(x, y, z);
                        if (cloud.isPresent() && Math.abs(cloud.get().center().y) < 1500 && field.density(cloud.get(), cloud.get().center()) > 0.6) {
                            return cloud.get();
                        }
                    }
                }
            }
        }
        throw new IllegalStateException("no dense dust cloud found near the origin");
    }

    /** A spot far from every cloud. */
    private static BlockPos emptySpace(DustField field) {
        for (int i = 0; i < 1000; i++) {
            Vec3 pos = new Vec3(40_000 + i * 113.0, 80, i * 57.0);
            if (!field.sample(pos).inCloud() && field.nearest(pos, 40, false).isEmpty()) {
                return BlockPos.containing(pos);
            }
        }
        throw new IllegalStateException("no empty space found");
    }

    /** Places a siphon at {@code lane} blocks beside the dense cloud's heart and runs the body on the loaded chunk. */
    private static void inCloud(GameTestHelper helper, int lane, Test test) {
        DustCloud cloud = denseCloud(helper);
        BlockPos pos = BlockPos.containing(cloud.center()).offset(0, 0, lane * 3);
        Item raw = RawItems.forKind(cloud.kind());
        NaniteGameTests.inLoadedArea(helper, pos, 2, () -> test.run(TestPlayers.deepSpace(helper), pos, raw));
    }

    private static CloudSiphonBlockEntity place(ServerLevel level, BlockPos pos) {
        level.setBlock(pos, ModBlocks.CLOUD_SIPHON.get().defaultBlockState(), 3);
        CloudSiphonBlockEntity siphon = (CloudSiphonBlockEntity) level.getBlockEntity(pos);
        siphon.tick(); // Create attaches the block to the kinetic network on its first tick, which resets the speed
        return siphon;
    }

    /** Create resets the speed of a block with no source, so the test sets it before every tick. */
    private static void tick(KineticBlockEntity machine, float rpm, int ticks) {
        for (int i = 0; i < ticks; i++) {
            machine.setSpeed(rpm);
            machine.tick();
        }
    }

    private static int count(CloudSiphonBlockEntity siphon, Item item) {
        int total = 0;
        for (int slot = 0; slot < CloudSiphonBlockEntity.SLOTS; slot++) {
            ItemStack stack = siphon.inventory().getStackInSlot(slot);
            total += stack.is(item) ? stack.getCount() : 0;
        }
        return total;
    }

    @GameTest(template = "empty", timeoutTicks = 300)
    public static void siphonCondensesTheCloudsKindWhenTurned(GameTestHelper helper) {
        inCloud(helper, 0, (level, pos, raw) -> {
            CloudSiphonBlockEntity siphon = place(level, pos);
            IItemHandler automation = level.getCapability(Capabilities.ItemHandler.BLOCK, pos, null);
            helper.assertTrue(automation != null, "no item handler");
            helper.assertTrue(!automation.insertItem(0, new ItemStack(raw), false).isEmpty(), "automation could insert");

            tick(siphon, 64f, TICKS);
            helper.assertTrue(siphon.density() > Config.SIPHON_MIN_DENSITY.get(), "test cloud not dense: " + siphon.density());
            helper.assertTrue(Math.abs(siphon.power() - 1f) < 1e-4, "power at 64 RPM was " + siphon.power());
            double perSecond = siphon.density() * Config.SIPHON_RATE.get();
            double expected = perSecond * TICKS / 20.0;
            int made = count(siphon, raw);
            helper.assertTrue(made >= Math.floor(expected) - 2 && made <= Math.ceil(expected) + 1, made + " items, expected about " + expected);
            helper.assertTrue(made >= 3, "made only " + made);
            ItemStack taken = automation.extractItem(0, 64, false);
            helper.assertTrue(taken.is(raw) && !taken.isEmpty(), "nothing extractable from slot 0: " + taken);
            helper.succeed();
        });
    }

    @GameTest(template = "empty", timeoutTicks = 300)
    public static void siphonDoesNothingTooSlowOrInThinDust(GameTestHelper helper) {
        inCloud(helper, 1, (level, pos, raw) -> {
            CloudSiphonBlockEntity slow = place(level, pos);
            tick(slow, Config.MACHINE_MIN_RPM.get() - 1, TICKS);
            helper.assertTrue(count(slow, raw) == 0, "ran below the minimum speed");
            helper.assertTrue(slow.power() == 0f, "power below the minimum speed was " + slow.power());

            BlockPos empty = emptySpace(DustGameTests.field(helper));
            NaniteGameTests.inLoadedArea(helper, empty, 2, () -> {
                CloudSiphonBlockEntity thin = place(level, empty);
                tick(thin, 64f, TICKS);
                helper.assertTrue(thin.density() == 0.0, "density outside any cloud: " + thin.density());
                for (int slot = 0; slot < CloudSiphonBlockEntity.SLOTS; slot++) {
                    helper.assertTrue(thin.inventory().getStackInSlot(slot).isEmpty(), "made items outside a cloud");
                }
                helper.succeed();
            });
        });
    }

    /** The fringe of a cloud has dust, but too thin to siphon. */
    @GameTest(template = "empty", timeoutTicks = 300)
    public static void siphonDoesNothingInTheThinFringeOfACloud(GameTestHelper helper) {
        DustCloud cloud = denseCloud(helper);
        DustField field = DustGameTests.field(helper);
        BlockPos fringe = null;
        for (int dx = 0; dx < 600 && fringe == null; dx += 4) {
            BlockPos candidate = BlockPos.containing(cloud.center()).offset(dx, 0, 5 * 3);
            double density = field.sample(Vec3.atCenterOf(candidate)).density();
            if (density > 0.05 && density < Config.SIPHON_MIN_DENSITY.get() - 0.05) {
                fringe = candidate;
            }
        }
        helper.assertTrue(fringe != null, "no thin fringe found beside the dense cloud");
        BlockPos pos = fringe;
        NaniteGameTests.inLoadedArea(helper, pos, 2, () -> {
            ServerLevel level = TestPlayers.deepSpace(helper);
            CloudSiphonBlockEntity siphon = place(level, pos);
            tick(siphon, 64f, TICKS);
            helper.assertTrue(siphon.density() > 0.0 && siphon.density() < Config.SIPHON_MIN_DENSITY.get(), "density " + siphon.density());
            for (int slot = 0; slot < CloudSiphonBlockEntity.SLOTS; slot++) {
                helper.assertTrue(siphon.inventory().getStackInSlot(slot).isEmpty(), "made items in thin dust");
            }
            helper.succeed();
        });
    }

    @GameTest(template = "empty", timeoutTicks = 300)
    public static void addonPowerRunsItWithoutRotation(GameTestHelper helper) {
        inCloud(helper, 2, (level, pos, raw) -> {
            CloudSiphonBlockEntity siphon = place(level, pos);
            Consumer<CollectorPowerEvent> addon = event -> event.setPower(1f);
            NeoForge.EVENT_BUS.addListener(addon);
            try {
                tick(siphon, 0f, TICKS);
            } finally {
                NeoForge.EVENT_BUS.unregister(addon);
            }
            helper.assertTrue(count(siphon, raw) >= 3, "an addon-powered siphon made only " + count(siphon, raw));
            helper.succeed();
        });
    }

    @GameTest(template = "empty", timeoutTicks = 300)
    public static void forgeEnergyRunsItWithoutRotation(GameTestHelper helper) {
        inCloud(helper, 4, (level, pos, raw) -> {
            CloudSiphonBlockEntity siphon = place(level, pos);
            IEnergyStorage fe = level.getCapability(Capabilities.EnergyStorage.BLOCK, pos, null);
            helper.assertTrue(fe != null && fe.canReceive() && !fe.canExtract(), "no receive-only energy capability");
            int perTick = Config.SIPHON_FE_PER_TICK.get();
            helper.assertTrue(perTick > 0, "the energy input is off by default");
            helper.assertTrue(fe.receiveEnergy(500, false) == 500 && fe.extractEnergy(100, false) == 0, "energy came back out");
            for (int i = 0; i < TICKS; i++) {
                fe.receiveEnergy(perTick, false);
                tick(siphon, 0f, 1);
            }
            helper.assertTrue(count(siphon, raw) >= 3, "a siphon on electricity alone made only " + count(siphon, raw));
            // Every tick fed 40 and drew 40, so the 500 put in first is still there: the draw matched the feed.
            helper.assertTrue(Math.abs(siphon.storedEnergy() - 500) <= 2 * perTick, "the draw did not match the feed: " + siphon.storedEnergy());
            helper.succeed();
        });
    }

    @GameTest(template = "empty", timeoutTicks = 300)
    public static void aFullShaftUsesNoEnergy(GameTestHelper helper) {
        inCloud(helper, 5, (level, pos, raw) -> {
            CloudSiphonBlockEntity siphon = place(level, pos);
            // Power is sampled once a second and place() sampled it at 0 RPM: let the shaft's sample replace it first.
            tick(siphon, 64f, 25);
            IEnergyStorage fe = level.getCapability(Capabilities.EnergyStorage.BLOCK, pos, null);
            int filled = fe.receiveEnergy(Integer.MAX_VALUE, false);
            helper.assertTrue(filled == Config.SIPHON_FE_BUFFER.get() && fe.receiveEnergy(1, false) == 0, "buffer is not bounded: " + filled);
            tick(siphon, 64f, TICKS);
            helper.assertTrue(count(siphon, raw) >= 3, "the shaft made only " + count(siphon, raw));
            helper.assertTrue(siphon.storedEnergy() == filled, "a siphon at full shaft power drew energy: " + siphon.storedEnergy());
            helper.succeed();
        });
    }

    @GameTest(template = "empty", timeoutTicks = 300)
    public static void aSiphonWithNoCloudDrawsNoEnergy(GameTestHelper helper) {
        BlockPos empty = emptySpace(DustGameTests.field(helper));
        NaniteGameTests.inLoadedArea(helper, empty, 2, () -> {
            ServerLevel level = TestPlayers.deepSpace(helper);
            CloudSiphonBlockEntity siphon = place(level, empty);
            int filled = level.getCapability(Capabilities.EnergyStorage.BLOCK, empty, null).receiveEnergy(1000, false);
            tick(siphon, 0f, 100);
            helper.assertTrue(siphon.storedEnergy() == filled, "an idle siphon drew energy: " + siphon.storedEnergy());
            helper.succeed();
        });
    }

    @GameTest(template = "empty", timeoutTicks = 300)
    public static void fullOutputPausesWithoutLosingItems(GameTestHelper helper) {
        inCloud(helper, 3, (level, pos, raw) -> {
            CloudSiphonBlockEntity siphon = place(level, pos);
            for (int slot = 0; slot < CloudSiphonBlockEntity.SLOTS; slot++) {
                siphon.inventory().setStackInSlot(slot, new ItemStack(raw, raw.getDefaultMaxStackSize()));
            }
            int before = count(siphon, raw);
            tick(siphon, 64f, TICKS);
            helper.assertTrue(count(siphon, raw) == before, "a full siphon changed its contents");
            siphon.inventory().extractItem(0, 10, false);
            tick(siphon, 64f, 60);
            helper.assertTrue(count(siphon, raw) > before - 10, "it did not resume once there was room");
            helper.succeed();
        });
    }
}
