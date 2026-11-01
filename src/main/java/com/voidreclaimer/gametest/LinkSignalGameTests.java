package com.voidreclaimer.gametest;

import com.voidreclaimer.Config;
import com.voidreclaimer.VoidReclaimer;
import com.voidreclaimer.body.Bodies;
import com.voidreclaimer.body.BuiltinBodies;
import com.voidreclaimer.dyson.Dyson;
import com.voidreclaimer.dyson.DysonSphereData;
import com.voidreclaimer.link.BodyHost;
import com.voidreclaimer.link.HostRole;
import com.voidreclaimer.link.LinkState;
import com.voidreclaimer.link.Links;
import com.voidreclaimer.link.Signal;
import com.voidreclaimer.link.SignalMath;
import com.voidreclaimer.link.UplinkSeatBlockEntity;
import com.voidreclaimer.network.UnlinkPayload;
import com.voidreclaimer.registry.ModAttachments;
import com.voidreclaimer.registry.ModBlocks;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/** A link from a seat lives by its signal. */
@GameTestHolder(VoidReclaimer.MOD_ID)
@PrefixGameTestTemplate(false)
public final class LinkSignalGameTests {
    private static final Vec3 AWAY = new Vec3(0.0, 0.0, 4.0);

    private LinkSignalGameTests() {}

    private static UplinkSeatBlockEntity seat(ServerLevel level, BlockPos pos) {
        level.setBlock(pos, ModBlocks.UPLINK_SEAT.get().defaultBlockState(), 3);
        return (UplinkSeatBlockEntity) level.getBlockEntity(pos);
    }

    private static ServerPlayer player(GameTestHelper helper, BlockPos origin) {
        return TestPlayers.survivalInDeepSpace(helper, origin.getX() + 0.5, origin.getY() + 2, origin.getZ() + 0.5);
    }

    private static UUID shell(ServerPlayer player) {
        return BodyHost.spawn(player.serverLevel(), player.position().add(AWAY), HostRole.SHELL, Bodies.preset(BuiltinBodies.DRONE), player.getUUID()).getUUID();
    }

    private static void finish(GameTestHelper helper, ServerPlayer player, ServerLevel level, BlockPos pos, UUID id) {
        if (level.getEntity(id) != null) {
            level.getEntity(id).discard();
        }
        level.removeBlock(pos, false);
        player.setData(ModAttachments.LINK, LinkState.NONE);
        TestPlayers.remove(player);
        helper.succeed();
    }

    @GameTest(template = "empty", timeoutTicks = 900)
    public static void aSeatWillNotReachAShellOutOfRange(GameTestHelper helper) {
        BlockPos pos = new BlockPos(148_000, 80, 0);
        double beyond = Config.LINK_SEAT_RANGE.get() + 200.0;
        LinkReachGameTests.farShell(helper, pos, new Vec3(pos.getX() + beyond, 80, 0.5), (player, id) -> {
            ServerLevel level = player.serverLevel();
            UplinkSeatBlockEntity seat = seat(level, pos);
            seat.connect(player);
            helper.runAfterDelay(60, () -> {
                helper.assertFalse(Links.state(player).linked(), "linked out of range");
                finish(helper, player, level, pos, id);
            });
        });
    }

    /** Puts a player in a shell from a seat, with chunks ready {@code distance} blocks off, and then runs {@code test}. */
    private static void linked(GameTestHelper helper, BlockPos pos, double distance, java.util.function.BiConsumer<ServerPlayer, UUID> test) {
        NaniteGameTests.inEntityTickingArea(helper, pos, 1, () -> {
            ServerPlayer player = player(helper, pos);
            ServerLevel level = player.serverLevel();
            ChunkPos far = new ChunkPos(BlockPos.containing(pos.getX() + distance, pos.getY(), pos.getZ()));
            level.setChunkForced(far.x, far.z, true);
            UUID id = shell(player);
            seat(level, pos).connect(player);
            helper.startSequence()
                    .thenWaitUntil(() -> helper.assertTrue(Links.state(player).linked(), "the seat made no link"))
                    .thenExecute(() -> test.accept(player, id));
        });
    }

    @GameTest(template = "empty", timeoutTicks = 400)
    public static void aShellOutOfRangeLosesItsSignal(GameTestHelper helper) {
        BlockPos pos = new BlockPos(148_200, 80, 0);
        double beyond = Config.LINK_SEAT_RANGE.get() + 200.0;
        linked(helper, pos, beyond, (player, id) -> {
            ServerLevel level = player.serverLevel();
            player.teleportTo(pos.getX() + beyond, pos.getY(), pos.getZ());
            Vec3 left = player.position();
            helper.startSequence()
                    .thenWaitUntil(() -> helper.assertFalse(Links.state(player).linked(), "the link outlived its signal"))
                    .thenExecuteAfter(2, () -> {
                        helper.assertTrue(level.getEntity(id) instanceof BodyHost shell && shell.role() == HostRole.SHELL, "the shell did not stay");
                        helper.assertTrue(level.getEntity(id).position().distanceTo(left) < 1.0, "the shell moved");
                        helper.assertTrue(player.position().distanceTo(Vec3.atCenterOf(pos)) < 4.0, "woke at " + player.position());
                        finish(helper, player, level, pos, id);
                    });
        });
    }

