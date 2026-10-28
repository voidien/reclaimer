package com.voidreclaimer.gametest;

import com.voidreclaimer.VoidReclaimer;
import com.voidreclaimer.energy.EnergyData;
import com.voidreclaimer.flight.FlightInput;
import com.voidreclaimer.flight.FlightStats;
import com.voidreclaimer.registry.ModAttachments;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/** Phase 1 energy and Phase 2 flight, driven through real player ticks. */
@GameTestHolder(VoidReclaimer.MOD_ID)
@PrefixGameTestTemplate(false)
public final class FlightGameTests {
    private static final int TICKS = 20;

    private FlightGameTests() {}

    @GameTest(template = "empty")
    public static void sunlightChargesInOpenSpace(GameTestHelper helper) {
        ServerPlayer player = TestPlayers.survivalInDeepSpace(helper, 20_000.5, 80, 0.5);
        try {
            setEnergy(player, 50f);
            tick(player, TICKS);
            // Net +0.8/s in deep space: sun 1.5 x 2/3 minus idle drain 0.2.
            float energy = energy(player);
            helper.assertTrue(Math.abs(energy - 50.8f) < 0.05f, "expected ~50.8 after 1 s of sunlight, got " + energy);
            helper.succeed();
        } finally {
            TestPlayers.remove(player);
        }
    }

    @GameTest(template = "empty", timeoutTicks = 80)
    public static void darknessDrainsAndThrustCostsEnergy(GameTestHelper helper) {
        ServerLevel space = TestPlayers.deepSpace(helper);
        BlockPos center = new BlockPos(21_000, 80, 0);
        space.getChunk(center);
        BlockPos.betweenClosed(center.offset(-2, -2, -2), center.offset(2, 3, 2))
                .forEach(pos -> space.setBlock(pos, Blocks.STONE.defaultBlockState(), 3));
        BlockPos.betweenClosed(center.offset(-1, -1, -1), center.offset(1, 2, 1))
                .forEach(pos -> space.setBlock(pos, Blocks.AIR.defaultBlockState(), 3));

        // Let the light engine catch up with the sealed box before measuring.
        helper.runAfterDelay(20, () -> {
            ServerPlayer player = TestPlayers.survivalInDeepSpace(helper, center.getX() + 0.5, center.getY(), center.getZ() + 0.5);
            try {
                setEnergy(player, 50f);
                tick(player, TICKS);
                float idle = 50f - energy(player);
                helper.assertTrue(Math.abs(idle - 0.2f) < 0.02f, "expected 0.2 idle drain in darkness, got " + idle);

                setEnergy(player, 50f);
                player.setData(ModAttachments.FLIGHT_INPUT, FlightInput.of(true, false, false, false, false, false));
                tick(player, TICKS);
                float thrusting = 50f - energy(player);
                helper.assertTrue(Math.abs(thrusting - 2.2f) < 0.05f, "expected 2.2 drain while thrusting, got " + thrusting);
                helper.succeed();
            } finally {
                TestPlayers.remove(player);
            }
        });
    }

    @GameTest(template = "empty")
    public static void zeroEnergyEntersLowPower(GameTestHelper helper) {
        ServerPlayer player = TestPlayers.survivalInDeepSpace(helper, 22_000.5, 80, 0.5);
        try {
            setEnergy(player, 0.05f);
            player.setData(ModAttachments.FLIGHT_INPUT, FlightInput.of(true, false, false, false, false, false));
            tick(player, 2);
            EnergyData data = player.getData(ModAttachments.ENERGY);
            helper.assertTrue(data.lowPower(), "thrusting to zero did not enter low power: " + data);
            helper.succeed();
        } finally {
            TestPlayers.remove(player);
        }
    }

    @GameTest(template = "empty")
    public static void legalFlightIsNeverCorrected(GameTestHelper helper) {
        ServerPlayer player = TestPlayers.survivalInDeepSpace(helper, 23_000.5, 80, 0.5);
        try {
            double step = FlightStats.of(player).maxSpeed();
            double x = player.getX();
            for (int i = 0; i < 100; i++) {
                x += step;
                player.setPos(x, player.getY(), player.getZ());
                player.doTick();
                helper.assertTrue(Math.abs(player.getX() - x) < 1.0E-6, "legal movement was corrected at tick " + i);
            }
            helper.succeed();
        } finally {
            TestPlayers.remove(player);
        }
    }

    @GameTest(template = "empty")
    public static void speedHackIsPulledBack(GameTestHelper helper) {
        ServerPlayer player = TestPlayers.survivalInDeepSpace(helper, 24_000.5, 80, 0.5);
        try {
            // Twice the top speed for long enough to exceed the 1.5x tolerance over the 1 s window.
            double step = FlightStats.of(player).maxSpeed() * 2.0;
            double startX = player.getX();
            double x = startX;
            int correctedAt = -1;
            for (int i = 0; i < 60 && correctedAt < 0; i++) {
                x += step;
                player.setPos(x, player.getY(), player.getZ());
                player.doTick();
                if (Math.abs(player.getX() - x) > 1.0E-6) {
                    correctedAt = i;
                }
            }
            helper.assertTrue(correctedAt >= 0, "speed hack was never corrected");
            helper.assertTrue(player.getX() < x - 10, "player was not pulled back: x=" + player.getX() + " hack x=" + x);
            helper.assertTrue(player.getDeltaMovement().lengthSqr() == 0.0, "velocity not zeroed after correction");
            helper.succeed();
        } finally {
            TestPlayers.remove(player);
        }
    }

    private static void tick(ServerPlayer player, int ticks) {
        for (int i = 0; i < ticks; i++) {
            player.doTick();
        }
    }

    private static void setEnergy(ServerPlayer player, float energy) {
        EnergyData old = player.getData(ModAttachments.ENERGY);
        player.setData(ModAttachments.ENERGY, new EnergyData(energy, old.capacity(), false));
    }

    private static float energy(ServerPlayer player) {
        return player.getData(ModAttachments.ENERGY).energy();
    }
}
