package com.voidreclaimer.gametest;

import com.voidreclaimer.VoidReclaimer;
import com.voidreclaimer.earth.RuinedEarth;
import com.voidreclaimer.registry.ModBlocks;
import com.voidreclaimer.terraform.SpireAssembly;
import com.voidreclaimer.terraform.SpireBlock;
import com.voidreclaimer.terraform.SpireBlockEntity;
import com.voidreclaimer.terraform.SpireTower;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * The Spire tower: a Spire facing south with a frame of Scaffold. Each test stands at chunk-local x 8, z 0, so the whole
 * tower lies in one force-loaded chunk.
 */
@GameTestHolder(VoidReclaimer.MOD_ID)
@PrefixGameTestTemplate(false)
public final class SpireTowerGameTests {
    private SpireTowerGameTests() {}

    private static final int Y = 80;
    private static final Direction FACING = Direction.SOUTH;
    private static final int COLUMN_CELLS = 27;

    private interface Test {
        void run(ServerLevel level, BlockPos pos);
    }

    private static void inArea(GameTestHelper helper, int x, Test test) {
        BlockPos pos = new BlockPos(x, Y, 0);
        NaniteGameTests.inLoadedArea(helper, pos, 2, () -> {
            ServerLevel level = TestPlayers.deepSpace(helper);
            clear(level, pos);
            test.run(level, pos);
        });
    }

    /** Empties the tower's box (the world persists between runs). */
    static void clear(ServerLevel level, BlockPos pos) {
        for (int across = -SpireTower.HALF; across <= SpireTower.HALF; across++) {
            for (int forward = 0; forward < SpireTower.SIZE; forward++) {
                for (int up = 0; up <= SpireTower.HEIGHT; up++) {
                    level.setBlock(SpireTower.cell(pos, FACING, across, forward, up), Blocks.AIR.defaultBlockState(), 3);
                }
            }
        }
    }

    static SpireBlockEntity place(ServerLevel level, BlockPos pos) {
        level.setBlock(pos, ModBlocks.TERRAFORMING_SPIRE.get().defaultBlockState().setValue(BlockStateProperties.HORIZONTAL_FACING, FACING), 3);
        return (SpireBlockEntity) level.getBlockEntity(pos);
    }

    static void frame(ServerLevel level, BlockPos pos, Block block) {
        for (BlockPos cell : SpireTower.frame(pos, FACING)) {
            level.setBlock(cell, block.defaultBlockState(), 3);
        }
    }

    private static int columnBlocks(ServerLevel level, BlockPos pos) {
        int count = 0;
        for (BlockPos cell : SpireTower.column(pos, FACING)) {
            if (level.getBlockState(cell).is(ModBlocks.SPIRE_COLUMN.get())) {
                count++;
            }
        }
        return count;
    }

    /** A Spire with a complete Scaffold frame, assembled. */
    static SpireBlockEntity build(GameTestHelper helper, ServerLevel level, BlockPos pos) {
        SpireBlockEntity spire = place(level, pos);
        frame(level, pos, ModBlocks.SCAFFOLD.get());
        SpireAssembly.Result result = SpireAssembly.assemble(spire);
        helper.assertTrue(result == SpireAssembly.Result.ASSEMBLED, "assembly: " + result);
        return spire;
    }

    private static void ticks(SpireBlockEntity spire, int count) {
        for (int i = 0; i < count; i++) {
            spire.tick();
        }
    }

