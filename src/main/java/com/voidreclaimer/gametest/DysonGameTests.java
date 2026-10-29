package com.voidreclaimer.gametest;

import com.voidreclaimer.Config;
import com.voidreclaimer.VoidReclaimer;
import com.voidreclaimer.dyson.Dyson;
import com.voidreclaimer.dyson.DysonSphereData;
import com.voidreclaimer.replicator.Milestones;
import com.voidreclaimer.replicator.Replicator;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/** Dyson Sphere counters, skybox tiers and their AI log milestones. The counters are world-wide, so each test restores them. */
@GameTestHolder(VoidReclaimer.MOD_ID)
@PrefixGameTestTemplate(false)
public final class DysonGameTests {
    private DysonGameTests() {}

    @GameTest(template = "empty")
    public static void launchesRaiseTiersAndUnlockLogEntries(GameTestHelper helper) {
        MinecraftServer server = helper.getLevel().getServer();
        DysonSphereData saved = DysonSphereData.of(server);
        ServerPlayer player = TestPlayers.survivalInDeepSpace(helper, 74_000.5, 80, 0.5);
        try {
            Dyson.set(server, new DysonSphereData(0, 0, 0, 0));
            Replicator.checkMilestones(player);
            helper.assertTrue(DysonSphereData.of(server).tier() == 0, "fresh sphere is not tier 0");
            helper.assertFalse(Replicator.data(player).isUnlocked(Milestones.DYSON_TIER_1), "tier 1 log without a launch");

            Dyson.launchSatellites(server, 1);
            helper.assertTrue(DysonSphereData.of(server).tier() == 1, "one satellite is not tier 1");
            helper.assertTrue(Replicator.data(player).isUnlocked(Milestones.DYSON_TIER_1), "tier 1 log not unlocked by the launch");
            helper.assertFalse(Replicator.data(player).isUnlocked(Milestones.DYSON_TIER_2), "tier 2 log too early");

            Dyson.launchSatellites(server, 50);
            helper.assertTrue(DysonSphereData.of(server).tier() == 2, "51 satellites are not tier 2");
            helper.assertTrue(Replicator.data(player).isUnlocked(Milestones.DYSON_TIER_2), "tier 2 log not unlocked");

            Dyson.launchTrusses(server, Config.DYSON_LATTICE_START.get());
            helper.assertTrue(DysonSphereData.of(server).tier() == 3, "first trusses are not tier 3");
            helper.assertTrue(Replicator.data(player).isUnlocked(Milestones.DYSON_TIER_3), "tier 3 log not unlocked");
            helper.assertFalse(Replicator.data(player).isUnlocked(Milestones.DYSON_TIER_4), "tier 4 log too early");

            Dyson.launchTrusses(server, Config.DYSON_SHELL_TOTAL.get());
            helper.assertTrue(DysonSphereData.of(server).tier() == 4, "the full truss count is not tier 4");
            helper.assertTrue(Replicator.data(player).isUnlocked(Milestones.DYSON_TIER_4), "tier 4 log not unlocked");
            helper.succeed();
        } finally {
            Dyson.set(server, saved);
            TestPlayers.remove(player);
        }
    }

    @GameTest(template = "empty")
    public static void everyDimensionSeesTheSameSphere(GameTestHelper helper) {
        MinecraftServer server = helper.getLevel().getServer();
        DysonSphereData saved = DysonSphereData.of(server);
        try {
            Dyson.set(server, new DysonSphereData(123, 4, 0, 0));
            DysonSphereData fromOverworld = DysonSphereData.of(server.overworld());
            helper.assertTrue(fromOverworld.satellites() == 123 && fromOverworld.trusses() == 4, "overworld read " + fromOverworld);
            helper.assertTrue(fromOverworld.shellTotal() == Config.DYSON_SHELL_TOTAL.get(), "thresholds not refreshed from the config");
            helper.succeed();
        } finally {
            Dyson.set(server, saved);
        }
    }

    @GameTest(template = "empty")
    public static void unchangedTierDoesNotRepeatLogEntries(GameTestHelper helper) {
        MinecraftServer server = helper.getLevel().getServer();
        DysonSphereData saved = DysonSphereData.of(server);
        ServerPlayer player = TestPlayers.survivalInDeepSpace(helper, 75_000.5, 80, 0.5);
        try {
            // The persistent GameTest world may hold leftover launches from an earlier (failed) run: start from tier 0.
            Dyson.set(server, DysonSphereData.createDefault());
            Dyson.set(server, new DysonSphereData(5, 0, 0, 0));
            helper.assertTrue(Replicator.data(player).isUnlocked(Milestones.DYSON_TIER_1), "tier 1 log missing");
            // Lock it again by hand; another launch within the same tier must not re-grant it.
            Replicator.store(player, Replicator.data(player).withoutUnlocked(Milestones.DYSON_TIER_1));
            Dyson.launchSatellites(server, 3);
            helper.assertFalse(Replicator.data(player).isUnlocked(Milestones.DYSON_TIER_1), "a same-tier launch re-ran the milestone check");
            helper.succeed();
        } finally {
            Dyson.set(server, saved);
            TestPlayers.remove(player);
        }
    }
}
