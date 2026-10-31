package com.voidreclaimer.launch;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.Vec3;

/**
 * The launch complex's footprint, pure: a {@value #SIZE} x {@value #SIZE} square pad in front of the Launch Controller,
 * which sits in the middle of the pad's near edge. The ring is the pad's outer row of cells without the controller; the
 * interior is everything inside the ring. Cells are addressed as (across, forward) from the controller: across is positive
 * to the controller's right (clockwise from its facing), forward is along its facing.
 */
public final class LaunchPad {
    /** The pad's width and depth in blocks (the model's pad is 15.25 blocks across). */
    public static final int SIZE = 15;
    public static final int HALF = SIZE / 2;
    /** The muzzle's height above the pad: the gun tower's top in the model. */
    public static final double MUZZLE_HEIGHT = 14.3;

    private LaunchPad() {}

    /** The cell {@code across} blocks to the controller's right and {@code forward} blocks along its facing. */
    public static BlockPos cell(BlockPos controller, Direction facing, int across, int forward) {
        Direction right = facing.getClockWise();
        return controller.offset(right.getStepX() * across + facing.getStepX() * forward, 0,
                right.getStepZ() * across + facing.getStepZ() * forward);
    }

    /** The pad's centre cell, {@link #HALF} blocks in front of the controller. */
    public static BlockPos centre(BlockPos controller, Direction facing) {
        return cell(controller, facing, 0, HALF);
    }

    /** Where payloads leave: above the pad's centre, at the top of the gun tower. */
    public static Vec3 muzzle(BlockPos controller, Direction facing) {
        return Vec3.atBottomCenterOf(centre(controller, facing)).add(0, MUZZLE_HEIGHT, 0);
    }

    /** The pad's outer row of cells, without the controller's own cell: these must be frame blocks. */
    public static List<BlockPos> ring(BlockPos controller, Direction facing) {
        List<BlockPos> cells = new ArrayList<>(4 * SIZE - 4 - 1);
        for (int across = -HALF; across <= HALF; across++) {
            for (int forward = 0; forward < SIZE; forward++) {
                boolean edge = across == -HALF || across == HALF || forward == 0 || forward == SIZE - 1;
                if (edge && !(across == 0 && forward == 0)) {
                    cells.add(cell(controller, facing, across, forward));
                }
            }
        }
        return cells;
    }

    /** The cells inside the ring: these are filled by invisible pad blocks while the complex is assembled. */
    public static List<BlockPos> interior(BlockPos controller, Direction facing) {
        List<BlockPos> cells = new ArrayList<>((SIZE - 2) * (SIZE - 2));
        for (int across = -HALF + 1; across <= HALF - 1; across++) {
            for (int forward = 1; forward < SIZE - 1; forward++) {
                cells.add(cell(controller, facing, across, forward));
            }
        }
        return cells;
    }
}
