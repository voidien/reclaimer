package com.voidreclaimer.gametest;

import com.voidreclaimer.VoidReclaimer;
import com.voidreclaimer.body.Bodies;
import com.voidreclaimer.body.BuiltinBodies;
import com.voidreclaimer.link.BodyHost;
import com.voidreclaimer.link.HostRole;
import com.voidreclaimer.link.LinkState;
import com.voidreclaimer.link.Links;
import com.voidreclaimer.link.ShellReach;
import com.voidreclaimer.registry.ModAttachments;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.BiConsumer;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/** A link to a shell by its id, wherever it is. */
@GameTestHolder(VoidReclaimer.MOD_ID)
@PrefixGameTestTemplate(false)
public final class LinkReachGameTests {
    private static final Vec3 AWAY = new Vec3(0.0, 0.0, 4.0);
    private static final int NOW = 0;

    private LinkReachGameTests() {}

    private static ServerPlayer player(GameTestHelper helper, BlockPos origin) {
        return TestPlayers.survivalInDeepSpace(helper, origin.getX() + 0.5, origin.getY(), origin.getZ() + 0.5);
    }

    private static BodyHost beside(ServerPlayer player, Vec3 where) {
        return BodyHost.spawn(player.serverLevel(), where, HostRole.SHELL, Bodies.preset(BuiltinBodies.DRONE), player.getUUID());
    }

    private static void finish(GameTestHelper helper, ServerPlayer player) {
        player.setData(ModAttachments.LINK, LinkState.NONE);
        TestPlayers.remove(player);
        helper.succeed();
    }

    /** A player, and a shell of theirs at {@code far} that has been left there and unloaded. */
    private static void farShell(GameTestHelper helper, BlockPos origin, Vec3 far, BiConsumer<ServerPlayer, UUID> test) {
        NaniteGameTests.inEntityTickingArea(helper, origin, 1, () -> {
            ServerPlayer player = player(helper, origin);
            ServerLevel level = player.serverLevel();
            ChunkPos chunk = new ChunkPos(BlockPos.containing(far));
            helper.assertFalse(level.isLoaded(BlockPos.containing(far)), "the test's far chunk is loaded already");
            level.setChunkForced(chunk.x, chunk.z, true);
            UUID[] id = new UUID[1];
            helper.startSequence()
                    .thenWaitUntil(() -> helper.assertTrue(level.areEntitiesLoaded(chunk.toLong()), "the far chunk did not load"))
                    .thenExecute(() -> {
                        id[0] = beside(player, far).getUUID();
                        level.setChunkForced(chunk.x, chunk.z, false);
                    })
                    .thenWaitUntil(() -> helper.assertTrue(level.getEntity(id[0]) == null, "the far shell did not unload"))
                    .thenExecute(() -> test.accept(player, id[0]));
        });
    }

    @GameTest(template = "empty", timeoutTicks = 900)
    public static void aLinkReachesAShellInAnUnloadedChunk(GameTestHelper helper) {
        Vec3 far = new Vec3(144_400.5, 80, 800.5);
        farShell(helper, new BlockPos(144_000, 80, 0), far, (player, id) -> {
            List<Links.Result> heard = new ArrayList<>();
            Links.requestLink(player, id, heard::add);
            helper.startSequence()
                    .thenWaitUntil(() -> helper.assertFalse(heard.isEmpty(), "nothing came of the request"))
                    .thenExecute(() -> {
                        helper.assertTrue(heard.get(0) == Links.Result.LINKED, "the link ended " + heard.get(0));
                        helper.assertTrue(player.position().distanceTo(far) < 1.0, "the player is at " + player.position());
                        helper.assertTrue(Links.state(player).link().filter(link -> link.shell().equals(id)).isPresent(), "not in the shell");
                        helper.assertTrue(Links.wake(player) == Links.Result.WOKEN, "no wake");
                        player.serverLevel().getEntity(id).discard();
                        finish(helper, player);
                    });
        });
    }

