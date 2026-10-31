package com.voidreclaimer.gametest;

import com.voidreclaimer.VoidReclaimer;
import com.voidreclaimer.debug.ChunkReadout;
import com.voidreclaimer.mixin.ChunkMapAccessor;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ChunkTaskPriorityQueueSorter;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/** The GameTest server itself: chunk work has to keep up with the ticks, or a forced chunk outlasts its test. */
@GameTestHolder(VoidReclaimer.MOD_ID)
@PrefixGameTestTemplate(false)
public final class ChunkTaskGameTests {
    private static final int TASKS = 40;
    private static final int TICKS = 5;

    private ChunkTaskGameTests() {}

    /**
     * Each task is for its own chunk, as a chunk reaching full status is, so the queue hands them to the main thread one
     * at a time. It starts once the queue is empty: chunks an earlier run left forced are in it at first, and what is
     * measured here is the rate, not the length of the queue.
     */
    @GameTest(template = "empty", timeoutTicks = 400)
    public static void chunkTasksAreNotLimitedToOnePerTick(GameTestHelper helper) {
        ServerLevel space = TestPlayers.deepSpace(helper);
        ChunkMapAccessor chunks = (ChunkMapAccessor) space.getChunkSource().chunkMap;
        if (chunks.void_reclaimer$queueSorter().hasWork() || space.getChunkSource().getPendingTasksCount() > 0) {
            helper.runAfterDelay(1, () -> chunkTasksAreNotLimitedToOnePerTick(helper));
            return;
        }
        AtomicInteger done = new AtomicInteger();
        for (int i = 0; i < TASKS; i++) {
            chunks.void_reclaimer$mainThreadMailbox().tell(
                    ChunkTaskPriorityQueueSorter.message(done::incrementAndGet, ChunkPos.asLong(90_000 + i, 90_000), () -> 0));
        }
        helper.runAfterDelay(TICKS, () -> {
            helper.assertTrue(done.get() == TASKS, done.get() + " of " + TASKS + " chunk tasks ran in " + TICKS + " ticks");
            helper.succeed();
        });
    }

    /** A chunk nothing holds says so, and one forced and ticking says that: the readout has to tell the two apart. */
    @GameTest(template = "empty", timeoutTicks = 300)
    public static void theChunkReadoutTellsATickingChunkFromAnUnloadedOne(GameTestHelper helper) {
        BlockPos spot = new BlockPos(76_000, 80, 0);
        NaniteGameTests.inEntityTickingArea(helper, spot, 1, () -> {
            ServerLevel space = TestPlayers.deepSpace(helper);
            List<String> held = ChunkReadout.of(space, new ChunkPos(spot));
            List<String> nowhere = ChunkReadout.of(space, new ChunkPos(1_000_000, 1_000_000));
            helper.assertTrue(held.get(1).startsWith("Level ") && held.get(2).equals("Entities: loaded, ticking"), "a forced chunk: " + held);
            helper.assertTrue(nowhere.get(1).equals("Not loaded") && nowhere.get(2).equals("Entities: not loaded, not ticking"), "no chunk: " + nowhere);
            helper.assertTrue(held.get(3).matches("Chunks here: [1-9].*"), "no chunks counted: " + held);
            helper.succeed();
        });
    }
}
