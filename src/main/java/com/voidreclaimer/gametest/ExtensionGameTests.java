package com.voidreclaimer.gametest;

import com.voidreclaimer.VoidReclaimer;
import com.voidreclaimer.api.VoidReclaimerRegistries;
import com.voidreclaimer.api.milestone.Milestone;
import com.voidreclaimer.dust.DustData;
import com.voidreclaimer.dust.DustKinds;
import com.voidreclaimer.registry.ModAttachments;
import com.voidreclaimer.replicator.Replicator;
import com.voidreclaimer.replicator.ReplicatorLogic;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * Extension points, exercised with the {@link GameTestPack} (namespace {@value GameTestPack#NAMESPACE}) the way a
 * datapack or addon uses them.
 */
@GameTestHolder(VoidReclaimer.MOD_ID)
@PrefixGameTestTemplate(false)
public final class ExtensionGameTests {
    private static final ResourceLocation PLASMA = ResourceLocation.fromNamespaceAndPath(GameTestPack.NAMESPACE, "plasma");
    private static final ResourceKey<Milestone> PLASMA_HOARDER = ResourceKey.create(VoidReclaimerRegistries.MILESTONE,
            ResourceLocation.fromNamespaceAndPath(GameTestPack.NAMESPACE, "plasma_hoarder"));
    private static final ResourceLocation PLASMA_GLASS = ResourceLocation.fromNamespaceAndPath(GameTestPack.NAMESPACE, "replicator/plasma_glass");

    private ExtensionGameTests() {}

    /** A datapack dust kind is registered after the built-in ones, keeps its name, and stays out of clouds at weight 0. */
    @GameTest(template = "empty")
    public static void datapackDustKindIsRegistered(GameTestHelper helper) {
        var kinds = DustKinds.all(helper.getLevel().registryAccess());
        helper.assertTrue(kinds.size() == 5, "expected 4 built-in kinds + plasma, got " + kinds);
        helper.assertTrue(kinds.get(0).id().equals(DustKinds.METALLIC), "built-in order changed: " + kinds);
        helper.assertTrue(kinds.get(4).id().equals(PLASMA), "plasma not last: " + kinds);
        helper.assertTrue(DustKinds.name(helper.getLevel().registryAccess(), PLASMA).getString().equals("Plasma"), "inline name ignored");
        helper.assertTrue(DustKinds.pick(kinds, 0.999).equals(DustKinds.CARBON), "a weight-0 kind took part in cloud generation");
        helper.succeed();
    }

    /** A datapack milestone with a built-in criterion type unlocks a datapack recipe that costs the datapack kind. */
    @GameTest(template = "empty")
    public static void datapackMilestoneUnlocksDatapackRecipe(GameTestHelper helper) {
        ServerPlayer player = TestPlayers.survivalInDeepSpace(helper, 60_000.5, 80, 0.5);
        try {
            Replicator.checkMilestones(player);
            helper.assertFalse(Replicator.data(player).isUnlocked(PLASMA_HOARDER), "unlocked without plasma");
            helper.assertTrue(Replicator.queue(player, PLASMA_GLASS) == ReplicatorLogic.QueueResult.LOCKED, "recipe not locked");

            player.setData(ModAttachments.DUST, DustData.EMPTY.add(PLASMA, 5f, 200f));
            Replicator.checkMilestones(player);
            helper.assertTrue(Replicator.data(player).isUnlocked(PLASMA_HOARDER), "milestone not reached at 5 plasma");
            helper.assertTrue(Replicator.queue(player, PLASMA_GLASS) == ReplicatorLogic.QueueResult.QUEUED, "recipe not queued");
            helper.assertTrue(Math.abs(player.getData(ModAttachments.DUST).get(PLASMA) - 3f) < 1e-4, "plasma not paid");
            helper.succeed();
        } finally {
            TestPlayers.remove(player);
        }
    }
}
