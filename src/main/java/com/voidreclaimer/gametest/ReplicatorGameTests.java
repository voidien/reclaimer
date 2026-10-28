package com.voidreclaimer.gametest;

import com.voidreclaimer.VoidReclaimer;
import com.voidreclaimer.dust.DustData;
import com.voidreclaimer.energy.EnergyData;
import com.voidreclaimer.registry.ModAttachments;
import com.voidreclaimer.registry.ModBlocks;
import com.voidreclaimer.registry.ModItems;
import com.voidreclaimer.replicator.Milestone;
import com.voidreclaimer.replicator.Replicator;
import com.voidreclaimer.replicator.ReplicatorData;
import com.voidreclaimer.replicator.ReplicatorLogic;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/** Phase 4a: Replicator queue, milestones, output buffer, and the Salvage Tool, against the datagen recipes. */
@GameTestHolder(VoidReclaimer.MOD_ID)
@PrefixGameTestTemplate(false)
public final class ReplicatorGameTests {
    private static final ResourceLocation SCAFFOLD = VoidReclaimer.id("replicator/scaffold");
    private static final ResourceLocation SALVAGE_TOOL = VoidReclaimer.id("replicator/salvage_tool");
    /** Scaffold recipe: 5 s build. */
    private static final int SCAFFOLD_TICKS = 100;

    private ReplicatorGameTests() {}

    @GameTest(template = "empty")
    public static void awakeningUnlocksStarterDesigns(GameTestHelper helper) {
        ServerPlayer player = TestPlayers.survivalInDeepSpace(helper, 40_000.5, 80, 0.5);
        try {
            helper.assertTrue(Replicator.queue(player, SCAFFOLD) == ReplicatorLogic.QueueResult.LOCKED, "scaffold unlocked before awakening");
            Replicator.checkMilestones(player);
            helper.assertTrue(Replicator.data(player).isUnlocked(Milestone.AWAKENING), "awakening not granted");
            helper.assertFalse(Replicator.data(player).isUnlocked(Milestone.FIRST_DUST), "first dust granted without dust");
            helper.succeed();
        } finally {
            TestPlayers.remove(player);
        }
    }

    @GameTest(template = "empty")
    public static void firstDustUnlocksSalvageTool(GameTestHelper helper) {
        ServerPlayer player = TestPlayers.survivalInDeepSpace(helper, 41_000.5, 80, 0.5);
        try {
            Replicator.checkMilestones(player);
            player.setData(ModAttachments.DUST, new DustData(50, 50, 0, 0));
            // Dust alone doesn't unlock until the milestone check runs.
            helper.assertTrue(Replicator.queue(player, SALVAGE_TOOL) == ReplicatorLogic.QueueResult.LOCKED, "salvage tool unlocked too early");
            Replicator.checkMilestones(player);
            helper.assertTrue(Replicator.queue(player, SALVAGE_TOOL) == ReplicatorLogic.QueueResult.QUEUED, "salvage tool not unlocked by first dust");
            helper.succeed();
        } finally {
            TestPlayers.remove(player);
        }
    }

    @GameTest(template = "empty")
    public static void replicatingScaffoldTakesDustTimeAndDelivers(GameTestHelper helper) {
        ServerPlayer player = TestPlayers.survivalInDeepSpace(helper, 42_000.5, 80, 0.5);
        try {
            Replicator.checkMilestones(player);
            player.setData(ModAttachments.DUST, new DustData(0, 10, 0, 0));
            helper.assertTrue(Replicator.queue(player, SCAFFOLD) == ReplicatorLogic.QueueResult.QUEUED, "queue failed");
            helper.assertTrue(Math.abs(player.getData(ModAttachments.DUST).silicate() - 2f) < 1e-4, "dust not taken on queue");
            helper.assertTrue(Replicator.queue(player, SCAFFOLD) == ReplicatorLogic.QueueResult.NOT_ENOUGH_DUST, "queued without enough dust");

            for (int i = 0; i < SCAFFOLD_TICKS - 1; i++) {
                player.doTick();
            }
            helper.assertTrue(countOf(player, ModItems.SCAFFOLD.get().getDefaultInstance()) == 0, "delivered too early");
            player.doTick();
            player.doTick();
            helper.assertTrue(countOf(player, ModItems.SCAFFOLD.get().getDefaultInstance()) == 8, "8 scaffold not delivered");
            helper.assertTrue(Replicator.data(player).jobs().isEmpty(), "job not removed");
            helper.succeed();
        } finally {
            TestPlayers.remove(player);
        }
    }

    @GameTest(template = "empty")
    public static void lowPowerPausesTheReplicator(GameTestHelper helper) {
        ServerPlayer player = TestPlayers.survivalInDeepSpace(helper, 43_000.5, 80, 0.5);
        try {
            Replicator.checkMilestones(player);
            player.setData(ModAttachments.DUST, new DustData(0, 8, 0, 0));
            Replicator.queue(player, SCAFFOLD);
            EnergyData energy = player.getData(ModAttachments.ENERGY);
            player.setData(ModAttachments.ENERGY, new EnergyData(0.01f, energy.capacity(), true));
            for (int i = 0; i < 40; i++) {
                player.doTick();
            }
            helper.assertTrue(Replicator.data(player).jobs().get(0).progress() == 0, "worked in low power");
            helper.succeed();
        } finally {
            TestPlayers.remove(player);
        }
    }

