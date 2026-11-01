package com.voidreclaimer.gametest;

import com.voidreclaimer.VoidReclaimer;
import com.voidreclaimer.body.Bodies;
import com.voidreclaimer.body.BuiltinBodies;
import com.voidreclaimer.link.BodyHost;
import com.voidreclaimer.link.HostRole;
import com.voidreclaimer.link.LinkState;
import com.voidreclaimer.link.Links;
import com.voidreclaimer.link.UplinkSeatBlockEntity;
import com.voidreclaimer.registry.ModAttachments;
import com.voidreclaimer.registry.ModBlocks;
import java.util.List;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/** The Uplink Seat: a link that remembers where it came from. */
@GameTestHolder(VoidReclaimer.MOD_ID)
@PrefixGameTestTemplate(false)
public final class UplinkSeatGameTests {
    private static final Vec3 AWAY = new Vec3(0.0, 0.0, 4.0);

    private UplinkSeatGameTests() {}

    private static UplinkSeatBlockEntity seat(ServerLevel level, BlockPos pos) {
        level.setBlock(pos, ModBlocks.UPLINK_SEAT.get().defaultBlockState(), 3);
        return (UplinkSeatBlockEntity) level.getBlockEntity(pos);
    }

    private static ServerPlayer player(GameTestHelper helper, BlockPos origin) {
        return TestPlayers.survivalInDeepSpace(helper, origin.getX() + 0.5, origin.getY() + 2, origin.getZ() + 0.5);
    }

    private static BodyHost shell(ServerPlayer player) {
        return BodyHost.spawn(player.serverLevel(), player.position().add(AWAY), HostRole.SHELL, Bodies.preset(BuiltinBodies.DRONE), player.getUUID());
    }

    private static void finish(GameTestHelper helper, ServerPlayer player, ServerLevel level, BlockPos pos) {
        level.removeBlock(pos, false);
        player.setData(ModAttachments.LINK, LinkState.NONE);
        TestPlayers.remove(player);
        helper.succeed();
    }

    @GameTest(template = "empty", timeoutTicks = 200)
    public static void aSeatLinksAndALinkKnowsItsSeat(GameTestHelper helper) {
        BlockPos pos = new BlockPos(146_000, 80, 0);
        NaniteGameTests.inEntityTickingArea(helper, pos, 1, () -> {
            ServerPlayer player = player(helper, pos);
            ServerLevel level = player.serverLevel();
            UplinkSeatBlockEntity seat = seat(level, pos);
            UUID id = shell(player).getUUID();
            helper.assertTrue(seat.occupant().isEmpty(), "a new seat is taken");
            seat.connect(player);
            helper.startSequence()
                    .thenWaitUntil(() -> helper.assertTrue(Links.state(player).linked(), "the seat made no link"))
                    .thenExecute(() -> {
                        LinkState.Link link = Links.state(player).link().orElseThrow();
                        helper.assertTrue(link.shell().equals(id), "linked to the wrong shell");
                        helper.assertTrue(link.seat().filter(seat.id()::equals).isPresent(), "the link does not know its seat");
                        helper.assertTrue(seat.occupant().filter(player::equals).isPresent(), "the seat does not know its occupant");
                        helper.assertTrue(Links.wake(player) == Links.Result.WOKEN, "no wake");
                        helper.assertTrue(seat.occupant().isEmpty(), "a wake did not free the seat");
                        level.getEntity(id).discard();
                        finish(helper, player, level, pos);
                    });
        });
    }

    @GameTest(template = "empty", timeoutTicks = 200)
    public static void aSeatWithNoShellChangesNothing(GameTestHelper helper) {
        BlockPos pos = new BlockPos(146_200, 80, 0);
        NaniteGameTests.inEntityTickingArea(helper, pos, 1, () -> {
            ServerPlayer player = player(helper, pos);
            ServerLevel level = player.serverLevel();
            seat(level, pos).connect(player);
            helper.runAfterDelay(5, () -> {
                helper.assertFalse(Links.state(player).linked(), "linked to nothing");
                finish(helper, player, level, pos);
            });
        });
    }

    @GameTest(template = "empty", timeoutTicks = 200)
    public static void aTakenSeatTurnsASecondPlayerAway(GameTestHelper helper) {
        BlockPos pos = new BlockPos(146_400, 80, 0);
        NaniteGameTests.inEntityTickingArea(helper, pos, 1, () -> {
            ServerPlayer first = player(helper, pos);
            ServerPlayer second = player(helper, pos);
            ServerLevel level = first.serverLevel();
            UplinkSeatBlockEntity seat = seat(level, pos);
            UUID firstShell = shell(first).getUUID();
            UUID secondShell = shell(second).getUUID();
            seat.connect(first);
            helper.startSequence()
                    .thenWaitUntil(() -> helper.assertTrue(Links.state(first).linked(), "the seat made no link"))
                    .thenExecute(() -> seat.connect(second))
                    .thenExecuteAfter(5, () -> {
                        helper.assertFalse(Links.state(second).linked(), "two players in one seat");
                        Links.wake(first);
                        level.getEntity(firstShell).discard();
                        level.getEntity(secondShell).discard();
                        second.setData(ModAttachments.LINK, LinkState.NONE);
                        TestPlayers.remove(second);
                        finish(helper, first, level, pos);
                    });
        });
    }

