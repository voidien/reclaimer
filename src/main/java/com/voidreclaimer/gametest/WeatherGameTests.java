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

    private static void flyUntilItEnds(com.voidreclaimer.weather.DebrisRock rock) {
        for (int tick = 0; tick < 60 && !rock.isRemoved(); tick++) {
            rock.tickCount++;
            rock.tick();
        }
    }

    /** A rock chips a body by its hit boxes, well away from its vanilla box. */
    @GameTest(template = "empty", timeoutTicks = 200)
    public static void aDebrisRockChipsABody(GameTestHelper helper) {
        BlockPos origin = new BlockPos(105_400, 300, 0);
        NaniteGameTests.inEntityTickingArea(helper, origin, 2, () -> {
            ServerLevel space = TestPlayers.deepSpace(helper);
            ServerPlayer player = TestPlayers.survivalInDeepSpace(helper, origin.getX() + 0.5, origin.getY(), origin.getZ() + 0.5);
            com.voidreclaimer.body.Bodies.store(player, com.voidreclaimer.body.Bodies.preset(com.voidreclaimer.body.BuiltinBodies.TITAN));
            player.setHealth(player.getMaxHealth());
            player.setYRot(0f);
            player.setXRot(0f);
            TestPlayers.endSpawnProtection(player);
            var rock = com.voidreclaimer.weather.DebrisRock.launch(space, new net.minecraft.world.phys.Vec3(origin.getX() + 0.5, origin.getY() + 13.0, origin.getZ() + 44.0),
                    new net.minecraft.world.phys.Vec3(0.0, 0.0, -1.0));
            flyUntilItEnds(rock);
            helper.assertTrue(rock.isRemoved(), "the rock flew on");
            helper.assertTrue(rock.getZ() > origin.getZ() + 15.0, "it reached " + rock.getZ() + ", past the saucer");
            float lost = player.getMaxHealth() - player.getHealth();
            helper.assertTrue(lost > 0f && lost < player.getMaxHealth() * 0.1f, "it took " + lost + " of " + player.getMaxHealth());
            TestPlayers.remove(player);
            helper.succeed();
        });
    }

    /** A rock breaks a weak block and stops at a strong one, and leaves no block of its own. */
    @GameTest(template = "empty", timeoutTicks = 200)
    public static void aDebrisRockBreaksGlassAndStopsAtStone(GameTestHelper helper) {
        BlockPos origin = new BlockPos(105_800, 300, 0);
        NaniteGameTests.inEntityTickingArea(helper, origin, 2, () -> {
            ServerLevel space = TestPlayers.deepSpace(helper);
            BlockPos glass = origin.offset(0, 0, 4);
            BlockPos stone = origin.offset(0, 0, 8);
            space.setBlock(glass, Blocks.GLASS.defaultBlockState(), 3);
            space.setBlock(stone, Blocks.STONE.defaultBlockState(), 3);
            var from = net.minecraft.world.phys.Vec3.atCenterOf(origin);
            var speed = new net.minecraft.world.phys.Vec3(0.0, 0.0, 1.0);
            flyUntilItEnds(com.voidreclaimer.weather.DebrisRock.launch(space, from, speed));
            helper.assertTrue(space.getBlockState(glass).isAir(), "the rock left the glass");
            flyUntilItEnds(com.voidreclaimer.weather.DebrisRock.launch(space, from, speed));
            helper.assertTrue(space.getBlockState(stone).is(Blocks.STONE), "the rock broke stone");
            space.setBlock(stone, Blocks.AIR.defaultBlockState(), 3);
            space.getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class, new net.minecraft.world.phys.AABB(origin).inflate(16)).forEach(net.minecraft.world.entity.Entity::discard);
            helper.succeed();
        });
    }

    /** However long a storm runs, the rocks around a body stay few. */
    @GameTest(template = "empty", timeoutTicks = 200)
    public static void aStormKeepsItsRocksFew(GameTestHelper helper) {
        BlockPos origin = new BlockPos(106_200, 300, 0);
        NaniteGameTests.inEntityTickingArea(helper, origin, 2, () -> {
            ServerLevel space = TestPlayers.deepSpace(helper);
            ServerPlayer player = TestPlayers.survivalInDeepSpace(helper, origin.getX() + 0.5, origin.getY(), origin.getZ() + 0.5);
            for (int rock = 0; rock < 200; rock++) {
                com.voidreclaimer.weather.DebrisRock.aimAt(player);
            }
            var rocks = space.getEntitiesOfClass(com.voidreclaimer.weather.DebrisRock.class, player.getBoundingBox().inflate(64.0));
            helper.assertTrue(rocks.size() <= 24, rocks.size() + " rocks around one body");
            helper.assertTrue(!rocks.isEmpty(), "no rocks");
            helper.assertFalse(rocks.get(0).shouldBeSaved(), "a rock would be saved");
            rocks.forEach(net.minecraft.world.entity.Entity::discard);
            TestPlayers.remove(player);
            helper.succeed();
        });
    }

    /** High over Earth rocks come at a body, and lower down they don't. */
    @GameTest(template = "empty", timeoutTicks = 200)
    public static void theDebrisBeltThrowsRocksOnlyHighOverEarth(GameTestHelper helper) {
        ServerLevel earth = helper.getLevel().getServer().getLevel(com.voidreclaimer.earth.RuinedEarth.DIMENSION);
        BlockPos spot = new BlockPos(60_000, 250, 60_000);
        for (int dx = -5; dx <= 5; dx++) {
            for (int dz = -5; dz <= 5; dz++) {
                earth.setChunkForced((spot.getX() >> 4) + dx, (spot.getZ() >> 4) + dz, true);
            }
        }
        helper.runAfterDelay(5, () -> {
            ServerPlayer player = TestPlayers.survivalInDeepSpace(helper, 106_600.5, 300, 0.5);
            player.teleportTo(earth, spot.getX() + 0.5, 100, spot.getZ() + 0.5, 0f, 0f);
            var around = player.getBoundingBox().inflate(80.0, 400.0, 80.0);
            for (int second = 0; second < 40; second++) {
                com.voidreclaimer.earth.DebrisBelt.second(player);
            }
            int low = earth.getEntitiesOfClass(com.voidreclaimer.weather.DebrisRock.class, around).size();
            helper.assertTrue(low == 0, low + " rocks below the belt");
            player.teleportTo(earth, spot.getX() + 0.5, spot.getY(), spot.getZ() + 0.5, 0f, 0f);
            for (int second = 0; second < 40; second++) {
                com.voidreclaimer.earth.DebrisBelt.second(player);
            }
            var rocks = earth.getEntitiesOfClass(com.voidreclaimer.weather.DebrisRock.class, player.getBoundingBox().inflate(80.0));
            helper.assertTrue(!rocks.isEmpty(), "no rocks in the belt");
            rocks.forEach(net.minecraft.world.entity.Entity::discard);
            for (int dx = -5; dx <= 5; dx++) {
                for (int dz = -5; dz <= 5; dz++) {
                    earth.setChunkForced((spot.getX() >> 4) + dx, (spot.getZ() >> 4) + dz, false);
                }
            }
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