    @GameTest(template = "empty", timeoutTicks = 200)
    public static void aCompleteFrameAssemblesAndFillsTheColumn(GameTestHelper helper) {
        inArea(helper, 130_008, (level, pos) -> {
            SpireBlockEntity spire = place(level, pos);
            frame(level, pos, ModBlocks.PORTAL_FRAME.get());
            helper.assertTrue(SpireAssembly.assemble(spire) == SpireAssembly.Result.ASSEMBLED, "a Portal Frame tower did not assemble");
            helper.assertTrue(spire.assembled(), "not marked assembled");
            helper.assertTrue(columnBlocks(level, pos) == COLUMN_CELLS, "column blocks: " + columnBlocks(level, pos));
            helper.assertTrue(level.getBlockState(SpireTower.centre(pos, FACING).above(SpireTower.HEIGHT - 1)).isSolid(), "the column is not solid");
            helper.assertTrue(SpireAssembly.assemble(spire) == SpireAssembly.Result.ALREADY_ASSEMBLED, "assembled twice");

            helper.assertTrue(SpireAssembly.disassemble(spire) == SpireAssembly.Result.DISASSEMBLED, "did not set down");
            helper.assertTrue(!spire.assembled() && columnBlocks(level, pos) == 0, "column blocks left: " + columnBlocks(level, pos));
            helper.assertTrue(level.getBlockState(SpireTower.cell(pos, FACING, 2, 0, 0)).is(ModBlocks.PORTAL_FRAME.get()), "setting down took the frame");
            helper.assertTrue(SpireAssembly.disassemble(spire) == SpireAssembly.Result.NOT_ASSEMBLED, "set down twice");
            helper.succeed();
        });
    }

    @GameTest(template = "empty", timeoutTicks = 200)
    public static void aGapOrAnObstacleRefusesAssembly(GameTestHelper helper) {
        inArea(helper, 130_040, (level, pos) -> {
            SpireBlockEntity spire = place(level, pos);
            frame(level, pos, ModBlocks.SCAFFOLD.get());
            BlockPos top = SpireTower.cell(pos, FACING, SpireTower.HALF, SpireTower.SIZE - 1, SpireTower.HEIGHT - 1);
            level.setBlock(top, Blocks.AIR.defaultBlockState(), 3);
            helper.assertTrue(SpireAssembly.assemble(spire) == SpireAssembly.Result.FRAME_INCOMPLETE, "assembled with a gap");
            level.setBlock(top, Blocks.DIRT.defaultBlockState(), 3);
            helper.assertTrue(SpireAssembly.assemble(spire) == SpireAssembly.Result.FRAME_INCOMPLETE, "dirt counted as frame");
            level.setBlock(top, ModBlocks.SCAFFOLD.get().defaultBlockState(), 3);

            BlockPos inside = SpireTower.cell(pos, FACING, 1, 2, 7);
            level.setBlock(inside, Blocks.STONE.defaultBlockState(), 3);
            helper.assertTrue(SpireAssembly.assemble(spire) == SpireAssembly.Result.BLOCKED, "assembled over a stone");
            helper.assertTrue(columnBlocks(level, pos) == 0 && !spire.assembled(), "a refused assembly left column blocks");
            level.setBlock(inside, Blocks.AIR.defaultBlockState(), 3);
            helper.assertTrue(SpireAssembly.assemble(spire) == SpireAssembly.Result.ASSEMBLED, "did not assemble once clear");
            SpireAssembly.disassemble(spire);
            helper.succeed();
        });
    }

    /** The GameTest datapack adds amethyst to #void_reclaimer:spire_frame: no code names it. */
    @GameTest(template = "empty", timeoutTicks = 200)
    public static void theFrameTagDecidesWhatCounts(GameTestHelper helper) {
        inArea(helper, 130_072, (level, pos) -> {
            SpireBlockEntity spire = place(level, pos);
            frame(level, pos, Blocks.AMETHYST_BLOCK);
            helper.assertTrue(SpireAssembly.assemble(spire) == SpireAssembly.Result.ASSEMBLED, "a tagged block did not form the frame");
            SpireAssembly.disassemble(spire);
            helper.succeed();
        });
    }

