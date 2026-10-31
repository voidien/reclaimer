package com.voidreclaimer.gametest;

import com.voidreclaimer.VoidReclaimer;
import com.voidreclaimer.earth.RuinedEarth;
import com.voidreclaimer.replicator.Milestones;
import com.voidreclaimer.replicator.Replicator;
import com.voidreclaimer.replicator.ReplicatorLogic;
import com.voidreclaimer.registry.ModAttachments;
import com.voidreclaimer.dust.DustData;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/** The Earthfall milestone: setting foot on ruined Earth, and the Aeronautics parts it unlocks in the Replicator. */
@GameTestHolder(VoidReclaimer.MOD_ID)
@PrefixGameTestTemplate(false)
public final class EarthMilestoneGameTests {
    private static final net.minecraft.resources.ResourceLocation WOODEN_PROPELLER = VoidReclaimer.id("replicator/aeronautics_wooden_propeller");

    private EarthMilestoneGameTests() {}

    /** A player in deep space has not reached Earthfall; stepping into ruined Earth reaches it, and it stays reached. */
    @GameTest(template = "empty")
    public static void steppingOntoRuinedEarthReachesEarthfall(GameTestHelper helper) {
        ServerPlayer player = TestPlayers.survivalInDeepSpace(helper, 64_000.5, 80, 0.5);
        try {
            Replicator.noteDimension(player);
            Replicator.checkMilestones(player);
            helper.assertFalse(Replicator.data(player).isUnlocked(Milestones.EARTHFALL), "Earthfall reached in deep space");
            ServerLevel earth = helper.getLevel().getServer().getLevel(RuinedEarth.DIMENSION);
            helper.assertTrue(earth != null, "ruined_earth is not loaded");
            player.teleportTo(earth, 64_000.5, 200, 0.5, 0f, 0f);
            // A cross-dimension teleport can hand the server a fresh ServerPlayer object for the same player.
            ServerPlayer now = helper.getLevel().getServer().getPlayerList().getPlayer(player.getUUID());
            helper.assertTrue(now != null && now.level().dimension() == RuinedEarth.DIMENSION, "the player did not arrive on Earth");
            Replicator.noteDimension(now);
            Replicator.checkMilestones(now);
            helper.assertTrue(Replicator.data(now).isUnlocked(Milestones.EARTHFALL), "Earthfall not reached on Earth: " + Replicator.data(now));
            player.teleportTo(TestPlayers.deepSpace(helper), 64_000.5, 80, 0.5, 0f, 0f);
            now = helper.getLevel().getServer().getPlayerList().getPlayer(player.getUUID());
            Replicator.noteDimension(now);
            Replicator.checkMilestones(now);
            helper.assertTrue(Replicator.data(now).isUnlocked(Milestones.EARTHFALL), "Earthfall forgotten after leaving");
            helper.succeed();
        } finally {
            TestPlayers.remove(player);
        }
    }

    /** The Aeronautics parts wait for Earthfall: a propeller cannot be queued before, and can after. */
    @GameTest(template = "empty")
    public static void aeronauticsPartsWaitForEarthfall(GameTestHelper helper) {
        ServerPlayer player = TestPlayers.survivalInDeepSpace(helper, 65_000.5, 80, 0.5);
        try {
            DustData dust = DustData.EMPTY;
            for (var kind : java.util.List.of(com.voidreclaimer.dust.DustKinds.METALLIC, com.voidreclaimer.dust.DustKinds.SILICATE,
                    com.voidreclaimer.dust.DustKinds.ICE, com.voidreclaimer.dust.DustKinds.CARBON)) {
                dust = dust.add(kind, 200f, 200f);
            }
            player.setData(ModAttachments.DUST, dust);
            helper.assertTrue(Replicator.queue(player, WOODEN_PROPELLER) == ReplicatorLogic.QueueResult.LOCKED,
                    "a propeller could be queued before Earthfall");
            Replicator.store(player, Replicator.data(player).withUnlocked(Milestones.EARTHFALL));
            helper.assertTrue(Replicator.queue(player, WOODEN_PROPELLER) == ReplicatorLogic.QueueResult.QUEUED,
                    "a propeller could not be queued after Earthfall");
            helper.succeed();
        } finally {
            TestPlayers.remove(player);
        }
    }
}
