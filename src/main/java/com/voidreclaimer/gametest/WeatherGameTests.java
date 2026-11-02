package com.voidreclaimer.gametest;

import com.voidreclaimer.VoidReclaimer;
import com.voidreclaimer.energy.Energy;
import com.voidreclaimer.energy.EnergyData;
import com.voidreclaimer.registry.ModAttachments;
import com.voidreclaimer.weather.SpaceWeatherTicker;
import com.voidreclaimer.weather.WeatherState;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/** The weather system, with the test pack's weather. Each test has its batch: a level has one weather, and it reaches everyone in it. */
@GameTestHolder(VoidReclaimer.MOD_ID)
@PrefixGameTestTemplate(false)
public final class WeatherGameTests {
    private static final ResourceLocation STORM = ResourceLocation.fromNamespaceAndPath("void_reclaimer_test", "test_storm");
    private static final ResourceLocation COVER = ResourceLocation.fromNamespaceAndPath("void_reclaimer_test", "test_cover");

    private WeatherGameTests() {}

    private static void charged(ServerPlayer player) {
        EnergyData energy = player.getData(ModAttachments.ENERGY);
        Energy.update(player, new EnergyData(energy.capacity(), energy.capacity(), false), true);
    }

    private static float energy(ServerPlayer player) {
        return player.getData(ModAttachments.ENERGY).energy();
    }

    private static void seconds(ServerLevel level, int seconds) {
        for (int second = 0; second < seconds; second++) {
            SpaceWeatherTicker.second(level);
        }
    }

    /** A warning, then the storm, then the calm, with an event at each step and the effect only while it lasts. */
    @GameTest(template = "empty", timeoutTicks = 200, batch = "weather_cycle")
    public static void aStormWarnsThenStrikesThenEnds(GameTestHelper helper) {
        BlockPos origin = new BlockPos(103_800, 300, 0);
        NaniteGameTests.inLoadedArea(helper, origin, 1, () -> {
            ServerLevel space = TestPlayers.deepSpace(helper);
            ServerPlayer player = TestPlayers.survivalInDeepSpace(helper, origin.getX() + 0.5, origin.getY(), origin.getZ() + 0.5);
            charged(player);
            float full = energy(player);
            TestEventListeners.WEATHER_LOG.clear();
            helper.assertTrue(SpaceWeatherTicker.start(space, STORM), "no such weather");
            helper.assertTrue(SpaceWeatherTicker.of(space).warning(), "no warning");
            seconds(space, 1);
            helper.assertTrue(energy(player) == full, "the warning did the storm's work");
            seconds(space, 1);
            helper.assertTrue(SpaceWeatherTicker.of(space).active(), "not started: " + SpaceWeatherTicker.of(space));
            helper.assertTrue(energy(player) < full, "the storm did nothing");
            seconds(space, 3);
            WeatherState after = SpaceWeatherTicker.of(space);
            helper.assertTrue(after.weather().isEmpty(), "still here: " + after);
            helper.assertTrue(after.nextIn() > 0, "nothing planned");
            helper.assertTrue(TestEventListeners.WEATHER_LOG.equals(java.util.List.of("warning " + STORM, "started " + STORM, "ended " + STORM)),
                    "the events were " + TestEventListeners.WEATHER_LOG);
            space.setData(ModAttachments.WEATHER, WeatherState.NONE);
            TestPlayers.remove(player);
            helper.succeed();
        });
    }

    /** A storm started over another ends that one first, and a kind with no warning still says it has started. */
    @GameTest(template = "empty", timeoutTicks = 200, batch = "weather_restart")
    public static void aStormStartedOverAnotherEndsIt(GameTestHelper helper) {
        BlockPos origin = new BlockPos(107_000, 300, 0);
        NaniteGameTests.inLoadedArea(helper, origin, 1, () -> {
            ServerLevel space = TestPlayers.deepSpace(helper);
            ServerPlayer player = TestPlayers.survivalInDeepSpace(helper, origin.getX() + 0.5, origin.getY(), origin.getZ() + 0.5);
            TestEventListeners.WEATHER_LOG.clear();
            SpaceWeatherTicker.start(space, STORM);
            seconds(space, 3);
            SpaceWeatherTicker.start(space, COVER);
            helper.assertTrue(TestEventListeners.WEATHER_LOG.equals(java.util.List.of("warning " + STORM, "started " + STORM, "ended " + STORM, "warning " + COVER, "started " + COVER)),
                    "the events were " + TestEventListeners.WEATHER_LOG);
            SpaceWeatherTicker.stop(space);
            space.setData(ModAttachments.WEATHER, WeatherState.NONE);
            TestPlayers.remove(player);
            helper.succeed();
        });
    }

    /** A listener can spare a body, as a shielding part will. */
    @GameTest(template = "empty", timeoutTicks = 200, batch = "weather_spare")
    public static void anAddonCanSpareABody(GameTestHelper helper) {
        BlockPos origin = new BlockPos(104_200, 300, 0);
        NaniteGameTests.inLoadedArea(helper, origin, 1, () -> {
            ServerLevel space = TestPlayers.deepSpace(helper);
            ServerPlayer player = TestPlayers.survivalInDeepSpace(helper, origin.getX() + 0.5, origin.getY(), origin.getZ() + 0.5);
            player.addTag(TestEventListeners.NO_WEATHER);
            charged(player);
            float full = energy(player);
            SpaceWeatherTicker.start(space, STORM);
            seconds(space, 4);
            helper.assertTrue(energy(player) == full, "a spared body lost energy");
            SpaceWeatherTicker.stop(space);
            space.setData(ModAttachments.WEATHER, WeatherState.NONE);
            TestPlayers.remove(player);
            helper.succeed();
        });
    }

    /** Rock between a body and the sun is cover, and the open is not. */
    @GameTest(template = "empty", timeoutTicks = 200, batch = "weather_cover")
    public static void rockBetweenABodyAndTheSunIsCover(GameTestHelper helper) {
        BlockPos origin = new BlockPos(104_600, 300, 0);
        NaniteGameTests.inLoadedArea(helper, origin, 3, () -> {
            ServerLevel space = TestPlayers.deepSpace(helper);
            ServerPlayer open = TestPlayers.survivalInDeepSpace(helper, origin.getX() + 0.5, origin.getY(), origin.getZ() + 40.5);
            ServerPlayer covered = TestPlayers.survivalInDeepSpace(helper, origin.getX() + 0.5, origin.getY(), origin.getZ() + 0.5);
            // Straight overhead, where the sky draws deep space's sun, and not worked out from the code under test.
            BlockPos shield = BlockPos.containing(covered.getEyePosition()).above(6);
            BlockPos.betweenClosed(shield.offset(-5, -1, -5), shield.offset(5, 1, 5)).forEach(p -> space.setBlock(p, Blocks.STONE.defaultBlockState(), 3));
            charged(open);
            charged(covered);
            float openFull = energy(open);
            float coveredFull = energy(covered);
            SpaceWeatherTicker.start(space, COVER);
            seconds(space, 2);
            helper.assertTrue(energy(open) < openFull, "the sun did not reach a body in the open");
            helper.assertTrue(energy(covered) == coveredFull, "the sun reached a body behind rock");
            BlockPos.betweenClosed(shield.offset(-5, -1, -5), shield.offset(5, 1, 5)).forEach(p -> space.setBlock(p, Blocks.AIR.defaultBlockState(), 3));
            SpaceWeatherTicker.stop(space);
            space.setData(ModAttachments.WEATHER, WeatherState.NONE);
            TestPlayers.remove(open);
            TestPlayers.remove(covered);
            helper.succeed();
        });
    }
}
