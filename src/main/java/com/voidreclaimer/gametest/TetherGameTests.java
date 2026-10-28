package com.voidreclaimer.gametest;

import com.voidreclaimer.Config;
import com.voidreclaimer.VoidReclaimer;
import com.voidreclaimer.flight.FlightInput;
import com.voidreclaimer.flight.ThrusterFlight;
import com.voidreclaimer.registry.ModAttachments;
import com.voidreclaimer.registry.ModBlocks;
import com.voidreclaimer.tether.Tether;
import com.voidreclaimer.tether.TetherData;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/** Phase 4b: Tether Anchor clip, leash, recall pull and recall teleport. */
@GameTestHolder(VoidReclaimer.MOD_ID)
@PrefixGameTestTemplate(false)
public final class TetherGameTests {
    private TetherGameTests() {}

    @GameTest(template = "empty", timeoutTicks = 60)
    public static void clipUnclipAndLoseAnchor(GameTestHelper helper) {
        BlockPos anchor = new BlockPos(53_000, 80, 0);
        NaniteGameTests.inLoadedArea(helper, anchor, 3, () -> {
            ServerLevel space = TestPlayers.deepSpace(helper);
            space.setBlock(anchor, ModBlocks.TETHER_ANCHOR.get().defaultBlockState(), 3);
            ServerPlayer player = TestPlayers.survivalInDeepSpace(helper, anchor.getX() + 0.5, anchor.getY() + 1, anchor.getZ() + 2.5);
            try {
                Tether.toggleClip(player, anchor);
                TetherData data = Tether.data(player);
                helper.assertTrue(data.clipped() && data.isAnchoredAt(GlobalPos.of(space.dimension(), anchor)), "not clipped");
                Tether.toggleClip(player, anchor);
                helper.assertFalse(Tether.data(player).clipped(), "still clipped");
                helper.assertTrue(Tether.data(player).anchor().isPresent(), "recall point forgotten on unclip");

                Tether.toggleClip(player, anchor);
                space.setBlock(anchor, Blocks.AIR.defaultBlockState(), 3);
                helper.assertTrue(Tether.data(player).anchor().isEmpty() && !Tether.data(player).clipped(), "anchor removal not noticed");
                helper.succeed();
            } finally {
                TestPlayers.remove(player);
            }
        });
    }

    @GameTest(template = "empty", timeoutTicks = 60)
    public static void leashPullsBackPastItsLength(GameTestHelper helper) {
        BlockPos anchor = new BlockPos(54_000, 80, 0);
        NaniteGameTests.inLoadedArea(helper, anchor, 3, () -> {
            ServerLevel space = TestPlayers.deepSpace(helper);
            space.setBlock(anchor, ModBlocks.TETHER_ANCHOR.get().defaultBlockState(), 3);
            double past = Config.TETHER_LENGTH.get() + 3;
            ServerPlayer player = TestPlayers.survivalInDeepSpace(helper, anchor.getX() + 0.5 + past, anchor.getY(), anchor.getZ() + 0.5);
            try {
                player.setData(ModAttachments.TETHER, TetherData.NONE.withClip(GlobalPos.of(space.dimension(), anchor)));
                Vec3 velocity = ThrusterFlight.step(player, new Vec3(0.3, 0, 0), FlightInput.NONE);
                helper.assertTrue(velocity.x < 0, "leash did not pull back: " + velocity);
                // Unclipped, the same drift continues outward (dampener braking only).
                player.setData(ModAttachments.TETHER, TetherData.NONE);
                helper.assertTrue(ThrusterFlight.step(player, new Vec3(0.3, 0, 0), FlightInput.NONE).x > 0, "pulled without a tether");
                helper.succeed();
            } finally {
                TestPlayers.remove(player);
            }
        });
    }

