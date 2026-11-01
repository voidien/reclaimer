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
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/** The list of shells in {@link LinkState}: what still knows a shell once its outbox has ended. */
@GameTestHolder(VoidReclaimer.MOD_ID)
@PrefixGameTestTemplate(false)
public final class LinkLedgerGameTests {
    private static final Vec3 AWAY = new Vec3(0.0, 0.0, 4.0);

    private LinkLedgerGameTests() {}

    private static LinkState.KnownShell known(ServerPlayer player, UUID id) {
        return Links.state(player).known(id).orElseThrow(() -> new IllegalStateException("the shell is not on the list"));
    }

    private static void noOutbox(ServerPlayer player, UUID id) {
        player.setData(ModAttachments.LINK, Links.state(player).withoutOutbox(id));
    }

    private static BodyHost beside(ServerPlayer player) {
        return BodyHost.spawn(player.serverLevel(), player.position().add(AWAY), HostRole.SHELL, Bodies.preset(BuiltinBodies.DRONE), player.getUUID());
    }

    @GameTest(template = "empty", timeoutTicks = 200)
    public static void aShellIsOnTheListFromTheStart(GameTestHelper helper) {
        BlockPos origin = new BlockPos(142_000, 80, 0);
        NaniteGameTests.inEntityTickingArea(helper, origin, 1, () -> {
            ServerPlayer player = TestPlayers.survivalInDeepSpace(helper, origin.getX() + 0.5, origin.getY(), origin.getZ() + 0.5);
            BodyHost shell = beside(player);
            UUID id = shell.getUUID();
            helper.assertTrue(known(player, id).epoch() == 0L, "a new shell starts with no links");
            helper.assertTrue(Links.link(player, shell) == Links.Result.LINKED, "no link");
            helper.assertTrue(known(player, id).epoch() == 1L, "the link did not reach the list");
            player.teleportTo(player.getX() + 3.0, player.getY(), player.getZ());
            Vec3 left = player.position();
            helper.assertTrue(Links.wake(player) == Links.Result.WOKEN, "no wake");
            LinkState.KnownShell after = known(player, id);
            helper.assertTrue(after.epoch() == 1L, "the wake changed the link number");
            helper.assertTrue(after.at().position().distanceTo(left) < 0.01, "the list has it at " + after.at().position() + ", not " + left);
            helper.assertFalse(after.wrecked() || after.gone(), "a shell left alone is neither a wreck nor gone");
            LinkOutboxGameTests.finish(helper, player, player.serverLevel(), id);
        });
    }

    /** The case the outbox cannot cover: it has ended, and a chunk nobody visited since a crash loads the copy from before the link. */
    @GameTest(template = "empty", timeoutTicks = 200)
    public static void anOldCopyGoesEvenAfterItsOutboxHasEnded(GameTestHelper helper) {
        LinkOutboxGameTests.woken(helper, new BlockPos(142_200, 80, 0), (player, id) -> {
            ServerLevel level = player.serverLevel();
            long epoch = known(player, id).epoch();
            noOutbox(player, id);
            BodyHost old = (BodyHost) level.getEntity(id);
            old.setEpoch(epoch - 1);
            helper.assertTrue(Links.stale(old), "an older copy passed for the shell");
            helper.assertTrue(Links.link(player, old) == Links.Result.NOT_A_SHELL, "linked to an old copy");
            helper.startSequence()
                    .thenWaitUntil(() -> helper.assertTrue(old.isRemoved(), "the old copy is still there"))
                    .thenExecute(() -> LinkOutboxGameTests.finish(helper, player, level, id));
        });
    }

    @GameTest(template = "empty", timeoutTicks = 200)
    public static void theShellItselfIsNotAnOldCopy(GameTestHelper helper) {
        LinkOutboxGameTests.woken(helper, new BlockPos(142_400, 80, 0), (player, id) -> {
            ServerLevel level = player.serverLevel();
            noOutbox(player, id);
            helper.runAfterDelay(40, () -> {
                helper.assertTrue(level.getEntity(id) instanceof BodyHost shell && !Links.stale(shell), "the list took the shell for an old copy");
                LinkOutboxGameTests.finish(helper, player, level, id);
            });
        });
    }

