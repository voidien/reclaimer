package com.voidreclaimer.gametest;

import com.voidreclaimer.VoidReclaimer;
import com.voidreclaimer.body.Bodies;
import com.voidreclaimer.body.BuiltinBodies;
import com.voidreclaimer.link.BodyHost;
import com.voidreclaimer.link.HostRole;
import com.voidreclaimer.link.LinkState;
import com.voidreclaimer.link.Links;
import com.voidreclaimer.registry.ModAttachments;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.GameType;
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

    private static final UUID LINKED_PLAYER = new UUID(7L, 7L);
    private static final BlockPos LINK_SPOT = new BlockPos(75_500, 80, 0);

    /** A move ends by saving the player file: a crash right after it must find the link, not a main body the shell has left. */
    @GameTest(template = "empty", timeoutTicks = 300)
    public static void aLinkOutlivesACrashBeforeRestart(GameTestHelper helper) {
        ServerLevel space = TestPlayers.deepSpace(helper);
        NaniteGameTests.inLoadedArea(helper, LINK_SPOT, 1, () -> {
            ServerPlayer player = TestPlayers.joinAs(helper, LINKED_PLAYER);
            player.setData(ModAttachments.LINK, LinkState.NONE);
            player.teleportTo(space, LINK_SPOT.getX() + 0.5, LINK_SPOT.getY(), LINK_SPOT.getZ() + 0.5, 0f, 0f);
            player.gameMode.changeGameModeForPlayer(GameType.SURVIVAL);
            BodyHost shell = BodyHost.spawn(space, player.position().add(0.0, 0.0, 4.0), HostRole.SHELL, Bodies.preset(BuiltinBodies.DRONE), player.getUUID());
            helper.assertTrue(Links.link(player, shell) == Links.Result.LINKED, "no link");
            RestartTests.crash();
        });
    }

    @GameTest(template = "empty", timeoutTicks = 300)
    public static void aLinkOutlivesACrashAfterRestart(GameTestHelper helper) {
        ServerPlayer player = TestPlayers.joinAs(helper, LINKED_PLAYER);
        LinkState state = Links.state(player);
        player.setData(ModAttachments.LINK, LinkState.NONE);
        TestPlayers.remove(player);
        helper.assertTrue(state.linked(), "the player file does not know about the link");
        helper.succeed();
    }
}
