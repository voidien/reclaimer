package com.voidreclaimer.asteroid;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import org.junit.jupiter.api.Test;

class FractureCarveTest {
    private static final int RADIUS = 12;

    /** A round rock with a pit {@code halfWidth * 2 + 1} wide, down from the top to y 4. */
    private static Predicate<BlockPos> rockWithPit(int halfWidth) {
        return pos -> pos.distSqr(BlockPos.ZERO) <= RADIUS * RADIUS && !(Math.abs(pos.getX()) <= halfWidth && Math.abs(pos.getZ()) <= halfWidth && pos.getY() >= 4);
    }

    /** Whether every block of {@code piece} can move {@code far} along {@code way} without meeting rock that stays behind. */
    private static boolean leaves(List<BlockPos> piece, Direction way, Predicate<BlockPos> rock, int far) {
        Set<BlockPos> own = new HashSet<>(piece);
        for (BlockPos block : piece) {
            for (int step = 1; step <= far; step++) {
                BlockPos there = block.relative(way, step);
                if (rock.test(there) && !own.contains(there)) {
                    return false;
                }
            }
        }
        return true;
    }

    @Test
    void aPieceFromTheBottomOfAPitComesOutOfThePit() {
        Predicate<BlockPos> rock = rockWithPit(5);
        List<BlockPos> piece = Fracture.carve(new BlockPos(0, 3, 0), Direction.UP, rock, 48, 3, 24);
        assertFalse(piece.isEmpty());
        assertTrue(leaves(piece, Direction.UP, rock, 24), "it is wedged");
        assertTrue(piece.size() >= 8, "only " + piece.size() + " blocks");
    }

    @Test
    void aPitWithNoRoomToSpareGivesUpNoPiece() {
        assertTrue(Fracture.carve(new BlockPos(0, 3, 0), Direction.UP, rockWithPit(1), 48, 3, 24).size() < 8);
    }

    @Test
    void aPieceFromAFlatFaceIsThinAndStopsAtItsSize() {
        Predicate<BlockPos> wall = pos -> pos.getZ() <= 0;
        List<BlockPos> piece = Fracture.carve(BlockPos.ZERO, Direction.SOUTH, wall, 48, 3, 24);
        assertEquals(48, piece.size());
        assertEquals(48, new HashSet<>(piece).size(), "no block twice");
        assertTrue(piece.stream().allMatch(pos -> pos.getZ() <= 0 && pos.getZ() >= -2), "more than three thick");
        assertTrue(leaves(piece, Direction.SOUTH, wall, 24), "it is wedged");
    }

    @Test
    void aBlockWithRockAheadIsNotAFace() {
        Predicate<BlockPos> solid = pos -> pos.equals(BlockPos.ZERO) || pos.equals(new BlockPos(0, 5, 0));
        assertTrue(Fracture.carve(BlockPos.ZERO, Direction.UP, solid, 48, 3, 24).isEmpty());
    }
}