    @GameTest(template = "empty", timeoutTicks = 200)
    public static void aShellDestroyedForGoodDoesNotComeBack(GameTestHelper helper) {
        LinkOutboxGameTests.woken(helper, new BlockPos(142_600, 80, 0), (player, id) -> {
            ServerLevel level = player.serverLevel();
            BodyHost shell = (BodyHost) level.getEntity(id);
            BodySnapshot body = BodySnapshot.of(shell);
            long epoch = shell.epoch();
            shell.remove(Entity.RemovalReason.KILLED);
            helper.assertTrue(known(player, id).gone(), "a destroyed shell is not marked gone");
            LinkState.Place place = new LinkState.Place(level.dimension(), player.position().add(AWAY), 0f, 0f);
            Optional<BodyHost> copy = BodyHost.restore(level, place, id, epoch, body, player.getUUID());
            helper.assertTrue(copy.isPresent(), "no copy to test with");
            helper.assertTrue(Links.link(player, copy.get()) == Links.Result.NOT_A_SHELL, "linked to a shell that was destroyed");
            helper.startSequence()
                    .thenWaitUntil(() -> helper.assertTrue(copy.get().isRemoved(), "the copy is still there"))
                    .thenExecute(() -> LinkOutboxGameTests.finish(helper, player, level, id));
        });
    }

    /** {@code Wrecks.salvage} saves the player, so an emptied wreck that a crash brings back would give everything twice. */
    @GameTest(template = "empty", timeoutTicks = 300)
    public static void anEmptiedWreckDoesNotComeBackFull(GameTestHelper helper) {
        BlockPos origin = new BlockPos(142_800, 80, 0);
        NaniteGameTests.inEntityTickingArea(helper, origin, 1, () -> {
            ServerPlayer player = TestPlayers.survivalInDeepSpace(helper, origin.getX() + 0.5, origin.getY(), origin.getZ() + 0.5);
            ServerLevel level = player.serverLevel();
            BodyHost shell = beside(player);
            UUID id = shell.getUUID();
            BodySnapshot body = BodySnapshot.of(shell);
            shell.hurt(level.damageSources().genericKill(), Float.MAX_VALUE);
            helper.assertTrue(shell.role() == HostRole.WRECK, "no wreck");
            helper.assertTrue(known(player, id).wrecked(), "the list does not say it is a wreck");
            helper.assertFalse(known(player, id).gone(), "a wreck is not gone");
            shell.interact(player, InteractionHand.MAIN_HAND);
            helper.assertTrue(shell.isRemoved(), "the wreck was not emptied");
            helper.assertTrue(known(player, id).gone(), "an emptied wreck is not marked gone");
            LinkState.Place place = new LinkState.Place(level.dimension(), player.position().add(AWAY), 0f, 0f);
            BodyHost copy = BodyHost.restore(level, place, id, 0L, body, player.getUUID()).orElseThrow();
            copy.setRole(HostRole.WRECK);
            helper.startSequence()
                    .thenWaitUntil(() -> helper.assertTrue(copy.isRemoved(), "the full copy of the wreck is still there"))
                    .thenExecute(() -> LinkOutboxGameTests.finish(helper, player, level, id));
        });
    }

    @GameTest(template = "empty", timeoutTicks = 200)
    public static void dyingInAShellMarksItAWreck(GameTestHelper helper) {
        BlockPos origin = new BlockPos(143_000, 80, 0);
        NaniteGameTests.inEntityTickingArea(helper, origin, 1, () -> {
            ServerPlayer player = TestPlayers.survivalInDeepSpace(helper, origin.getX() + 0.5, origin.getY(), origin.getZ() + 0.5);
            ServerLevel level = player.serverLevel();
            BodyHost shell = beside(player);
            UUID id = shell.getUUID();
            helper.assertTrue(Links.link(player, shell) == Links.Result.LINKED, "no link");
            player.hurt(level.damageSources().genericKill(), Float.MAX_VALUE);
            helper.startSequence()
                    .thenWaitUntil(() -> helper.assertFalse(Links.state(player).linked(), "still in the shell"))
                    .thenExecute(() -> {
                        helper.assertTrue(known(player, id).wrecked(), "the list does not say it is a wreck");
                        helper.assertFalse(known(player, id).gone(), "a wreck is not gone");
                        LinkOutboxGameTests.finish(helper, player, level, id);
                    });
        });
    }

