package com.voidreclaimer.gametest;

import com.voidreclaimer.Config;
import com.voidreclaimer.VoidReclaimer;
import com.voidreclaimer.api.body.ModuleKind;
import com.voidreclaimer.dust.DustCloud;
import com.voidreclaimer.dust.DustData;
import com.voidreclaimer.dust.DustField;
import com.voidreclaimer.dust.DustFieldSeed;
import com.voidreclaimer.dust.DustKinds;
import com.voidreclaimer.dust.SensorPulse;
import com.voidreclaimer.energy.EnergyData;
import com.voidreclaimer.registry.ModAttachments;
import com.voidreclaimer.registry.ModItems;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/** Phase 3: dust field seed, dust collection, and the sensor pulse. */
@GameTestHolder(VoidReclaimer.MOD_ID)
@PrefixGameTestTemplate(false)
public final class DustGameTests {
    private static final int TICKS = 40;
    private static final double STEP = 0.4;

    private DustGameTests() {}

    @GameTest(template = "empty")
    public static void fieldSeedIsDerivedAndPresent(GameTestHelper helper) {
        ServerLevel space = TestPlayers.deepSpace(helper);
        DustFieldSeed seed = space.getExistingDataOrNull(ModAttachments.DUST_FIELD_SEED);
        helper.assertTrue(seed != null, "deep space has no dust field seed");
        helper.assertTrue(seed.seed() == DustFieldSeed.derive(space.getSeed()), "seed not derived from the world seed");
        helper.assertTrue(seed.seed() != space.getSeed(), "world seed leaked as the field seed");
        helper.assertTrue(DustField.of(helper.getLevel()) == null, "overworld must have no dust field");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void flyingThroughCloudCollectsItsKind(GameTestHelper helper) {
        DustField field = field(helper);
        DustCloud cloud = findCloud(field);
        ServerPlayer player = TestPlayers.survivalInDeepSpace(helper, cloud.center().x, cloud.center().y, cloud.center().z);
        try {
            moveAndTick(player, TICKS);
            DustData dust = player.getData(ModAttachments.DUST);
            float collected = dust.get(cloud.kind());
            helper.assertTrue(collected > 0.05f, "collected only " + collected + " " + cloud.kind());
            for (DustKinds.Entry entry : DustKinds.all(player.registryAccess())) {
                net.minecraft.resources.ResourceLocation other = entry.id();
                if (!other.equals(cloud.kind())) {
                    helper.assertTrue(dust.get(other) == 0f, "collected " + other + " in a " + cloud.kind() + " cloud");
                }
            }
            helper.succeed();
        } finally {
            TestPlayers.remove(player);
        }
    }

    @GameTest(template = "empty")
    public static void flyingCollectsMoreThanDrifting(GameTestHelper helper) {
        DustField field = field(helper);
        DustCloud cloud = findCloud(field);
        Vec3 c = cloud.center();
        ServerPlayer still = TestPlayers.survivalInDeepSpace(helper, c.x, c.y, c.z);
        ServerPlayer moving = TestPlayers.survivalInDeepSpace(helper, c.x, c.y, c.z);
        try {
            for (int i = 0; i < TICKS; i++) {
                still.doTick();
            }
            moveAndTick(moving, TICKS);
            float stillAmount = still.getData(ModAttachments.DUST).get(cloud.kind());
            float movingAmount = moving.getData(ModAttachments.DUST).get(cloud.kind());
            helper.assertTrue(stillAmount > 0f, "no trickle while still");
            helper.assertTrue(movingAmount > stillAmount * 3, "scooping (" + movingAmount + ") should beat the trickle (" + stillAmount + ")");
            helper.succeed();
        } finally {
            TestPlayers.remove(still);
            TestPlayers.remove(moving);
        }
    }

    @GameTest(template = "empty")
    public static void noDustOutsideClouds(GameTestHelper helper) {
        DustField field = field(helper);
        Vec3 empty = findEmptySpace(field);
        ServerPlayer player = TestPlayers.survivalInDeepSpace(helper, empty.x, empty.y, empty.z);
        try {
            moveAndTick(player, TICKS);
            helper.assertTrue(player.getData(ModAttachments.DUST).equals(DustData.EMPTY), "dust gained outside any cloud");
            helper.succeed();
        } finally {
            TestPlayers.remove(player);
        }
    }

    @GameTest(template = "empty")
    public static void dustNeverExceedsCapacity(GameTestHelper helper) {
        DustField field = field(helper);
        DustCloud cloud = findCloud(field);
        ServerPlayer player = TestPlayers.survivalInDeepSpace(helper, cloud.center().x, cloud.center().y, cloud.center().z);
        try {
            float capacity = DustData.capacity(player);
            player.setData(ModAttachments.DUST, DustData.EMPTY.with(cloud.kind(), capacity - 0.001f));
            moveAndTick(player, TICKS);
            helper.assertTrue(player.getData(ModAttachments.DUST).get(cloud.kind()) == capacity, "dust not clamped to capacity");
            helper.succeed();
        } finally {
            TestPlayers.remove(player);
        }
    }

    @GameTest(template = "empty")
    public static void sensorPulseNeedsSensorCostsEnergyAndCoolsDown(GameTestHelper helper) {
        ServerPlayer player = TestPlayers.survivalInDeepSpace(helper, 30_000.5, 80, 0.5);
        try {
            helper.assertTrue(SensorPulse.pulse(player) == SensorPulse.Result.NO_SENSOR, "pulse without a sensor");

            // A carried sensor does nothing: it has to be installed.
            player.getInventory().add(new ItemStack(ModItems.DUST_SENSOR.get()));
            helper.assertTrue(SensorPulse.pulse(player) == SensorPulse.Result.NO_SENSOR, "carried sensor worked");
            TestPlayers.installModule(player, ModuleKind.SENSOR, 0, new ItemStack(ModItems.DUST_SENSOR.get()));
            setEnergy(player, 50f, false);
            helper.assertTrue(SensorPulse.pulse(player) == SensorPulse.Result.SENT, "pulse with a sensor failed");
            float cost = 50f - player.getData(ModAttachments.ENERGY).energy();
            helper.assertTrue(Math.abs(cost - Config.PULSE_ENERGY_COST.get()) < 1e-3, "pulse cost " + cost);
            helper.assertTrue(player.getData(ModAttachments.SENSOR_STATE).hasPendingEcho(), "no echo pending after a pulse");

            helper.assertTrue(SensorPulse.pulse(player) == SensorPulse.Result.COOLING_DOWN, "second pulse ignored the cooldown");
            helper.assertTrue(Math.abs(player.getData(ModAttachments.ENERGY).energy() - 45f) < 1e-3, "cooldown pulse cost energy");
            helper.succeed();
        } finally {
            TestPlayers.remove(player);
        }
    }

    @GameTest(template = "empty")
    public static void sensorPulseRefusedInLowPower(GameTestHelper helper) {
        ServerPlayer player = TestPlayers.survivalInDeepSpace(helper, 31_000.5, 80, 0.5);
        try {
            TestPlayers.installModule(player, ModuleKind.SENSOR, 0, new ItemStack(ModItems.DUST_SENSOR.get()));
            setEnergy(player, 1f, true);
            helper.assertTrue(SensorPulse.pulse(player) == SensorPulse.Result.LOW_POWER, "pulse allowed in low power");
            helper.succeed();
        } finally {
            TestPlayers.remove(player);
        }
    }

    @GameTest(template = "empty", timeoutTicks = 200)
    public static void echoIsDeliveredAfterItsDelay(GameTestHelper helper) {
        ServerPlayer player = TestPlayers.survivalInDeepSpace(helper, 32_000.5, 80, 0.5);
        TestPlayers.installModule(player, ModuleKind.SENSOR, 0, new ItemStack(ModItems.DUST_SENSOR.get()));
        setEnergy(player, 50f, false);
        helper.assertTrue(SensorPulse.pulse(player) == SensorPulse.Result.SENT, "pulse failed");
        helper.succeedWhen(() -> {
            // The fake connection isn't ticked, so drive the player tick that delivers echoes.
            player.doTick();
            helper.assertFalse(player.getData(ModAttachments.SENSOR_STATE).hasPendingEcho(), "echo still travelling");
            TestPlayers.remove(player);
        });
    }

    static DustField field(GameTestHelper helper) {
        DustField field = DustField.of(TestPlayers.deepSpace(helper));
        if (field == null) {
            throw new IllegalStateException("no dust field in deep space");
        }
        return field;
    }

    /** A cloud with its center comfortably inside the world height, searched outward from the origin. */
    static DustCloud findCloud(DustField field) {
        for (int r = 0; r < 40; r++) {
            for (int x = -r; x <= r; x++) {
                for (int z = -r; z <= r; z++) {
                    if (Math.max(Math.abs(x), Math.abs(z)) != r) {
                        continue;
                    }
                    for (int y = -1; y <= 0; y++) {
                        var cloud = field.cloudInCell(x, y, z);
                        if (cloud.isPresent() && Math.abs(cloud.get().center().y) < 1500 && field.density(cloud.get(), cloud.get().center()) > 0.1) {
                            return cloud.get();
                        }
                    }
                }
            }
        }
        throw new IllegalStateException("no dust cloud found near the origin");
    }

    private static Vec3 findEmptySpace(DustField field) {
        for (int i = 0; i < 1000; i++) {
            Vec3 pos = new Vec3(40_000 + i * 113.0, 80, i * 57.0);
            Vec3 ahead = pos.add(STEP * TICKS, 0, 0);
            if (!field.sample(pos).inCloud() && !field.sample(ahead).inCloud()
                    && field.nearest(pos, STEP * TICKS + 2, false).isEmpty()) {
                return pos;
            }
        }
        throw new IllegalStateException("no empty space found");
    }

    /** Moves the player in a straight line at a legal speed, one player tick per step. */
    static void moveAndTick(ServerPlayer player, int ticks) {
        double x = player.getX();
        for (int i = 0; i < ticks; i++) {
            x += STEP;
            player.setPos(x, player.getY(), player.getZ());
            player.doTick();
        }
    }

    private static void setEnergy(ServerPlayer player, float energy, boolean lowPower) {
        EnergyData old = player.getData(ModAttachments.ENERGY);
        player.setData(ModAttachments.ENERGY, new EnergyData(energy, old.capacity(), lowPower));
    }
}
