package com.voidreclaimer.gametest;

import com.voidreclaimer.VoidReclaimer;
import com.voidreclaimer.display.HoloDisplay;
import com.voidreclaimer.display.HoloProjectorBlockEntity;
import com.voidreclaimer.registry.ModBlocks;
import com.voidreclaimer.registry.ModRegistries;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/** The Holo Projector: its block, its saved content, its refresh and what writes to it. */
@GameTestHolder(VoidReclaimer.MOD_ID)
@PrefixGameTestTemplate(false)
public final class HoloGameTests {
    private HoloGameTests() {}

    static HoloProjectorBlockEntity place(ServerLevel level, BlockPos pos) {
        level.setBlock(pos, Blocks.AIR.defaultBlockState(), 3);
        level.setBlock(pos, ModBlocks.HOLO_PROJECTOR.get().defaultBlockState(), 3);
        return (HoloProjectorBlockEntity) level.getBlockEntity(pos);
    }

    @GameTest(template = "empty", timeoutTicks = 300)
    public static void projectorPlacesAndSavesItsLines(GameTestHelper helper) {
        BlockPos pos = new BlockPos(130_000, 80, 0);
        NaniteGameTests.inLoadedArea(helper, pos, 2, () -> {
            ServerLevel level = TestPlayers.deepSpace(helper);
            HoloProjectorBlockEntity projector = place(level, pos);
            helper.assertTrue(projector.display().equals(HoloDisplay.EMPTY), "a new projector is empty");
            projector.setLine(0, "hello", HoloDisplay.NO_COLOR);
            projector.setLine(2, "world", 0xFF0000);
            projector.setScale(2f);
            HolderLookup.Provider registries = level.registryAccess();
            CompoundTag saved = projector.saveWithFullMetadata(registries);
            level.setBlock(pos, Blocks.AIR.defaultBlockState(), 3);
            HoloProjectorBlockEntity restored = place(level, pos);
            helper.assertTrue(restored.display().equals(HoloDisplay.EMPTY), "the new block started with the old content");
            restored.loadWithComponents(saved, registries);
            helper.assertTrue(restored.display().texts().equals(List.of("hello", "", "world")), "lines " + restored.display().texts());
            helper.assertTrue(restored.display().lines().get(2).color() == 0xFF0000, "a line lost its colour");
            helper.assertTrue(restored.display().scale() == 2f, "scale " + restored.display().scale());
            helper.succeed();
        });
    }

    @GameTest(template = "empty", timeoutTicks = 300)
    public static void aSneakUseCyclesTheReadoutSources(GameTestHelper helper) {
        BlockPos pos = new BlockPos(130_100, 80, 0);
        NaniteGameTests.inLoadedArea(helper, pos, 2, () -> {
            ServerLevel level = TestPlayers.deepSpace(helper);
            ServerPlayer player = TestPlayers.survivalInDeepSpace(helper, pos.getX() + 0.5, pos.getY() + 1, pos.getZ() + 0.5);
            HoloProjectorBlockEntity projector = place(level, pos);
            projector.setLine(0, "text", HoloDisplay.NO_COLOR);
            BlockHitResult hit = new BlockHitResult(Vec3.atCenterOf(pos), net.minecraft.core.Direction.UP, pos, false);
            // A plain use does nothing: the block is not a button.
            player.setShiftKeyDown(false);
            player.gameMode.useItemOn(player, level, player.getMainHandItem(), InteractionHand.MAIN_HAND, hit);
            helper.assertTrue(projector.display().mode() == HoloDisplay.Mode.TEXT, "a plain use changed the projector");
            player.setShiftKeyDown(true);
            player.gameMode.useItemOn(player, level, player.getMainHandItem(), InteractionHand.MAIN_HAND, hit);
            helper.assertTrue(projector.display().mode() == HoloDisplay.Mode.READOUT, "a sneak-use did not pick a readout");
            var first = projector.display().source();
            helper.assertTrue(ModRegistries.HOLO_READOUTS.containsKey(first), "picked " + first);
            int kinds = ModRegistries.HOLO_READOUTS.size();
            for (int i = 0; i < kinds; i++) {
                player.gameMode.useItemOn(player, level, player.getMainHandItem(), InteractionHand.MAIN_HAND, hit);
            }
            helper.assertTrue(projector.display().source().equals(first), "cycling every kind did not come back round");
            helper.succeed();
        });
    }

