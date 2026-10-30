package com.voidreclaimer.earth;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import org.junit.jupiter.api.Test;

class PortalShapeTest {
    private static final BlockPos ORIGIN = new BlockPos(100, 64, -40);

    /** A frame ring around a width x height interior whose bottom-left interior block is {@link #ORIGIN}. */
    private static Set<BlockPos> ring(Direction.Axis axis, int width, int height) {
        Direction along = axis == Direction.Axis.X ? Direction.EAST : Direction.SOUTH;
        Set<BlockPos> frame = new HashSet<>();
        for (int across = -1; across <= width; across++) {
            for (int up = -1; up <= height; up++) {
                boolean interior = across >= 0 && across < width && up >= 0 && up < height;
                boolean corner = (across == -1 || across == width) && (up == -1 || up == height);
                if (!interior && !corner) {
                    frame.add(ORIGIN.relative(along, across).above(up));
                }
            }
        }
        return frame;
    }

    private static Optional<PortalShape.Rect> find(Set<BlockPos> frame, Set<BlockPos> solid, BlockPos start, Direction.Axis axis) {
        return PortalShape.find(frame::contains, pos -> !frame.contains(pos) && !solid.contains(pos), start, axis);
    }

    @Test
    void findsAShipSizedFrameFromAnyInteriorBlock() {
        Set<BlockPos> frame = ring(Direction.Axis.X, 12, 6);
        for (BlockPos start : new BlockPos[] {ORIGIN, ORIGIN.east(5).above(3), ORIGIN.east(11).above(5)}) {
            PortalShape.Rect rect = find(frame, Set.of(), start, Direction.Axis.X).orElseThrow();
            assertEquals(ORIGIN, rect.bottomLeft());
            assertEquals(12, rect.width());
            assertEquals(6, rect.height());
        }
    }

    @Test
    void findsAFrameAlongZ() {
        Set<BlockPos> frame = ring(Direction.Axis.Z, 4, 5);
        PortalShape.Rect rect = find(frame, Set.of(), ORIGIN.south(2).above(), Direction.Axis.Z).orElseThrow();
        assertEquals(ORIGIN, rect.bottomLeft());
        assertEquals(4, rect.width());
        assertEquals(5, rect.height());
    }

    @Test
    void wrongAxisFindsNothing() {
        assertTrue(find(ring(Direction.Axis.X, 6, 6), Set.of(), ORIGIN.east(2).above(2), Direction.Axis.Z).isEmpty());
    }

    @Test
    void aMissingFrameBlockMeansNoPortal() {
        for (BlockPos missing : new BlockPos[] {ORIGIN.below().east(4), ORIGIN.above(6).east(4), ORIGIN.west().above(2), ORIGIN.east(6).above(3)}) {
            Set<BlockPos> frame = ring(Direction.Axis.X, 6, 6);
            assertTrue(frame.remove(missing), missing.toString());
            assertTrue(find(frame, Set.of(), ORIGIN.east(2).above(2), Direction.Axis.X).isEmpty(), "gap at " + missing);
        }
    }

    @Test
    void anObstructionInsideMeansNoPortal() {
        Set<BlockPos> frame = ring(Direction.Axis.X, 6, 6);
        assertTrue(find(frame, Set.of(ORIGIN.east(3).above(3)), ORIGIN.east(2).above(2), Direction.Axis.X).isEmpty());
    }

    @Test
    void sizeLimitsApply() {
        assertTrue(find(ring(Direction.Axis.X, 2, 5), Set.of(), ORIGIN.above(), Direction.Axis.X).isEmpty(), "too narrow");
        assertTrue(find(ring(Direction.Axis.X, 5, 2), Set.of(), ORIGIN.east(), Direction.Axis.X).isEmpty(), "too low");
        assertTrue(find(ring(Direction.Axis.X, PortalShape.MIN_SIZE, PortalShape.MIN_SIZE), Set.of(), ORIGIN, Direction.Axis.X).isPresent());
        assertTrue(find(ring(Direction.Axis.X, PortalShape.MAX_SIZE, PortalShape.MAX_SIZE), Set.of(), ORIGIN, Direction.Axis.X).isPresent());
        assertTrue(find(ring(Direction.Axis.X, PortalShape.MAX_SIZE + 1, 5), Set.of(), ORIGIN, Direction.Axis.X).isEmpty(), "too wide");
        assertTrue(find(ring(Direction.Axis.X, 5, PortalShape.MAX_SIZE + 1), Set.of(), ORIGIN, Direction.Axis.X).isEmpty(), "too tall");
    }

    @Test
    void openSkyAboveIsNotAFrame() {
        Set<BlockPos> frame = ring(Direction.Axis.X, 5, 5);
        frame.removeIf(pos -> pos.getY() > ORIGIN.getY() + 4);
        assertTrue(find(frame, Set.of(), ORIGIN.east(), Direction.Axis.X).isEmpty());
    }
}