    /** A shell the list has wrong: the chunks load, it is not there, and nothing has changed. */
    @GameTest(template = "empty", timeoutTicks = 400)
    public static void aShellThatIsNotThereIsNotFound(GameTestHelper helper) {
        BlockPos origin = new BlockPos(144_200, 80, 0);
        NaniteGameTests.inEntityTickingArea(helper, origin, 1, () -> {
            ServerPlayer player = player(helper, origin);
            UUID id = new UUID(144_200L, 1L);
            Vec3 where = player.position();
            LinkState.Place at = new LinkState.Place(player.level().dimension(), new Vec3(144_600.5, 80, 800.5), 0f, 0f);
            LinkState state = LinkState.NONE.withKnown(new LinkState.KnownShell(id, 1L, at, false, false));
            player.setData(ModAttachments.LINK, state);
            List<Links.Result> heard = new ArrayList<>();
            Links.requestLink(player, id, heard::add);
            helper.startSequence()
                    .thenWaitUntil(() -> helper.assertFalse(heard.isEmpty(), "nothing came of the request"))
                    .thenExecute(() -> {
                        helper.assertTrue(heard.get(0) == Links.Result.NOT_FOUND, "the link ended " + heard.get(0));
                        helper.assertTrue(player.position().distanceTo(where) < 0.01, "the player moved");
                        helper.assertTrue(Links.state(player).equals(state), "the state changed");
                        finish(helper, player);
                    });
        });
    }

    @GameTest(template = "empty", timeoutTicks = 900)
    public static void aChunkThatTakesTooLongEndsTheWait(GameTestHelper helper) {
        BlockPos origin = new BlockPos(144_400, 80, 0);
        NaniteGameTests.inEntityTickingArea(helper, origin, 1, () -> {
            ServerPlayer player = player(helper, origin);
            UUID id = new UUID(144_400L, 1L);
            LinkState.Place at = new LinkState.Place(player.level().dimension(), new Vec3(144_800.5, 80, 1_600.5), 0f, 0f);
            player.setData(ModAttachments.LINK, LinkState.NONE.withKnown(new LinkState.KnownShell(id, 1L, at, false, false)));
            List<Links.Result> heard = new ArrayList<>();
            ShellReach.request(player, id, NOW, heard::add);
            helper.startSequence()
                    .thenWaitUntil(() -> helper.assertFalse(heard.isEmpty(), "the wait did not end"))
                    .thenExecute(() -> {
                        helper.assertTrue(heard.get(0) == Links.Result.TIMED_OUT, "the link ended " + heard.get(0));
                        finish(helper, player);
                    });
        });
    }

    @GameTest(template = "empty", timeoutTicks = 900)
    public static void aLoadedShellIsReachedToo(GameTestHelper helper) {
        BlockPos origin = new BlockPos(144_600, 80, 0);
        NaniteGameTests.inEntityTickingArea(helper, origin, 1, () -> {
            ServerPlayer player = player(helper, origin);
            UUID id = beside(player, player.position().add(AWAY)).getUUID();
            List<Links.Result> heard = new ArrayList<>();
            Links.requestLink(player, id, heard::add);
            helper.startSequence()
                    .thenWaitUntil(() -> helper.assertFalse(heard.isEmpty(), "nothing came of the request"))
                    .thenExecute(() -> {
                        helper.assertTrue(heard.get(0) == Links.Result.LINKED, "the link ended " + heard.get(0));
                        helper.assertTrue(Links.wake(player) == Links.Result.WOKEN, "no wake");
                        player.serverLevel().getEntity(id).discard();
                        finish(helper, player);
                    });
        });
    }

