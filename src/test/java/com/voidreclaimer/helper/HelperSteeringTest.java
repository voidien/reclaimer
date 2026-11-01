package com.voidreclaimer.helper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.Test;

class HelperSteeringTest {
    private static final double SPEED = 0.35, REACH = 1.5;
    private static final int NODES = 2048, RETRY = 100;

    /** Solid cells, counting how many cells were looked at. */
    private static final class World implements HelperSteering.Cells {
        final Set<Long> solid = new HashSet<>();
        long lookups;

        @Override
        public boolean free(int x, int y, int z) {
            lookups++;
            return !solid.contains(BlockPos.asLong(x, y, z));
        }

        boolean inside(Vec3 pos) {
            return solid.contains(BlockPos.asLong(Mth.floor(pos.x), Mth.floor(pos.y), Mth.floor(pos.z)));
        }

        World box(int x0, int y0, int z0, int x1, int y1, int z1) {
            for (BlockPos pos : BlockPos.betweenClosed(x0, y0, z0, x1, y1, z1)) {
                solid.add(pos.asLong());
            }
            return this;
        }

        /** A sealed hollow box around the cell, {@code gap} free cells to each side of it. */
        World shell(int x, int y, int z, int gap) {
            int r = gap + 1;
            for (BlockPos pos : BlockPos.betweenClosed(x - r, y - r, z - r, x + r, y + r, z + r)) {
                if (Math.max(Math.abs(pos.getX() - x), Math.max(Math.abs(pos.getY() - y), Math.abs(pos.getZ() - z))) == r) {
                    solid.add(pos.asLong());
                }
            }
            return this;
        }
    }

    private static Vec3 centre(int x, int y, int z) {
        return new Vec3(x + 0.5, y + 0.5, z + 0.5);
    }

    /** Flies up to {@code ticks} ticks; stops on arrival. Fails if the drone ever enters a solid cell. */
    private static Vec3 fly(HelperSteering.Route route, Vec3 from, Vec3 target, World world, int ticks) {
        Vec3 pos = from;
        for (int i = 0; i < ticks; i++) {
            pos = route.advance(pos, target, SPEED, REACH, NODES, RETRY, world);
            assertFalse(world.inside(pos), "the drone flew into a block at " + pos);
            if (HelperSteering.reached(pos, target, REACH, world)) {
                return pos;
            }
        }
        return pos;
    }

    @Test
    void aStepMovesTheSpeedTowardsTheTargetAndStopsOnIt() {
        Vec3 from = new Vec3(0, 0, 0), to = new Vec3(10, 0, 0);
        Vec3 step = HelperSteering.step(from, to, 0.35);
        assertEquals(0.35, step.x, 1e-9);
        assertEquals(0.0, step.y, 1e-9);
        Vec3 diagonal = HelperSteering.step(from, new Vec3(3, 4, 0), 1.0);
        assertEquals(1.0, diagonal.length(), 1e-9, "a step is exactly the speed long");
        assertEquals(to, HelperSteering.step(new Vec3(9.8, 0, 0), to, 0.35), "the last step lands on the target");
        assertEquals(to, HelperSteering.step(to, to, 0.35), "on the target it stays");
    }

    @Test
    void arrivalIsWithinReachAndYawFacesTheTarget() {
        Vec3 target = new Vec3(5, 5, 5);
        assertTrue(HelperSteering.arrived(new Vec3(5, 5, 6.4), target, 1.5));
        assertFalse(HelperSteering.arrived(new Vec3(5, 5, 7), target, 1.5));
        assertEquals(0f, HelperSteering.yaw(Vec3.ZERO, new Vec3(0, 0, 1), 42f), 1e-4, "south is yaw 0");
        assertEquals(-90f, HelperSteering.yaw(Vec3.ZERO, new Vec3(1, 0, 0), 42f), 1e-4, "east is yaw -90");
        assertEquals(42f, HelperSteering.yaw(Vec3.ZERO, new Vec3(0, 3, 0), 42f), 1e-4, "straight up keeps the yaw");
    }

