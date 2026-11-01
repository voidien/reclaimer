package com.voidreclaimer.helper;

import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.PriorityQueue;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

/**
 * Pure flight of a helper drone. It flies straight at its target and only looks at the cells its next step enters. When a
 * block is in the way, a bounded search over free cells gives it waypoints round it ({@link Route}). A target it cannot get
 * closer to makes it hold still and retry later, so it never jitters or wanders off.
 */
public final class HelperSteering {
    /** Weight of a cell's distance to the target in the search: above 1 it dives at the target instead of flooding open space. */
    private static final double GREED = 2.0;
    /** Where a drone prefers to hover over a block: on top, then beside it, then under it. */
    private static final Direction[] FACES = {Direction.UP, Direction.NORTH, Direction.SOUTH, Direction.WEST, Direction.EAST, Direction.DOWN};

    private HelperSteering() {}

    /** Whether a drone can fly through a block cell. */
    @FunctionalInterface
    public interface Cells {
        boolean free(int x, int y, int z);
    }

    /** The position after one tick flying from {@code pos} towards {@code target} at {@code speed} blocks per tick. */
    public static Vec3 step(Vec3 pos, Vec3 target, double speed) {
        Vec3 to = target.subtract(pos);
        double distance = to.length();
        if (distance <= speed || distance < 1.0e-6) {
            return target;
        }
        return pos.add(to.scale(speed / distance));
    }

    /** Whether {@code pos} is within {@code reach} of {@code target}. */
    public static boolean arrived(Vec3 pos, Vec3 target, double reach) {
        return pos.distanceToSqr(target) <= reach * reach;
    }

    /** Whether {@code pos} is within {@code reach} of {@code target} with nothing between them: a drone does not work through a wall. */
    public static boolean reached(Vec3 pos, Vec3 target, double reach, Cells cells) {
        return arrived(pos, target, reach) && clear(pos, target, cells);
    }

    /** The yaw (degrees, Minecraft's convention) that faces from {@code from} towards {@code to}; unchanged when they coincide. */
    public static float yaw(Vec3 from, Vec3 to, float current) {
        double dx = to.x - from.x, dz = to.z - from.z;
        if (dx * dx + dz * dz < 1.0e-6) {
            return current;
        }
        return (float) (Mth.atan2(dz, dx) * (180.0 / Math.PI)) - 90f;
    }

    /** Where to hover to work on {@code block}: the middle of its first free neighbour, or on top when it has none. */
    public static Vec3 workSpot(BlockPos block, Cells cells) {
        for (Direction face : FACES) {
            if (faceFree(block, face, cells)) {
                return Vec3.atCenterOf(block.relative(face));
            }
        }
        return Vec3.atCenterOf(block.above());
    }

    /** Whether any face of {@code block} is open. */
    public static boolean exposed(BlockPos block, Cells cells) {
        for (Direction face : FACES) {
            if (faceFree(block, face, cells)) {
                return true;
            }
        }
        return false;
    }

    private static boolean faceFree(BlockPos block, Direction face, Cells cells) {
        return cells.free(block.getX() + face.getStepX(), block.getY() + face.getStepY(), block.getZ() + face.getStepZ());
    }

    /** Whether the straight line between two points only crosses free cells; the cell {@code from} is in does not count. */
    static boolean clear(Vec3 from, Vec3 to, Cells cells) {
        int x = Mth.floor(from.x), y = Mth.floor(from.y), z = Mth.floor(from.z);
        int endX = Mth.floor(to.x), endY = Mth.floor(to.y), endZ = Mth.floor(to.z);
        double dx = to.x - from.x, dy = to.y - from.y, dz = to.z - from.z;
        int stepX = dx > 0 ? 1 : -1, stepY = dy > 0 ? 1 : -1, stepZ = dz > 0 ? 1 : -1;
        double deltaX = dx == 0 ? Double.MAX_VALUE : 1.0 / Math.abs(dx);
        double deltaY = dy == 0 ? Double.MAX_VALUE : 1.0 / Math.abs(dy);
        double deltaZ = dz == 0 ? Double.MAX_VALUE : 1.0 / Math.abs(dz);
        double nextX = dx == 0 ? Double.MAX_VALUE : (dx > 0 ? x + 1 - from.x : from.x - x) * deltaX;
        double nextY = dy == 0 ? Double.MAX_VALUE : (dy > 0 ? y + 1 - from.y : from.y - y) * deltaY;
        double nextZ = dz == 0 ? Double.MAX_VALUE : (dz > 0 ? z + 1 - from.z : from.z - z) * deltaZ;
        while (x != endX || y != endY || z != endZ) {
            double xAt = x == endX ? Double.MAX_VALUE : nextX;
            double yAt = y == endY ? Double.MAX_VALUE : nextY;
            double zAt = z == endZ ? Double.MAX_VALUE : nextZ;
            if (xAt <= yAt && xAt <= zAt) {
                x += stepX;
                nextX += deltaX;
            } else if (yAt <= zAt) {
                y += stepY;
                nextY += deltaY;
            } else {
                z += stepZ;
                nextZ += deltaZ;
            }
            if (!cells.free(x, y, z)) {
                return false;
            }
        }
        return true;
    }

