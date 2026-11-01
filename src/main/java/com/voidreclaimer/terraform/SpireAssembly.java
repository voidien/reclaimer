package com.voidreclaimer.terraform;

import com.voidreclaimer.registry.ModBlocks;
import java.util.Locale;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Assembles and sets down a Spire tower. Assembling needs every {@link SpireTower#frame} cell to hold a block tagged
 * {@code void_reclaimer:spire_frame} and every {@link SpireTower#column} cell to be free; it then fills the column with
 * invisible blocks and marks the Spire assembled. Setting down removes them. An assembled tower checks itself every
 * {@value SpireBlockEntity#CHECK_TICKS} ticks: a broken frame sets it down, a missing column block grows back.
 */
public final class SpireAssembly {
    public enum Result { ASSEMBLED, DISASSEMBLED, ALREADY_ASSEMBLED, NOT_ASSEMBLED, FRAME_INCOMPLETE, BLOCKED, FAILED }

    private SpireAssembly() {}

    /** Sneak-use: assemble, or set down when assembled, and tell the player what happened. */
    public static void toggle(SpireBlockEntity spire, Player player) {
        Result result = spire.assembled() ? disassemble(spire) : assemble(spire);
        player.displayClientMessage(Component.translatable("message.void_reclaimer.spire." + result.name().toLowerCase(Locale.ROOT)), true);
    }

    public static Result assemble(SpireBlockEntity spire) {
        if (!(spire.getLevel() instanceof ServerLevel level)) {
            return Result.FAILED;
        }
        if (spire.assembled()) {
            return Result.ALREADY_ASSEMBLED;
        }
        BlockPos pos = spire.getBlockPos();
        Direction facing = spire.facing();
        if (missingFrame(level, pos, facing) != null) {
            return Result.FRAME_INCOMPLETE;
        }
        for (BlockPos cell : SpireTower.column(pos, facing)) {
            BlockState state = level.getBlockState(cell);
            if (!state.isAir() && !state.canBeReplaced() && !state.is(ModBlocks.SPIRE_COLUMN.get())) {
                return Result.BLOCKED;
            }
        }
        for (BlockPos cell : SpireTower.column(pos, facing)) {
            level.setBlock(cell, ModBlocks.SPIRE_COLUMN.get().defaultBlockState(), 3);
        }
        spire.setAssembled(true);
        return Result.ASSEMBLED;
    }

    public static Result disassemble(SpireBlockEntity spire) {
        if (!(spire.getLevel() instanceof ServerLevel level)) {
            return Result.FAILED;
        }
        if (!spire.assembled()) {
            return Result.NOT_ASSEMBLED;
        }
        for (BlockPos cell : SpireTower.column(spire.getBlockPos(), spire.facing())) {
            if (level.getBlockState(cell).is(ModBlocks.SPIRE_COLUMN.get())) {
                level.setBlock(cell, Blocks.AIR.defaultBlockState(), 3);
            }
        }
        spire.setAssembled(false);
        return Result.DISASSEMBLED;
    }

    /** The first frame cell without a frame block, or null when the frame is complete. */
    @Nullable
    public static BlockPos missingFrame(ServerLevel level, BlockPos spire, Direction facing) {
        for (BlockPos cell : SpireTower.frame(spire, facing)) {
            if (!level.getBlockState(cell).is(ModBlocks.SPIRE_FRAME)) {
                return cell;
            }
        }
        return null;
    }

    /** An assembled tower keeps itself whole: a broken frame sets it down, a missing column block grows back. */
    static void check(SpireBlockEntity spire) {
        if (!(spire.getLevel() instanceof ServerLevel level) || !spire.assembled()) {
            return;
        }
        BlockPos pos = spire.getBlockPos();
        Direction facing = spire.facing();
        if (missingFrame(level, pos, facing) != null) {
            disassemble(spire);
            return;
        }
        for (BlockPos cell : SpireTower.column(pos, facing)) {
            if (level.getBlockState(cell).isAir()) {
                level.setBlock(cell, ModBlocks.SPIRE_COLUMN.get().defaultBlockState(), 3);
            }
        }
    }
}
