package com.voidreclaimer.gametest;

import com.voidreclaimer.VoidReclaimer;
import com.voidreclaimer.body.Bodies;
import com.voidreclaimer.body.BuiltinBodies;
import com.voidreclaimer.link.BodyHost;
import com.voidreclaimer.link.BodySnapshot;
import com.voidreclaimer.link.HostRole;
import com.voidreclaimer.link.LinkState;
import com.voidreclaimer.link.Links;
import com.voidreclaimer.registry.ModAttachments;
import java.util.Optional;
import java.util.UUID;
import java.util.function.BiConsumer;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/** A shell woken from must reach the disk, and an old copy of one must not come back. These wait for ticks, so they clean up last. */
@GameTestHolder(VoidReclaimer.MOD_ID)
@PrefixGameTestTemplate(false)
public final class LinkOutboxGameTests {
    private static final Vec3 AWAY = new Vec3(0.0, 0.0, 4.0);

    private LinkOutboxGameTests() {}

    /** A player who linked to a shell and woke again: the shell is back in the world and in the outbox. */
    private static void woken(GameTestHelper helper, BlockPos origin, BiConsumer<ServerPlayer, UUID> test) {
        NaniteGameTests.inEntityTickingArea(helper, origin, 1, () -> {
            ServerPlayer player = TestPlayers.survivalInDeepSpace(helper, origin.getX() + 0.5, origin.getY(), origin.getZ() + 0.5);
            BodyHost shell = BodyHost.spawn(player.serverLevel(), player.position().add(AWAY), HostRole.SHELL, Bodies.preset(BuiltinBodies.DRONE), player.getUUID());
            UUID id = shell.getUUID();
            helper.assertTrue(Links.link(player, shell) == Links.Result.LINKED, "no link");
            helper.assertTrue(Links.wake(player) == Links.Result.WOKEN, "no wake");
            helper.assertTrue(Links.state(player).outbox(id).isPresent(), "nothing in the outbox");
            test.accept(player, id);
        });
    }

    private static void finish(GameTestHelper helper, ServerPlayer player, ServerLevel level, UUID id) {
        if (level.getEntity(id) != null) {
            level.getEntity(id).discard();
        }
        player.setData(ModAttachments.LINK, LinkState.NONE);
        TestPlayers.remove(player);
        helper.succeed();
    }

    @GameTest(template = "empty", timeoutTicks = 200)
    public static void aMissingShellIsBuiltAgainFromTheOutbox(GameTestHelper helper) {
        woken(helper, new BlockPos(79_300, 80, 0), (player, id) -> {
            ServerLevel level = player.serverLevel();
            long epoch = Links.state(player).outbox(id).orElseThrow().epoch();
            level.getEntity(id).discard();
            helper.startSequence()
                    .thenWaitUntil(() -> helper.assertTrue(level.getEntity(id) instanceof BodyHost, "the shell did not come back"))
                    .thenExecute(() -> {
                        BodyHost back = (BodyHost) level.getEntity(id);
                        helper.assertTrue(back.epoch() == epoch, "epoch " + back.epoch() + ", not " + epoch);
                        helper.assertTrue(back.role() == HostRole.SHELL, "came back as " + back.role());
                        helper.assertTrue(player.getUUID().equals(back.owner()), "came back without its owner");
                        helper.assertTrue(Links.state(player).outbox(id).isPresent(), "the outbox went before the shell reached the disk");
                        finish(helper, player, level, id);
                    });
        });
    }

