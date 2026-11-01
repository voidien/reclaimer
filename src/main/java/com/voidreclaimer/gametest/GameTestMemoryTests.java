package com.voidreclaimer.gametest;

import com.voidreclaimer.VoidReclaimer;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/** The memory of a run: each batch lets go of the chunks of the one before (GameTestBatchChunkMixin). */
@GameTestHolder(VoidReclaimer.MOD_ID)
@PrefixGameTestTemplate(false)
public final class GameTestMemoryTests {
    private static final BlockPos FIRST = new BlockPos(263_000, 80, 0);
    private static final BlockPos SECOND = new BlockPos(263_400, 80, 0);

    private GameTestMemoryTests() {}

    /** Each forces its own chunk and checks the other's, so it does not matter which batch comes first: the later one proves it. */
    private static void forcesAndChecks(GameTestHelper helper, BlockPos mine, BlockPos other) {
        ServerLevel space = TestPlayers.deepSpace(helper);
        helper.assertFalse(space.getForcedChunks().contains(ChunkPos.asLong(other)), "a chunk of the last batch is still forced");
        space.setChunkForced(mine.getX() >> 4, mine.getZ() >> 4, true);
        helper.succeed();
    }

    @GameTest(template = "empty", batch = "memory_first")
    public static void aBatchLetsGoOfTheChunksOfTheOneBefore(GameTestHelper helper) {
        forcesAndChecks(helper, FIRST, SECOND);
    }

    @GameTest(template = "empty", batch = "memory_second")
    public static void aBatchLetsGoOfTheChunksOfTheOneAfter(GameTestHelper helper) {
        forcesAndChecks(helper, SECOND, FIRST);
    }
}