    /** Its own batch: a save counts for every player's list. */
    @GameTest(template = "empty", timeoutTicks = 200, batch = "ledger_saves_a")
    public static void aSaveKeepsTrackOfAShell(GameTestHelper helper) {
        LinkOutboxGameTests.woken(helper, new BlockPos(143_200, 80, 0), (player, id) -> {
            ServerLevel level = player.serverLevel();
            Entity shell = level.getEntity(id);
            shell.setPos(shell.position().add(1.0, 0.0, 0.0));
            level.save(null, false, false);
            helper.assertTrue(known(player, id).at().position().distanceTo(shell.position()) < 0.01, "the list lost track of the shell");
            LinkOutboxGameTests.finish(helper, player, level, id);
        });
    }

    /** Its own batch: a save counts for every player's list. */
    @GameTest(template = "empty", timeoutTicks = 200, batch = "ledger_saves_b")
    public static void anOldCopySaysNothingToTheList(GameTestHelper helper) {
        LinkOutboxGameTests.woken(helper, new BlockPos(143_400, 80, 0), (player, id) -> {
            ServerLevel level = player.serverLevel();
            LinkState.KnownShell before = known(player, id);
            noOutbox(player, id);
            BodyHost old = (BodyHost) level.getEntity(id);
            old.setEpoch(before.epoch() - 1);
            old.setPos(old.position().add(1.0, 0.0, 0.0));
            level.save(null, false, false);
            helper.assertTrue(known(player, id).equals(before), "an old copy rewrote the list");
            LinkOutboxGameTests.finish(helper, player, level, id);
        });
    }

    /** A save made before the list has outboxes and no entries. Its own batch: a save counts for every player's outbox. */
    @GameTest(template = "empty", timeoutTicks = 200, batch = "ledger_saves_c")
    public static void anOutboxFromBeforeTheListEndsOnTheList(GameTestHelper helper) {
        LinkOutboxGameTests.woken(helper, new BlockPos(143_600, 80, 0), (player, id) -> {
            ServerLevel level = player.serverLevel();
            LinkState state = Links.state(player);
            player.setData(ModAttachments.LINK, new LinkState(state.link(), state.outboxes(), List.of()));
            level.save(null, false, false);
            level.save(null, false, false);
            helper.assertTrue(Links.state(player).outbox(id).isEmpty(), "two saves should end the outbox");
            helper.assertTrue(known(player, id).epoch() == 1L, "the shell's link number went with the outbox");
            LinkOutboxGameTests.finish(helper, player, level, id);
        });
    }

    @GameTest(template = "empty", timeoutTicks = 200)
    public static void theShellsCommandCountsWhatIsLeft(GameTestHelper helper) {
        BlockPos origin = new BlockPos(143_800, 80, 0);
        NaniteGameTests.inEntityTickingArea(helper, origin, 1, () -> {
            ServerPlayer player = TestPlayers.survivalInDeepSpace(helper, origin.getX() + 0.5, origin.getY(), origin.getZ() + 0.5);
            var source = player.createCommandSourceStack().withPermission(4).withSuppressedOutput();
            var dispatcher = helper.getLevel().getServer().getCommands().getDispatcher();
            try {
                helper.assertTrue(dispatcher.execute("void_reclaimer link shells @s", source) == 0, "shells on a new list");
                BodyHost kept = beside(player);
                BodyHost lost = beside(player);
                lost.remove(Entity.RemovalReason.KILLED);
                helper.assertTrue(dispatcher.execute("void_reclaimer link shells @s", source) == 1, "a shell that is gone was counted");
                kept.discard();
            } catch (com.mojang.brigadier.exceptions.CommandSyntaxException e) {
                helper.fail("the command did not parse: " + e.getMessage());
            }
            player.setData(ModAttachments.LINK, LinkState.NONE);
            TestPlayers.remove(player);
            helper.succeed();
        });
    }
}
