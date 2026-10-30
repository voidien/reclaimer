package com.voidreclaimer.replicator;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import com.voidreclaimer.api.milestone.Milestone;
import com.voidreclaimer.api.milestone.MilestoneContext;
import com.voidreclaimer.dust.DustData;
import com.voidreclaimer.dust.DustKinds;
import java.util.Optional;
import net.minecraft.resources.ResourceKey;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.junit.jupiter.api.Test;

class ReplicatorLogicTest {
    private static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath("void_reclaimer", "replicator/test");
    private static final ReplicatorRecipe RECIPE = new ReplicatorRecipe(new ItemStack(Items.STONE, 8),
            DustCost.of(2, 8, 0, 0), 3, ReplicatorCategories.BLOCKS, Milestones.AWAKENING);
    private static final ReplicatorData UNLOCKED = ReplicatorData.EMPTY.withUnlocked(Milestones.AWAKENING);
    private static final DustData RICH = DustData.of(10, 10, 0, 0);

    private static ReplicatorData queueOf(String... names) {
        ReplicatorData data = UNLOCKED;
        DustData dust = DustData.of(100, 100, 0, 0);
        for (String name : names) {
            ReplicatorLogic.Queued q = ReplicatorLogic.queue(data, dust, ResourceLocation.fromNamespaceAndPath("void_reclaimer", "replicator/" + name), RECIPE);
            data = q.data();
            dust = q.dust();
        }
        return data;
    }

    private static List<String> order(ReplicatorData data) {
        return data.jobs().stream().map(j -> j.recipe().getPath().substring("replicator/".length())).toList();
    }

    @Test
    void reorderMovesAWaitingJobAndNeverTheRunningOne() {
        ReplicatorData data = queueOf("a", "b", "c", "d");
        assertEquals(List.of("a", "d", "b", "c"), order(ReplicatorLogic.reorder(data, 3, 1)));
        assertEquals(List.of("a", "c", "b", "d"), order(ReplicatorLogic.reorder(data, 1, 2)));
        assertEquals(List.of("a", "c", "d", "b"), order(ReplicatorLogic.reorder(data, 1, 99)), "targets clamp to the end of the queue");
        assertEquals(List.of("a", "d", "b", "c"), order(ReplicatorLogic.reorder(data, 3, 0)), "nothing goes in front of the running job");
    }

    @Test
    void invalidOrUselessMovesChangeNothing() {
        ReplicatorData data = queueOf("a", "b", "c");
        assertSame(data, ReplicatorLogic.reorder(data, 0, 2), "the running job stays put");
        assertSame(data, ReplicatorLogic.reorder(data, -1, 1));
        assertSame(data, ReplicatorLogic.reorder(data, 3, 1), "out of range");
        assertSame(data, ReplicatorLogic.reorder(data, 1, 1), "same place");
    }

    @Test
    void queuePaysDustUpFront() {
        ReplicatorLogic.Queued queued = ReplicatorLogic.queue(UNLOCKED, RICH, ID, RECIPE);
        assertEquals(ReplicatorLogic.QueueResult.QUEUED, queued.result());
        assertEquals(1, queued.data().jobs().size());
        assertEquals(DustData.of(8, 2, 0, 0), queued.dust());
        assertEquals(RECIPE.cost(), queued.data().jobs().get(0).paid());
    }

    @Test
    void queueRejections() {
        assertEquals(ReplicatorLogic.QueueResult.UNKNOWN_RECIPE, ReplicatorLogic.queue(UNLOCKED, RICH, ID, null).result());
        assertEquals(ReplicatorLogic.QueueResult.LOCKED, ReplicatorLogic.queue(ReplicatorData.EMPTY, RICH, ID, RECIPE).result());
        assertEquals(ReplicatorLogic.QueueResult.NOT_ENOUGH_DUST,
                ReplicatorLogic.queue(UNLOCKED, DustData.of(10, 7.9f, 0, 0), ID, RECIPE).result());

        ReplicatorData data = UNLOCKED;
        DustData dust = DustData.of(100, 100, 0, 0);
        for (int i = 0; i < ReplicatorData.MAX_JOBS; i++) {
            ReplicatorLogic.Queued queued = ReplicatorLogic.queue(data, dust, ID, RECIPE);
            data = queued.data();
            dust = queued.dust();
        }
        ReplicatorLogic.Queued full = ReplicatorLogic.queue(data, dust, ID, RECIPE);
        assertEquals(ReplicatorLogic.QueueResult.QUEUE_FULL, full.result());
        assertSame(dust, full.dust(), "rejected queue must not take dust");
    }

