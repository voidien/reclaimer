package com.voidreclaimer.gametest;

import com.voidreclaimer.VoidReclaimer;
import com.voidreclaimer.earth.AtmosphericPortalBlock;
import com.voidreclaimer.earth.AtmosphericPortals;
import com.voidreclaimer.registry.ModBlocks;
import com.voidreclaimer.registry.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/** Phase 11 slice: building, lighting and breaking an Atmospheric Portal frame. */
@GameTestHolder(VoidReclaimer.MOD_ID)
@PrefixGameTestTemplate(false)
public final class PortalGameTests {
    private static final int WIDTH = 12;
    private static final int HEIGHT = 6;

    private PortalGameTests() {}

    /** A ring of frame blocks around a WIDTH x HEIGHT interior whose bottom-left block is {@code origin}, width along X. */
    private static void buildFrame(ServerLevel level, BlockPos origin) {
        for (int across = -1; across <= WIDTH; across++) {
            for (int up = -1; up <= HEIGHT; up++) {
                boolean interior = across >= 0 && across < WIDTH && up >= 0 && up < HEIGHT;
                boolean corner = (across == -1 || across == WIDTH) && (up == -1 || up == HEIGHT);
                if (!interior && !corner) {
                    level.setBlock(origin.east(across).above(up), ModBlocks.PORTAL_FRAME.get().defaultBlockState(), 3);
                }
            }
        }
    }

    private static void clear(ServerLevel level, BlockPos origin) {
        for (int across = -2; across <= WIDTH + 1; across++) {
            for (int up = -2; up <= HEIGHT + 1; up++) {
                level.setBlock(origin.east(across).above(up), Blocks.AIR.defaultBlockState(), 2);
            }
        }
    }

    private static int panes(ServerLevel level, BlockPos origin) {
        int count = 0;
        for (int across = 0; across < WIDTH; across++) {
            for (int up = 0; up < HEIGHT; up++) {
                if (level.getBlockState(origin.east(across).above(up)).is(ModBlocks.ATMOSPHERIC_PORTAL.get())) {
                    count++;
                }
            }
        }
        return count;
    }

    @GameTest(template = "empty", timeoutTicks = 200)
    public static void aShipSizedFrameLightsAndBreaksTogether(GameTestHelper helper) {
        ServerLevel space = TestPlayers.deepSpace(helper);
        BlockPos origin = new BlockPos(13_000, 64, 0);
        space.setChunkForced(origin.getX() >> 4, origin.getZ() >> 4, true);
        space.setChunkForced((origin.getX() + WIDTH) >> 4, origin.getZ() >> 4, true);
        helper.runAfterDelay(5, () -> {
            try {
                clear(space, origin);
                buildFrame(space, origin);
                helper.assertTrue(AtmosphericPortals.findFrame(space, origin.below()).isPresent(), "frame not found from its own block");
                helper.assertTrue(AtmosphericPortals.tryLight(space, origin.below()), "frame did not light");
                helper.assertTrue(panes(space, origin) == WIDTH * HEIGHT, "wrong pane count: " + panes(space, origin));
                helper.assertTrue(space.getBlockState(origin).getValue(AtmosphericPortalBlock.AXIS) == Direction.Axis.X, "wrong axis");

                // Breaking any frame block takes the whole portal with it.
                space.setBlock(origin.above(HEIGHT).east(4), Blocks.AIR.defaultBlockState(), 3);
                helper.assertTrue(panes(space, origin) == 0, "panes left after the frame broke: " + panes(space, origin));
                helper.succeed();
            } finally {
                clear(space, origin);
                space.setChunkForced(origin.getX() >> 4, origin.getZ() >> 4, false);
                space.setChunkForced((origin.getX() + WIDTH) >> 4, origin.getZ() >> 4, false);
            }
        });
    }

    @GameTest(template = "empty", timeoutTicks = 200)
    public static void aBrokenFrameDoesNotLight(GameTestHelper helper) {
        ServerLevel space = TestPlayers.deepSpace(helper);
        BlockPos origin = new BlockPos(13_100, 64, 0);
        space.setChunkForced(origin.getX() >> 4, origin.getZ() >> 4, true);
        space.setChunkForced((origin.getX() + WIDTH) >> 4, origin.getZ() >> 4, true);
        helper.runAfterDelay(5, () -> {
            try {
                clear(space, origin);
                buildFrame(space, origin);
                space.setBlock(origin.west().above(2), Blocks.AIR.defaultBlockState(), 3);
                helper.assertFalse(AtmosphericPortals.tryLight(space, origin.below()), "a frame with a gap lit");
                helper.assertTrue(panes(space, origin) == 0, "panes appeared");
                helper.succeed();
            } finally {
                clear(space, origin);
                space.setChunkForced(origin.getX() >> 4, origin.getZ() >> 4, false);
                space.setChunkForced((origin.getX() + WIDTH) >> 4, origin.getZ() >> 4, false);
            }
        });
    }

    @GameTest(template = "empty", timeoutTicks = 200)
    public static void theIgniterLightsTheFrameAndIsUsedUp(GameTestHelper helper) {
        ServerLevel space = TestPlayers.deepSpace(helper);
        BlockPos origin = new BlockPos(13_200, 64, 0);
        space.setChunkForced(origin.getX() >> 4, origin.getZ() >> 4, true);
        space.setChunkForced((origin.getX() + WIDTH) >> 4, origin.getZ() >> 4, true);
        helper.runAfterDelay(5, () -> {
            try {
                clear(space, origin);
                buildFrame(space, origin);
                ServerPlayer player = TestPlayers.join(helper);
                ItemStack igniter = new ItemStack(ModItems.PORTAL_IGNITER.get(), 2);
                player.setItemInHand(InteractionHand.MAIN_HAND, igniter);
                BlockPos clicked = origin.below().east(3);
                BlockHitResult hit = new BlockHitResult(Vec3.atCenterOf(clicked).add(0, 0.5, 0), Direction.UP, clicked, false);
                // Survival players use the item up; creative ones keep it.
                player.setGameMode(net.minecraft.world.level.GameType.SURVIVAL);
                InteractionResult result = igniter.useOn(new UseOnContext(space, player, InteractionHand.MAIN_HAND, igniter, hit));
                helper.assertTrue(result.consumesAction(), "igniter did nothing: " + result);
                helper.assertTrue(panes(space, origin) == WIDTH * HEIGHT, "igniter did not fill the frame");
                helper.assertTrue(igniter.getCount() == 1, "igniter not used up: " + igniter.getCount());
                helper.succeed();
            } finally {
                clear(space, origin);
                space.setChunkForced(origin.getX() >> 4, origin.getZ() >> 4, false);
                space.setChunkForced((origin.getX() + WIDTH) >> 4, origin.getZ() >> 4, false);
            }
        });
    }
}
