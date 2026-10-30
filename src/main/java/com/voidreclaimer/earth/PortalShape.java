package com.voidreclaimer.earth;

import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Predicate;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.Vec3;

/**
 * Finds the rectangle an Atmospheric Portal fills inside a frame. Pure over two predicates, so it is unit-tested with a
 * plain set of positions. The frame is a flat vertical rectangle of frame blocks (corners are not needed, like the
 * Nether portal); the interior must be empty and between {@link #MIN_SIZE} and {@link #MAX_SIZE} blocks each way.
 * The Ship (about 12 x 5.6 blocks) fits comfortably.
 */
public final class PortalShape {
    public static final int MIN_SIZE = 3;
    public static final int MAX_SIZE = 32;

    private PortalShape() {}

    /**
     * The interior rectangle.
     *
     * @param bottomLeft the interior block with the lowest x or z (the plane's "left") on the bottom row
     * @param axis       the horizontal axis the width runs along (X or Z); the plane faces the other one
     */
    public record Rect(BlockPos bottomLeft, Direction.Axis axis, int width, int height) {
        public BlockPos at(int across, int up) {
            return bottomLeft.relative(alongWidth(axis), across).above(up);
        }

        /** The middle of the bottom row, at floor level: where an entity would stand to face the portal. */
        public Vec3 centerBottom() {
            return axis == Direction.Axis.X
                    ? new Vec3(bottomLeft.getX() + width / 2.0, bottomLeft.getY(), bottomLeft.getZ() + 0.5)
                    : new Vec3(bottomLeft.getX() + 0.5, bottomLeft.getY(), bottomLeft.getZ() + width / 2.0);
        }

        /** The side of the plane, as a direction; {@code sign} positive is the south or east side. */
        public Direction normal(int sign) {
            if (axis == Direction.Axis.X) {
                return sign >= 0 ? Direction.SOUTH : Direction.NORTH;
            }
            return sign >= 0 ? Direction.EAST : Direction.WEST;
        }

        /** Which side of the plane an entity moving by {@code moved} (along the plane's normal axis) ends up on; positive if still. */
        public int travelSign(Vec3 moved) {
            double along = axis == Direction.Axis.X ? moved.z : moved.x;
            return along < -1.0e-3 ? -1 : 1;
        }

        public void forEach(Consumer<BlockPos> action) {
            for (int up = 0; up < height; up++) {
                for (int across = 0; across < width; across++) {
                    action.accept(at(across, up));
                }
            }
        }
    }

    private static Direction alongWidth(Direction.Axis axis) {
        return axis == Direction.Axis.X ? Direction.EAST : Direction.SOUTH;
    }

    /**
     * @param frame whether a position holds a frame block
     * @param empty whether a position may be filled (air, or this portal's own blocks)
     * @param start an empty position inside the frame
     * @param axis  the axis the portal's width runs along
     */
    public static Optional<Rect> find(Predicate<BlockPos> frame, Predicate<BlockPos> empty, BlockPos start, Direction.Axis axis) {
        if (axis.isVertical() || !empty.test(start)) {
            return Optional.empty();
        }
        Direction right = alongWidth(axis);
        Direction left = right.getOpposite();

        BlockPos bottom = start;
        int fall = 0;
        while (empty.test(bottom.below())) {
            bottom = bottom.below();
            if (++fall > MAX_SIZE) {
                return Optional.empty();
            }
        }
        if (!frame.test(bottom.below())) {
            return Optional.empty();
        }

        int toLeft = 0;
        BlockPos leftmost = bottom;
        while (empty.test(leftmost.relative(left))) {
            leftmost = leftmost.relative(left);
            if (++toLeft > MAX_SIZE) {
                return Optional.empty();
            }
        }
        if (!frame.test(leftmost.relative(left))) {
            return Optional.empty();
        }
        int toRight = 0;
        BlockPos rightmost = bottom;
        while (empty.test(rightmost.relative(right))) {
            rightmost = rightmost.relative(right);
            if (++toRight > MAX_SIZE) {
                return Optional.empty();
            }
        }
        if (!frame.test(rightmost.relative(right))) {
            return Optional.empty();
        }
        int width = toLeft + toRight + 1;
        if (width < MIN_SIZE || width > MAX_SIZE) {
            return Optional.empty();
        }

        Rect probe = new Rect(leftmost, axis, width, 1);
        for (int across = 0; across < width; across++) {
            if (!frame.test(probe.at(across, -1))) {
                return Optional.empty();
            }
        }
        int height = 0;
        while (height <= MAX_SIZE && isInteriorRow(frame, empty, probe, height)) {
            height++;
        }
        if (height < MIN_SIZE || height > MAX_SIZE) {
            return Optional.empty();
        }
        for (int across = 0; across < width; across++) {
            if (!frame.test(probe.at(across, height))) {
                return Optional.empty();
            }
        }
        return Optional.of(new Rect(leftmost, axis, width, height));
    }

    private static boolean isInteriorRow(Predicate<BlockPos> frame, Predicate<BlockPos> empty, Rect row, int up) {
        if (!frame.test(row.at(-1, up)) || !frame.test(row.at(row.width(), up))) {
            return false;
        }
        for (int across = 0; across < row.width(); across++) {
            if (!empty.test(row.at(across, up))) {
                return false;
            }
        }
        return true;
    }
}
