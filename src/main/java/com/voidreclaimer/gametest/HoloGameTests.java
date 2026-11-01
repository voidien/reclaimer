package com.voidreclaimer.gametest;

import com.simibubi.create.AllBlocks;
import com.simibubi.create.api.behaviour.display.DisplaySource;
import com.simibubi.create.api.behaviour.display.DisplayTarget;
import com.simibubi.create.content.redstone.displayLink.DisplayLinkBlock;
import com.simibubi.create.content.redstone.displayLink.DisplayLinkBlockEntity;
import com.simibubi.create.content.redstone.displayLink.DisplayLinkContext;
import com.simibubi.create.content.redstone.nixieTube.NixieTubeBlockEntity;
import com.voidreclaimer.VoidReclaimer;
import com.voidreclaimer.compat.create.HoloDisplaySetup;
import com.voidreclaimer.core.BackupCoreBlockEntity;
import com.voidreclaimer.display.HoloDisplay;
import com.voidreclaimer.display.HoloProjectorBlockEntity;
import com.voidreclaimer.dyson.Dyson;
import com.voidreclaimer.dyson.DysonSphereData;
import com.voidreclaimer.registry.ModBlocks;
import com.voidreclaimer.registry.ModRegistries;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
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

    @GameTest(template = "empty", timeoutTicks = 300)
    public static void aReadoutSourceComposesAndFollowsItsSource(GameTestHelper helper) {
        BlockPos pos = new BlockPos(130_600, 80, 0);
        NaniteGameTests.inLoadedArea(helper, pos, 2, () -> {
            ServerLevel level = TestPlayers.deepSpace(helper);
            HoloProjectorBlockEntity projector = place(level, pos);
            BlockPos near = pos.east(3);
            BlockPos nearer = pos.west(2);
            BlockPos far = pos.south(9);
            level.setBlock(far, ModBlocks.BACKUP_CORE.get().defaultBlockState(), 3);
            projector.setSource(VoidReclaimer.id("backup_core"));
            helper.assertTrue(projector.display().lines().isEmpty(), "a core out of range showed " + projector.display().texts());
            level.setBlock(near, ModBlocks.BACKUP_CORE.get().defaultBlockState(), 3);
            level.setBlock(nearer, ModBlocks.BACKUP_CORE.get().defaultBlockState(), 3);
            ((BackupCoreBlockEntity) level.getBlockEntity(near)).setName("near");
            ((BackupCoreBlockEntity) level.getBlockEntity(nearer)).setName("nearer");
            projector.refresh();
            helper.assertTrue(projector.display().text(1).equals("nearer"), "read " + projector.display().texts());
            level.setBlock(nearer, Blocks.AIR.defaultBlockState(), 3);
            projector.refresh();
            helper.assertTrue(projector.display().text(1).equals("near"), "did not move on: " + projector.display().texts());
            level.setBlock(near, Blocks.AIR.defaultBlockState(), 3);
            level.setBlock(far, Blocks.AIR.defaultBlockState(), 3);
            projector.refresh();
            helper.assertTrue(projector.display().lines().isEmpty(), "still showing " + projector.display().texts());
            helper.succeed();
        });
    }

    @GameTest(template = "empty", timeoutTicks = 300)
    public static void playerReadoutReadsTheNearestBody(GameTestHelper helper) {
        BlockPos pos = new BlockPos(130_700, 80, 0);
        NaniteGameTests.inLoadedArea(helper, pos, 2, () -> {
            ServerLevel level = TestPlayers.deepSpace(helper);
            HoloProjectorBlockEntity projector = place(level, pos);
            projector.setSource(VoidReclaimer.id("player"));
            helper.assertTrue(projector.display().lines().isEmpty(), "no body in reach, but " + projector.display().texts());
            ServerPlayer player = TestPlayers.survivalInDeepSpace(helper, pos.getX() + 0.5, pos.getY() + 3, pos.getZ() + 0.5);
            player.setHealth(5);
            projector.refresh();
            helper.assertTrue(projector.display().text(0).equals(player.getGameProfile().getName()), "lines " + projector.display().texts());
            helper.assertTrue(projector.display().text(1).startsWith("Health 5.0 /"), "lines " + projector.display().texts());
            helper.succeed();
        });
    }

    @GameTest(template = "empty", timeoutTicks = 300)
    public static void theDysonReadoutNeedsNoSource(GameTestHelper helper) {
        BlockPos pos = new BlockPos(130_800, 80, 0);
        NaniteGameTests.inLoadedArea(helper, pos, 2, () -> {
            ServerLevel level = TestPlayers.deepSpace(helper);
            HoloProjectorBlockEntity projector = place(level, pos);
            DysonSphereData saved = DysonSphereData.of(level);
            Dyson.set(level.getServer(), new DysonSphereData(123, 4, 0, 0));
            projector.setSource(VoidReclaimer.id("dyson"));
            List<String> lines = projector.display().texts();
            Dyson.set(level.getServer(), saved);
            helper.assertTrue(lines.get(2).equals("Satellites 123"), "lines " + lines);
            helper.assertTrue(lines.get(3).startsWith("Trusses 4 / "), "lines " + lines);
            helper.succeed();
        });
    }

    @GameTest(template = "empty", timeoutTicks = 300)
    public static void theDeviceReadoutReadsAnyTankBesideIt(GameTestHelper helper) {
        BlockPos pos = new BlockPos(130_900, 80, 0);
        NaniteGameTests.inLoadedArea(helper, pos, 2, () -> {
            ServerLevel level = TestPlayers.deepSpace(helper);
            HoloProjectorBlockEntity projector = place(level, pos);
            projector.setSource(VoidReclaimer.id("device"));
            helper.assertTrue(projector.display().lines().isEmpty(), "nothing beside it, but " + projector.display().texts());
            level.setBlock(pos.below(), ModBlocks.ION_THRUSTER.get().defaultBlockState(), 3);
            projector.refresh();
            helper.assertTrue(projector.display().text(1).startsWith("Energy 0 / "), "lines " + projector.display().texts());
            helper.succeed();
        });
    }

    @GameTest(template = "empty", timeoutTicks = 300)
    public static void aCreateDisplayLinkDeliversText(GameTestHelper helper) {
        BlockPos pos = new BlockPos(131_000, 80, 0);
        NaniteGameTests.inLoadedArea(helper, pos, 2, () -> {
            ServerLevel level = TestPlayers.deepSpace(helper);
            HoloProjectorBlockEntity projector = place(level, pos);
            projector.setLine(1, "kept", HoloDisplay.NO_COLOR);
            // The link reads the block behind it: here the Nixie Tube below, which needs no player to set its text.
            BlockPos nixie = pos.east(2);
            BlockPos link = nixie.above();
            level.setBlock(nixie, AllBlocks.ORANGE_NIXIE_TUBE.getDefaultState(), 3);
            level.setBlock(link, AllBlocks.DISPLAY_LINK.getDefaultState().setValue(DisplayLinkBlock.FACING, Direction.UP), 3);
            ((NixieTubeBlockEntity) level.getBlockEntity(nixie)).displayCustomText("{\"text\":\"from create\"}", 1);
            DisplayLinkBlockEntity display = (DisplayLinkBlockEntity) level.getBlockEntity(link);
            display.target(pos);
            display.activeSource = DisplaySource.getAll(level, nixie).get(0);
            display.targetLine = 0;
            display.updateGatheredData();
            helper.assertTrue(projector.display().mode() == HoloDisplay.Mode.TEXT, "mode " + projector.display().mode());
            helper.assertTrue(projector.display().text(0).startsWith("from create"), "lines " + projector.display().texts());
            helper.assertTrue(projector.display().text(1).equals("kept"), "a link wiped another line: " + projector.display().texts());
            helper.succeed();
        });
    }

    @GameTest(template = "empty", timeoutTicks = 300)
    public static void aDirectAcceptTextKeepsOtherLines(GameTestHelper helper) {
        BlockPos pos = new BlockPos(131_100, 80, 0);
        NaniteGameTests.inLoadedArea(helper, pos, 2, () -> {
            ServerLevel level = TestPlayers.deepSpace(helper);
            HoloProjectorBlockEntity projector = place(level, pos);
            projector.setLines(List.of("one", "two", "three"));
            DisplayTarget target = DisplayTarget.get(level, pos);
            helper.assertTrue(target == HoloDisplaySetup.HOLO_PROJECTOR.get(), "the projector is not a display target: " + target);
            helper.assertTrue(target.provideStats(null).maxRows() == HoloDisplay.MAX_LINES, "rows");
            helper.assertTrue(target.provideStats(null).maxColumns() == HoloDisplay.MAX_CHARS, "columns");
            DisplayLinkContext context = new DisplayLinkContext(level, null) {
                @Override
                public BlockEntity getTargetBlockEntity() {
                    return projector;
                }
            };
            target.acceptText(1, List.of(Component.literal("TWO"), Component.literal("THREE")), context);
            helper.assertTrue(projector.display().texts().equals(List.of("one", "TWO", "THREE")), "lines " + projector.display().texts());
            // Past the last line is ignored, as the link's own offset can run off the end.
            target.acceptText(7, List.of(Component.literal("eight"), Component.literal("nine")), context);
            helper.assertTrue(projector.display().text(7).equals("eight"), "lines " + projector.display().texts());
            helper.succeed();
        });
    }
}
