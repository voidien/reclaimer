package com.voidreclaimer.gametest;

import com.voidreclaimer.VoidReclaimer;
import com.voidreclaimer.registry.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/** Mining by hand in zero-G is not slowed by vanilla's "not on the ground" penalty. */
@GameTestHolder(VoidReclaimer.MOD_ID)
@PrefixGameTestTemplate(false)
public final class ZeroGMiningGameTests {
    private ZeroGMiningGameTests() {}

    @GameTest(template = "empty", timeoutTicks = 60)
    public static void handMiningInZeroGIsNotSlowed(GameTestHelper helper) {
        ServerPlayer player = TestPlayers.survivalInDeepSpace(helper, 95_000.5, 80, 0.5);
        try {
            BlockState scaffold = ModBlocks.SCAFFOLD.get().defaultBlockState();
            BlockPos pos = new BlockPos(95_002, 80, 0);
            helper.assertTrue(!player.onGround(), "the test player stands on something");
            float speed = player.getDigSpeed(scaffold, pos);
            helper.assertTrue(speed >= 1f - 1e-4f, "bare-hand mining speed in zero-G was " + speed + ", vanilla's airborne penalty is still applied");
            helper.succeed();
        } finally {
            TestPlayers.remove(player);
        }
    }

    @GameTest(template = "empty", timeoutTicks = 60)
    public static void airborneMiningWhereThereIsGravityKeepsTheVanillaPenalty(GameTestHelper helper) {
        ServerPlayer player = TestPlayers.join(helper);
        try {
            player.setPos(95_000.5, 200, 0.5);
            helper.assertTrue(!player.onGround(), "the test player stands on something");
            BlockState scaffold = ModBlocks.SCAFFOLD.get().defaultBlockState();
            float speed = player.getDigSpeed(scaffold, new BlockPos(95_002, 200, 0));
            helper.assertTrue(Math.abs(speed - 0.2f) < 1e-3f, "airborne mining speed outside zero-G was " + speed + ", expected vanilla's 0.2");
            helper.succeed();
        } finally {
            TestPlayers.remove(player);
        }
    }
}