    @GameTest(template = "empty", timeoutTicks = 200)
    public static void aBrokenFrameSetsTheTowerDownAndAMissingColumnBlockGrowsBack(GameTestHelper helper) {
        inArea(helper, 130_104, (level, pos) -> {
            SpireBlockEntity spire = build(helper, level, pos);
            BlockPos hole = SpireTower.cell(pos, FACING, -1, 3, 4);
            level.setBlock(hole, Blocks.AIR.defaultBlockState(), 3);
            ticks(spire, 25);
            helper.assertTrue(level.getBlockState(hole).is(ModBlocks.SPIRE_COLUMN.get()), "the column did not grow back");

            level.setBlock(SpireTower.cell(pos, FACING, -SpireTower.HALF, 0, 5), Blocks.AIR.defaultBlockState(), 3);
            ticks(spire, 25);
            helper.assertFalse(spire.assembled(), "still assembled with a broken frame");
            helper.assertTrue(columnBlocks(level, pos) == 0, "column blocks left after the frame broke: " + columnBlocks(level, pos));
            helper.succeed();
        });
    }

    @GameTest(template = "empty", timeoutTicks = 200)
    public static void breakingTheSpireSetsTheTowerDown(GameTestHelper helper) {
        inArea(helper, 130_136, (level, pos) -> {
            build(helper, level, pos);
            level.destroyBlock(pos, false);
            helper.assertTrue(columnBlocks(level, pos) == 0, "column blocks left after the Spire broke: " + columnBlocks(level, pos));
            helper.succeed();
        });
    }

    /** Sneak-use is what a player does: it assembles, and sets down again. */
    @GameTest(template = "empty", timeoutTicks = 200)
    public static void sneakUseAssemblesAndSetsDown(GameTestHelper helper) {
        inArea(helper, 130_168, (level, pos) -> {
            SpireBlockEntity spire = place(level, pos);
            frame(level, pos, ModBlocks.SCAFFOLD.get());
            ServerPlayer player = TestPlayers.survivalInDeepSpace(helper, pos.getX() + 0.5, pos.getY() + 1, pos.getZ() - 1.5);
            try {
                BlockHitResult hit = new BlockHitResult(Vec3.atCenterOf(pos), Direction.NORTH, pos, false);
                BlockState state = level.getBlockState(pos);
                player.setShiftKeyDown(true);
                state.useWithoutItem(level, player, hit);
                helper.assertTrue(spire.assembled(), "sneak-use did not assemble");
                state.useWithoutItem(level, player, hit);
                helper.assertFalse(spire.assembled(), "sneak-use did not set down");
                helper.succeed();
            } finally {
                TestPlayers.remove(player);
            }
        });
    }

    /** The frame and column are found from the facing, so a wrench must leave a standing tower alone; a lone Spire turns. */
    @GameTest(template = "empty", timeoutTicks = 200)
    public static void aWrenchCannotTurnAStandingTower(GameTestHelper helper) {
        inArea(helper, 130_200, (level, pos) -> {
            SpireBlockEntity spire = build(helper, level, pos);
            ServerPlayer player = TestPlayers.survivalInDeepSpace(helper, pos.getX() + 0.5, pos.getY() + 1, pos.getZ() - 1.5);
            try {
                BlockHitResult hit = new BlockHitResult(Vec3.atCenterOf(pos).add(0, 0.5, 0), Direction.UP, pos, false);
                UseOnContext wrench = new UseOnContext(player, InteractionHand.MAIN_HAND, hit);
                SpireBlock block = ModBlocks.TERRAFORMING_SPIRE.get();
                block.onWrenched(level.getBlockState(pos), wrench);
                helper.assertTrue(level.getBlockState(pos).getValue(BlockStateProperties.HORIZONTAL_FACING) == FACING, "the wrench turned a standing tower");

                SpireAssembly.disassemble(spire);
                block.onWrenched(level.getBlockState(pos), wrench);
                helper.assertTrue(level.getBlockState(pos).getValue(BlockStateProperties.HORIZONTAL_FACING) != FACING, "the wrench did not turn a lone Spire");
                helper.succeed();
            } finally {
                TestPlayers.remove(player);
            }
        });
    }