    @Test
    void cancelRefundsWithinCapacity() {
        ReplicatorLogic.Queued queued = ReplicatorLogic.queue(UNLOCKED, RICH, ID, RECIPE);
        ReplicatorLogic.Queued cancelled = ReplicatorLogic.cancel(queued.data(), queued.dust(), 0, 200f);
        assertTrue(cancelled.data().jobs().isEmpty());
        assertEquals(RICH, cancelled.dust());
        // A full reserve can't overflow past capacity on refund.
        ReplicatorLogic.Queued capped = ReplicatorLogic.cancel(queued.data(), DustData.of(9, 9, 0, 0), 0, 10f);
        assertEquals(DustData.of(10, 10, 0, 0), capped.dust());
        // Bad index changes nothing.
        assertSame(queued.data(), ReplicatorLogic.cancel(queued.data(), queued.dust(), 3, 200f).data());
    }

    @Test
    void jobsRunOnlyWhenPoweredAndFinishInOrder() {
        ReplicatorData data = ReplicatorLogic.queue(UNLOCKED, RICH, ID, RECIPE).data();
        ReplicatorLogic.Tick unpowered = ReplicatorLogic.tick(data, false);
        assertFalse(unpowered.worked());
        assertEquals(0, unpowered.data().jobs().get(0).progress());

        ReplicatorLogic.Tick t1 = ReplicatorLogic.tick(data, true);
        ReplicatorLogic.Tick t2 = ReplicatorLogic.tick(t1.data(), true);
        assertTrue(t1.worked() && t2.worked());
        assertTrue(t2.finished().isEmpty());
        ReplicatorLogic.Tick t3 = ReplicatorLogic.tick(t2.data(), true);
        assertEquals(1, t3.finished().size());
        assertEquals(8, t3.finished().get(0).getCount());
        assertTrue(t3.finished().get(0).is(Items.STONE));
        assertTrue(t3.data().jobs().isEmpty());
        assertFalse(ReplicatorLogic.tick(t3.data(), true).worked(), "an empty queue does no work");
    }

    @Test
    void finishedJobWaitsWhileBufferIsFull() {
        List<ItemStack> buffer = new ArrayList<>();
        for (int i = 0; i < ReplicatorData.BUFFER_SLOTS; i++) {
            buffer.add(new ItemStack(Items.DIRT));
        }
        ReplicatorData data = ReplicatorLogic.queue(UNLOCKED, RICH, ID, RECIPE).data().withBuffer(buffer);
        for (int i = 0; i < 10; i++) {
            ReplicatorLogic.Tick tick = ReplicatorLogic.tick(data, true);
            assertTrue(tick.finished().isEmpty());
            data = tick.data();
        }
        assertEquals(1, data.jobs().size());
        assertTrue(data.jobs().get(0).isDone());
        // Once there's room it is delivered without more work or energy.
        ReplicatorLogic.Tick delivered = ReplicatorLogic.tick(data.withBuffer(List.of()), true);
        assertFalse(delivered.worked());
        assertEquals(8, delivered.finished().get(0).getCount());
    }