    /** Nothing keeps the shell's chunk loaded here: the outbox has to fetch it. */
    @GameTest(template = "empty", timeoutTicks = 300)
    public static void anOutboxLoadsTheChunkItsShellIsIn(GameTestHelper helper) {
        BlockPos origin = new BlockPos(79_500, 80, 0);
        NaniteGameTests.inEntityTickingArea(helper, origin, 1, () -> {
            ServerPlayer player = TestPlayers.survivalInDeepSpace(helper, origin.getX() + 0.5, origin.getY(), origin.getZ() + 0.5);
            ServerLevel level = player.serverLevel();
            UUID id = new UUID(79_500L, 1L);
            Vec3 far = new Vec3(79_900.5, 80, 800.5);
            helper.assertFalse(level.isLoaded(BlockPos.containing(far)), "the test's far chunk is loaded already");
            CompoundTag body = BodySnapshot.take(player).save(player.registryAccess());
            LinkState.Place at = new LinkState.Place(level.dimension(), far, 0f, 0f);
            player.setData(ModAttachments.LINK, LinkState.NONE.withOutbox(new LinkState.Outbox(id, 3L, body, at, false)));
            helper.startSequence()
                    .thenWaitUntil(() -> helper.assertTrue(level.getEntity(id) instanceof BodyHost, "the far shell was not rebuilt"))
                    .thenExecute(() -> finish(helper, player, level, id));
        });
    }

    @GameTest(template = "empty", timeoutTicks = 200)
    public static void anOldCopyOfAShellGivesWayToTheOutbox(GameTestHelper helper) {
        woken(helper, new BlockPos(79_700, 80, 0), (player, id) -> {
            ServerLevel level = player.serverLevel();
            long epoch = Links.state(player).outbox(id).orElseThrow().epoch();
            BodyHost old = (BodyHost) level.getEntity(id);
            old.setEpoch(epoch - 1);
            helper.assertTrue(Links.stale(old), "an older copy does not count as stale");
            helper.assertTrue(Links.link(player, old) == Links.Result.NOT_A_SHELL, "linked to an old copy");
            helper.startSequence()
                    .thenWaitUntil(() -> helper.assertTrue(level.getEntity(id) instanceof BodyHost shell && shell.epoch() == epoch, "the old copy is still there"))
                    .thenExecute(() -> {
                        helper.assertTrue(old.isRemoved(), "the old copy was not removed");
                        finish(helper, player, level, id);
                    });
        });
    }

    /** One save only hands the shell to the IO thread. */
    @GameTest(template = "empty", timeoutTicks = 200)
    public static void theOutboxGoesAfterTwoSaves(GameTestHelper helper) {
        woken(helper, new BlockPos(79_900, 80, 0), (player, id) -> {
            ServerLevel level = player.serverLevel();
            level.save(null, false, false);
            Optional<LinkState.Outbox> first = Links.state(player).outbox(id);
            helper.assertTrue(first.isPresent() && first.get().saved(), "one save should mark the outbox and keep it");
            level.save(null, false, false);
            helper.assertTrue(Links.state(player).outbox(id).isEmpty(), "two saves should end the outbox");
            finish(helper, player, level, id);
        });
    }

    /** While the owner is in the shell, any copy of it in the world is left over from before the link. */
    @GameTest(template = "empty", timeoutTicks = 200)
    public static void aCopyOfTheShellYouAreInRemovesItself(GameTestHelper helper) {
        BlockPos origin = new BlockPos(80_100, 80, 0);
        NaniteGameTests.inEntityTickingArea(helper, origin, 1, () -> {
            ServerPlayer player = TestPlayers.survivalInDeepSpace(helper, origin.getX() + 0.5, origin.getY(), origin.getZ() + 0.5);
            ServerLevel level = player.serverLevel();
            BodyHost shell = BodyHost.spawn(level, player.position().add(AWAY), HostRole.SHELL, Bodies.preset(BuiltinBodies.DRONE), player.getUUID());
            UUID id = shell.getUUID();
            BodySnapshot body = BodySnapshot.of(shell);
            helper.assertTrue(Links.link(player, shell) == Links.Result.LINKED, "no link");
            LinkState.Place place = new LinkState.Place(level.dimension(), player.position().add(AWAY), 0f, 0f);
            Optional<BodyHost> copy = BodyHost.restore(level, place, id, 0L, body, player.getUUID());
            helper.assertTrue(copy.isPresent(), "no copy to test with");
            helper.startSequence()
                    .thenWaitUntil(() -> helper.assertTrue(copy.get().isRemoved(), "the copy is still there"))
                    .thenExecute(() -> {
                        Links.wake(player);
                        finish(helper, player, level, id);
                    });
        });
    }
}
