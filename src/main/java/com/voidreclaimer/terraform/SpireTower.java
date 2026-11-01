package com.voidreclaimer.terraform;

import com.voidreclaimer.launch.LaunchPad;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;

/**
 * The Spire tower's shape, pure: a {@value #SIZE} x {@value #SIZE} base in front of the Terraforming Spire, which sits in
 * the middle of the base's near edge, {@value #HEIGHT} blocks tall. The frame is the base's outer row of cells without the
 * Spire, plus four corner pillars; the column is the 3 x 3 inside, which invisible blocks fill while the tower stands.
 * Cells are (across, forward, up) from the Spire: across is positive to its right (clockwise from its facing), forward is
 * along its facing.
 */
public final class SpireTower {
    public static final int SIZE = 5;
    public static final int HALF = SIZE / 2;
    /** Height of the corner pillars and of the column, the base layer included. */
    public static final int HEIGHT = 9;

    private SpireTower() {}

    public static BlockPos cell(BlockPos spire, Direction facing, int across, int forward, int up) {
        return LaunchPad.cell(spire, facing, across, forward).above(up);
    }

    /** The base's centre cell: the pulse sweeps out from here. */
    public static BlockPos centre(BlockPos spire, Direction facing) {
        return cell(spire, facing, 0, HALF, 0);
    }

    /** The cells that must be frame blocks. */
    public static List<BlockPos> frame(BlockPos spire, Direction facing) {
        List<BlockPos> cells = new ArrayList<>(4 * SIZE - 5 + 4 * (HEIGHT - 1));
        for (int across = -HALF; across <= HALF; across++) {
            for (int forward = 0; forward < SIZE; forward++) {
                boolean sideEdge = across == -HALF || across == HALF;
                boolean endEdge = forward == 0 || forward == SIZE - 1;
                if ((sideEdge || endEdge) && !(across == 0 && forward == 0)) {
                    cells.add(cell(spire, facing, across, forward, 0));
                }
                if (sideEdge && endEdge) {
                    for (int up = 1; up < HEIGHT; up++) {
                        cells.add(cell(spire, facing, across, forward, up));
                    }
                }
            }
        }
        return cells;
    }

    /** The cells inside the frame: these must be free to assemble, and invisible column blocks fill them. */
    public static List<BlockPos> column(BlockPos spire, Direction facing) {
        List<BlockPos> cells = new ArrayList<>((SIZE - 2) * (SIZE - 2) * HEIGHT);
        for (int across = -HALF + 1; across <= HALF - 1; across++) {
            for (int forward = 1; forward < SIZE - 1; forward++) {
                for (int up = 0; up < HEIGHT; up++) {
                    cells.add(cell(spire, facing, across, forward, up));
                }
            }
        }
        return cells;
    }
}