    @Test
    void aLineIsClearOnlyWhenEveryCellItCrossesIsFree() {
        World world = new World().box(5, 0, 0, 5, 0, 0);
        assertFalse(HelperSteering.clear(centre(0, 0, 0), centre(9, 0, 0), world), "the block is on the line");
        assertTrue(HelperSteering.clear(centre(0, 1, 0), centre(9, 1, 0), world), "the line above it");
        assertFalse(HelperSteering.clear(centre(9, 0, 0), centre(0, 0, 0), world), "the other way as well");
        assertFalse(HelperSteering.clear(centre(0, 0, 0), centre(5, 0, 0), world), "the end cell counts");
        assertTrue(HelperSteering.clear(centre(5, 0, 0), centre(9, 0, 0), world), "the start cell does not");
        assertTrue(HelperSteering.clear(centre(2, 2, 2), centre(2, 2, 2), world), "no line at all");
        assertFalse(HelperSteering.clear(new Vec3(0.5, 0.5, 0.2), new Vec3(9.5, 0.5, 0.7), world), "a slanted line through it");
    }

    @Test
    void aLineDoesNotSlipBetweenTwoBlocksTouchingAtACorner() {
        World world = new World().box(1, 0, 0, 1, 0, 0).box(0, 1, 0, 0, 1, 0);
        assertFalse(HelperSteering.clear(centre(0, 0, 0), centre(1, 1, 0), world));
    }

    @Test
    void reachingNeedsReachAndAClearLine() {
        World world = new World().box(2, 0, 0, 2, 0, 0);
        assertTrue(HelperSteering.reached(centre(0, 0, 0), centre(1, 0, 0), 1.5, world));
        assertFalse(HelperSteering.reached(centre(0, 0, 0), centre(3, 0, 0), 1.5, world), "out of reach");
        assertFalse(HelperSteering.reached(centre(1, 0, 0), centre(3, 0, 0), 2.5, world), "in reach, through a wall");
    }

    @Test
    void theWorkSpotIsOnTopThenBesideThenUnder() {
        BlockPos block = new BlockPos(0, 0, 0);
        World world = new World();
        assertEquals(centre(0, 1, 0), HelperSteering.workSpot(block, world));
        world.box(0, 1, 0, 0, 1, 0);
        assertEquals(centre(0, 0, -1), HelperSteering.workSpot(block, world), "north comes first of the sides");
        world.box(-1, 0, -1, 1, 0, 1);
        assertEquals(centre(0, -1, 0), HelperSteering.workSpot(block, world), "only under it is free");
        assertTrue(HelperSteering.exposed(block, world));
        world.box(0, -1, 0, 0, -1, 0);
        assertEquals(centre(0, 1, 0), HelperSteering.workSpot(block, world), "shut in on all sides: on top, which it will never reach");
        assertFalse(HelperSteering.exposed(block, world));
    }

    @Test
    void aSearchGoesRoundAWallAndItsLegsAreClearAndFew() {
        World world = new World().box(5, -3, -3, 5, 3, 3);
        Vec3 from = centre(0, 0, 0), target = centre(10, 0, 0);
        HelperSteering.Path path = HelperSteering.search(from, target, REACH, world, NODES);
        assertTrue(path.complete(), "the way round a small wall");
        Vec3 last = from;
        for (Vec3 point : path.points()) {
            assertTrue(HelperSteering.clear(last, point, world), "a leg crosses the wall: " + last + " to " + point);
            last = point;
        }
        assertTrue(HelperSteering.reached(last, target, REACH, world), "it ends within reach of the target: " + last);
        assertTrue(path.points().size() <= 4, "straight legs, not a cell at a time: " + path.points());
    }

    @Test
    void aSearchOutOfNodesEndsAsCloseAsItGot() {
        World world = new World().box(5, -40, -40, 5, 40, 40);
        HelperSteering.Path path = HelperSteering.search(centre(0, 0, 0), centre(10, 0, 0), REACH, world, 200);
        assertFalse(path.complete());
        assertEquals(centre(4, 0, 0), path.points().getLast());
    }

    @Test
    void aDroneFliesRoundAWallAndArrives() {
        World world = new World().box(5, -3, -3, 5, 3, 3);
        Vec3 target = centre(10, 0, 0);
        Vec3 end = fly(new HelperSteering.Route(), centre(0, 0, 0), target, world, 200);
        assertTrue(HelperSteering.reached(end, target, REACH, world), "ended at " + end);
    }