    @GameTest(template = "empty")
    public static void cancelRefundsDust(GameTestHelper helper) {
        ServerPlayer player = TestPlayers.survivalInDeepSpace(helper, 44_000.5, 80, 0.5);
        try {
            Replicator.checkMilestones(player);
            player.setData(ModAttachments.DUST, new DustData(0, 8, 0, 0));
            Replicator.queue(player, SCAFFOLD);
            helper.assertTrue(Replicator.cancel(player, 0), "cancel failed");
            helper.assertTrue(Math.abs(player.getData(ModAttachments.DUST).silicate() - 8f) < 1e-4, "dust not refunded");
            helper.assertFalse(Replicator.cancel(player, 0), "cancelled a job that doesn't exist");
            helper.succeed();
        } finally {
            TestPlayers.remove(player);
        }
    }

    @GameTest(template = "empty")
    public static void fullInventoryFillsTheOutputBuffer(GameTestHelper helper) {
        ServerPlayer player = TestPlayers.survivalInDeepSpace(helper, 45_000.5, 80, 0.5);
        try {
            Replicator.checkMilestones(player);
            for (int i = 0; i < player.getInventory().items.size(); i++) {
                player.getInventory().items.set(i, new ItemStack(Items.STONE, 64));
            }
            player.setData(ModAttachments.DUST, new DustData(0, 8, 0, 0));
            Replicator.queue(player, SCAFFOLD);
            for (int i = 0; i < SCAFFOLD_TICKS + 1; i++) {
                player.doTick();
            }
            ReplicatorData data = Replicator.data(player);
            helper.assertTrue(data.buffer().size() == 1 && data.buffer().get(0).getCount() == 8, "output not buffered: " + data.buffer());

            player.getInventory().items.set(0, ItemStack.EMPTY);
            Replicator.collect(player);
            helper.assertTrue(Replicator.data(player).buffer().isEmpty(), "buffer not collected");
            helper.assertTrue(player.getInventory().items.get(0).is(ModItems.SCAFFOLD.get()), "collected item not in inventory");
            helper.succeed();
        } finally {
            TestPlayers.remove(player);
        }
    }

    @GameTest(template = "empty", timeoutTicks = 60)
    public static void salvageToolDismantlesForDustWithoutDrops(GameTestHelper helper) {
        ServerLevel space = TestPlayers.deepSpace(helper);
        BlockPos pos = new BlockPos(46_000, 80, 2);
        space.setChunkForced(pos.getX() >> 4, pos.getZ() >> 4, true);
        helper.runAfterDelay(5, () -> {
            ServerPlayer player = TestPlayers.survivalInDeepSpace(helper, pos.getX() + 0.5, pos.getY(), pos.getZ() - 1.5);
            try {
                space.getEntitiesOfClass(ItemEntity.class, new AABB(pos).inflate(4)).forEach(ItemEntity::discard);
                space.setBlock(pos, ModBlocks.SCAFFOLD.get().defaultBlockState(), 3);
                space.setBlock(pos.east(), ModBlocks.SCAFFOLD.get().defaultBlockState(), 3);
                space.setBlock(pos.west(), Blocks.STONE.defaultBlockState(), 3);
                player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ModItems.SALVAGE_TOOL.get()));

                player.gameMode.destroyBlock(pos);
                helper.assertTrue(space.getBlockState(pos).isAir(), "scaffold not removed");
                float refund = player.getData(ModAttachments.DUST).silicate();
                // 8 silicate per 8 scaffold, 50% refund: 0.5 per block.
                helper.assertTrue(Math.abs(refund - 0.5f) < 1e-4, "expected 0.5 silicate refund, got " + refund);

                // Without the tool the block drops itself and refunds nothing.
                player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
                player.gameMode.destroyBlock(pos.east());
                helper.assertTrue(Math.abs(player.getData(ModAttachments.DUST).silicate() - 0.5f) < 1e-4, "refund without the tool");

                // Natural blocks aren't salvageable: no refund.
                player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ModItems.SALVAGE_TOOL.get()));
                player.gameMode.destroyBlock(pos.west());
                helper.assertTrue(Math.abs(player.getData(ModAttachments.DUST).silicate() - 0.5f) < 1e-4, "stone gave a refund");
            } finally {
                TestPlayers.remove(player);
            }
            helper.runAfterDelay(2, () -> {
                long scaffoldDrops = space.getEntitiesOfClass(ItemEntity.class, new AABB(pos).inflate(4)).stream()
                        .filter(e -> e.getItem().is(ModItems.SCAFFOLD.get())).mapToInt(e -> e.getItem().getCount()).sum();
                helper.assertTrue(scaffoldDrops == 1, "expected exactly the hand-broken scaffold to drop, got " + scaffoldDrops);
                space.getEntitiesOfClass(ItemEntity.class, new AABB(pos).inflate(4)).forEach(ItemEntity::discard);
                space.setChunkForced(pos.getX() >> 4, pos.getZ() >> 4, false);
                helper.succeed();
            });
        });
    }

    private static int countOf(ServerPlayer player, ItemStack target) {
        int count = 0;
        for (ItemStack stack : player.getInventory().items) {
            if (ItemStack.isSameItem(stack, target)) {
                count += stack.getCount();
            }
        }
        return count;
    }
}