    @Test
    void kitsDeliverEveryPartAndNeedRoomForAll() {
        ReplicatorRecipe kit = new ReplicatorRecipe(List.of(new ItemStack(Items.STONE, 2), new ItemStack(Items.DIRT),
                new ItemStack(Items.GLASS)), DustCost.of(3, 0, 0, 0), 1, ReplicatorCategories.BLOCKS, Milestones.AWAKENING,
                Optional.empty());
        assertEquals(4, kit.totalCount());
        assertEquals(2, kit.count(Items.STONE));
        assertEquals(0, kit.count(Items.SAND));

        ReplicatorData queued = ReplicatorLogic.queue(UNLOCKED, RICH, ID, kit).data();
        ReplicatorLogic.Tick done = ReplicatorLogic.tick(queued, true);
        assertEquals(3, done.finished().size());
        assertTrue(done.finished().get(2).is(Items.GLASS));

        // Only two buffer slots free: the three-part kit waits rather than losing a part.
        List<ItemStack> buffer = new ArrayList<>();
        for (int i = 0; i < ReplicatorData.BUFFER_SLOTS - 2; i++) {
            buffer.add(new ItemStack(Items.DIRT));
        }
        ReplicatorLogic.Tick waiting = ReplicatorLogic.tick(queued.withBuffer(buffer), true);
        assertTrue(waiting.finished().isEmpty());
        assertEquals(1, waiting.data().jobs().size());
    }

    @Test
    void leftoversGoToTheBuffer() {
        ReplicatorData data = ReplicatorLogic.toBuffer(UNLOCKED, new ItemStack(Items.STONE, 3));
        assertEquals(1, data.buffer().size());
        assertSame(data, ReplicatorLogic.toBuffer(data, ItemStack.EMPTY));
    }

    @Test
    void dustMilestonesCountWhatWasGatheredNotWhatIsHeld() {
        DustData gift = DustData.of(25, 30, 0, 5);
        MilestoneContext gifted = new MilestoneContext(gift, DustData.EMPTY, 0, Float.MAX_VALUE, 0, java.util.Set.of());
        assertFalse(reached(Milestones.FIRST_DUST, gifted), "starter dust is not first dust");
        assertFalse(reached(Milestones.DIVERSIFIED, gifted), "starter dust does not make the AI diversified");
        MilestoneContext gathered = new MilestoneContext(DustData.EMPTY, DustData.of(1, 1, 0, 1), 0, Float.MAX_VALUE, 0, java.util.Set.of());
        assertTrue(reached(Milestones.FIRST_DUST, gathered), "gathered dust counts even after it was spent");
        assertTrue(reached(Milestones.DIVERSIFIED, gathered));
    }

    @Test
    void milestonesAndUnlockDiff() {
        assertTrue(reached(Milestones.AWAKENING, MilestoneContext.of(DustData.EMPTY)));
        assertFalse(reached(Milestones.FIRST_DUST, MilestoneContext.of(DustData.EMPTY)));
        assertTrue(reached(Milestones.FIRST_DUST, MilestoneContext.of(DustData.of(0, 0, 0.01f, 0))));

        assertFalse(reached(Milestones.FIRST_STRUCTURE, new MilestoneContext(DustData.EMPTY, Milestones.STRUCTURE_BLOCKS - 1)));
        assertTrue(reached(Milestones.FIRST_STRUCTURE, new MilestoneContext(DustData.EMPTY, Milestones.STRUCTURE_BLOCKS)));
        assertFalse(reached(Milestones.DIVERSIFIED, MilestoneContext.of(DustData.of(5, 5, 0, 0))));
        assertTrue(reached(Milestones.DIVERSIFIED, MilestoneContext.of(DustData.of(5, 5, 0, 0.1f))));

        ReplicatorData before = UNLOCKED;
        ReplicatorData after = before.withUnlocked(Milestones.FIRST_DUST);
        assertEquals(List.of(Milestones.FIRST_DUST), ReplicatorData.newlyUnlocked(before, after));
        assertEquals(List.of(), ReplicatorData.newlyUnlocked(null, after), "initial sync must not toast");
        assertSame(after, after.withUnlocked(Milestones.FIRST_DUST));
    }

    @Test
    void builtinCriteria() {
        assertFalse(reached(Milestones.EXPANSION, new MilestoneContext(DustData.of(199, 0, 0, 0), 0, 200)));
        assertTrue(reached(Milestones.EXPANSION, new MilestoneContext(DustData.of(0, 0, 200, 0), 0, 200)));

        MilestoneContext some = MilestoneContext.of(DustData.of(5, 3, 0, 0));
        assertTrue(new MilestoneCriteria.DustCollected(Optional.empty(), 8).isReached(some), "total of all kinds");
        assertFalse(new MilestoneCriteria.DustCollected(Optional.of(DustKinds.METALLIC), 6).isReached(some));
        assertTrue(new MilestoneCriteria.DustCollected(Optional.of(DustKinds.METALLIC), 5).isReached(some));
        assertFalse(new MilestoneCriteria.DustFull(Optional.of(DustKinds.ICE)).isReached(
                new MilestoneContext(DustData.of(200, 0, 0, 0), 0, 200)), "full of the wrong kind");
    }

