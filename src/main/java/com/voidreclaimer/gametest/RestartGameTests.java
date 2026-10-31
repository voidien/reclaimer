package com.voidreclaimer.gametest;

import com.voidreclaimer.VoidReclaimer;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/** Keeps {@link RestartTests#crash} honest: a crash has to cost what was not saved, or a crash test proves nothing. */
@GameTestHolder(VoidReclaimer.MOD_ID)
@PrefixGameTestTemplate(false)
public final class RestartGameTests {
    private static final BlockPos SPOT = new BlockPos(75_000, 80, 0);

    private RestartGameTests() {}

    @GameTest(template = "empty", timeoutTicks = 300)
    public static void aCrashKeepsOnlyWhatWasSavedBeforeRestart(GameTestHelper helper) {
        ServerLevel space = TestPlayers.deepSpace(helper);
        NaniteGameTests.inLoadedArea(helper, SPOT, 1, () -> {
            space.setBlock(SPOT, Blocks.DIAMOND_BLOCK.defaultBlockState(), 3);
            space.getChunkSource().save(true);
            space.setBlock(SPOT, Blocks.GOLD_BLOCK.defaultBlockState(), 3);
            RestartTests.crash();
        });
    }

    @GameTest(template = "empty", timeoutTicks = 300)
    public static void aCrashKeepsOnlyWhatWasSavedAfterRestart(GameTestHelper helper) {
        ServerLevel space = TestPlayers.deepSpace(helper);
        var found = space.getChunk(SPOT).getBlockState(SPOT);
        helper.assertTrue(found.is(Blocks.DIAMOND_BLOCK), "the crash should have cost the gold and kept the diamond: " + found);
        helper.succeed();
    }
}
