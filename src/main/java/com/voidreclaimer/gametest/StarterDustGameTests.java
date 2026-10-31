package com.voidreclaimer.gametest;

import com.voidreclaimer.Config;
import com.voidreclaimer.VoidReclaimer;
import com.voidreclaimer.dust.DustData;
import com.voidreclaimer.dust.DustKinds;
import com.voidreclaimer.registry.ModAttachments;
import com.voidreclaimer.replicator.Milestones;
import com.voidreclaimer.replicator.Replicator;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(VoidReclaimer.MOD_ID)
@PrefixGameTestTemplate(false)
public final class StarterDustGameTests {
    private StarterDustGameTests() {}

    @GameTest(template = "empty")
    public static void aNewBodyStartsWithTheConfiguredDustExactlyOnce(GameTestHelper helper) {
        ServerPlayer player = TestPlayers.survivalInDeepSpace(helper, 97_000.5, 80, 0.5);
        try {
            TestPlayers.notYetAwake(player);
            player.setData(ModAttachments.DUST, DustData.EMPTY);
            Replicator.checkMilestones(player); // reaches Awakening
            DustData dust = player.getData(ModAttachments.DUST);
            helper.assertTrue(dust.get(DustKinds.METALLIC) == Config.STARTER_METALLIC.get().floatValue(), "metallic " + dust.get(DustKinds.METALLIC));
            helper.assertTrue(dust.get(DustKinds.SILICATE) == Config.STARTER_SILICATE.get().floatValue(), "silicate " + dust.get(DustKinds.SILICATE));
            helper.assertTrue(dust.get(DustKinds.CARBON) == Config.STARTER_CARBON.get().floatValue(), "carbon " + dust.get(DustKinds.CARBON));
            helper.assertTrue(dust.get(DustKinds.ICE) == 0f, "ice " + dust.get(DustKinds.ICE));
            Replicator.checkMilestones(player);
            helper.assertTrue(player.getData(ModAttachments.DUST).equals(dust), "granted a second time");
            var unlocked = Replicator.data(player).unlocked();
            helper.assertFalse(unlocked.contains(Milestones.FIRST_DUST) || unlocked.contains(Milestones.DIVERSIFIED),
                    "the starter dust unlocked a dust milestone: " + unlocked);
            helper.succeed();
        } finally {
            TestPlayers.remove(player);
        }
    }

    @GameTest(template = "empty")
    public static void theStarterDustCoversTheFirstRecipesAndCanBeSwitchedOff(GameTestHelper helper) {
        ServerPlayer player = TestPlayers.survivalInDeepSpace(helper, 98_000.5, 80, 0.5);
        boolean was = Config.STARTER_DUST_ENABLED.get();
        try {
            // Dust Sensor (M10 S15 C5) + Thruster Mk I (M10 S5) + Scaffold (S8) + Rotor Arm (M5 S2) = M25 S30 C5.
            helper.assertTrue(Config.STARTER_METALLIC.get() >= 25.0 && Config.STARTER_SILICATE.get() >= 30.0
                    && Config.STARTER_CARBON.get() >= 5.0, "the defaults no longer pay for the first four Awakening recipes");
            Config.STARTER_DUST_ENABLED.set(false);
            TestPlayers.notYetAwake(player);
            player.setData(ModAttachments.DUST, DustData.EMPTY);
            Replicator.checkMilestones(player);
            helper.assertTrue(player.getData(ModAttachments.DUST).total() == 0f, "dust granted while switched off");
            helper.succeed();
        } finally {
            Config.STARTER_DUST_ENABLED.set(was);
            TestPlayers.remove(player);
        }
    }

    /** What an unrelated test's Dyson tier change does to every online player: it must not give anyone dust. */
    @GameTest(template = "empty")
    public static void aTestPlayersDustIsNotChangedByAnotherTestsMilestoneCheck(GameTestHelper helper) {
        ServerPlayer player = TestPlayers.survivalInDeepSpace(helper, 98_500.5, 80, 0.5);
        try {
            player.setData(ModAttachments.DUST, DustData.EMPTY);
            Replicator.checkMilestones(player);
            helper.assertTrue(player.getData(ModAttachments.DUST).total() == 0f, "a milestone check gave a test player dust");
            helper.succeed();
        } finally {
            TestPlayers.remove(player);
        }
    }
}