    @GameTest(template = "empty", timeoutTicks = 200)
    public static void theTowerReachesTheClientAndTheSave(GameTestHelper helper) {
        inArea(helper, 130_232, (level, pos) -> {
            SpireBlockEntity spire = build(helper, level, pos);
            helper.assertTrue(spire.getUpdateTag(level.registryAccess()).getBoolean("assembled"), "the client packet lacks the tower");
            CompoundTag tag = spire.saveWithFullMetadata(level.registryAccess());
            BlockEntity loaded = BlockEntity.loadStatic(pos, level.getBlockState(pos), tag, level.registryAccess());
            helper.assertTrue(loaded instanceof SpireBlockEntity restored && restored.assembled(), "the tower did not survive a reload");
            SpireAssembly.disassemble(spire);
            helper.succeed();
        });
    }

    /** A Spire with no tower can only say so, on ruined Earth, where a right-click used to fire it. */
    @GameTest(template = "empty", timeoutTicks = 200)
    public static void aSpireWithoutItsTowerRefuses(GameTestHelper helper) {
        ServerLevel earth = helper.getLevel().getServer().getLevel(RuinedEarth.DIMENSION);
        BlockPos pos = new BlockPos(360, 200, -400);
        try {
            SpireBlockEntity spire = place(earth, pos);
            helper.assertTrue(spire.tryStart() == SpireBlockEntity.Start.NOT_ASSEMBLED, "fired without a tower");
            helper.assertFalse(spire.running(), "running without a tower");
            helper.succeed();
        } finally {
            earth.setBlock(pos, Blocks.AIR.defaultBlockState(), 3);
        }
    }

    /**
     * What an older world holds: a block with only {@code powered} and a block entity with only the pulse's numbers. It
     * becomes a Spire without a tower, keeps its count, and a pulse caught mid-sweep runs out from the block itself.
     */
    @GameTest(template = "empty", timeoutTicks = 200)
    public static void anOldSpireLoadsWithItsPulseAndCount(GameTestHelper helper) {
        inArea(helper, 130_264, (level, pos) -> {
            CompoundTag oldState = new CompoundTag();
            oldState.putString("Name", "void_reclaimer:terraforming_spire");
            CompoundTag properties = new CompoundTag();
            properties.putString("powered", "true");
            oldState.put("Properties", properties);
            BlockState state = NbtUtils.readBlockState(level.holderLookup(Registries.BLOCK), oldState);
            helper.assertTrue(state.is(ModBlocks.TERRAFORMING_SPIRE.get()) && state.getValue(SpireBlock.POWERED), "old block state: " + state);
            helper.assertTrue(state.getValue(BlockStateProperties.HORIZONTAL_FACING) == Direction.NORTH, "old Spires should face north: " + state);
            level.setBlock(pos, state, 3);

            CompoundTag oldEntity = new CompoundTag();
            oldEntity.putString("id", "void_reclaimer:terraforming_spire");
            oldEntity.putInt("x", pos.getX());
            oldEntity.putInt("y", pos.getY());
            oldEntity.putInt("z", pos.getZ());
            oldEntity.putInt("elapsed", 2);
            oldEntity.putInt("radius", 2);
            oldEntity.putInt("duration", 4);
            oldEntity.putInt("pulses", 3);
            SpireBlockEntity spire = (SpireBlockEntity) level.getBlockEntity(pos);
            spire.loadWithComponents(oldEntity, level.registryAccess());
            helper.assertTrue(spire.pulses() == 3, "pulses lost: " + spire.pulses());
            helper.assertTrue(spire.running() && spire.elapsed() == 2, "the pulse was lost: " + spire.elapsed());
            helper.assertFalse(spire.assembled(), "an old Spire has no tower");
            helper.assertTrue(spire.pulseCentre().equals(pos), "the old pulse moved to " + spire.pulseCentre());

            ticks(spire, 5);
            helper.assertTrue(!spire.running() && spire.pulses() == 4, "the old pulse did not finish: " + spire.elapsed() + ", " + spire.pulses());
            helper.succeed();
        });
    }
}
