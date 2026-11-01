package com.voidreclaimer.terraform;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import org.junit.jupiter.api.Test;

class SpireTowerTest {
    private static final BlockPos SPIRE = new BlockPos(100, 64, -20);

    @Test
    void theFrameIsTheBaseEdgeAndFourPillarsWithoutTheSpire() {
        for (Direction facing : Direction.Plane.HORIZONTAL) {
            List<BlockPos> frame = SpireTower.frame(SPIRE, facing);
            assertEquals(15 + 4 * (SpireTower.HEIGHT - 1), frame.size(), "frame cells facing " + facing);
            assertEquals(frame.size(), new HashSet<>(frame).size(), "no cell twice");
            assertFalse(frame.contains(SPIRE), "the Spire is its own cell");
            assertTrue(frame.contains(SpireTower.cell(SPIRE, facing, 1, 0, 0)), "beside the Spire");
            assertTrue(frame.contains(SpireTower.cell(SPIRE, facing, 2, 2, 0)), "the middle of a side");
            for (int across : new int[] {-SpireTower.HALF, SpireTower.HALF}) {
                for (int forward : new int[] {0, SpireTower.SIZE - 1}) {
                    assertTrue(frame.contains(SpireTower.cell(SPIRE, facing, across, forward, SpireTower.HEIGHT - 1)), "the top of a pillar");
                }
            }
            assertFalse(frame.contains(SpireTower.cell(SPIRE, facing, 2, 2, 1)), "only corners rise");
            assertTrue(frame.stream().noneMatch(cell -> cell.getY() >= SPIRE.getY() + SpireTower.HEIGHT), "nothing above the pillars");
        }
    }

    @Test
    void theColumnFillsTheMiddleWithoutTouchingTheFrame() {
        for (Direction facing : Direction.Plane.HORIZONTAL) {
            List<BlockPos> column = SpireTower.column(SPIRE, facing);
            Set<BlockPos> frame = new HashSet<>(SpireTower.frame(SPIRE, facing));
            assertEquals(3 * 3 * SpireTower.HEIGHT, column.size());
            assertEquals(column.size(), new HashSet<>(column).size(), "no cell twice");
            for (BlockPos cell : column) {
                assertFalse(frame.contains(cell), cell + " is in both the frame and the column");
            }
            assertFalse(column.contains(SPIRE));
            assertTrue(column.contains(SpireTower.centre(SPIRE, facing)), "the centre is inside");
            assertTrue(column.contains(SpireTower.centre(SPIRE, facing).above(SpireTower.HEIGHT - 1)), "the column reaches the top");
        }
    }

    @Test
    void theCentreLiesInFrontOfTheSpire() {
        assertEquals(SPIRE.south(2), SpireTower.centre(SPIRE, Direction.SOUTH));
        assertEquals(SPIRE.north(2), SpireTower.centre(SPIRE, Direction.NORTH));
        assertEquals(SPIRE.east(2), SpireTower.centre(SPIRE, Direction.EAST));
        assertEquals(SPIRE.west(2), SpireTower.centre(SPIRE, Direction.WEST));
        // Across is to the Spire's right: facing south, right is west.
        assertEquals(SPIRE.west(1).south(2).above(3), SpireTower.cell(SPIRE, Direction.SOUTH, 1, 2, 3));
    }
}