    private static boolean reached(ResourceKey<Milestone> key, MilestoneContext context) {
        return Milestones.BUILTIN.get(key).isReached(context);
    }

    @Test
    void dustCostArithmetic() {
        DustCost cost = DustCost.of(4, 2, 0, 1);
        assertTrue(cost.canAfford(DustData.of(4, 2, 0, 1)));
        assertFalse(cost.canAfford(DustData.of(4, 2, 0, 0.5f)));
        assertEquals(DustData.of(1, 0, 3, 0), cost.takeFrom(DustData.of(5, 2, 3, 1)));
        assertEquals(DustCost.of(2, 1, 0, 0.5f), cost.scale(0.5f));
        assertTrue(DustCost.NONE.isFree());
    }

    @Test
    void savedDataToleratesMissingAndBrokenFields() {
        // Old saves without newer fields load with defaults.
        ReplicatorData old = ReplicatorData.CODEC.parse(JsonOps.INSTANCE, JsonParser.parseString(
                "{\"unlocked\": [\"awakening\"]}")).getOrThrow();
        assertEquals(List.of(Milestones.AWAKENING), old.unlocked());
        assertEquals(0, old.blocksPlaced());
        // Unknown milestones are kept (a datapack may add them back); a field that no longer parses resets alone.
        ReplicatorData unknown = ReplicatorData.CODEC.parse(JsonOps.INSTANCE, JsonParser.parseString(
                "{\"unlocked\": [\"addon:no_such_milestone\"]}")).getOrThrow();
        assertEquals(1, unknown.unlocked().size());
        ReplicatorData broken = ReplicatorData.CODEC.parse(JsonOps.INSTANCE, JsonParser.parseString(
                "{\"unlocked\": [42], \"blocks_placed\": 7}")).getOrThrow();
        assertTrue(broken.unlocked().isEmpty());
        assertEquals(7, broken.blocksPlaced());
        assertEquals(com.voidreclaimer.flight.FlightSettings.DEFAULT,
                com.voidreclaimer.flight.FlightSettings.CODEC.parse(JsonOps.INSTANCE, JsonParser.parseString("{}")).getOrThrow());
    }

    @Test
    void recipeJsonRoundTrips() {
        String json = """
                {"result": {"id": "minecraft:stone", "count": 8}, "cost": {"silicate": 8.0},
                 "ticks": 100, "category": "blocks", "milestone": "awakening"}""";
        ReplicatorRecipe parsed = ReplicatorRecipe.CODEC.codec().parse(JsonOps.INSTANCE, JsonParser.parseString(json)).getOrThrow();
        assertEquals(DustCost.of(0, 8, 0, 0), parsed.cost());
        assertEquals(100, parsed.ticks());
        assertEquals(Milestones.AWAKENING, parsed.milestone());
        assertTrue(ItemStack.matches(new ItemStack(Items.STONE, 8), parsed.result()));

        ReplicatorRecipe kit = ReplicatorRecipe.CODEC.codec().parse(JsonOps.INSTANCE, JsonParser.parseString("""
                {"results": [{"id": "minecraft:stone"}, {"id": "minecraft:dirt", "count": 2}], "cost": {"metallic": 1.0},
                 "ticks": 20, "category": "blocks", "milestone": "awakening", "title": {"text": "Kit"}}""")).getOrThrow();
        assertEquals(2, kit.results().size());
        assertEquals("Kit", kit.displayName().getString());
        assertTrue(ReplicatorRecipe.CODEC.codec().parse(JsonOps.INSTANCE, JsonParser.parseString("""
                {"results": [], "cost": {}, "ticks": 20, "category": "blocks", "milestone": "awakening"}""")).isError(),
                "a recipe must make something");
    }
}