    private static List<BodyHost> mirrors(ServerLevel level, BlockPos pos) {
        return level.getEntitiesOfClass(BodyHost.class, new AABB(pos).inflate(3.0), host -> host.role() == HostRole.MIRROR);
    }

    @GameTest(template = "empty", timeoutTicks = 300)
    public static void aSeatShowsTheMainBodyWhileLinked(GameTestHelper helper) {
        BlockPos pos = new BlockPos(146_800, 80, 0);
        NaniteGameTests.inEntityTickingArea(helper, pos, 1, () -> {
            ServerPlayer player = player(helper, pos);
            ServerLevel level = player.serverLevel();
            UplinkSeatBlockEntity seat = seat(level, pos);
            Object home = Bodies.chassis(level.registryAccess(), Bodies.data(player));
            UUID id = BodyHost.spawn(level, player.position().add(AWAY), HostRole.SHELL, Bodies.preset(BuiltinBodies.MECH), player.getUUID()).getUUID();
            seat.connect(player);
            helper.startSequence()
                    .thenWaitUntil(() -> helper.assertTrue(mirrors(level, pos).size() == 1, "no mirror on the seat"))
                    .thenExecute(() -> {
                        BodyHost mirror = mirrors(level, pos).get(0);
                        Object worn = Bodies.chassis(level.registryAccess(), Bodies.data(mirror));
                        helper.assertTrue(worn.equals(home), "the mirror wears " + worn + ", not " + home);
                        helper.assertFalse(worn.equals(Bodies.chassis(level.registryAccess(), Bodies.data(player))), "the test needs two different bodies");
                        helper.assertFalse(mirror.shouldBeSaved(), "a mirror would be saved");
                        helper.assertFalse(mirror.hurt(level.damageSources().generic(), 5f), "a mirror was hurt");
                        mirror.discard();
                    })
                    .thenWaitUntil(() -> helper.assertTrue(mirrors(level, pos).size() == 1, "the seat did not build a new mirror"))
                    .thenExecute(() -> helper.assertTrue(Links.wake(player) == Links.Result.WOKEN, "no wake"))
                    .thenWaitUntil(() -> helper.assertTrue(mirrors(level, pos).isEmpty(), "the mirror outstayed the link"))
                    .thenExecute(() -> {
                        level.getEntity(id).discard();
                        finish(helper, player, level, pos);
                    });
        });
    }

    @GameTest(template = "empty", timeoutTicks = 300)
    public static void aBrokenSeatTakesItsMirrorAndLeavesTheLink(GameTestHelper helper) {
        BlockPos pos = new BlockPos(147_000, 80, 0);
        NaniteGameTests.inEntityTickingArea(helper, pos, 1, () -> {
            ServerPlayer player = player(helper, pos);
            ServerLevel level = player.serverLevel();
            UUID id = shell(player).getUUID();
            seat(level, pos).connect(player);
            helper.startSequence()
                    .thenWaitUntil(() -> helper.assertTrue(mirrors(level, pos).size() == 1, "no mirror on the seat"))
                    .thenExecute(() -> level.removeBlock(pos, false))
                    .thenWaitUntil(() -> helper.assertTrue(mirrors(level, pos).isEmpty(), "a mirror with no seat"))
                    .thenExecute(() -> {
                        helper.assertTrue(Links.state(player).linked(), "a broken seat ended the link");
                        helper.assertTrue(Links.wake(player) == Links.Result.WOKEN, "no wake");
                        level.getEntity(id).discard();
                        finish(helper, player, level, pos);
                    });
        });
    }

    /** A seat that lost its id would lose every link that names it. */
    @GameTest(template = "empty", timeoutTicks = 100)
    public static void aSeatKeepsItsIdThroughASave(GameTestHelper helper) {
        BlockPos pos = new BlockPos(146_600, 80, 0);
        NaniteGameTests.inLoadedArea(helper, pos, 1, () -> {
            ServerLevel level = TestPlayers.deepSpace(helper);
            UplinkSeatBlockEntity seat = seat(level, pos);
            CompoundTag saved = seat.saveWithoutMetadata(level.registryAccess());
            UplinkSeatBlockEntity loaded = new UplinkSeatBlockEntity(pos, seat.getBlockState());
            loaded.loadWithComponents(saved, level.registryAccess());
            helper.assertTrue(loaded.id().equals(seat.id()), "the id changed");
            level.removeBlock(pos, false);
            helper.succeed();
        });
    }
}
