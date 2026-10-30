package com.voidreclaimer.gametest;

import com.voidreclaimer.Config;
import com.voidreclaimer.VoidReclaimer;
import com.voidreclaimer.energy.EnergyData;
import com.voidreclaimer.flight.FlightPhysics;
import com.voidreclaimer.flight.FlightStats;
import com.voidreclaimer.flight.ServerFlightHandler;
import com.voidreclaimer.flight.ThrusterFlight;
import com.voidreclaimer.registry.ModAttachments;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.GameType;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.living.LivingFallEvent;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/** Hover flight on ruined Earth: which bodies fly, what hovering costs, and when falling hurts. */
@GameTestHolder(VoidReclaimer.MOD_ID)
@PrefixGameTestTemplate(false)
public final class HoverGameTests {
    private static final int TICKS = 40;

    private HoverGameTests() {}

    private static void tick(ServerPlayer player, int ticks) {
        for (int i = 0; i < ticks; i++) {
            player.doTick();
        }
    }

    private static void setEnergy(ServerPlayer player, float energy, boolean lowPower) {
        EnergyData old = player.getData(ModAttachments.ENERGY);
        player.setData(ModAttachments.ENERGY, new EnergyData(energy, old.capacity(), lowPower));
    }

    private static float energy(ServerPlayer player) {
        return player.getData(ModAttachments.ENERGY).energy();
    }

    private static void setDampener(ServerPlayer player, boolean on) {
        player.setData(ModAttachments.FLIGHT_SETTINGS, player.getData(ModAttachments.FLIGHT_SETTINGS).withDampener(on));
    }

    @GameTest(template = "empty", timeoutTicks = 100)
    public static void bodiesFlyOnThrustersWithWeightOnRuinedEarth(GameTestHelper helper) {
        ServerPlayer player = TestPlayers.survivalOnRuinedEarth(helper, 8.5, 120, 8.5);
        try {
            helper.assertTrue(FlightStats.of(player).thrust() > 0.0, "the starter body has no thrust");
            helper.assertTrue(ThrusterFlight.isActive(player), "a body with thrust does not fly on Earth");
            double expected = player.getAttributeValue(Attributes.GRAVITY) * Config.HOVER_WEIGHT_FACTOR.get();
            helper.assertTrue(expected > 0.0 && Math.abs(ThrusterFlight.weight(player) - expected) < 1.0E-9,
                    "weight is not gravity times the factor: " + ThrusterFlight.weight(player));
            // Creative keeps vanilla flight, as in deep space.
            player.gameMode.changeGameModeForPlayer(GameType.CREATIVE);
            helper.assertFalse(ThrusterFlight.isActive(player), "a creative player was put on thrusters");
            helper.succeed();
        } finally {
            TestPlayers.remove(player);
        }
    }

    @GameTest(template = "empty", timeoutTicks = 200)
    public static void hoveringCostsTheShareOfThrustSpentOnWeight(GameTestHelper helper) {
        ServerPlayer player = TestPlayers.survivalOnRuinedEarth(helper, 8.5, 120, 40.5);
        try {
            // The server simulates vanilla gravity on this fake player (a real one is moved by its client), and the
            // fall would cost braking energy: keep it still so only the hover cost is measured.
            player.setNoGravity(true);
            tick(player, 20);
            // The same minute with the dampener off costs no hover energy, so the difference is the hover cost.
            setDampener(player, false);
            setEnergy(player, 50f, false);
            tick(player, TICKS);
            float without = 50f - energy(player);

            setDampener(player, true);
            setEnergy(player, 50f, false);
            tick(player, TICKS);
            float with = 50f - energy(player);

            double share = FlightPhysics.hoverShare(FlightStats.of(player), ThrusterFlight.weight(player), true, true);
            helper.assertTrue(share > 0.0 && share <= 1.0, "no hover share: " + share);
            double expected = Config.THRUST_ENERGY_COST.get() * share * TICKS / FlightStats.TICKS_PER_SECOND
                    * com.voidreclaimer.body.Bodies.stats(player).energyUse();
            helper.assertTrue(Math.abs((with - without) - expected) < 0.05 * expected + 0.01,
                    "hover cost " + (with - without) + " expected " + expected + " (share " + share + ", with " + with + ", without " + without
                            + ", weight " + ThrusterFlight.weight(player) + ", stats " + FlightStats.of(player) + ")");
            helper.assertTrue(ServerFlightHandler.hoverShare(player) == share, "the server and the physics disagree on the share");
            helper.succeed();
        } finally {
            TestPlayers.remove(player);
        }
    }

    @GameTest(template = "empty", timeoutTicks = 100)
    public static void controlledLandingsAreSafeAndFreeFallsHurt(GameTestHelper helper) {
        ServerPlayer player = TestPlayers.survivalOnRuinedEarth(helper, 8.5, 120, 72.5);
        try {
            setEnergy(player, 50f, false);
            setDampener(player, true);
            helper.assertTrue(fall(player).isCanceled(), "a controlled landing (power on, dampener on) hurt");

            setDampener(player, false);
            helper.assertFalse(fall(player).isCanceled(), "a free fall with the dampener off did not hurt");

            setDampener(player, true);
            setEnergy(player, 0f, true);
            helper.assertFalse(fall(player).isCanceled(), "a fall in low power mode did not hurt");
            helper.succeed();
        } finally {
            TestPlayers.remove(player);
        }
    }

    private static LivingFallEvent fall(ServerPlayer player) {
        return NeoForge.EVENT_BUS.post(new LivingFallEvent(player, 12f, 1f));
    }
}
