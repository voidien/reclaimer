package com.voidreclaimer.replicator;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import com.voidreclaimer.dust.DustData;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.junit.jupiter.api.Test;

class ReplicatorLogicTest {
    private static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath("void_reclaimer", "replicator/test");
    private static final ReplicatorRecipe RECIPE = new ReplicatorRecipe(new ItemStack(Items.STONE, 8),
            new DustCost(2, 8, 0, 0), 3, ReplicatorRecipe.Category.BLOCKS, Milestone.AWAKENING);
    private static final ReplicatorData UNLOCKED = ReplicatorData.EMPTY.withUnlocked(Milestone.AWAKENING);
    private static final DustData RICH = new DustData(10, 10, 0, 0);

    @Test
    void queuePaysDustUpFront() {
        ReplicatorLogic.Queued queued = ReplicatorLogic.queue(UNLOCKED, RICH, ID, RECIPE);
        assertEquals(ReplicatorLogic.QueueResult.QUEUED, queued.result());
        assertEquals(1, queued.data().jobs().size());
        assertEquals(new DustData(8, 2, 0, 0), queued.dust());
        assertEquals(RECIPE.cost(), queued.data().jobs().get(0).paid());
    }

    @Test
    void queueRejections() {
        assertEquals(ReplicatorLogic.QueueResult.UNKNOWN_RECIPE, ReplicatorLogic.queue(UNLOCKED, RICH, ID, null).result());
        assertEquals(ReplicatorLogic.QueueResult.LOCKED, ReplicatorLogic.queue(ReplicatorData.EMPTY, RICH, ID, RECIPE).result());
        assertEquals(ReplicatorLogic.QueueResult.NOT_ENOUGH_DUST,
                ReplicatorLogic.queue(UNLOCKED, new DustData(10, 7.9f, 0, 0), ID, RECIPE).result());

        ReplicatorData data = UNLOCKED;
        DustData dust = new DustData(100, 100, 0, 0);
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
        ReplicatorLogic.Queued capped = ReplicatorLogic.cancel(queued.data(), new DustData(9, 9, 0, 0), 0, 10f);
        assertEquals(new DustData(10, 10, 0, 0), capped.dust());
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
        assertEquals(8, t3.finished().getCount());
        assertTrue(t3.finished().is(Items.STONE));
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
        assertEquals(8, delivered.finished().getCount());
    }

    @Test
    void leftoversGoToTheBuffer() {
        ReplicatorData data = ReplicatorLogic.toBuffer(UNLOCKED, new ItemStack(Items.STONE, 3));
        assertEquals(1, data.buffer().size());
        assertSame(data, ReplicatorLogic.toBuffer(data, ItemStack.EMPTY));
    }

    @Test
    void milestonesAndUnlockDiff() {
        assertTrue(Milestone.AWAKENING.isReached(MilestoneContext.of(DustData.EMPTY)));
        assertFalse(Milestone.FIRST_DUST.isReached(MilestoneContext.of(DustData.EMPTY)));
        assertTrue(Milestone.FIRST_DUST.isReached(MilestoneContext.of(new DustData(0, 0, 0.01f, 0))));

        assertFalse(Milestone.FIRST_STRUCTURE.isReached(new MilestoneContext(DustData.EMPTY, Milestone.STRUCTURE_BLOCKS - 1)));
        assertTrue(Milestone.FIRST_STRUCTURE.isReached(new MilestoneContext(DustData.EMPTY, Milestone.STRUCTURE_BLOCKS)));
        assertFalse(Milestone.DIVERSIFIED.isReached(MilestoneContext.of(new DustData(5, 5, 0, 0))));
        assertTrue(Milestone.DIVERSIFIED.isReached(MilestoneContext.of(new DustData(5, 5, 0, 0.1f))));

        ReplicatorData before = UNLOCKED;
        ReplicatorData after = before.withUnlocked(Milestone.FIRST_DUST);
        assertEquals(List.of(Milestone.FIRST_DUST), ReplicatorData.newlyUnlocked(before, after));
        assertEquals(List.of(), ReplicatorData.newlyUnlocked(null, after), "initial sync must not toast");
        assertSame(after, after.withUnlocked(Milestone.FIRST_DUST));
    }

    @Test
    void dustCostArithmetic() {
        DustCost cost = new DustCost(4, 2, 0, 1);
        assertTrue(cost.canAfford(new DustData(4, 2, 0, 1)));
        assertFalse(cost.canAfford(new DustData(4, 2, 0, 0.5f)));
        assertEquals(new DustData(1, 0, 3, 0), cost.takeFrom(new DustData(5, 2, 3, 1)));
        assertEquals(new DustCost(2, 1, 0, 0.5f), cost.scale(0.5f));
        assertTrue(DustCost.NONE.isFree());
    }

    @Test
    void savedDataToleratesMissingAndBrokenFields() {
        // Old saves without newer fields load with defaults.
        ReplicatorData old = ReplicatorData.CODEC.parse(JsonOps.INSTANCE, JsonParser.parseString(
                "{\"unlocked\": [\"awakening\"]}")).getOrThrow();
        assertEquals(List.of(Milestone.AWAKENING), old.unlocked());
        assertEquals(0, old.blocksPlaced());
        // A field that no longer parses resets alone instead of failing the whole attachment.
        ReplicatorData broken = ReplicatorData.CODEC.parse(JsonOps.INSTANCE, JsonParser.parseString(
                "{\"unlocked\": [\"no_such_milestone\"], \"blocks_placed\": 7}")).getOrThrow();
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
        assertEquals(new DustCost(0, 8, 0, 0), parsed.cost());
        assertEquals(100, parsed.ticks());
        assertEquals(Milestone.AWAKENING, parsed.milestone());
        assertTrue(ItemStack.matches(new ItemStack(Items.STONE, 8), parsed.result()));
    }
}