    /** What the list rules out takes no chunk loading at all, so these end on the spot. */
    @GameTest(template = "empty", timeoutTicks = 900)
    public static void whatTheListRulesOutEndsAtOnce(GameTestHelper helper) {
        BlockPos origin = new BlockPos(144_800, 80, 0);
        NaniteGameTests.inEntityTickingArea(helper, origin, 1, () -> {
            ServerPlayer player = player(helper, origin);
            ServerLevel level = player.serverLevel();
            List<Links.Result> heard = new ArrayList<>();
            Links.requestLink(player, new UUID(144_800L, 1L), heard::add);
            helper.assertTrue(heard.equals(List.of(Links.Result.NOT_A_SHELL)), "a shell off the list ended " + heard);
            heard.clear();
            BodyHost wreck = beside(player, player.position().add(AWAY));
            wreck.hurt(level.damageSources().genericKill(), Float.MAX_VALUE);
            Links.requestLink(player, wreck.getUUID(), heard::add);
            helper.assertTrue(heard.equals(List.of(Links.Result.NOT_A_SHELL)), "a wreck ended " + heard);
            heard.clear();
            BodyHost gone = beside(player, player.position().add(AWAY));
            gone.remove(Entity.RemovalReason.KILLED);
            Links.requestLink(player, gone.getUUID(), heard::add);
            helper.assertTrue(heard.equals(List.of(Links.Result.NOT_A_SHELL)), "a destroyed shell ended " + heard);
            heard.clear();
            BodyHost shell = beside(player, player.position().add(AWAY));
            UUID id = shell.getUUID();
            BodyHost other = beside(player, player.position().add(AWAY));
            helper.assertTrue(Links.link(player, shell) == Links.Result.LINKED, "no link");
            Links.requestLink(player, other.getUUID(), heard::add);
            helper.assertTrue(heard.equals(List.of(Links.Result.ALREADY_LINKED)), "a player in a shell ended " + heard);
            helper.assertTrue(Links.wake(player) == Links.Result.WOKEN, "no wake");
            wreck.discard();
            other.discard();
            level.getEntity(id).discard();
            finish(helper, player);
        });
    }

    @GameTest(template = "empty", timeoutTicks = 400)
    public static void aSecondRequestWaitsItsTurn(GameTestHelper helper) {
        BlockPos origin = new BlockPos(145_000, 80, 0);
        NaniteGameTests.inEntityTickingArea(helper, origin, 1, () -> {
            ServerPlayer player = player(helper, origin);
            UUID id = new UUID(145_000L, 1L);
            LinkState.Place at = new LinkState.Place(player.level().dimension(), new Vec3(145_400.5, 80, 800.5), 0f, 0f);
            player.setData(ModAttachments.LINK, LinkState.NONE.withKnown(new LinkState.KnownShell(id, 1L, at, false, false)));
            List<Links.Result> first = new ArrayList<>();
            List<Links.Result> second = new ArrayList<>();
            Links.requestLink(player, id, first::add);
            Links.requestLink(player, id, second::add);
            helper.assertTrue(second.equals(List.of(Links.Result.WAITING)), "a second request ended " + second);
            helper.startSequence()
                    .thenWaitUntil(() -> helper.assertFalse(first.isEmpty(), "the first wait did not end"))
                    .thenExecute(() -> finish(helper, player));
        });
    }

    /** A player who leaves ends the wait, or it would hold chunks and a move for nobody. */
    @GameTest(template = "empty", timeoutTicks = 900)
    public static void aPlayerWhoLeavesEndsTheWait(GameTestHelper helper) {
        BlockPos origin = new BlockPos(145_200, 80, 0);
        NaniteGameTests.inEntityTickingArea(helper, origin, 1, () -> {
            ServerPlayer player = player(helper, origin);
            UUID id = new UUID(145_200L, 1L);
            LinkState.Place at = new LinkState.Place(player.level().dimension(), new Vec3(145_600.5, 80, 800.5), 0f, 0f);
            player.setData(ModAttachments.LINK, LinkState.NONE.withKnown(new LinkState.KnownShell(id, 1L, at, false, false)));
            List<Links.Result> heard = new ArrayList<>();
            Links.requestLink(player, id, heard::add);
            TestPlayers.remove(player);
            helper.startSequence()
                    .thenWaitUntil(() -> helper.assertFalse(heard.isEmpty(), "the wait did not end"))
                    .thenExecute(() -> {
                        helper.assertTrue(heard.get(0) == Links.Result.PLAYER_GONE, "the link ended " + heard.get(0));
                        helper.succeed();
                    });
        });
    }
}