    @Test
    void aDroneGoesRoundABigRoundRockInSeveralShortSearches() {
        World world = new World();
        int radius = 24;
        for (BlockPos pos : BlockPos.betweenClosed(-radius, -radius, -radius, radius, radius, radius)) {
            if (pos.distSqr(BlockPos.ZERO) <= radius * radius) {
                world.solid.add(pos.asLong());
            }
        }
        Vec3 pos = centre(-40, 3, 2), target = centre(40, 3, 2);
        HelperSteering.Route route = new HelperSteering.Route();
        for (int i = 0; i < 2000 && !HelperSteering.reached(pos, target, REACH, world); i++) {
            pos = route.advance(pos, target, SPEED, REACH, 256, RETRY, world);
            assertFalse(world.inside(pos), "flew into the rock at " + pos);
            assertFalse(route.blocked(), "gave up at " + pos);
        }
        assertTrue(HelperSteering.reached(pos, target, REACH, world), "ended at " + pos);
    }

    @Test
    void searchesOfOneDroneAreAtLeastAGapApart() {
        // Searches this short only gain a cell or two, so without the gap the drone would search every few ticks.
        World world = new World().box(2, -40, -40, 2, 40, 40);
        Vec3 pos = centre(0, 0, 0), target = centre(4, 30, 0);
        HelperSteering.Route route = new HelperSteering.Route();
        int lastSearch = -HelperSteering.Route.SEARCH_GAP, searches = 0;
        for (int i = 0; i < 300; i++) {
            long before = world.lookups;
            pos = route.advance(pos, target, SPEED, REACH, 4, RETRY, world);
            if (world.lookups - before > 8) {
                assertTrue(i - lastSearch >= HelperSteering.Route.SEARCH_GAP, "two searches " + (i - lastSearch) + " ticks apart");
                lastSearch = i;
                searches++;
            }
        }
        assertTrue(searches >= 5, "searches: " + searches);
    }

    @Test
    void flyingInTheOpenLooksAtAFewCellsATickAndNeverSearches() {
        World world = new World();
        HelperSteering.Route route = new HelperSteering.Route();
        Vec3 pos = centre(0, 0, 0), target = centre(30, 7, 12);
        for (int i = 0; i < 80; i++) {
            pos = route.advance(pos, target, SPEED, REACH, NODES, RETRY, world);
        }
        assertTrue(world.lookups <= 80 * 4, "cells looked at: " + world.lookups);
        assertTrue(route.waypoints().isEmpty());
    }

    @Test
    void aSealedTargetStopsTheDroneForGoodAndItDoesNotJitterOrSearchEveryTick() {
        World world = new World().shell(10, 0, 0, 2);
        Vec3 target = centre(10, 1, 0);
        HelperSteering.Route route = new HelperSteering.Route();
        Vec3 pos = fly(route, centre(0, 0, 0), target, world, 200);
        assertTrue(route.blocked(), "stopped at " + pos);
        long before = world.lookups;
        for (int i = 0; i < 1000; i++) {
            Vec3 next = route.advance(pos, target, SPEED, REACH, NODES, RETRY, world);
            assertEquals(pos, next, "tick " + i + ": it moved");
            pos = next;
            assertTrue(route.blocked() , "tick " + i + ": it stopped saying so");
        }
        // Ten tries at most, each at most six looks per node, plus the look before each.
        assertTrue(world.lookups - before <= 10 * (NODES * 6L + 16), "cells looked at while holding: " + (world.lookups - before));
    }

    @Test
    void aDroneInADeadEndBiggerThanItsSearchHoldsInsteadOfWandering() {
        World world = new World();
        // A cup open to the west: the target is behind its closed east end.
        world.box(0, -30, -30, 30, -30, 30).box(0, 30, -30, 30, 30, 30);
        world.box(0, -30, -30, 30, 30, -30).box(0, -30, 30, 30, 30, 30);
        world.box(30, -30, -30, 30, 30, 30);
        Vec3 target = centre(40, 0, 0);
        HelperSteering.Route route = new HelperSteering.Route();
        Vec3 pos = centre(20, 0, 0);
        List<Vec3> trail = new ArrayList<>();
        for (int i = 0; i < 400; i++) {
            pos = route.advance(pos, target, SPEED, REACH, 200, RETRY, world);
            trail.add(pos);
            assertFalse(world.inside(pos));
        }
        assertTrue(route.blocked(), "still going at " + pos);
        assertEquals(trail.get(trail.size() - 1), trail.get(trail.size() - 50), "it should have stopped by now");
        assertTrue(pos.x > 28, "it holds as close as it could get: " + pos);
    }

