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

    /** Both ship with the mod, wait for the Expansion milestone, and are read from their files. */
    @GameTest(template = "empty")
    public static void theFlareAndTheIonStormAreThereAndWaitForTheirMilestone(GameTestHelper helper) {
        ServerLevel space = TestPlayers.deepSpace(helper);
        var registry = SpaceWeatherTicker.registry(space);
        var flare = registry.get(VoidReclaimer.id("solar_flare"));
        var ion = registry.get(VoidReclaimer.id("ion_storm"));
        helper.assertTrue(flare != null && flare.exposure().equals(VoidReclaimer.id("sun_line")), "no flare, or one that ignores cover");
        helper.assertTrue(flare.milestone().filter(VoidReclaimer.id("expansion")::equals).isPresent(), "the flare comes before the Expansion milestone");
        helper.assertTrue(ion != null && ion.signalLoss() > 0f && ion.scrambles(), "no ion storm, or one that leaves signals alone");
        helper.assertTrue(ion.dimensions().contains(com.voidreclaimer.earth.RuinedEarth.DIMENSION), "no ion storms on Earth");
        helper.succeed();
    }

    /** An ion storm thins a link, and one with little signal to start with is lost. */
    @GameTest(template = "empty", timeoutTicks = 200, batch = "weather_ion")
    public static void anIonStormThinsAStrongLinkAndLosesAWeakOne(GameTestHelper helper) {
        ServerLevel space = TestPlayers.deepSpace(helper);
        var server = space.getServer();
        var dimension = space.dimension();
        var seat = new com.voidreclaimer.link.LinkState.Place(dimension, new net.minecraft.world.phys.Vec3(105_000.5, 80, 0.5), 0f, 0f);
        double range = com.voidreclaimer.link.Signal.range(server, seat);
        var near = new com.voidreclaimer.link.LinkState.Place(dimension, seat.position().add(4.0, 0.0, 0.0), 0f, 0f);
        var far = new com.voidreclaimer.link.LinkState.Place(dimension, seat.position().add(range * 0.97, 0.0, 0.0), 0f, 0f);
        double clear = com.voidreclaimer.link.Signal.read(server, seat, near).strength();
        helper.assertTrue(com.voidreclaimer.link.Signal.read(server, seat, far).reachable(), "the far end is out of range before the storm");
        space.setData(ModAttachments.WEATHER, new WeatherState(java.util.Optional.of(VoidReclaimer.id("ion_storm")), 0, 100, 1f, -1));
        try {
            var storm = com.voidreclaimer.link.Signal.read(server, seat, near);
            helper.assertTrue(storm.reachable() && storm.strength() < clear, "a storm left the signal at " + storm.strength());
            helper.assertFalse(com.voidreclaimer.link.Signal.read(server, seat, far).reachable(), "a weak link came through");
        } finally {
            space.setData(ModAttachments.WEATHER, WeatherState.NONE);
        }
        helper.succeed();
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
            var sun = com.voidreclaimer.weather.BuiltinWeather.toSun(space);
            BlockPos shield = BlockPos.containing(covered.getEyePosition().add(sun.scale(6.0)));
            BlockPos.betweenClosed(shield.offset(-3, -3, -3), shield.offset(3, 3, 3)).forEach(p -> space.setBlock(p, Blocks.STONE.defaultBlockState(), 3));
            charged(open);
            charged(covered);
            float openFull = energy(open);
            float coveredFull = energy(covered);
            SpaceWeatherTicker.start(space, COVER);
            seconds(space, 2);
            helper.assertTrue(energy(open) < openFull, "the sun did not reach a body in the open");
            helper.assertTrue(energy(covered) == coveredFull, "the sun reached a body behind rock");
            BlockPos.betweenClosed(shield.offset(-3, -3, -3), shield.offset(3, 3, 3)).forEach(p -> space.setBlock(p, Blocks.AIR.defaultBlockState(), 3));
            SpaceWeatherTicker.stop(space);
            space.setData(ModAttachments.WEATHER, WeatherState.NONE);
            TestPlayers.remove(open);
            TestPlayers.remove(covered);
            helper.succeed();
        });
    }
}
