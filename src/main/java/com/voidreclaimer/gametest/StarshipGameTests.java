package com.voidreclaimer.gametest;

import com.voidreclaimer.VoidReclaimer;
import com.voidreclaimer.dust.DustData;
import com.voidreclaimer.registry.ModAttachments;
import com.voidreclaimer.registry.ModMaterials;
import com.voidreclaimer.replicator.Milestones;
import com.voidreclaimer.replicator.Replicator;
import com.voidreclaimer.replicator.ReplicatorLogic;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/** Phase 10: the Starship milestone (one item from each nebula chain) and the Ship section recipes it unlocks. */
@GameTestHolder(VoidReclaimer.MOD_ID)
@PrefixGameTestTemplate(false)
public final class StarshipGameTests {
    private StarshipGameTests() {}

    private static final ResourceLocation SHIP_KEEL = VoidReclaimer.id("replicator/ship_keel");
    private static final ResourceLocation SHIP_CORE_KIT = VoidReclaimer.id("replicator/ship_core_kit");

    private static void give(ServerPlayer player, net.minecraft.world.item.Item item) {
        player.getInventory().add(new ItemStack(item));
    }

    private static void take(ServerPlayer player, net.minecraft.world.item.Item item) {
        player.getInventory().clearOrCountMatchingItems(stack -> stack.is(item), -1, player.inventoryMenu.getCraftSlots());
    }

    @GameTest(template = "empty", timeoutTicks = 60)
    public static void starshipNeedsOneItemFromEveryChainEvenIfLaterGivenAway(GameTestHelper helper) {
        ServerPlayer player = TestPlayers.survivalInDeepSpace(helper, 88_000.5, 80, 0.5);
        try {
            Replicator.checkMilestones(player);
            give(player, ModMaterials.COOLANT_FRAME_PLATE.get());
            Replicator.checkMilestones(player);
            helper.assertFalse(Replicator.data(player).isUnlocked(Milestones.STARSHIP), "unlocked with one chain");

            // Made once is made: the plate may leave the inventory (into a machine, a build) and still count.
            take(player, ModMaterials.COOLANT_FRAME_PLATE.get());
            give(player, ModMaterials.CARBON_COMPOSITE_PLATE.get());
            give(player, ModMaterials.HIGH_EFFICIENCY_SOLAR_CELL.get());
            Replicator.checkMilestones(player);
            helper.assertFalse(Replicator.data(player).isUnlocked(Milestones.STARSHIP), "unlocked with three chains");
            helper.assertTrue(Replicator.data(player).itemsMade().contains(VoidReclaimer.id("coolant_frame_plate")), "coolant plate forgotten");

            give(player, ModMaterials.MICRO_ALLOY_SHEET.get());
            Replicator.checkMilestones(player);
            helper.assertTrue(Replicator.data(player).isUnlocked(Milestones.STARSHIP), "not unlocked with all four chains");
            helper.succeed();
        } finally {
            TestPlayers.remove(player);
        }
    }

    @GameTest(template = "empty", timeoutTicks = 60)
    public static void shipRecipesWaitForTheStarshipMilestone(GameTestHelper helper) {
        ServerPlayer player = TestPlayers.survivalInDeepSpace(helper, 88_020.5, 80, 0.5);
        try {
            Replicator.checkMilestones(player);
            player.setData(ModAttachments.DUST, DustData.of(1000, 1000, 1000, 1000));
            helper.assertTrue(Replicator.queue(player, SHIP_KEEL) == ReplicatorLogic.QueueResult.LOCKED, "keel unlocked before the milestone");
            helper.assertTrue(Replicator.queue(player, SHIP_CORE_KIT) == ReplicatorLogic.QueueResult.LOCKED, "core kit unlocked before the milestone");

            give(player, ModMaterials.COOLANT_FRAME_PLATE.get());
            give(player, ModMaterials.CARBON_COMPOSITE_PLATE.get());
            give(player, ModMaterials.HIGH_EFFICIENCY_SOLAR_CELL.get());
            give(player, ModMaterials.MICRO_ALLOY_SHEET.get());
            Replicator.checkMilestones(player);
            helper.assertTrue(Replicator.queue(player, SHIP_KEEL) == ReplicatorLogic.QueueResult.QUEUED, "keel still locked");
            helper.assertTrue(Replicator.queue(player, SHIP_CORE_KIT) == ReplicatorLogic.QueueResult.QUEUED, "core kit still locked");
            helper.succeed();
        } finally {
            TestPlayers.remove(player);
        }
    }

    /** The GameTest datapack adds a milestone with the same criterion type: addons can ask for any items. */
    @GameTest(template = "empty", timeoutTicks = 60)
    public static void datapackMilestonesCanAskForAnyItems(GameTestHelper helper) {
        ServerPlayer player = TestPlayers.survivalInDeepSpace(helper, 88_040.5, 80, 0.5);
        try {
            ResourceKey<com.voidreclaimer.api.milestone.Milestone> toolmaker = ResourceKey.create(
                    com.voidreclaimer.api.VoidReclaimerRegistries.MILESTONE, ResourceLocation.fromNamespaceAndPath(GameTestPack.NAMESPACE, "toolmaker"));
            give(player, Items.STICK);
            Replicator.checkMilestones(player);
            helper.assertFalse(Replicator.data(player).isUnlocked(toolmaker), "unlocked with only a stick");
            give(player, Items.FLINT);
            Replicator.checkMilestones(player);
            helper.assertTrue(Replicator.data(player).isUnlocked(toolmaker), "datapack items_made milestone not reached");
            helper.succeed();
        } finally {
            TestPlayers.remove(player);
        }
    }
}
