package com.voidreclaimer.gametest;

import com.voidreclaimer.VoidReclaimer;
import com.voidreclaimer.energy.EnergyData;
import com.voidreclaimer.nanite.Nanites;
import com.voidreclaimer.registry.ModAttachments;
import com.voidreclaimer.registry.ModBlocks;
import com.voidreclaimer.registry.ModItems;
import com.voidreclaimer.replicator.Milestones;
import com.voidreclaimer.replicator.Replicator;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/** Phase 4b: the Nanite Swarm builds in empty space, and placed blocks count toward First Structure. */
@GameTestHolder(VoidReclaimer.MOD_ID)
@PrefixGameTestTemplate(false)
public final class NaniteGameTests {
    private NaniteGameTests() {}

    /** Force-loads the chunk at {@code pos}, waits for it to tick entities, then runs the test body with a fresh area. */
    static void inLoadedArea(GameTestHelper helper, BlockPos pos, int radius, Runnable body) {
        ServerLevel space = TestPlayers.deepSpace(helper);
        space.setChunkForced(pos.getX() >> 4, pos.getZ() >> 4, true);
        // Scheduled ticks set before this are dropped, so wait a few ticks; then wait for the chunk to really tick, which on a
        // slow machine (CI) or a fresh world takes longer than a fixed delay. A chunk that never ticks gives up after 100 ticks.
        helper.runAfterDelay(5, () -> waitTicking(helper, space, pos, 0, () -> {
            BlockPos.betweenClosed(pos.offset(-radius, -radius, -radius), pos.offset(radius, radius, radius))
                    .forEach(p -> space.setBlock(p, Blocks.AIR.defaultBlockState(), 3));
            body.run();
        }));
    }

    private static void waitTicking(GameTestHelper helper, ServerLevel space, BlockPos pos, int waited, Runnable then) {
        if (space.isPositionEntityTicking(pos) || waited >= 100) {
            then.run();
        } else {
            helper.runAfterDelay(1, () -> waitTicking(helper, space, pos, waited + 1, then));
        }
    }

    @GameTest(template = "empty", timeoutTicks = 60)
    public static void buildsScaffoldInEmptySpace(GameTestHelper helper) {
        BlockPos origin = new BlockPos(50_000, 80, 0);
        inLoadedArea(helper, origin, 8, () -> {
            ServerPlayer player = TestPlayers.survivalInDeepSpace(helper, origin.getX() + 0.5, origin.getY(), origin.getZ() + 0.5);
            try {
                player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ModItems.NANITE_SWARM.get()));
                player.getInventory().setItem(5, new ItemStack(ModItems.SCAFFOLD.get(), 4));
                float energyBefore = player.getData(ModAttachments.ENERGY).energy();
                BlockPos target = Nanites.target(player, 3);

                helper.assertTrue(Nanites.build(player, 3) == Nanites.Result.BUILT, "build failed");
                ServerLevel space = player.serverLevel();
                helper.assertTrue(space.getBlockState(target).is(ModBlocks.SCAFFOLD.get()), "no scaffold at " + target);
                helper.assertTrue(target.getZ() > origin.getZ() + 1, "not built ahead: " + target);
                helper.assertTrue(player.getInventory().getItem(5).getCount() == 3, "scaffold not consumed");
                helper.assertTrue(Math.abs(energyBefore - player.getData(ModAttachments.ENERGY).energy() - 1f) < 1.0E-3, "energy not used");
                helper.assertTrue(Replicator.data(player).blocksPlaced() == 1, "block not counted");
                // Same spot again: occupied.
                helper.assertTrue(Nanites.build(player, 3) == Nanites.Result.BLOCKED, "built into a block");
                helper.succeed();
            } finally {
                TestPlayers.remove(player);
            }
        });
    }

    @GameTest(template = "empty", timeoutTicks = 60)
    public static void prefersOffhandAndNeedsMaterialAndPower(GameTestHelper helper) {
        BlockPos origin = new BlockPos(51_000, 80, 0);
        inLoadedArea(helper, origin, 8, () -> {
            ServerPlayer player = TestPlayers.survivalInDeepSpace(helper, origin.getX() + 0.5, origin.getY(), origin.getZ() + 0.5);
            try {
                player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ModItems.NANITE_SWARM.get()));
                player.setItemInHand(InteractionHand.OFF_HAND, new ItemStack(Items.STONE, 1));
                player.getInventory().setItem(5, new ItemStack(ModItems.SCAFFOLD.get(), 1));
                BlockPos target = Nanites.target(player, 4);
                helper.assertTrue(Nanites.build(player, 4) == Nanites.Result.BUILT, "offhand build failed");
                helper.assertTrue(player.serverLevel().getBlockState(target).is(Blocks.STONE), "offhand block not used");

                player.getInventory().setItem(5, ItemStack.EMPTY);
                helper.assertTrue(Nanites.build(player, 2) == Nanites.Result.NO_BLOCKS, "built without material");

                player.getInventory().setItem(5, new ItemStack(ModItems.SCAFFOLD.get(), 1));
                EnergyData energy = player.getData(ModAttachments.ENERGY);
                player.setData(ModAttachments.ENERGY, new EnergyData(0f, energy.capacity(), true));
                helper.assertTrue(Nanites.build(player, 2) == Nanites.Result.NO_POWER, "built in low power");
                helper.assertTrue(player.serverLevel().getBlockState(Nanites.target(player, 2)).isAir(), "low power placed a block");
                helper.succeed();
            } finally {
                TestPlayers.remove(player);
            }
        });
    }

    @GameTest(template = "empty")
    public static void tenPlacedBlocksReachFirstStructure(GameTestHelper helper) {
        ServerPlayer player = TestPlayers.survivalInDeepSpace(helper, 52_000.5, 80, 0.5);
        try {
            for (int i = 0; i < Milestones.STRUCTURE_BLOCKS - 1; i++) {
                Replicator.countBlockPlaced(player);
            }
            Replicator.checkMilestones(player);
            helper.assertFalse(Replicator.data(player).isUnlocked(Milestones.FIRST_STRUCTURE), "unlocked one block early");
            Replicator.countBlockPlaced(player);
            Replicator.countBlockPlaced(player);
            Replicator.checkMilestones(player);
            helper.assertTrue(Replicator.data(player).isUnlocked(Milestones.FIRST_STRUCTURE), "not unlocked");
            helper.assertTrue(Replicator.data(player).blocksPlaced() == Milestones.STRUCTURE_BLOCKS, "kept counting past the goal");
            helper.succeed();
        } finally {
            TestPlayers.remove(player);
        }
    }
}
