package com.voidreclaimer.gametest;

import com.voidreclaimer.VoidReclaimer;
import com.voidreclaimer.body.Bodies;
import com.voidreclaimer.body.BuiltinBodies;
import com.voidreclaimer.link.BodyHost;
import com.voidreclaimer.link.BodySnapshot;
import com.voidreclaimer.link.HostRole;
import com.voidreclaimer.link.LinkState;
import com.voidreclaimer.link.Links;
import com.voidreclaimer.link.ShellReach;
import com.voidreclaimer.link.ShellRecovery;
import com.voidreclaimer.registry.ModEntities;
import com.voidreclaimer.registry.ModAttachments;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
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

    /** A second copy of {@code shell}, not yet in the level. */
    private static BodyHost copy(BodyHost shell, long epoch) {
        BodyHost copy = new BodyHost(ModEntities.BODY_HOST.get(), shell.level());
        copy.setUUID(shell.getUUID());
        copy.setPos(shell.position());
        copy.setEpoch(epoch);
        return copy;
    }

    private static BodyHost beside(ServerPlayer player) {
        return BodyHost.spawn(player.serverLevel(), player.position().add(AWAY), HostRole.SHELL, Bodies.preset(BuiltinBodies.DRONE), player.getUUID());
    }

    @GameTest(template = "empty", timeoutTicks = 900)
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
    @GameTest(template = "empty", timeoutTicks = 900)
    public static void anOldCopyIsRefusedAfterItsOutboxHasEndedAndGoesWithTheShell(GameTestHelper helper) {
        LinkOutboxGameTests.woken(helper, new BlockPos(142_200, 80, 0), (player, id) -> {
            ServerLevel level = player.serverLevel();
            long epoch = known(player, id).epoch();
            noOutbox(player, id);
            BodyHost old = (BodyHost) level.getEntity(id);
            old.setEpoch(epoch - 1);
            helper.assertTrue(Links.stale(old), "an older copy passed for the shell");
            helper.assertTrue(Links.link(player, old) == Links.Result.NOT_A_SHELL, "linked to an old copy");
            // The newer copy may have been lost, so this one stays until something proves it is not needed.
            helper.runAfterDelay(40, () -> {
                helper.assertFalse(old.isRemoved(), "an old copy went on the list's word alone");
                LinkState.KnownShell entry = known(player, id);
                player.setData(ModAttachments.LINK, Links.state(player).withKnown(new LinkState.KnownShell(id, entry.epoch(), entry.at(), false, true)));
                helper.startSequence()
                        .thenWaitUntil(() -> helper.assertTrue(old.isRemoved(), "a copy of a shell that is gone is still there"))
                        .thenExecute(() -> LinkOutboxGameTests.finish(helper, player, level, id));
            });
        });
    }

    @GameTest(template = "empty", timeoutTicks = 900)
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

    @GameTest(template = "empty", timeoutTicks = 900)
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

    @GameTest(template = "empty", timeoutTicks = 900)
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
    @GameTest(template = "empty", timeoutTicks = 900, batch = "ledger_saves_a")
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
    @GameTest(template = "empty", timeoutTicks = 900, batch = "ledger_saves_b")
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
    @GameTest(template = "empty", timeoutTicks = 900, batch = "ledger_saves_c")
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

    @GameTest(template = "empty", timeoutTicks = 900)
    public static void anOlderCopyCannotJoinBesideTheShell(GameTestHelper helper) {
        BlockPos origin = new BlockPos(261_000, 80, 0);
        NaniteGameTests.inEntityTickingArea(helper, origin, 1, () -> {
            ServerPlayer player = TestPlayers.survivalInDeepSpace(helper, origin.getX() + 0.5, origin.getY(), origin.getZ() + 0.5);
            BodyHost shell = beside(player);
            shell.setEpoch(5L);
            BodyHost old = copy(shell, 4L);
            helper.assertFalse(player.serverLevel().addFreshEntity(old), "an old copy joined");
            helper.assertTrue(player.serverLevel().getEntity(shell.getUUID()) == shell && !shell.isRemoved(), "the old copy pushed the shell out");
            LinkOutboxGameTests.finish(helper, player, player.serverLevel(), shell.getUUID());
        });
    }

    /** The case that lost a body: the old copy's chunk loads first, and the level would refuse the newer copy that comes after it. */
    @GameTest(template = "empty", timeoutTicks = 900)
    public static void aNewerCopyPushesOutAnOldOne(GameTestHelper helper) {
        BlockPos origin = new BlockPos(261_200, 80, 0);
        NaniteGameTests.inEntityTickingArea(helper, origin, 1, () -> {
            ServerPlayer player = TestPlayers.survivalInDeepSpace(helper, origin.getX() + 0.5, origin.getY(), origin.getZ() + 0.5);
            BodyHost old = beside(player);
            old.setEpoch(4L);
            BodyHost newer = copy(old, 5L);
            helper.assertTrue(player.serverLevel().addFreshEntity(newer), "the level refused the newer copy");
            helper.assertTrue(old.isRemoved(), "the old copy is still here");
            helper.assertTrue(player.serverLevel().getEntity(old.getUUID()) == newer, "the newer copy is not the shell now");
            LinkOutboxGameTests.finish(helper, player, player.serverLevel(), old.getUUID());
        });
    }

    @GameTest(template = "empty", timeoutTicks = 900)
    public static void anOldCopyKilledLeavesTheRecordAlone(GameTestHelper helper) {
        LinkOutboxGameTests.woken(helper, new BlockPos(261_400, 80, 0), (player, id) -> {
            ServerLevel level = player.serverLevel();
            LinkState.KnownShell before = known(player, id);
            BodyHost old = (BodyHost) level.getEntity(id);
            old.setEpoch(before.epoch() - 1);
            old.remove(Entity.RemovalReason.KILLED);
            helper.assertFalse(known(player, id).gone(), "an old copy took the shell with it");
            helper.assertTrue(Links.state(player).outbox(id).isPresent(), "an old copy took the outbox with it");
            LinkOutboxGameTests.finish(helper, player, level, id);
        });
    }

    /** Linking to an old copy is allowed when the outbox cannot be read, and the list stays ahead of every copy. */
    @GameTest(template = "empty", timeoutTicks = 900)
    public static void aLinkNeverLowersTheList(GameTestHelper helper) {
        LinkOutboxGameTests.woken(helper, new BlockPos(261_600, 80, 0), (player, id) -> {
            ServerLevel level = player.serverLevel();
            LinkState.Outbox outbox = Links.state(player).outbox(id).orElseThrow();
            long before = known(player, id).epoch();
            BodyHost old = (BodyHost) level.getEntity(id);
            old.setEpoch(before - 1);
            player.setData(ModAttachments.LINK, Links.state(player).withOutbox(new LinkState.Outbox(id, outbox.epoch(), new CompoundTag(), outbox.at(), false)));
            helper.assertTrue(Links.link(player, old) == Links.Result.LINKED, "no link to the only readable copy");
            helper.assertTrue(known(player, id).epoch() == before + 1, "the list went back with an old copy");
            helper.assertTrue(Links.wake(player) == Links.Result.WOKEN, "no wake");
            LinkOutboxGameTests.finish(helper, player, level, id);
        });
    }

    @GameTest(template = "empty", timeoutTicks = 900)
    public static void anOldCopyOfAnEmptiedWreckGivesNothing(GameTestHelper helper) {
        BlockPos origin = new BlockPos(261_800, 80, 0);
        NaniteGameTests.inEntityTickingArea(helper, origin, 1, () -> {
            ServerPlayer player = TestPlayers.survivalInDeepSpace(helper, origin.getX() + 0.5, origin.getY(), origin.getZ() + 0.5);
            BodyHost wreck = beside(player);
            wreck.die(player.serverLevel().damageSources().genericKill());
            player.setData(ModAttachments.LINK, Links.state(player).withKnown(new LinkState.KnownShell(wreck.getUUID(), wreck.epoch(),
                    new LinkState.Place(player.serverLevel().dimension(), wreck.position(), 0f, 0f), true, true)));
            wreck.interact(player, InteractionHand.MAIN_HAND);
            helper.assertTrue(player.getInventory().isEmpty(), "a wreck that is gone was salvaged again");
            LinkOutboxGameTests.finish(helper, player, player.serverLevel(), wreck.getUUID());
        });
    }

    @GameTest(template = "empty", timeoutTicks = 900)
    public static void aParkedShellTakesNoHarmButACommandReachesIt(GameTestHelper helper) {
        BlockPos origin = new BlockPos(264_000, 80, 0);
        NaniteGameTests.inEntityTickingArea(helper, origin, 1, () -> {
            ServerPlayer player = TestPlayers.survivalInDeepSpace(helper, origin.getX() + 0.5, origin.getY(), origin.getZ() + 0.5);
            ServerLevel level = player.serverLevel();
            BodyHost shell = beside(player);
            float health = shell.getHealth();
            var sources = level.damageSources();
            for (var source : List.of(sources.generic(), sources.drown(), sources.inWall(), sources.playerAttack(player), sources.flyIntoWall(), sources.fall())) {
                helper.assertFalse(shell.hurt(source, 1_000f), "a parked shell took " + source.getMsgId());
            }
            helper.assertTrue(shell.getHealth() == health && shell.role() == HostRole.SHELL, "a parked shell was hurt");
            shell.hurt(sources.genericKill(), Float.MAX_VALUE);
            helper.assertTrue(shell.role() == HostRole.WRECK, "a command should leave a wreck");
            LinkOutboxGameTests.finish(helper, player, level, shell.getUUID());
        });
    }

    @GameTest(template = "empty", timeoutTicks = 900)
    public static void sneakUseScrapsYourOwnShellAndNoOneElses(GameTestHelper helper) {
        BlockPos origin = new BlockPos(264_200, 80, 0);
        NaniteGameTests.inEntityTickingArea(helper, origin, 1, () -> {
            ServerPlayer player = TestPlayers.survivalInDeepSpace(helper, origin.getX() + 0.5, origin.getY(), origin.getZ() + 0.5);
            ServerPlayer stranger = TestPlayers.survivalInDeepSpace(helper, origin.getX() + 0.5, origin.getY(), origin.getZ() + 1.5);
            BodyHost shell = beside(player);
            shell.interact(player, InteractionHand.MAIN_HAND);
            helper.assertTrue(shell.role() == HostRole.SHELL, "a plain use scrapped a shell");
            stranger.setShiftKeyDown(true);
            shell.interact(stranger, InteractionHand.MAIN_HAND);
            helper.assertTrue(shell.role() == HostRole.SHELL, "someone else scrapped a shell");
            player.setShiftKeyDown(true);
            shell.interact(player, InteractionHand.MAIN_HAND);
            helper.assertTrue(shell.role() == HostRole.WRECK, "sneak-use left the shell standing");
            helper.assertTrue(known(player, shell.getUUID()).wrecked(), "the list does not say it is a wreck");
            helper.assertFalse(shell.items().isEmpty(), "a scrapped shell has nothing in it");
            player.setShiftKeyDown(false);
            stranger.setShiftKeyDown(false);
            TestPlayers.remove(stranger);
            LinkOutboxGameTests.finish(helper, player, player.serverLevel(), shell.getUUID());
        });
    }

    /** The newer copy is lost, so the old one stands refused: the picker offers it, and it becomes the shell. */
    @GameTest(template = "empty", timeoutTicks = 900)
    public static void anOldCopyComesBackWhenTheNewerOneIsLost(GameTestHelper helper) {
        LinkOutboxGameTests.woken(helper, new BlockPos(265_400, 80, 0), (player, id) -> {
            ServerLevel level = player.serverLevel();
            long epoch = known(player, id).epoch();
            noOutbox(player, id);
            BodyHost old = (BodyHost) level.getEntity(id);
            old.setEpoch(epoch - 1);
            AtomicReference<ShellRecovery.Result> ended = new AtomicReference<>();
            helper.startSequence()
                    .thenWaitUntil(() -> helper.assertTrue(known(player, id).oldCopy().isPresent(), "the list did not hear of the old copy"))
                    .thenExecute(() -> {
                        helper.assertFalse(com.voidreclaimer.link.Signal.rows(player).get(0).recoverable(), "offered before a link looked for the shell");
                        ShellReach.request(player, id, 200, result -> helper.assertTrue(result == Links.Result.NOT_FOUND, "the pick ended " + result));
                    })
                    .thenWaitUntil(() -> helper.assertTrue(known(player, id).recoverable(), "the look found nothing, and the list did not hear"))
                    .thenExecute(() -> {
                        helper.assertTrue(com.voidreclaimer.link.Signal.rows(player).get(0).recoverable(), "the picker does not offer it");
                        ShellRecovery.request(player, id, ended::set);
                    })
                    .thenWaitUntil(() -> helper.assertTrue(ended.get() != null, "no answer"))
                    .thenExecute(() -> {
                        helper.assertTrue(ended.get() == ShellRecovery.Result.RECOVERED, "ended " + ended.get());
                        helper.assertTrue(old.epoch() > epoch && known(player, id).epoch() == old.epoch(), "the copy is not the shell: " + old.epoch());
                        helper.assertTrue(known(player, id).oldCopy().isEmpty(), "the list still offers it");
                        helper.assertTrue(Links.link(player, old) == Links.Result.LINKED, "no link to the recovered shell");
                        helper.assertTrue(Links.wake(player) == Links.Result.WOKEN, "no wake");
                        LinkOutboxGameTests.finish(helper, player, level, id);
                    });
        });
    }

    /** The worst thing recovery could do: replace a shell that is only far away with an older body. */
    @GameTest(template = "empty", timeoutTicks = 900)
    public static void recoveryLeavesAHealthyShellAlone(GameTestHelper helper) {
        LinkOutboxGameTests.woken(helper, new BlockPos(265_600, 80, 0), (player, id) -> {
            ServerLevel level = player.serverLevel();
            noOutbox(player, id);
            LinkState.KnownShell entry = known(player, id);
            BodyHost shell = (BodyHost) level.getEntity(id);
            player.setData(ModAttachments.LINK, Links.state(player).withKnown(new LinkState.KnownShell(id, entry.epoch(), entry.at(), false, false, Optional.of(entry.at()), true)));
            AtomicReference<ShellRecovery.Result> ended = new AtomicReference<>();
            ShellRecovery.request(player, id, ended::set);
            helper.startSequence()
                    .thenWaitUntil(() -> helper.assertTrue(ended.get() != null, "no answer"))
                    .thenExecute(() -> {
                        helper.assertTrue(ended.get() == ShellRecovery.Result.STILL_THERE, "ended " + ended.get());
                        helper.assertTrue(shell.epoch() == entry.epoch() && !shell.isRemoved(), "a healthy shell was touched");
                        helper.assertTrue(known(player, id).oldCopy().isEmpty(), "the list still offers an old copy");
                        LinkOutboxGameTests.finish(helper, player, level, id);
                    });
        });
    }

    /** The review's case: a link made during the wait must not leave two live bodies with one number. */
    @GameTest(template = "empty", timeoutTicks = 900)
    public static void aLinkDuringTheWaitEndsRecovery(GameTestHelper helper) {
        LinkOutboxGameTests.woken(helper, new BlockPos(266_200, 80, 0), (player, id) -> {
            ServerLevel level = player.serverLevel();
            noOutbox(player, id);
            LinkState.KnownShell entry = known(player, id);
            BodyHost old = (BodyHost) level.getEntity(id);
            old.setEpoch(entry.epoch() - 1);
            player.setData(ModAttachments.LINK, Links.state(player).withKnown(new LinkState.KnownShell(id, entry.epoch(), entry.at(), false, false, Optional.of(entry.at()), true)));
            AtomicReference<ShellRecovery.Result> ended = new AtomicReference<>();
            ShellRecovery.request(player, id, ended::set);
            // A destroyed shell during the wait does the same, and is the simplest way to change the list under it.
            player.setData(ModAttachments.LINK, Links.state(player).withKnown(new LinkState.KnownShell(id, entry.epoch(), entry.at(), false, true, Optional.of(entry.at()), true)));
            helper.startSequence()
                    .thenWaitUntil(() -> helper.assertTrue(ended.get() != null, "no answer"))
                    .thenExecute(() -> {
                        helper.assertTrue(ended.get() == ShellRecovery.Result.REFUSED, "ended " + ended.get());
                        helper.assertTrue(known(player, id).gone(), "recovery brought back a shell that is gone");
                        LinkOutboxGameTests.finish(helper, player, level, id);
                    });
        });
    }

    @GameTest(template = "empty", timeoutTicks = 900)
    public static void recoveryOfACopyThatHasGoneChangesNothing(GameTestHelper helper) {
        LinkOutboxGameTests.woken(helper, new BlockPos(265_800, 80, 0), (player, id) -> {
            ServerLevel level = player.serverLevel();
            noOutbox(player, id);
            LinkState.KnownShell entry = known(player, id);
            level.getEntity(id).discard();
            player.setData(ModAttachments.LINK, Links.state(player).withKnown(new LinkState.KnownShell(id, entry.epoch(), entry.at(), false, false, Optional.of(entry.at()), true)));
            AtomicReference<ShellRecovery.Result> ended = new AtomicReference<>();
            ShellRecovery.request(player, id, ended::set);
            helper.startSequence()
                    .thenWaitUntil(() -> helper.assertTrue(ended.get() != null, "no answer"))
                    .thenExecute(() -> {
                        helper.assertTrue(ended.get() == ShellRecovery.Result.NOT_FOUND, "ended " + ended.get());
                        helper.assertTrue(known(player, id).oldCopy().isEmpty() && known(player, id).epoch() == entry.epoch(), "the list changed");
                        LinkOutboxGameTests.finish(helper, player, level, id);
                    });
        });
    }

    /** A client can send anything, and an outbox still running decides alone. */
    @GameTest(template = "empty", timeoutTicks = 900)
    public static void recoveryAsksForASeatInReachAndNoOutbox(GameTestHelper helper) {
        LinkOutboxGameTests.woken(helper, new BlockPos(266_000, 80, 0), (player, id) -> {
            ServerLevel level = player.serverLevel();
            LinkState.KnownShell entry = known(player, id);
            player.setData(ModAttachments.LINK, Links.state(player).withKnown(new LinkState.KnownShell(id, entry.epoch(), entry.at(), false, false, Optional.of(entry.at()), true)));
            AtomicReference<ShellRecovery.Result> ended = new AtomicReference<>();
            ShellRecovery.request(player, id, ended::set);
            helper.assertTrue(ended.get() == ShellRecovery.Result.REFUSED, "ran with an outbox: " + ended.get());
            noOutbox(player, id);
            com.voidreclaimer.network.RecoverShellPayload.apply(player, player.blockPosition().above(40), id);
            helper.runAfterDelay(30, () -> {
                helper.assertTrue(known(player, id).oldCopy().isPresent(), "a request with no seat did something");
                LinkOutboxGameTests.finish(helper, player, level, id);
            });
        });
    }

    @GameTest(template = "empty", timeoutTicks = 900)
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
