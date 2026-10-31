package com.voidreclaimer.launch;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.Test;

class LaunchPadTest {
    private static final BlockPos CONTROLLER = new BlockPos(100, 64, -20);

    @Test
    void theRingIsThePadsEdgeWithoutTheController() {
        for (Direction facing : Direction.Plane.HORIZONTAL) {
            List<BlockPos> ring = LaunchPad.ring(CONTROLLER, facing);
            assertEquals(4 * LaunchPad.SIZE - 4 - 1, ring.size(), "ring cells facing " + facing);
            assertFalse(ring.contains(CONTROLLER), "the controller is its own cell");
            assertEquals(ring.size(), new HashSet<>(ring).size(), "no cell twice");
            // The controller's two neighbours along the edge and the far corners are in the ring.
            assertTrue(ring.contains(LaunchPad.cell(CONTROLLER, facing, 1, 0)));
            assertTrue(ring.contains(LaunchPad.cell(CONTROLLER, facing, -1, 0)));
            assertTrue(ring.contains(LaunchPad.cell(CONTROLLER, facing, LaunchPad.HALF, LaunchPad.SIZE - 1)));
            assertTrue(ring.contains(LaunchPad.cell(CONTROLLER, facing, -LaunchPad.HALF, LaunchPad.SIZE - 1)));
        }
    }

    @Test
    void theInteriorFillsTheRingWithoutTouchingIt() {
        for (Direction facing : Direction.Plane.HORIZONTAL) {
            List<BlockPos> interior = LaunchPad.interior(CONTROLLER, facing);
            Set<BlockPos> ring = new HashSet<>(LaunchPad.ring(CONTROLLER, facing));
            assertEquals((LaunchPad.SIZE - 2) * (LaunchPad.SIZE - 2), interior.size());
            for (BlockPos cell : interior) {
                assertFalse(ring.contains(cell), cell + " is in both the ring and the interior");
                assertEquals(CONTROLLER.getY(), cell.getY(), "the pad is one layer");
            }
            assertFalse(interior.contains(CONTROLLER));
            assertTrue(interior.contains(LaunchPad.centre(CONTROLLER, facing)), "the centre is inside");
        }
    }

    @Test
    void theCentreAndMuzzleLieInFrontOfTheController() {
        assertEquals(CONTROLLER.south(LaunchPad.HALF), LaunchPad.centre(CONTROLLER, Direction.SOUTH));
        assertEquals(CONTROLLER.north(LaunchPad.HALF), LaunchPad.centre(CONTROLLER, Direction.NORTH));
        assertEquals(CONTROLLER.east(LaunchPad.HALF), LaunchPad.centre(CONTROLLER, Direction.EAST));
        assertEquals(CONTROLLER.west(LaunchPad.HALF), LaunchPad.centre(CONTROLLER, Direction.WEST));
        // Across is to the controller's right: facing south, right is west.
        assertEquals(CONTROLLER.west(3).south(2), LaunchPad.cell(CONTROLLER, Direction.SOUTH, 3, 2));
        assertEquals(CONTROLLER.south(3).east(2), LaunchPad.cell(CONTROLLER, Direction.EAST, 3, 2), "facing east, right is south");
        Vec3 muzzle = LaunchPad.muzzle(CONTROLLER, Direction.WEST);
        assertEquals(CONTROLLER.getX() - LaunchPad.HALF + 0.5, muzzle.x, 1e-9);
        assertEquals(CONTROLLER.getY() + LaunchPad.MUZZLE_HEIGHT, muzzle.y, 1e-9);
        assertEquals(CONTROLLER.getZ() + 0.5, muzzle.z, 1e-9);
    }
}