    @GameTest(template = "empty", timeoutTicks = 300)
    public static void aReadoutOnlyResyncsWhenItsLinesChange(GameTestHelper helper) {
        BlockPos pos = new BlockPos(130_200, 80, 0);
        NaniteGameTests.inLoadedArea(helper, pos, 2, () -> {
            ServerLevel level = TestPlayers.deepSpace(helper);
            HoloProjectorBlockEntity projector = place(level, pos);
            GameTestPack.GAUGE_READINGS.put(pos, 1);
            projector.setSource(GameTestPack.GAUGE.location());
            int syncs = projector.syncs();
            // Several refreshes go by with the ticker running: the same lines are never sent again.
            helper.runAfterDelay(40, () -> {
                helper.assertTrue(projector.syncs() == syncs, "unchanged lines were sent " + (projector.syncs() - syncs) + " times");
                GameTestPack.GAUGE_READINGS.put(pos, 2);
                projector.refresh();
                helper.assertTrue(projector.display().texts().equals(List.of("gauge 2")), "lines " + projector.display().texts());
                helper.assertTrue(projector.syncs() == syncs + 1, "changed lines were sent " + (projector.syncs() - syncs) + " times");
                helper.succeed();
            });
        });
    }

    @GameTest(template = "empty", timeoutTicks = 300)
    public static void theServerTickerRefreshesAReadout(GameTestHelper helper) {
        BlockPos pos = new BlockPos(130_300, 80, 0);
        NaniteGameTests.inLoadedArea(helper, pos, 2, () -> {
            ServerLevel level = TestPlayers.deepSpace(helper);
            HoloProjectorBlockEntity projector = place(level, pos);
            projector.setSource(HoloDisplay.DEFAULT_SOURCE);
            List<String> shown = projector.display().texts();
            helper.runAfterDelay(60, () -> {
                helper.assertTrue(!projector.display().texts().equals(shown), "the ticker did not refresh the clock");
                helper.succeed();
            });
        });
    }

    @GameTest(template = "empty", timeoutTicks = 300)
    public static void aProgramWriteReplacesAReadoutAndTheTickerLeavesItAlone(GameTestHelper helper) {
        BlockPos pos = new BlockPos(130_400, 80, 0);
        NaniteGameTests.inLoadedArea(helper, pos, 2, () -> {
            ServerLevel level = TestPlayers.deepSpace(helper);
            HoloProjectorBlockEntity projector = place(level, pos);
            projector.setSource(HoloDisplay.DEFAULT_SOURCE);
            projector.setLine(0, "mine", HoloDisplay.NO_COLOR);
            helper.runAfterDelay(30, () -> {
                helper.assertTrue(projector.display().texts().equals(List.of("mine")), "lines " + projector.display().texts());
                helper.succeed();
            });
        });
    }

    @GameTest(template = "empty", timeoutTicks = 300)
    public static void anAddonsReadoutKindShowsOnAProjector(GameTestHelper helper) {
        BlockPos pos = new BlockPos(130_500, 80, 0);
        NaniteGameTests.inLoadedArea(helper, pos, 2, () -> {
            ServerLevel level = TestPlayers.deepSpace(helper);
            HoloProjectorBlockEntity projector = place(level, pos);
            projector.setSource(GameTestPack.GAUGE.location());
            helper.assertTrue(projector.display().texts().equals(List.of("gauge 0")), "lines " + projector.display().texts());
            projector.setSource(VoidReclaimer.id("no_such_kind"));
            helper.assertTrue(projector.display().lines().isEmpty(), "an unknown kind showed " + projector.display().texts());
            helper.assertTrue(projector.display().source().equals(VoidReclaimer.id("no_such_kind")), "an unknown kind was not kept");
            helper.succeed();
        });
    }
}
