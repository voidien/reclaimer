package com.voidreclaimer.launch;

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
 * Assembles and sets down a launch complex around its controller. Assembling needs every ring cell
 * ({@link LaunchPad#ring}) to hold a block tagged {@code void_reclaimer:launch_frame} and every interior cell to be
 * empty; it then fills the interior with invisible pad blocks and marks the controller assembled. Setting down removes
 * the pad blocks. An assembled complex checks itself every {@value LaunchControllerBlockEntity#CHECK_TICKS} ticks: a
 * broken ring sets it down, a missing pad block grows back.
 */
public final class LaunchComplex {
    public enum Result { ASSEMBLED, DISASSEMBLED, ALREADY_ASSEMBLED, NOT_ASSEMBLED, RING_INCOMPLETE, BLOCKED, FAILED }

    private LaunchComplex() {}

    /** Sneak-use: assemble, or set down when assembled, and tell the player what happened. */
    public static void toggle(LaunchControllerBlockEntity controller, Player player) {
        Result result = controller.assembled() ? disassemble(controller) : assemble(controller);
        player.displayClientMessage(message(result), true);
    }

    public static Result assemble(LaunchControllerBlockEntity controller) {
        if (!(controller.getLevel() instanceof ServerLevel level)) {
            return Result.FAILED;
        }
        if (controller.assembled()) {
            return Result.ALREADY_ASSEMBLED;
        }
        BlockPos pos = controller.getBlockPos();
        Direction facing = controller.facing();
        if (missingFrame(level, pos, facing) != null) {
            return Result.RING_INCOMPLETE;
        }
        for (BlockPos cell : LaunchPad.interior(pos, facing)) {
            BlockState state = level.getBlockState(cell);
            if (!state.isAir() && !state.canBeReplaced() && !state.is(ModBlocks.LAUNCH_PAD.get())) {
                return Result.BLOCKED;
            }
        }
        for (BlockPos cell : LaunchPad.interior(pos, facing)) {
            level.setBlock(cell, ModBlocks.LAUNCH_PAD.get().defaultBlockState(), 3);
        }
        controller.setAssembled(true);
        return Result.ASSEMBLED;
    }

    public static Result disassemble(LaunchControllerBlockEntity controller) {
        if (!(controller.getLevel() instanceof ServerLevel level)) {
            return Result.FAILED;
        }
        if (!controller.assembled()) {
            return Result.NOT_ASSEMBLED;
        }
        for (BlockPos cell : LaunchPad.interior(controller.getBlockPos(), controller.facing())) {
            if (level.getBlockState(cell).is(ModBlocks.LAUNCH_PAD.get())) {
                level.setBlock(cell, Blocks.AIR.defaultBlockState(), 3);
            }
        }
        controller.setAssembled(false);
        return Result.DISASSEMBLED;
    }

    /** The first ring cell without a frame block, or null when the ring is complete. */
    @Nullable
    public static BlockPos missingFrame(ServerLevel level, BlockPos controller, Direction facing) {
        for (BlockPos cell : LaunchPad.ring(controller, facing)) {
            if (!level.getBlockState(cell).is(ModBlocks.LAUNCH_FRAME)) {
                return cell;
            }
        }
        return null;
    }

    /** An assembled complex keeps itself whole: a broken ring sets the pad down, a missing pad block grows back. */
    static void check(LaunchControllerBlockEntity controller) {
        if (!(controller.getLevel() instanceof ServerLevel level) || !controller.assembled()) {
            return;
        }
        BlockPos pos = controller.getBlockPos();
        Direction facing = controller.facing();
        if (missingFrame(level, pos, facing) != null) {
            disassemble(controller);
            return;
        }
        for (BlockPos cell : LaunchPad.interior(pos, facing)) {
            if (level.getBlockState(cell).isAir()) {
                level.setBlock(cell, ModBlocks.LAUNCH_PAD.get().defaultBlockState(), 3);
            }
        }
    }

    public static Component message(Result result) {
        return Component.translatable("message.void_reclaimer.launch." + result.name().toLowerCase(Locale.ROOT));
    }
}
