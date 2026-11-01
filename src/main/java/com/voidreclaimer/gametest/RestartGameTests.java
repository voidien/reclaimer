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

    private static final UUID WOKEN_PLAYER = new UUID(0x7E57L, 0xB0B2L);
    private static final BlockPos WOKEN_SPOT = new BlockPos(75_700, 80, 0);

    /** A shell put back in the world reaches the disk with its chunk: a crash first must not cost the player that body. */
    @GameTest(template = "empty", timeoutTicks = 300)
    public static void aWokenShellOutlivesACrashBeforeRestart(GameTestHelper helper) {
        ServerLevel space = TestPlayers.deepSpace(helper);
        NaniteGameTests.inLoadedArea(helper, WOKEN_SPOT, 1, () -> {
            ServerPlayer player = TestPlayers.joinAs(helper, WOKEN_PLAYER);
            player.setData(ModAttachments.LINK, LinkState.NONE);
            player.teleportTo(space, WOKEN_SPOT.getX() + 0.5, WOKEN_SPOT.getY(), WOKEN_SPOT.getZ() + 0.5, 0f, 0f);
            player.gameMode.changeGameModeForPlayer(GameType.SURVIVAL);
            BodyHost shell = BodyHost.spawn(space, player.position().add(0.0, 0.0, 4.0), HostRole.SHELL, Bodies.preset(BuiltinBodies.DRONE), player.getUUID());
            helper.assertTrue(Links.link(player, shell) == Links.Result.LINKED, "no link");
            helper.assertTrue(Links.wake(player) == Links.Result.WOKEN, "no wake");
            RestartTests.crash();
        });
    }

    @GameTest(template = "empty", timeoutTicks = 300)
    public static void aWokenShellOutlivesACrashAfterRestart(GameTestHelper helper) {
        ServerLevel space = TestPlayers.deepSpace(helper);
        ServerPlayer player = TestPlayers.joinAs(helper, WOKEN_PLAYER);
        LinkState state = Links.state(player);
        helper.assertTrue(state.outboxes().size() == 1, "the player file holds " + state.outboxes().size() + " outboxes");
        UUID id = state.outboxes().get(0).shell();
        helper.startSequence()
                .thenWaitUntil(() -> helper.assertTrue(space.getEntity(id) instanceof BodyHost, "the shell is lost"))
                .thenExecute(() -> {
                    space.getEntity(id).discard();
                    player.setData(ModAttachments.LINK, LinkState.NONE);
                    TestPlayers.remove(player);
                    helper.succeed();
                });
    }

    private static final UUID STALE_PLAYER = new UUID(0x7E57L, 0xB0B3L);
    private static final BlockPos STALE_SPOT = new BlockPos(75_900, 80, 0);

    /** The chunk reaches the disk with the shell in it, and the crash comes after the link: that copy must not walk again. */
    @GameTest(template = "empty", timeoutTicks = 300)
    public static void anOldShellGoesAfterACrashBeforeRestart(GameTestHelper helper) {
        ServerLevel space = TestPlayers.deepSpace(helper);
        NaniteGameTests.inLoadedArea(helper, STALE_SPOT, 1, () -> {
            ServerPlayer player = TestPlayers.joinAs(helper, STALE_PLAYER);
            player.setData(ModAttachments.LINK, LinkState.NONE);
            player.teleportTo(space, STALE_SPOT.getX() + 0.5, STALE_SPOT.getY(), STALE_SPOT.getZ() + 0.5, 0f, 0f);
            player.gameMode.changeGameModeForPlayer(GameType.SURVIVAL);
            BodyHost shell = BodyHost.spawn(space, player.position().add(0.0, 0.0, 4.0), HostRole.SHELL, Bodies.preset(BuiltinBodies.DRONE), player.getUUID());
            helper.runAfterDelay(5, () -> {
                space.save(null, true, false);
                helper.assertTrue(Links.link(player, shell) == Links.Result.LINKED, "no link");
                RestartTests.crash();
            });
        });
    }

    @GameTest(template = "empty", timeoutTicks = 300)
    public static void anOldShellGoesAfterACrashAfterRestart(GameTestHelper helper) {
        ServerLevel space = TestPlayers.deepSpace(helper);
        ServerPlayer player = TestPlayers.joinAs(helper, STALE_PLAYER);
        LinkState state = Links.state(player);
        helper.assertTrue(state.linked(), "the player file does not know about the link");
        UUID id = state.link().orElseThrow().shell();
        // Waits for the chunk's entities to load and then past the shell's own check, which runs once a second.
        NaniteGameTests.inEntityTickingArea(helper, STALE_SPOT, 1, () -> helper.runAfterDelay(60, () -> {
            helper.assertTrue(space.getEntity(id) == null, "the old copy of the shell came back");
            player.setData(ModAttachments.LINK, LinkState.NONE);
            TestPlayers.remove(player);
            helper.succeed();
        }));
    }
}
