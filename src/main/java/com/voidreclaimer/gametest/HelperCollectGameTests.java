package com.voidreclaimer.gametest;

import com.voidreclaimer.Config;
import com.voidreclaimer.VoidReclaimer;
import com.voidreclaimer.dust.DustCloud;
import com.voidreclaimer.dust.DustData;
import com.voidreclaimer.dust.DustField;
import com.voidreclaimer.helper.Collecting;
import com.voidreclaimer.helper.HelperDrone;
import com.voidreclaimer.helper.HelperLinks;
import com.voidreclaimer.registry.ModAttachments;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * Helper drones collecting cloud dust. The tests run with a tiny tank and a fast collect rate (config set for the test
 * and restored) so a fill takes seconds, not minutes.
 */
@GameTestHolder(VoidReclaimer.MOD_ID)
@PrefixGameTestTemplate(false)
public final class HelperCollectGameTests {
    private HelperCollectGameTests() {}

    /** The cloud nearest a fixed far-off spot, so a test always uses the same one; each test has its own spot and cloud. */
    private static DustCloud cloud(ServerLevel space, Vec3 from) {
        DustField field = DustField.of(space);
        if (field == null) {
            throw new IllegalStateException("deep space has no dust field");
        }
        return field.nearest(from, 4000, false).orElseThrow(() -> new IllegalStateException("no dust cloud within 4000 blocks of " + from));
    }

    private static void removeDrones(ServerLevel level, BlockPos around) {
        level.getEntitiesOfClass(HelperDrone.class, new AABB(around).inflate(64)).forEach(HelperDrone::discard);
    }

    private interface Body {
        void run(ServerLevel space, DustCloud cloud, BlockPos centre);
    }

    private static void withFastCollecting(GameTestHelper helper, Vec3 from, Body body) {
        ServerLevel space = TestPlayers.deepSpace(helper);
        DustCloud cloud = cloud(space, from);
        BlockPos centre = BlockPos.containing(cloud.center());
        double tank = Config.HELPER_TANK.get(), rate = Config.HELPER_COLLECT_RATE.get();
        Config.HELPER_TANK.set(0.5);
        Config.HELPER_COLLECT_RATE.set(40.0);
        NaniteGameTests.inEntityTickingArea(helper, centre, 2, () -> body.run(space, cloud, centre));
        // The bodies restore the values when they finish; this covers a body that never gets that far.
        helper.runAfterDelay(590, () -> {
            Config.HELPER_TANK.set(tank);
            Config.HELPER_COLLECT_RATE.set(rate);
        });
    }

    @GameTest(template = "empty", timeoutTicks = 300)
    public static void aFlatCollectorGathersSlowly(GameTestHelper helper) {
        ServerLevel space = TestPlayers.deepSpace(helper);
        BlockPos centre = BlockPos.containing(cloud(space, new Vec3(91_000, 80, 20_000)).center());
        NaniteGameTests.inEntityTickingArea(helper, centre, 2, () -> {
            removeDrones(space, centre);
            HelperLinks links = new HelperLinks(Optional.empty(), Optional.empty(), Optional.empty(), Optional.of(centre));
            Vec3 spot = Vec3.atCenterOf(centre.above());
            HelperDrone full = HelperDrone.spawn(space, spot, links, null);
            HelperDrone flat = HelperDrone.spawn(space, spot, links, null);
            flat.setBattery(0f);
            helper.runAfterDelay(40, () -> {
                float gotFull = full.tank().total(), gotFlat = flat.tank().total();
                helper.assertTrue(gotFull > 0f, "the full drone gathered nothing, " + full.collectPhase());
                helper.assertTrue(gotFlat > 0f && gotFlat < gotFull * 0.5f, "flat " + gotFlat + ", full " + gotFull);
                removeDrones(space, centre);
                helper.succeed();
            });
        });
    }

    @GameTest(template = "empty", timeoutTicks = 600)
    public static void aCollectorFillsItsTankInTheCloudAndHandsTheDustToItsOwner(GameTestHelper helper) {
        withFastCollecting(helper, new Vec3(91_000, 80, 0), (space, cloud, centre) -> {
            removeDrones(space, centre);
            ServerPlayer owner = TestPlayers.survivalInDeepSpace(helper, centre.getX() + 3.5, centre.getY(), centre.getZ() + 0.5);
            owner.setData(ModAttachments.DUST, DustData.EMPTY);
            owner.addTag(TestEventListeners.NO_DUST);
            HelperDrone drone = HelperDrone.spawn(space, Vec3.atCenterOf(centre).add(0, 2, 0),
                    new HelperLinks(Optional.empty(), Optional.empty(), Optional.empty(), Optional.of(centre)), owner.getUUID());
            helper.assertTrue(drone.links().collecting(), "a drone with a collect point does not collect");
            helper.runAfterDelay(200, () -> {
                float got = owner.getData(ModAttachments.DUST).get(cloud.kind());
                helper.assertTrue(got > 0.4f, "the owner got " + got + " " + cloud.kind() + "; drone " + drone.collectPhase() + " tank " + drone.tank().total());
                helper.assertTrue(drone.tank().total() < 0.5f, "the drone kept its dust: " + drone.tank().total());
                removeDrones(space, centre);
                owner.discard();
                Config.HELPER_TANK.set(50.0);
                Config.HELPER_COLLECT_RATE.set(1.0);
                helper.succeed();
            });
        });
    }

    @GameTest(template = "empty", timeoutTicks = 600)
    public static void aFullCollectorWithItsOwnerFarAwayWaits(GameTestHelper helper) {
        // Its own cloud: two tests on one cloud would discard each other's drones when they clean up.
        withFastCollecting(helper, new Vec3(-91_000, 80, 0), (space, cloud, centre) -> {
            removeDrones(space, centre);
            ServerPlayer owner = TestPlayers.survivalInDeepSpace(helper, centre.getX() + 500.5, centre.getY(), centre.getZ() + 0.5);
            owner.setData(ModAttachments.DUST, DustData.EMPTY);
            owner.addTag(TestEventListeners.NO_DUST);
            HelperDrone drone = HelperDrone.spawn(space, Vec3.atCenterOf(centre).add(0, 2, 0),
                    new HelperLinks(Optional.empty(), Optional.empty(), Optional.empty(), Optional.of(centre)), owner.getUUID());
            helper.runAfterDelay(200, () -> {
                helper.assertTrue(drone.collectPhase() == Collecting.Phase.WAITING, "owner 500 blocks away: " + drone.collectPhase());
                helper.assertTrue(drone.tank().total() >= 0.5f, "the tank should be full: " + drone.tank().total());
                helper.assertTrue(owner.getData(ModAttachments.DUST).total() == 0f, "dust reached an owner out of reach");
                removeDrones(space, centre);
                owner.discard();
                Config.HELPER_TANK.set(50.0);
                Config.HELPER_COLLECT_RATE.set(1.0);
                helper.succeed();
            });
        });
    }
}
