package com.voidreclaimer.asteroid;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import net.minecraft.core.BlockPos;
import org.junit.jupiter.api.Test;

class FractureGatherTest {
    private static boolean inRock(BlockPos pos) {
        return Math.abs(pos.getX()) <= 3 && Math.abs(pos.getY()) <= 3 && Math.abs(pos.getZ()) <= 3;
    }

    @Test
    void aPieceStopsAtItsSizeAndStaysJoined() {
        List<BlockPos> piece = Fracture.gather(BlockPos.ZERO, FractureGatherTest::inRock, 20);
        assertEquals(20, piece.size());
        Set<BlockPos> blocks = new HashSet<>(piece);
        assertEquals(20, blocks.size(), "no block twice");
        for (BlockPos pos : piece) {
            assertTrue(pos.equals(BlockPos.ZERO) || net.minecraft.core.Direction.stream().anyMatch(side -> blocks.contains(pos.relative(side))), pos + " is on its own");
        }
    }

    @Test
    void airStopsItAndAStartInAirFindsNothing() {
        assertEquals(1, Fracture.gather(BlockPos.ZERO, BlockPos.ZERO::equals, 20).size());
        assertTrue(Fracture.gather(new BlockPos(9, 9, 9), FractureGatherTest::inRock, 20).isEmpty());
    }
}