    @GameTest(template = "empty", timeoutTicks = 80)
    public static void recallHoldStartsWinchThatCostsEnergyAndStopsOnArrival(GameTestHelper helper) {
        BlockPos anchor = new BlockPos(55_000, 80, 0);
        NaniteGameTests.inLoadedArea(helper, anchor, 3, () -> {
            ServerLevel space = TestPlayers.deepSpace(helper);
            space.setBlock(anchor, ModBlocks.TETHER_ANCHOR.get().defaultBlockState(), 3);
            ServerPlayer player = TestPlayers.survivalInDeepSpace(helper, anchor.getX() + 40.5, anchor.getY(), anchor.getZ() + 0.5);
            try {
                player.setData(ModAttachments.TETHER, TetherData.NONE.withClip(GlobalPos.of(space.dimension(), anchor)).withClipped(false));
                Tether.setRecallHeld(player, true);
                for (int i = 0; i < Tether.PULL_HOLD_TICKS - 1; i++) {
                    player.doTick();
                }
                helper.assertFalse(Tether.data(player).recalling(), "winch started before the hold finished");
                player.doTick();
                helper.assertTrue(Tether.data(player).recalling(), "winch did not start");
                Vec3 velocity = ThrusterFlight.step(player, Vec3.ZERO, FlightInput.NONE);
                helper.assertTrue(velocity.x < 0, "winch does not pull toward the anchor: " + velocity);

                // Sunlight charging and idle drain also run; compare against a second without the winch.
                float before = player.getData(ModAttachments.ENERGY).energy();
                for (int i = 0; i < 20; i++) {
                    player.doTick();
                }
                float usedWinching = before - player.getData(ModAttachments.ENERGY).energy();
                player.setData(ModAttachments.TETHER, Tether.data(player).withRecalling(false));
                Tether.setRecallHeld(player, false);
                before = player.getData(ModAttachments.ENERGY).energy();
                for (int i = 0; i < 20; i++) {
                    player.doTick();
                }
                float usedIdle = before - player.getData(ModAttachments.ENERGY).energy();
                double winchCost = usedWinching - usedIdle;
                helper.assertTrue(Math.abs(winchCost - Config.RECALL_PULL_ENERGY.get()) < 0.3,
                        "winch cost " + winchCost + " per second, expected " + Config.RECALL_PULL_ENERGY.get());
                Tether.setRecallHeld(player, true);
                for (int i = 0; i < Tether.PULL_HOLD_TICKS; i++) {
                    player.doTick();
                }
                helper.assertTrue(Tether.data(player).recalling(), "winch did not restart");

                player.teleportTo(space, anchor.getX() + 0.5, anchor.getY() + 1, anchor.getZ() + 0.5, 0f, 0f);
                player.doTick();
                helper.assertFalse(Tether.data(player).recalling(), "winch kept pulling on arrival");

                // Releasing the key stops it immediately.
                player.teleportTo(space, anchor.getX() + 30.5, anchor.getY(), anchor.getZ() + 0.5, 0f, 0f);
                Tether.setRecallHeld(player, true);
                for (int i = 0; i < Tether.PULL_HOLD_TICKS; i++) {
                    player.doTick();
                }
                Tether.setRecallHeld(player, false);
                helper.assertFalse(Tether.data(player).recalling(), "release did not stop the winch");
                helper.succeed();
            } finally {
                TestPlayers.remove(player);
            }
        });
    }

    @GameTest(template = "empty", timeoutTicks = 120)
    public static void farRecallTeleportsWithCooldown(GameTestHelper helper) {
        BlockPos anchor = new BlockPos(56_000, 80, 0);
        NaniteGameTests.inLoadedArea(helper, anchor, 3, () -> {
            ServerLevel space = TestPlayers.deepSpace(helper);
            space.setBlock(anchor, ModBlocks.TETHER_ANCHOR.get().defaultBlockState(), 3);
            double far = Config.RECALL_PULL_RANGE.get() + 50;
            ServerPlayer player = TestPlayers.survivalInDeepSpace(helper, anchor.getX() + 0.5 + far, anchor.getY(), anchor.getZ() + 0.5);
            try {
                player.setData(ModAttachments.TETHER, TetherData.NONE.withClip(GlobalPos.of(space.dimension(), anchor)).withClipped(false));
                float before = player.getData(ModAttachments.ENERGY).energy();
                Tether.setRecallHeld(player, true);
                for (int i = 0; i < Tether.TELEPORT_HOLD_TICKS; i++) {
                    player.doTick();
                }
                double distance = player.position().distanceTo(Vec3.atCenterOf(anchor));
                helper.assertTrue(distance < 3, "not teleported to the anchor, " + distance + " away");
                helper.assertFalse(Tether.data(player).recalling(), "teleport started the winch");
                float used = before - player.getData(ModAttachments.ENERGY).energy();
                // Sunlight charge over the hold offsets a little of the cost.
                helper.assertTrue(used >= Config.RECALL_TELEPORT_ENERGY.get() - 1.0, "teleport cost " + used);
                Tether.setRecallHeld(player, false);

                // Immediately again: on cooldown.
                player.teleportTo(space, anchor.getX() + 0.5 + far, anchor.getY(), anchor.getZ() + 0.5, 0f, 0f);
                Tether.setRecallHeld(player, true);
                for (int i = 0; i < Tether.TELEPORT_HOLD_TICKS; i++) {
                    player.doTick();
                }
                helper.assertTrue(player.position().distanceTo(Vec3.atCenterOf(anchor)) > far - 1, "teleported during cooldown");
                helper.succeed();
            } finally {
                TestPlayers.remove(player);
            }
        });
    }
}