    /** Waypoints from a search. {@code complete} means they end within reach of the target; if not, they end as close as the search got. */
    public record Path(List<Vec3> points, boolean complete) {}

    private record Node(int x, int y, int z, Node parent, int cost, double left) {
        Vec3 centre() {
            return new Vec3(x + 0.5, y + 0.5, z + 0.5);
        }

        double rank() {
            return cost + GREED * left;
        }
    }

    /**
     * Looks for a way from {@code from} to within {@code reach} of {@code target}, expanding at most {@code maxNodes} cells.
     * Moves are across cell faces; the result is then straightened.
     */
    public static Path search(Vec3 from, Vec3 target, double reach, Cells cells, int maxNodes) {
        PriorityQueue<Node> open = new PriorityQueue<>(Comparator.comparingDouble(Node::rank));
        LongOpenHashSet seen = new LongOpenHashSet();
        int x = Mth.floor(from.x), y = Mth.floor(from.y), z = Mth.floor(from.z);
        Node start = new Node(x, y, z, null, 0, new Vec3(x + 0.5, y + 0.5, z + 0.5).distanceTo(target));
        open.add(start);
        seen.add(BlockPos.asLong(x, y, z));
        Node best = start;
        for (int expanded = 0; expanded < maxNodes && !open.isEmpty(); expanded++) {
            Node node = open.poll();
            if (node.left() < best.left()) {
                best = node;
            }
            if (reached(node.centre(), target, reach, cells)) {
                return new Path(straighten(from, node, cells), true);
            }
            for (Direction face : Direction.values()) {
                int nx = node.x() + face.getStepX(), ny = node.y() + face.getStepY(), nz = node.z() + face.getStepZ();
                if (seen.add(BlockPos.asLong(nx, ny, nz)) && cells.free(nx, ny, nz)) {
                    open.add(new Node(nx, ny, nz, node, node.cost() + 1, new Vec3(nx + 0.5, ny + 0.5, nz + 0.5).distanceTo(target)));
                }
            }
        }
        return new Path(straighten(from, best, cells), false);
    }

    /** The cells from the start to {@code end}, without those a straight line can skip. */
    private static List<Vec3> straighten(Vec3 from, Node end, Cells cells) {
        List<Vec3> path = new ArrayList<>();
        for (Node node = end; node.parent() != null; node = node.parent()) {
            path.add(node.centre());
        }
        Collections.reverse(path);
        List<Vec3> points = new ArrayList<>();
        Vec3 anchor = from;
        for (int i = 0; i < path.size(); i++) {
            int last = i;
            while (last + 1 < path.size() && clear(anchor, path.get(last + 1), cells)) {
                last++;
            }
            anchor = path.get(last);
            points.add(anchor);
            i = last;
        }
        return points;
    }

    /** One drone's way to its current target: the waypoints it follows, and how it gives up when there is no way. */
    public static final class Route {
        /** A target that moves further than this (an owner flying off) is a new target. */
        private static final double TARGET_MOVED = 2.0;
        /** Ticks between two searches of one drone. */
        static final int SEARCH_GAP = 10;

        private final ArrayDeque<Vec3> waypoints = new ArrayDeque<>();
        private Vec3 goal;
        private int searchIn;
        private int blockedFor;

        /** Whether the drone found no way and is holding still until its next try. */
        public boolean blocked() {
            return blockedFor > 0;
        }

        /** The waypoints still ahead. */
        public List<Vec3> waypoints() {
            return List.copyOf(waypoints);
        }

        /** Forgets the way: the drone has arrived or has nowhere to go. */
        public void clear() {
            waypoints.clear();
            goal = null;
            blockedFor = 0;
        }

        /**
         * The drone's position after this tick. It stays put while it holds or waits for its next search. A drone inside a
         * block (one was set on it, or it comes from an old save) leaves straight towards its target.
         */
        public Vec3 advance(Vec3 pos, Vec3 target, double speed, double reach, int maxNodes, int retryTicks, Cells cells) {
            if (searchIn > 0) {
                searchIn--;
            }
            if (goal == null || goal.distanceToSqr(target) > TARGET_MOVED * TARGET_MOVED) {
                waypoints.clear();
                goal = target;
            }
            if (blockedFor > 0 && --blockedFor > 0) {
                return pos;
            }
            if (!cells.free(Mth.floor(pos.x), Mth.floor(pos.y), Mth.floor(pos.z))) {
                return step(pos, target, speed);
            }
            while (!waypoints.isEmpty() && pos.distanceToSqr(waypoints.peek()) < 1.0e-6) {
                waypoints.poll();
            }
            Vec3 next = step(pos, waypoints.isEmpty() ? target : waypoints.peek(), speed);
            if (HelperSteering.clear(pos, next, cells)) {
                return next;
            }
            waypoints.clear();
            if (searchIn > 0) {
                return pos;
            }
            searchIn = SEARCH_GAP;
            Path path = search(pos, target, reach, cells, maxNodes);
            // A search that stops short ends on a cell closer than the one it began in, so a chain of them must end;
            // with no such cell the points are empty and this is as close as the drone can get.
            if (path.points().isEmpty()) {
                blockedFor = retryTicks;
                return pos;
            }
            waypoints.addAll(path.points());
            return step(pos, waypoints.peek(), speed);
        }
    }
}
