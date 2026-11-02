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
import com.voidreclaimer.registry.ModAttachments;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
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

    private static final BlockPos RELAY_SPOT = new BlockPos(75_200, 80, 0);
    private static final UUID FAR_RELAY = new UUID(75_200L, 1L);
    private static final BlockPos FAR_RELAY_SPOT = new BlockPos(75_200, 80, 900_000);
    private static final int RELAY_CHARGE = 1500;
    private static final BlockPos SHIP_RELAY_SPOT = new BlockPos(75_204, 90, 4);

    /** The list is on the level and the relay's id and energy are in its chunk: all of it has to come back, and agree. */
    @GameTest(template = "empty", timeoutTicks = 300)
    public static void aRelayListOutlivesARestartBeforeRestart(GameTestHelper helper) {
        ServerLevel space = TestPlayers.deepSpace(helper);
        NaniteGameTests.inLoadedArea(helper, RELAY_SPOT, 1, () -> {
            space.setBlock(RELAY_SPOT, com.voidreclaimer.registry.ModBlocks.RELAY.get().defaultBlockState(), 3);
            var relay = (com.voidreclaimer.link.RelayBlockEntity) space.getBlockEntity(RELAY_SPOT);
            relay.energy().receiveEnergy(RELAY_CHARGE, false);
            com.voidreclaimer.link.Relays.note(space, new com.voidreclaimer.link.RelayList.Entry(FAR_RELAY, FAR_RELAY_SPOT, Vec3.atCenterOf(FAR_RELAY_SPOT), true, java.util.Optional.empty()));
            var server = space.getServer();
            AeronauticsGameTests.command(server, space, SHIP_RELAY_SPOT, "setblock " + SHIP_RELAY_SPOT.getX() + " " + (SHIP_RELAY_SPOT.getY() - 1) + " " + SHIP_RELAY_SPOT.getZ() + " minecraft:iron_block");
            space.setBlock(SHIP_RELAY_SPOT, com.voidreclaimer.registry.ModBlocks.RELAY.get().defaultBlockState(), 3);
            var aboard = (com.voidreclaimer.link.RelayBlockEntity) space.getBlockEntity(SHIP_RELAY_SPOT);
            helper.startSequence()
                    .thenWaitUntil(() -> helper.assertTrue(com.voidreclaimer.link.Relays.of(space).find(aboard.id()).isPresent(), "the relay meant for the ship never reached the list"))
                    .thenExecute(() -> AeronauticsGameTests.command(server, space, SHIP_RELAY_SPOT, "sable assemble connected " + SHIP_RELAY_SPOT.getX() + " " + (SHIP_RELAY_SPOT.getY() - 1) + " " + SHIP_RELAY_SPOT.getZ()))
                    .thenWaitUntil(() -> helper.assertTrue(com.voidreclaimer.link.Relays.of(space).find(aboard.id()).filter(entry -> entry.ship().isPresent()).isPresent(), "the relay never went aboard"))
                    .thenWaitUntil(() -> helper.assertTrue(com.voidreclaimer.link.Relays.of(space).find(relay.id()).filter(com.voidreclaimer.link.RelayList.Entry::lit).isPresent(), "the relay never reached the list"))
                    .thenSucceed();
        });
    }

    @GameTest(template = "empty", timeoutTicks = 300)
    public static void aRelayListOutlivesARestartAfterRestart(GameTestHelper helper) {
        ServerLevel space = TestPlayers.deepSpace(helper);
        var list = com.voidreclaimer.link.Relays.of(space);
        helper.assertTrue(list.find(FAR_RELAY).filter(entry -> entry.lit() && entry.block().equals(FAR_RELAY_SPOT)).isPresent(), "the list lost a relay it could not check: " + list);
        helper.assertTrue(list.relays().stream().anyMatch(entry -> entry.ship().isPresent()), "a relay aboard a ship left the list when the server stopped: " + list);
        var saved = list.relays().stream().filter(entry -> entry.block().equals(RELAY_SPOT)).findFirst();
        helper.assertTrue(saved.isPresent(), "the list lost the relay at " + RELAY_SPOT + ": " + list);
        var relay = (com.voidreclaimer.link.RelayBlockEntity) space.getChunk(RELAY_SPOT).getBlockEntity(RELAY_SPOT);
        helper.assertTrue(relay != null, "the relay's block did not come back");
        helper.assertTrue(relay.id().equals(saved.get().id()), "the relay came back as " + relay.id() + ", the list says " + saved.get().id());
        helper.assertTrue(relay.storedEnergy() > 0 && relay.storedEnergy() < RELAY_CHARGE, "the relay came back with " + relay.storedEnergy() + " FE");
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

    private static final UUID QUEUE_PLAYER = new UUID(0x7E57L, 0xB0B3L);
    private static final BlockPos QUEUE_SPOT = new BlockPos(76_100, 80, 0);
    private static final net.minecraft.resources.ResourceLocation ITEMS_TOO =
            net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("void_reclaimer_test", "replicator/items_too");

    /** A shell dies with a job running, and the server dies before the wreck reaches the disk: the items must be somewhere, once. */
    @GameTest(template = "empty", timeoutTicks = 300)
    public static void aShellsQueuedItemsOutliveACrashBeforeRestart(GameTestHelper helper) {
        ServerLevel space = TestPlayers.deepSpace(helper);
        NaniteGameTests.inLoadedArea(helper, QUEUE_SPOT, 1, () -> {
            ServerPlayer player = TestPlayers.joinAs(helper, QUEUE_PLAYER);
            player.setData(ModAttachments.LINK, LinkState.NONE);
            player.teleportTo(space, QUEUE_SPOT.getX() + 0.5, QUEUE_SPOT.getY(), QUEUE_SPOT.getZ() + 0.5, 0f, 0f);
            player.gameMode.changeGameModeForPlayer(GameType.SURVIVAL);
            BodyHost shell = BodyHost.spawn(space, player.position().add(0.0, 0.0, 4.0), HostRole.SHELL, Bodies.preset(BuiltinBodies.DRONE), player.getUUID());
            helper.assertTrue(Links.link(player, shell) == Links.Result.LINKED, "no link");
            player.getInventory().clearContent();
            var replicator = com.voidreclaimer.replicator.ReplicatorData.EMPTY.withUnlocked(com.voidreclaimer.replicator.Milestones.AWAKENING);
            com.voidreclaimer.replicator.Replicator.store(player, replicator);
            player.setData(ModAttachments.DUST, com.voidreclaimer.dust.DustData.of(10, 0, 0, 0));
            player.getInventory().setItem(0, new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.IRON_INGOT, 3));
            player.getInventory().setItem(1, new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.OAK_PLANKS, 1));
            helper.assertTrue(com.voidreclaimer.replicator.Replicator.queue(player, ITEMS_TOO) == com.voidreclaimer.replicator.ReplicatorLogic.QueueResult.QUEUED, "not queued");
            // Vanilla leaves a player invulnerable until the client says it has changed dimension, and a test player never says so.
            player.hasChangedDimension();
            player.hurt(space.damageSources().genericKill(), Float.MAX_VALUE);
            helper.startSequence()
                    .thenWaitUntil(() -> helper.assertFalse(Links.state(player).linked(), "still in the shell"))
                    .thenExecute(RestartTests::crash);
        });
    }

    @GameTest(template = "empty", timeoutTicks = 300)
    public static void aShellsQueuedItemsOutliveACrashAfterRestart(GameTestHelper helper) {
        ServerLevel space = TestPlayers.deepSpace(helper);
        ServerPlayer player = TestPlayers.joinAs(helper, QUEUE_PLAYER);
        LinkState state = Links.state(player);
        helper.assertTrue(state.outboxes().size() == 1, "the player file holds " + state.outboxes().size() + " outboxes");
        helper.assertTrue(com.voidreclaimer.replicator.Replicator.data(player).jobs().isEmpty(), "the job is in the queue as well as the shell");
        UUID id = state.outboxes().get(0).shell();
        helper.startSequence()
                .thenWaitUntil(() -> helper.assertTrue(space.getEntity(id) instanceof BodyHost, "the shell is lost"))
                .thenExecute(() -> {
                    BodyHost shell = (BodyHost) space.getEntity(id);
                    var carried = com.voidreclaimer.debug.ItemCensus.of(com.voidreclaimer.link.BodySnapshot.of(shell), player.registryAccess());
                    var ingot = net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(net.minecraft.world.item.Items.IRON_INGOT);
                    helper.assertTrue(carried.counts().getOrDefault(ingot, 0) == 3, "the shell holds " + carried.counts().getOrDefault(ingot, 0) + " of 3 ingots");
                    shell.discard();
                    player.setData(ModAttachments.LINK, LinkState.NONE);
                    TestPlayers.remove(player);
                    helper.succeed();
                });
    }

    private static final UUID CLASH_SHELL = new UUID(0x7E57L, 0xC1A5L);
    private static final BlockPos CLASH_OLD = new BlockPos(262_000, 80, 0);
    private static final BlockPos CLASH_NEW = new BlockPos(262_400, 80, 0);

    private static long chunk(BlockPos pos) {
        return ChunkPos.asLong(pos);
    }

    private static void force(ServerLevel level, BlockPos pos, boolean forced) {
        level.setChunkForced(pos.getX() >> 4, pos.getZ() >> 4, forced);
    }

    private static String found(ServerLevel level) {
        return level.getEntity(CLASH_SHELL) instanceof BodyHost shell ? "link number " + shell.epoch() + " at " + shell.blockPosition() : "nothing";
    }

    private static LinkState.Place place(ServerLevel level, BlockPos pos) {
        return new LinkState.Place(level.dimension(), Vec3.atCenterOf(pos), 0f, 0f);
    }

    /**
     * Two real copies on disk in two chunks, as a crash leaves them: link number 4 is written, its chunk unloads, and link number 5
     * goes into another chunk. The owner stays away, so only the clash itself can decide.
     */
    @GameTest(template = "empty", timeoutTicks = 900)
    public static void aNewerShellWinsAClashBeforeRestart(GameTestHelper helper) {
        ServerLevel space = TestPlayers.deepSpace(helper);
        NaniteGameTests.inLoadedArea(helper, CLASH_OLD, 1, () -> {
            force(space, CLASH_NEW, true);
            BodyHost pattern = BodyHost.spawn(space, Vec3.atCenterOf(CLASH_OLD), HostRole.SHELL, Bodies.preset(BuiltinBodies.DRONE), null);
            BodySnapshot body = BodySnapshot.of(pattern);
            pattern.discard();
            helper.startSequence()
                    .thenWaitUntil(() -> helper.assertTrue(space.areEntitiesLoaded(chunk(CLASH_NEW)), "the new copy's chunk has not loaded"))
                    .thenExecute(() -> {
                        // A failed run leaves a copy behind, and the fix under test would let it push out the one written here.
                        BodyHost.restore(space, place(space, CLASH_OLD), CLASH_SHELL, 4L, body, null).orElseThrow();
                        space.save(null, true, false);
                        force(space, CLASH_OLD, false);
                    })
                    .thenWaitUntil(() -> helper.assertFalse(space.areEntitiesLoaded(chunk(CLASH_OLD)), "the old copy's chunk has not unloaded"))
                    .thenExecute(() -> {
                        BodyHost.restore(space, place(space, CLASH_NEW), CLASH_SHELL, 5L, body, null).orElseThrow();
                        // Forced chunks are saved too, and the second server has to choose which copy loads first.
                        force(space, CLASH_NEW, false);
                        space.save(null, true, false);
                        helper.succeed();
                    });
        });
    }

    @GameTest(template = "empty", timeoutTicks = 900)
    public static void aNewerShellWinsAClashAfterRestart(GameTestHelper helper) {
        ServerLevel space = TestPlayers.deepSpace(helper);
        force(space, CLASH_OLD, true);
        helper.startSequence()
                .thenWaitUntil(() -> helper.assertTrue(space.areEntitiesLoaded(chunk(CLASH_OLD)), "the old copy's chunk has not loaded"))
                .thenExecute(() -> {
                    helper.assertTrue(space.getEntity(CLASH_SHELL) instanceof BodyHost old && old.epoch() == 4L, "expected link number 4 to clash with, found " + found(space));
                    force(space, CLASH_NEW, true);
                })
                .thenWaitUntil(() -> helper.assertTrue(space.areEntitiesLoaded(chunk(CLASH_NEW)), "the new copy's chunk has not loaded"))
                .thenExecute(() -> {
                    helper.assertTrue(space.getEntity(CLASH_SHELL) instanceof BodyHost shell && shell.epoch() == 5L, "the newer copy was refused, found " + found(space));
                    space.getEntity(CLASH_SHELL).discard();
                    force(space, CLASH_OLD, false);
                    force(space, CLASH_NEW, false);
                    helper.succeed();
                });
    }

    private static final UUID RECOVERY_PLAYER = new UUID(0x7E57L, 0xB0B4L);
    private static final UUID RECOVERY_SHELL = new UUID(0x7E57L, 0xC1A6L);
    private static final BlockPos RECOVERY_SPOT = new BlockPos(262_800, 80, 0);
    private static final BlockPos RECOVERY_NEWER = new BlockPos(263_200, 80, 0);

    /** What a crash leaves, on disk for real: a copy with link number 4, and a list that says 5 at a place where nothing stands. */
    @GameTest(template = "empty", timeoutTicks = 900)
    public static void anOldCopyIsRecoveredBeforeRestart(GameTestHelper helper) {
        ServerLevel space = TestPlayers.deepSpace(helper);
        NaniteGameTests.inLoadedArea(helper, RECOVERY_SPOT, 1, () -> {
            ServerPlayer player = TestPlayers.joinAs(helper, RECOVERY_PLAYER);
            BodyHost pattern = BodyHost.spawn(space, Vec3.atCenterOf(RECOVERY_SPOT), HostRole.SHELL, Bodies.preset(BuiltinBodies.DRONE), null);
            BodySnapshot body = BodySnapshot.of(pattern);
            pattern.discard();
            BodyHost.restore(space, place(space, RECOVERY_SPOT), RECOVERY_SHELL, 4L, body, player.getUUID()).orElseThrow();
            player.setData(ModAttachments.LINK, LinkState.NONE.withKnown(new LinkState.KnownShell(RECOVERY_SHELL, 5L, place(space, RECOVERY_NEWER), false, false)));
            space.save(null, true, false);
            player.server.getPlayerList().saveAll();
            RestartTests.crash();
        });
    }

    @GameTest(template = "empty", timeoutTicks = 900)
    public static void anOldCopyIsRecoveredAfterRestart(GameTestHelper helper) {
        ServerLevel space = TestPlayers.deepSpace(helper);
        ServerPlayer player = TestPlayers.joinAs(helper, RECOVERY_PLAYER);
        NaniteGameTests.inEntityTickingArea(helper, RECOVERY_SPOT, 1, () -> {
            AtomicReference<ShellRecovery.Result> ended = new AtomicReference<>();
            AtomicReference<Links.Result> picked = new AtomicReference<>();
            helper.startSequence()
                    .thenWaitUntil(() -> helper.assertTrue(Links.state(player).known(RECOVERY_SHELL).orElseThrow().oldCopy().isPresent(), "the old copy did not report in"))
                    .thenExecute(() -> ShellReach.request(player, RECOVERY_SHELL, 200, picked::set))
                    .thenWaitUntil(() -> helper.assertTrue(picked.get() != null, "the pick did not end"))
                    .thenExecute(() -> helper.assertTrue(picked.get() == Links.Result.NOT_FOUND, "the pick ended " + picked.get()))
                    .thenWaitUntil(() -> helper.assertTrue(Links.state(player).known(RECOVERY_SHELL).orElseThrow().recoverable(), "the picker would not offer it"))
                    .thenExecute(() -> ShellRecovery.request(player, RECOVERY_SHELL, ended::set))
                    .thenWaitUntil(() -> helper.assertTrue(ended.get() != null, "no answer"))
                    .thenExecute(() -> {
                        helper.assertTrue(ended.get() == ShellRecovery.Result.RECOVERED, "ended " + ended.get());
                        helper.assertTrue(space.getEntity(RECOVERY_SHELL) instanceof BodyHost shell && shell.epoch() == 6L, "the copy is not the shell");
                        helper.assertTrue(Links.link(player, (BodyHost) space.getEntity(RECOVERY_SHELL)) == Links.Result.LINKED, "no link to the recovered shell");
                        helper.assertTrue(Links.wake(player) == Links.Result.WOKEN, "no wake");
                        space.getEntity(RECOVERY_SHELL).discard();
                        player.setData(ModAttachments.LINK, LinkState.NONE);
                        TestPlayers.remove(player);
                        helper.succeed();
                    });
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
