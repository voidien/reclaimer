package com.voidreclaimer.gametest;

import com.voidreclaimer.VoidReclaimer;
import com.voidreclaimer.body.Bodies;
import com.voidreclaimer.body.BuiltinBodies;
import com.voidreclaimer.link.BodyHost;
import com.voidreclaimer.link.HostRole;
import com.voidreclaimer.link.LinkState;
import com.voidreclaimer.link.Links;
import com.voidreclaimer.registry.ModAttachments;
import java.util.List;
import java.util.UUID;
import java.util.function.BiConsumer;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/** {@link com.voidreclaimer.api.event.ShellLinkEvent}: what an addon can see of a link, and stop. */
@GameTestHolder(VoidReclaimer.MOD_ID)
@PrefixGameTestTemplate(false)
public final class LinkEventGameTests {
    private LinkEventGameTests() {}

    private static void withShell(GameTestHelper helper, BlockPos origin, BiConsumer<ServerPlayer, BodyHost> test) {
        NaniteGameTests.inEntityTickingArea(helper, origin, 1, () -> {
            ServerPlayer player = TestPlayers.survivalInDeepSpace(helper, origin.getX() + 0.5, origin.getY(), origin.getZ() + 0.5);
            test.accept(player, BodyHost.spawn(player.serverLevel(), player.position().add(new Vec3(0.0, 0.0, 4.0)), HostRole.SHELL, Bodies.preset(BuiltinBodies.DRONE), player.getUUID()));
        });
    }

    private static List<String> events(ServerPlayer player) {
        return TestEventListeners.LINK_EVENTS.getOrDefault(player.getUUID(), List.of());
    }

    private static void finish(GameTestHelper helper, ServerPlayer player, UUID shell) {
        LinkOutboxGameTests.finish(helper, player, player.serverLevel(), shell);
    }

    @GameTest(template = "empty", timeoutTicks = 900)
    public static void aListenerCanRefuseALinkAndNothingMoves(GameTestHelper helper) {
        withShell(helper, new BlockPos(264_400, 80, 0), (player, shell) -> {
            player.addTag(TestEventListeners.REFUSE_LINK);
            Vec3 before = player.position();
            helper.assertTrue(Links.link(player, shell) == Links.Result.CANCELLED, "the link was not refused");
            helper.assertFalse(Links.state(player).linked(), "a refused link went ahead");
            helper.assertFalse(shell.isRemoved(), "a refused link took the shell");
            helper.assertTrue(player.position().distanceTo(before) < 0.01, "a refused link moved the player");
            helper.assertTrue(events(player).isEmpty(), "a refused link was reported: " + events(player));
            finish(helper, player, shell.getUUID());
        });
    }

    @GameTest(template = "empty", timeoutTicks = 900)
    public static void aLinkReportsItsStartAndItsEnd(GameTestHelper helper) {
        withShell(helper, new BlockPos(264_600, 80, 0), (player, shell) -> {
            UUID id = shell.getUUID();
            helper.assertTrue(Links.link(player, shell) == Links.Result.LINKED, "no link");
            helper.assertTrue(Links.wake(player) == Links.Result.WOKEN, "no wake");
            helper.assertTrue(events(player).equals(List.of("started", "ended LEFT")), "reported " + events(player));
            finish(helper, player, id);
        });
    }

    @GameTest(template = "empty", timeoutTicks = 900)
    public static void aLostBodyEndsItsLinkWithThatReason(GameTestHelper helper) {
        withShell(helper, new BlockPos(264_800, 80, 0), (player, shell) -> {
            UUID id = shell.getUUID();
            helper.assertTrue(Links.link(player, shell) == Links.Result.LINKED, "no link");
            player.hurt(player.serverLevel().damageSources().genericKill(), Float.MAX_VALUE);
            helper.startSequence()
                    .thenWaitUntil(() -> helper.assertFalse(Links.state(player).linked(), "still in the shell"))
                    .thenExecute(() -> {
                        helper.assertTrue(events(player).equals(List.of("started", "ended BODY_LOST")), "reported " + events(player));
                        finish(helper, player, id);
                    });
        });
    }

    /** Before a link nothing has changed, so a throw refuses it. After one, the link stands and so does the way home. */
    @GameTest(template = "empty", timeoutTicks = 900)
    public static void aBrokenListenerStrandsNobody(GameTestHelper helper) {
        withShell(helper, new BlockPos(265_000, 80, 0), (player, shell) -> {
            UUID id = shell.getUUID();
            player.addTag(TestEventListeners.THROW_BEFORE_LINK);
            helper.assertTrue(Links.link(player, shell) == Links.Result.CANCELLED, "a throw before a link did not refuse it");
            helper.assertFalse(Links.state(player).linked(), "a throw left a link");
            player.removeTag(TestEventListeners.THROW_BEFORE_LINK);
            player.addTag(TestEventListeners.THROW_AFTER_LINK);
            helper.assertTrue(Links.link(player, shell) == Links.Result.LINKED, "a throw after a link undid it");
            helper.assertTrue(Links.wake(player) == Links.Result.WOKEN, "a throw kept a player from home");
            helper.assertFalse(Links.state(player).linked(), "still linked");
            player.setData(ModAttachments.LINK, LinkState.NONE);
            finish(helper, player, id);
        });
    }
}
