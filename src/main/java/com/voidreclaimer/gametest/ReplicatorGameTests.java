package com.voidreclaimer.gametest;

import com.voidreclaimer.dust.DustKinds;
import com.voidreclaimer.VoidReclaimer;
import com.voidreclaimer.api.body.ModuleKind;
import com.voidreclaimer.dust.DustData;
import com.voidreclaimer.energy.EnergyData;
import com.voidreclaimer.registry.ModAttachments;
import com.voidreclaimer.registry.ModBlocks;
import com.voidreclaimer.registry.ModItems;
import com.voidreclaimer.replicator.Milestones;
import com.voidreclaimer.replicator.Replicator;
import com.voidreclaimer.replicator.ReplicatorData;
import com.voidreclaimer.replicator.ReplicatorJob;
import com.voidreclaimer.replicator.ReplicatorLogic;
import com.voidreclaimer.replicator.DustCost;
import com.voidreclaimer.salvage.Salvage;
import java.util.List;
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
    private static final ResourceLocation DRONE_KIT = VoidReclaimer.id("replicator/drone_kit");
    /** Scaffold recipe: 5 s build. */
    private static final int SCAFFOLD_TICKS = 100;

    private ReplicatorGameTests() {}

    @GameTest(template = "empty")
    public static void queuedJobsCanBeReorderedButTheRunningOneStays(GameTestHelper helper) {
        ServerPlayer player = TestPlayers.survivalInDeepSpace(helper, 99_000.5, 80, 0.5);
        try {
            Replicator.checkMilestones(player);
            player.setData(ModAttachments.DUST, DustData.of(100, 100, 0, 0));
            ResourceLocation thruster = VoidReclaimer.id("replicator/thruster_mk1");
            ResourceLocation rotor = VoidReclaimer.id("replicator/rotor_arm");
            helper.assertTrue(Replicator.queue(player, SCAFFOLD) == ReplicatorLogic.QueueResult.QUEUED, "queue 1");
            helper.assertTrue(Replicator.queue(player, thruster) == ReplicatorLogic.QueueResult.QUEUED, "queue 2");
            helper.assertTrue(Replicator.queue(player, rotor) == ReplicatorLogic.QueueResult.QUEUED, "queue 3");
            helper.assertFalse(Replicator.move(player, 0, 2), "the running job moved");
            helper.assertFalse(Replicator.move(player, 1, 1), "a move to the same place changed the queue");
            helper.assertTrue(Replicator.move(player, 2, 1), "a waiting job could not be moved up");
            var jobs = Replicator.data(player).jobs();
            helper.assertTrue(jobs.get(0).recipe().equals(SCAFFOLD) && jobs.get(1).recipe().equals(rotor) && jobs.get(2).recipe().equals(thruster),
                    "order after the move: " + jobs.stream().map(j -> j.recipe().getPath()).toList());
            helper.assertTrue(jobs.get(0).progress() == Replicator.data(player).jobs().get(0).progress(), "the running job lost progress");
            helper.succeed();
        } finally {
            TestPlayers.remove(player);
        }
    }

    @GameTest(template = "empty")
    public static void awakeningUnlocksStarterDesigns(GameTestHelper helper) {
        ServerPlayer player = TestPlayers.survivalInDeepSpace(helper, 40_000.5, 80, 0.5);
        try {
            TestPlayers.notYetAwake(player);
            helper.assertTrue(Replicator.queue(player, SCAFFOLD) == ReplicatorLogic.QueueResult.LOCKED, "scaffold unlocked before awakening");
            Replicator.checkMilestones(player);
            helper.assertTrue(Replicator.data(player).isUnlocked(Milestones.AWAKENING), "awakening not granted");
            helper.assertFalse(Replicator.data(player).isUnlocked(Milestones.FIRST_DUST), "first dust granted without dust");
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
            player.setData(ModAttachments.DUST, DustData.of(50, 50, 0, 0));
            // Held dust (a gift, say) isn't first dust: only dust gathered from a cloud or by drilling counts.
            Replicator.checkMilestones(player);
            helper.assertTrue(Replicator.queue(player, SALVAGE_TOOL) == ReplicatorLogic.QueueResult.LOCKED, "salvage tool unlocked by held dust");
            player.setData(ModAttachments.DUST_COLLECTED, DustData.of(50, 50, 0, 0));
            // Gathering alone doesn't unlock until the milestone check runs.
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
            player.setData(ModAttachments.DUST, DustData.of(0, 10, 0, 0));
            helper.assertTrue(Replicator.queue(player, SCAFFOLD) == ReplicatorLogic.QueueResult.QUEUED, "queue failed");
            helper.assertTrue(Math.abs(player.getData(ModAttachments.DUST).get(DustKinds.SILICATE) - 2f) < 1e-4, "dust not taken on queue");
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
            player.setData(ModAttachments.DUST, DustData.of(0, 8, 0, 0));
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
            player.setData(ModAttachments.DUST, DustData.of(0, 8, 0, 0));
            Replicator.queue(player, SCAFFOLD);
            helper.assertTrue(Replicator.cancel(player, 0), "cancel failed");
            helper.assertTrue(Math.abs(player.getData(ModAttachments.DUST).get(DustKinds.SILICATE) - 8f) < 1e-4, "dust not refunded");
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
            player.setData(ModAttachments.DUST, DustData.of(0, 8, 0, 0));
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

    @GameTest(template = "empty", timeoutTicks = 120)
    public static void salvageToolDismantlesForDustWithoutDrops(GameTestHelper helper) {
        ServerLevel space = TestPlayers.deepSpace(helper);
        BlockPos pos = new BlockPos(46_000, 80, 2);
        space.setChunkForced(pos.getX() >> 4, pos.getZ() >> 4, true);
        // A fresh world has to generate this chunk first, and a dropped item is only found by getEntitiesOfClass once the chunk
        // ticks entities, which can come a few ticks after the chunk is loaded: wait for it instead of a fixed delay.
        space.getChunk(pos);
        helper.startSequence()
                .thenWaitUntil(() -> helper.assertTrue(space.isPositionEntityTicking(pos), "chunk not ticking entities yet"))
                .thenExecute(() -> {
                    ServerPlayer player = TestPlayers.survivalInDeepSpace(helper, pos.getX() + 0.5, pos.getY(), pos.getZ() - 1.5);
                    try {
                        space.getEntitiesOfClass(ItemEntity.class, new AABB(pos).inflate(4)).forEach(ItemEntity::discard);
                        space.setBlock(pos, ModBlocks.SCAFFOLD.get().defaultBlockState(), 3);
                        space.setBlock(pos.east(), ModBlocks.SCAFFOLD.get().defaultBlockState(), 3);
                        space.setBlock(pos.west(), Blocks.STONE.defaultBlockState(), 3);
                        TestPlayers.installModule(player, ModuleKind.TOOL, 0, new ItemStack(ModItems.SALVAGE_TOOL.get()));

                        player.gameMode.destroyBlock(pos);
                        helper.assertTrue(space.getBlockState(pos).isAir(), "scaffold not removed");
                        float refund = player.getData(ModAttachments.DUST).get(DustKinds.SILICATE);
                        // 8 silicate per 8 scaffold, 50% refund: 0.5 per block.
                        helper.assertTrue(Math.abs(refund - 0.5f) < 1e-4, "expected 0.5 silicate refund, got " + refund);

                        // With something in hand the module is idle: the block drops itself and refunds nothing.
                        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.STICK));
                        player.gameMode.destroyBlock(pos.east());
                        helper.assertTrue(Math.abs(player.getData(ModAttachments.DUST).get(DustKinds.SILICATE) - 0.5f) < 1e-4, "refund without the tool");

                        // Natural blocks aren't salvageable: no refund.
                        player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
                        player.gameMode.destroyBlock(pos.west());
                        helper.assertTrue(Math.abs(player.getData(ModAttachments.DUST).get(DustKinds.SILICATE) - 0.5f) < 1e-4, "stone gave a refund");
                    } finally {
                        TestPlayers.remove(player);
                    }
                })
                .thenIdle(2)
                .thenExecute(() -> {
                    long scaffoldDrops = space.getEntitiesOfClass(ItemEntity.class, new AABB(pos).inflate(4)).stream()
                            .filter(e -> e.getItem().is(ModItems.SCAFFOLD.get())).mapToInt(e -> e.getItem().getCount()).sum();
                    space.getEntitiesOfClass(ItemEntity.class, new AABB(pos).inflate(4)).forEach(ItemEntity::discard);
                    space.setChunkForced(pos.getX() >> 4, pos.getZ() >> 4, false);
                    helper.assertTrue(scaffoldDrops == 1, "expected exactly the hand-broken scaffold to drop, got " + scaffoldDrops);
                })
                .thenSucceed();
    }

    private static final net.minecraft.resources.ResourceLocation ITEMS_TOO =
            net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("void_reclaimer_test", "replicator/items_too");

    private static int held(ServerPlayer player, net.minecraft.world.item.Item item) {
        return player.getInventory().countItem(item);
    }

    /** Queue in a shell, wake at home and cancel: that would carry the items any distance for nothing, so only the body that paid can. */
    @GameTest(template = "empty", timeoutTicks = 200)
    public static void aCancelFromAnotherBodyIsRefused(GameTestHelper helper) {
        BlockPos origin = new BlockPos(47_800, 80, 0);
        NaniteGameTests.inEntityTickingArea(helper, origin, 1, () -> {
            ServerPlayer player = TestPlayers.survivalInDeepSpace(helper, origin.getX() + 0.5, origin.getY(), origin.getZ() + 0.5);
            var shell = com.voidreclaimer.link.BodyHost.spawn(player.serverLevel(), player.position().add(0, 0, 4), com.voidreclaimer.link.HostRole.SHELL,
                    com.voidreclaimer.body.Bodies.preset(com.voidreclaimer.body.BuiltinBodies.DRONE), player.getUUID());
            var id = shell.getUUID();
            helper.assertTrue(com.voidreclaimer.link.Links.link(player, shell) == com.voidreclaimer.link.Links.Result.LINKED, "no link");
            player.getInventory().clearContent();
            Replicator.store(player, Replicator.data(player).withUnlocked(Milestones.AWAKENING));
            player.setData(ModAttachments.DUST, DustData.of(10, 0, 0, 0));
            player.getInventory().setItem(0, new ItemStack(net.minecraft.world.item.Items.IRON_INGOT, 3));
            player.getInventory().setItem(1, new ItemStack(net.minecraft.world.item.Items.OAK_PLANKS, 1));
            helper.assertTrue(Replicator.queue(player, ITEMS_TOO) == ReplicatorLogic.QueueResult.QUEUED, "not queued in the shell");

            helper.assertTrue(com.voidreclaimer.link.Links.wake(player) == com.voidreclaimer.link.Links.Result.WOKEN, "no wake");
            helper.assertFalse(Replicator.cancel(player, 0), "the main body cancelled a job its shell paid for");
            helper.assertTrue(held(player, net.minecraft.world.item.Items.IRON_INGOT) == 0, "the items came home for free");
            helper.assertTrue(Replicator.data(player).jobs().size() == 1, "a refused cancel lost the job");

            var back = (com.voidreclaimer.link.BodyHost) player.serverLevel().getEntity(id);
            helper.assertTrue(com.voidreclaimer.link.Links.link(player, back) == com.voidreclaimer.link.Links.Result.LINKED, "no second link");
            helper.assertTrue(Replicator.cancel(player, 0), "the shell that paid could not cancel");
            helper.assertTrue(held(player, net.minecraft.world.item.Items.IRON_INGOT) == 3, "the items did not come back to the shell");
            com.voidreclaimer.link.Links.wake(player);
            TestPlayers.remove(player);
            helper.succeed();
        });
    }

    /** Queued items are carried things: a death drops them with the rest, and the dust comes back. */
    @GameTest(template = "empty", timeoutTicks = 100)
    public static void aDyingBodyDropsWhatItsJobsTook(GameTestHelper helper) {
        net.minecraft.core.BlockPos origin = new net.minecraft.core.BlockPos(48_100, 80, 0);
        NaniteGameTests.inEntityTickingArea(helper, origin, 1, () -> {
            ServerPlayer player = TestPlayers.survivalInDeepSpace(helper, origin.getX() + 0.5, origin.getY(), origin.getZ() + 0.5);
            player.getInventory().clearContent();
            Replicator.store(player, ReplicatorData.EMPTY.withUnlocked(Milestones.AWAKENING));
            player.setData(ModAttachments.DUST, DustData.of(10, 0, 0, 0));
            player.getInventory().setItem(0, new ItemStack(net.minecraft.world.item.Items.IRON_INGOT, 3));
            player.getInventory().setItem(1, new ItemStack(net.minecraft.world.item.Items.OAK_PLANKS, 1));
            helper.assertTrue(Replicator.queue(player, ITEMS_TOO) == ReplicatorLogic.QueueResult.QUEUED, "not queued");
            // The world outlives a run, and an entity can be saved in it, so what an earlier run dropped can still be here.
            var near = new net.minecraft.world.phys.AABB(origin).inflate(8);
            player.serverLevel().getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class, near).forEach(net.minecraft.world.entity.Entity::discard);
            player.hurt(player.serverLevel().damageSources().genericKill(), Float.MAX_VALUE);
            var drops = player.serverLevel().getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class, near);
            int ingots = drops.stream().filter(drop -> drop.getItem().is(net.minecraft.world.item.Items.IRON_INGOT)).mapToInt(drop -> drop.getItem().getCount()).sum();
            helper.assertTrue(ingots == 3, ingots + " of 3 ingots dropped");
            helper.assertTrue(Replicator.data(player).jobs().isEmpty(), "the job outlived the items it took");
            helper.assertTrue(Math.abs(player.getData(ModAttachments.DUST).get(DustKinds.METALLIC) - 10f) < 1e-4, "the dust did not come back");
            TestPlayers.remove(player);
            helper.succeed();
        });
    }

    /** A shell that dies with a job running leaves those items in its wreck, like everything else it carried. */
    @GameTest(template = "empty", timeoutTicks = 200)
    public static void aWreckKeepsWhatItsShellsJobsTook(GameTestHelper helper) {
        net.minecraft.core.BlockPos origin = new net.minecraft.core.BlockPos(48_300, 80, 0);
        NaniteGameTests.inEntityTickingArea(helper, origin, 1, () -> {
            ServerPlayer player = TestPlayers.survivalInDeepSpace(helper, origin.getX() + 0.5, origin.getY(), origin.getZ() + 0.5);
            var level = player.serverLevel();
            var shell = com.voidreclaimer.link.BodyHost.spawn(level, player.position().add(0, 0, 4), com.voidreclaimer.link.HostRole.SHELL,
                    com.voidreclaimer.body.Bodies.preset(com.voidreclaimer.body.BuiltinBodies.DRONE), player.getUUID());
            var id = shell.getUUID();
            helper.assertTrue(com.voidreclaimer.link.Links.link(player, shell) == com.voidreclaimer.link.Links.Result.LINKED, "no link");
            player.getInventory().clearContent();
            Replicator.store(player, ReplicatorData.EMPTY.withUnlocked(Milestones.AWAKENING));
            player.setData(ModAttachments.DUST, DustData.of(10, 0, 0, 0));
            player.getInventory().setItem(0, new ItemStack(net.minecraft.world.item.Items.IRON_INGOT, 3));
            player.getInventory().setItem(1, new ItemStack(net.minecraft.world.item.Items.OAK_PLANKS, 1));
            helper.assertTrue(Replicator.queue(player, ITEMS_TOO) == ReplicatorLogic.QueueResult.QUEUED, "not queued in the shell");
            player.hurt(level.damageSources().genericKill(), Float.MAX_VALUE);
            helper.startSequence()
                    .thenWaitUntil(() -> helper.assertFalse(com.voidreclaimer.link.Links.state(player).linked(), "still in the shell"))
                    .thenExecute(() -> {
                        var wreck = (com.voidreclaimer.link.BodyHost) level.getEntity(id);
                        helper.assertTrue(wreck != null && wreck.role() == com.voidreclaimer.link.HostRole.WRECK, "no wreck");
                        int ingots = 0;
                        for (int i = 0; i < wreck.items().size(); i++) {
                            ItemStack stack = ItemStack.parseOptional(player.registryAccess(), wreck.items().getCompound(i));
                            ingots += stack.is(net.minecraft.world.item.Items.IRON_INGOT) ? stack.getCount() : 0;
                        }
                        helper.assertTrue(ingots == 3, "the wreck holds " + ingots + " of 3 ingots");
                        helper.assertTrue(Replicator.data(player).jobs().isEmpty(), "the job outlived its shell");
                        helper.assertTrue(Math.abs(player.getData(ModAttachments.DUST).get(DustKinds.METALLIC) - 10f) < 1e-4, "the dust did not come back");
                        LinkOutboxGameTests.finish(helper, player, level, id);
                    });
        });
    }

    /** A part in the offhand or worn counts, as it does for milestones, and the main inventory is spent first. */
    @GameTest(template = "empty")
    public static void aRecipeLooksInTheOffhandAndArmorLast(GameTestHelper helper) {
        ServerPlayer player = TestPlayers.survivalInDeepSpace(helper, 47_900.5, 80, 0.5);
        try {
            player.getInventory().clearContent();
            Replicator.store(player, Replicator.data(player).withUnlocked(Milestones.AWAKENING));
            player.setData(ModAttachments.DUST, DustData.of(20, 0, 0, 0));
            player.getInventory().setItem(0, new ItemStack(net.minecraft.world.item.Items.IRON_INGOT, 1));
            player.getInventory().offhand.set(0, new ItemStack(net.minecraft.world.item.Items.IRON_INGOT, 3));
            player.getInventory().armor.set(0, new ItemStack(net.minecraft.world.item.Items.OAK_PLANKS, 1));
            helper.assertTrue(Replicator.queue(player, ITEMS_TOO) == ReplicatorLogic.QueueResult.QUEUED, "the offhand and armor did not count");
            helper.assertTrue(player.getInventory().items.get(0).isEmpty(), "the main inventory was not spent first");
            helper.assertTrue(player.getInventory().offhand.get(0).getCount() == 1, "the offhand lost " + (3 - player.getInventory().offhand.get(0).getCount()) + " of 2");
            helper.assertTrue(player.getInventory().armor.get(0).isEmpty(), "the armor slot kept its item");
            helper.assertTrue(Replicator.cancel(player, 0), "not cancelled");
            helper.assertTrue(held(player, net.minecraft.world.item.Items.IRON_INGOT) == 4, "the ingots did not all come back");
            helper.assertTrue(held(player, net.minecraft.world.item.Items.OAK_PLANKS) == 1, "the planks did not come back");
            helper.succeed();
        } finally {
            TestPlayers.remove(player);
        }
    }

    /** The extension point: a recipe's {@code inputs} are taken with the dust, by item or tag, and a cancel gives back the same items. */
    @GameTest(template = "empty")
    public static void aRecipeTakesItemsAndACancelGivesTheSameOnesBack(GameTestHelper helper) {
        ServerPlayer player = TestPlayers.survivalInDeepSpace(helper, 47_500.5, 80, 0.5);
        try {
            player.getInventory().clearContent();
            Replicator.store(player, Replicator.data(player).withUnlocked(Milestones.AWAKENING));
            player.setData(ModAttachments.DUST, DustData.of(10, 0, 0, 0));
            player.getInventory().setItem(0, new ItemStack(net.minecraft.world.item.Items.IRON_INGOT, 2));
            player.getInventory().setItem(1, new ItemStack(net.minecraft.world.item.Items.OAK_PLANKS, 1));

            helper.assertTrue(Replicator.queue(player, ITEMS_TOO) == ReplicatorLogic.QueueResult.NOT_ENOUGH_ITEMS, "queued with one ingot short");
            helper.assertTrue(held(player, net.minecraft.world.item.Items.IRON_INGOT) == 2, "a refused job took the ingots");
            helper.assertTrue(Math.abs(player.getData(ModAttachments.DUST).get(DustKinds.METALLIC) - 10f) < 1e-4, "a refused job took dust");

            // The third ingot is in another slot, and it has a name that a cancel must not lose.
            ItemStack named = new ItemStack(net.minecraft.world.item.Items.IRON_INGOT);
            named.set(net.minecraft.core.component.DataComponents.CUSTOM_NAME, net.minecraft.network.chat.Component.literal("Spare"));
            player.getInventory().setItem(8, named);
            var census = com.voidreclaimer.debug.ItemCensus.of(player);
            helper.assertTrue(Replicator.queue(player, ITEMS_TOO) == ReplicatorLogic.QueueResult.QUEUED, "not queued with everything there");
            helper.assertTrue(com.voidreclaimer.debug.ItemCensus.of(player).since(census).isEmpty(), "the census lost track of a queued job's items");
            helper.assertTrue(held(player, net.minecraft.world.item.Items.IRON_INGOT) == 0, "the ingots stayed");
            helper.assertTrue(held(player, net.minecraft.world.item.Items.OAK_PLANKS) == 0, "the tag did not take the planks");
            helper.assertTrue(Math.abs(player.getData(ModAttachments.DUST).get(DustKinds.METALLIC) - 5f) < 1e-4, "the dust was not paid");

            helper.assertTrue(Replicator.cancel(player, 0), "not cancelled");
            helper.assertTrue(held(player, net.minecraft.world.item.Items.IRON_INGOT) == 3, "the ingots did not come back");
            helper.assertTrue(held(player, net.minecraft.world.item.Items.OAK_PLANKS) == 1, "the planks did not come back");
            helper.assertTrue(Math.abs(player.getData(ModAttachments.DUST).get(DustKinds.METALLIC) - 10f) < 1e-4, "the dust did not come back");
            boolean nameKept = false;
            for (ItemStack stack : player.getInventory().items) {
                nameKept |= stack.has(net.minecraft.core.component.DataComponents.CUSTOM_NAME);
            }
            helper.assertTrue(nameKept, "a cancel gave back a plain ingot, not the same one");

            // With no room, nothing is lost: it waits in the output.
            helper.assertTrue(Replicator.queue(player, ITEMS_TOO) == ReplicatorLogic.QueueResult.QUEUED, "not queued the second time");
            for (int slot = 0; slot < player.getInventory().items.size(); slot++) {
                player.getInventory().setItem(slot, new ItemStack(net.minecraft.world.item.Items.DIRT, 64));
            }
            helper.assertTrue(Replicator.cancel(player, 0), "not cancelled with a full inventory");
            int waiting = Replicator.data(player).buffer().stream().mapToInt(ItemStack::getCount).sum();
            helper.assertTrue(waiting == 4, "with no room, " + waiting + " items of 4 wait in the output");

            // A finished job keeps what it took.
            player.getInventory().clearContent();
            Replicator.store(player, Replicator.data(player).withBuffer(List.of()));
            player.getInventory().setItem(0, new ItemStack(net.minecraft.world.item.Items.IRON_INGOT, 3));
            player.getInventory().setItem(1, new ItemStack(net.minecraft.world.item.Items.OAK_PLANKS, 1));
            helper.assertTrue(Replicator.queue(player, ITEMS_TOO) == ReplicatorLogic.QueueResult.QUEUED, "not queued the third time");
            ReplicatorData data = Replicator.data(player);
            ReplicatorJob job = data.jobs().get(0);
            Replicator.store(player, data.withJobs(List.of(new ReplicatorJob(job.recipe(), job.results(), job.paid(), job.items(), job.paidBy(), job.total() - 1, job.total()))));
            player.doTick();
            player.doTick();
            helper.assertTrue(held(player, net.minecraft.world.item.Items.DIAMOND) == 1, "no result");
            helper.assertTrue(held(player, net.minecraft.world.item.Items.IRON_INGOT) == 0, "a finished job gave the ingots back");
            helper.succeed();
        } finally {
            TestPlayers.remove(player);
        }
    }

    @GameTest(template = "empty")
    public static void kitsDeliverEveryPartAndKeepSalvagePrices(GameTestHelper helper) {
        ServerPlayer player = TestPlayers.survivalInDeepSpace(helper, 47_000.5, 80, 0.5);
        try {
            player.getInventory().clearContent();
            Replicator.store(player, Replicator.data(player).withUnlocked(Milestones.AWAKENING).withUnlocked(Milestones.FIRST_DUST));
            player.setData(ModAttachments.DUST, DustData.of(40, 20, 0, 0));
            helper.assertTrue(Replicator.queue(player, DRONE_KIT) == ReplicatorLogic.QueueResult.QUEUED, "kit not queued");
            // Skip to the last tick of the 80 s job.
            ReplicatorData data = Replicator.data(player);
            ReplicatorJob job = data.jobs().get(0);
            Replicator.store(player, data.withJobs(List.of(new ReplicatorJob(job.recipe(), job.results(), job.paid(), job.items(), job.paidBy(),
                    job.total() - 1, job.total()))));
            player.doTick();
            player.doTick();
            helper.assertTrue(countOf(player, ModItems.DRONE_HULL.get().getDefaultInstance()) == 1, "hull not delivered");
            helper.assertTrue(countOf(player, ModItems.SENSOR_DOME.get().getDefaultInstance()) == 1, "dome not delivered");
            helper.assertTrue(countOf(player, ModItems.TOOL_CLAW.get().getDefaultInstance()) == 1, "claw not delivered");
            helper.assertTrue(countOf(player, ModItems.ROTOR_ARM.get().getDefaultInstance()) == 4, "4 rotor arms not delivered");

            // Salvage prices a hull by its own recipe (20/10), not by a seventh of the kit.
            DustCost hull = Salvage.refundFor(player.server.getRecipeManager(), ModItems.DRONE_HULL.get(), 1f);
            helper.assertTrue(Math.abs(hull.get(DustKinds.METALLIC) - 20f) < 1e-4, "hull refund " + hull);
            helper.succeed();
        } finally {
            TestPlayers.remove(player);
        }
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