    @Test
    void aBlockedDroneGoesOnOnceAWayOpens() {
        World world = new World().shell(10, 0, 0, 2);
        Vec3 target = centre(10, 1, 0);
        HelperSteering.Route route = new HelperSteering.Route();
        Vec3 pos = fly(route, centre(0, 0, 0), target, world, 200);
        assertTrue(route.blocked());
        world.solid.clear();
        Vec3 end = fly(route, pos, target, world, RETRY + 200);
        assertTrue(HelperSteering.reached(end, target, REACH, world), "ended at " + end);
        assertFalse(route.blocked());
    }

    @Test
    void aBlockSetOnItsWayIsGoneRoundToo() {
        World world = new World().box(5, -3, -3, 5, 3, 3);
        Vec3 target = centre(10, 0, 0);
        HelperSteering.Route route = new HelperSteering.Route();
        Vec3 pos = centre(0, 0, 0);
        for (int i = 0; i < 30 && route.waypoints().isEmpty(); i++) {
            pos = route.advance(pos, target, SPEED, REACH, NODES, RETRY, world);
        }
        assertFalse(route.waypoints().isEmpty(), "not on a detour yet");
        Vec3 leg = route.waypoints().get(0).subtract(pos).normalize();
        world.solid.add(BlockPos.containing(pos.add(leg.scale(2.0))).asLong());
        Vec3 end = fly(route, pos, target, world, 400);
        assertTrue(HelperSteering.reached(end, target, REACH, world), "ended at " + end);
    }

    @Test
    void aNewTargetDropsTheOldDetourButOneThatShiftsALittleKeepsIt() {
        World world = new World().box(5, -3, -3, 5, 3, 3);
        Vec3 target = centre(10, 0, 0);
        HelperSteering.Route route = new HelperSteering.Route();
        Vec3 pos = centre(0, 0, 0);
        for (int i = 0; i < 30 && route.waypoints().isEmpty(); i++) {
            pos = route.advance(pos, target, SPEED, REACH, NODES, RETRY, world);
        }
        assertFalse(route.waypoints().isEmpty(), "not on a detour yet");
        pos = route.advance(pos, centre(11, 0, 0), SPEED, REACH, NODES, RETRY, world);
        assertFalse(route.waypoints().isEmpty(), "a target one block on is the same trip");
        pos = route.advance(pos, centre(-10, 0, 0), SPEED, REACH, NODES, RETRY, world);
        assertTrue(route.waypoints().isEmpty(), "a target far off starts again");
    }

    @Test
    void aDroneInsideABlockLeavesStraightAndThenSteersAgain() {
        World world = new World().box(-1, -1, -1, 1, 1, 1);
        Vec3 target = centre(8, 0, 0);
        HelperSteering.Route route = new HelperSteering.Route();
        Vec3 pos = centre(0, 0, 0);
        pos = route.advance(pos, target, SPEED, REACH, NODES, RETRY, world);
        assertEquals(0.5 + SPEED, pos.x, 1e-9, "the first step out");
        for (int i = 0; i < 10; i++) {
            pos = route.advance(pos, target, SPEED, REACH, NODES, RETRY, world);
        }
        assertFalse(world.inside(pos), "still inside at " + pos);
        assertFalse(route.blocked());
    }

    @Test
    void clearingForgetsTheHoldAndTheDetour() {
        World world = new World().shell(10, 0, 0, 2);
        Vec3 target = centre(10, 1, 0);
        HelperSteering.Route route = new HelperSteering.Route();
        fly(route, centre(0, 0, 0), target, world, 200);
        assertTrue(route.blocked());
        route.clear();
        assertFalse(route.blocked());
        assertTrue(route.waypoints().isEmpty());
    }
}