    @GameTest(template = "empty", timeoutTicks = 400)
    public static void theSignalFadesTowardsTheEdge(GameTestHelper helper) {
        BlockPos pos = new BlockPos(148_400, 80, 0);
        double range = Signal.range(helper.getLevel().getServer(), new LinkState.Place(TestPlayers.deepSpace(helper).dimension(), Vec3.atCenterOf(pos), 0f, 0f));
        double near = range * 0.9;
        linked(helper, pos, near, (player, id) -> {
            ServerLevel level = player.serverLevel();
            helper.assertTrue(player.getData(ModAttachments.LINK_SIGNAL) == 1f, "a link next to its seat shows static");
            player.teleportTo(pos.getX() + near, pos.getY(), pos.getZ());
            float expected = (float) SignalMath.strength(near, range, Config.LINK_STATIC_BAND.get());
            helper.startSequence()
                    .thenWaitUntil(() -> helper.assertTrue(Math.abs(player.getData(ModAttachments.LINK_SIGNAL) - expected) < 0.05f,
                            "strength " + player.getData(ModAttachments.LINK_SIGNAL) + ", not " + expected))
                    .thenExecute(() -> {
                        helper.assertTrue(Links.state(player).linked(), "the link dropped inside the range");
                        helper.assertTrue(Links.wake(player) == Links.Result.WOKEN, "no wake");
                        finish(helper, player, level, pos, id);
                    });
        });
    }

    @GameTest(template = "empty", timeoutTicks = 200)
    public static void theLeaveKeyWakesYou(GameTestHelper helper) {
        BlockPos pos = new BlockPos(149_200, 80, 0);
        linked(helper, pos, 0.0, (player, id) -> {
            ServerLevel level = player.serverLevel();
            UnlinkPayload.apply(player);
            helper.startSequence()
                    .thenWaitUntil(() -> helper.assertFalse(Links.state(player).linked(), "the key did nothing"))
                    .thenExecute(() -> finish(helper, player, level, pos, id));
        });
    }

    @GameTest(template = "empty", timeoutTicks = 200)
    public static void aLinkWithNoSeatHasNoSignalRules(GameTestHelper helper) {
        BlockPos pos = new BlockPos(148_600, 80, 0);
        NaniteGameTests.inEntityTickingArea(helper, pos, 1, () -> {
            ServerPlayer player = player(helper, pos);
            ServerLevel level = player.serverLevel();
            double beyond = Config.LINK_SEAT_RANGE.get() + 200.0;
            ChunkPos far = new ChunkPos(BlockPos.containing(pos.getX() + beyond, pos.getY(), pos.getZ()));
            level.setChunkForced(far.x, far.z, true);
            UUID id = shell(player);
            helper.assertTrue(Links.link(player, (BodyHost) level.getEntity(id)) == Links.Result.LINKED, "no link");
            player.teleportTo(pos.getX() + beyond, pos.getY(), pos.getZ());
            helper.runAfterDelay(45, () -> {
                helper.assertTrue(Links.state(player).linked(), "a debug link has a signal");
                helper.assertTrue(Links.wake(player) == Links.Result.WOKEN, "no wake");
                finish(helper, player, level, pos, id);
            });
        });
    }

    /** One tick, so that no other test sees this sphere. */
    @GameTest(template = "empty")
    public static void theDysonSphereAddsRange(GameTestHelper helper) {
        var server = helper.getLevel().getServer();
        LinkState.Place place = new LinkState.Place(TestPlayers.deepSpace(helper).dimension(), new Vec3(148_800.5, 80, 0.5), 0f, 0f);
        DysonSphereData before = DysonSphereData.of(server);
        double bare = Signal.range(server, place);
        Dyson.set(server, before.withTrusses(before.trusses() + 100_000L).withSatellites(before.satellites() + 100_000L));
        double more = Signal.range(server, place);
        boolean raised = DysonSphereData.of(server).tier() > before.tier();
        Dyson.set(server, before);
        helper.assertTrue(raised, "the test did not raise the tier");
        helper.assertTrue(more > bare, "a higher tier left the range at " + more);
        helper.succeed();
    }

    @GameTest(template = "empty", timeoutTicks = 200)
    public static void aSeatAtAStationReachesFarther(GameTestHelper helper) {
        BlockPos pos = new BlockPos(149_000, 80, 0);
        NaniteGameTests.inLoadedArea(helper, pos, 3, () -> {
            ServerLevel level = TestPlayers.deepSpace(helper);
            var server = level.getServer();
            UplinkSeatBlockEntity seat = seat(level, pos);
            double bare = seat.range();
            level.setBlock(pos.east(2), ModBlocks.STATION_CORE.get().defaultBlockState(), 3);
            helper.runAfterDelay(25, () -> {
                double at = seat.range();
                level.removeBlock(pos.east(2), false);
                level.removeBlock(pos, false);
                helper.assertTrue(Math.abs(at - bare * Config.LINK_STATION_RANGE.get()) < 0.5, "at a station it reaches " + at + ", away " + bare);
                helper.succeed();
            });
        });
    }
}
