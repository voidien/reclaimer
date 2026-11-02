package com.voidreclaimer.body;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.voidreclaimer.body.BodyBoxes.Box;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaterniond;
import org.joml.Vector3d;
import org.junit.jupiter.api.Test;

class BodyBoxesResolveTest {
    private static final double EPS = 1e-6;
    /** More than the overlap {@link BodyBoxes#push} calls touching, and well under anything that shows. */
    private static final double HAIR = 1e-6;

    private static Box box(double x, double y, double z, double half) {
        return box(x, y, z, half, half, half, new Quaterniond());
    }

    private static Box box(double x, double y, double z, double halfX, double halfY, double halfZ, Quaterniond turn) {
        return new Box("", new Vector3d(x, y, z), new Vector3d(halfX, halfY, halfZ), turn);
    }

    private static Box block(int x, int y, int z) {
        return Box.of(new AABB(x, y, z, x + 1, y + 1, z + 1));
    }

    /** Solid blocks from corner to corner, both included. */
    private static List<Box> blocks(int x1, int y1, int z1, int x2, int y2, int z2) {
        List<Box> blocks = new ArrayList<>();
        for (int x = x1; x <= x2; x++) {
            for (int y = y1; y <= y2; y++) {
                for (int z = z1; z <= z2; z++) {
                    blocks.add(block(x, y, z));
                }
            }
        }
        return blocks;
    }

    /** A wall one block thick across X, at {@code x}, wide enough for anything here. */
    private static List<Box> wall(int x) {
        return blocks(x, -6, -6, x, 6, 6);
    }

    private static List<Box> all(List<Box> first, List<Box> second) {
        List<Box> all = new ArrayList<>(first);
        all.addAll(second);
        return all;
    }

    private static Box moved(Box box, Vec3 by) {
        return new Box(box.socket(), new Vector3d(box.middle()).add(by.x, by.y, by.z), box.half(), box.turn());
    }

    /** How deep the box is in the deepest of {@code blocks}. */
    private static double depth(Box box, List<Box> blocks) {
        return blocks.stream().mapToDouble(block -> BodyBoxes.push(box, block).map(Vector3d::length).orElse(0.0)).max().orElse(0.0);
    }

    private static void assertVec(double x, double y, double z, Vec3 actual) {
        assertEquals(x, actual.x, EPS, "x of " + actual);
        assertEquals(y, actual.y, EPS, "y of " + actual);
        assertEquals(z, actual.z, EPS, "z of " + actual);
    }

    private static Vec3 resolve(Box box, List<Box> blocks, double x, double y, double z) {
        return BodyBoxes.resolve(List.of(box), blocks, new Vec3(x, y, z));
    }

    @Test
    void nothingInTheWayLeavesTheMoveAlone() {
        Vec3 velocity = new Vec3(1.23, -0.4, 0.7);
        assertEquals(velocity, BodyBoxes.resolve(List.of(box(0, 0, 0, 1)), List.of(), velocity));
        assertSame(velocity, BodyBoxes.resolve(List.of(box(0, 0, 0, 1)), wall(10), velocity), "free flight is not rounded");
        assertEquals(velocity, BodyBoxes.resolve(List.of(), wall(2), velocity));
    }

    @Test
    void noMoveIsNoMove() {
        assertEquals(Vec3.ZERO, BodyBoxes.resolve(List.of(box(0.5, 0.5, 0.5, 1)), wall(0), Vec3.ZERO));
    }

    @Test
    void aBoxStopsAtAWall() {
        Box box = box(0, 0.5, 0.5, 1);
        Vec3 allowed = resolve(box, wall(3), 5, 0, 0);
        assertVec(2, 0, 0, allowed);
        assertEquals(0.0, depth(moved(box, allowed), wall(3)), "it touches, and is not in");
    }

    @Test
    void aMoveThatEndsAtTheWallIsWhole() {
        assertVec(2, 0, 0, resolve(box(0, 0.5, 0.5, 1), wall(3), 2, 0, 0));
    }

    @Test
    void aFastBodyDoesNotPassThroughAWall() {
        assertVec(2, 0, 0, resolve(box(0, 0.5, 0.5, 1), wall(3), 40, 0, 0));
        assertVec(0, 2, 0, resolve(box(0.5, 0, 0.5, 1), blocks(-6, 3, -6, 6, 3, 6), 0, 40, 0));
    }

    @Test
    void aSmallBoxMeetsAThinBlockBetweenTwoLooks() {
        // A hardpoint's size and a pane's: the whole move would land past the block with no overlap at either end.
        List<Box> pane = List.of(Box.of(new AABB(0.5, -2, -2, 0.625, 2, 2)));
        assertVec(0.31, 0, 0, resolve(box(0, 0, 0, 0.19), pane, 0.9, 0, 0));
    }

    @Test
    void aBoxBesideAWallCanLeave() {
        assertVec(-1, 0, 0, resolve(box(2, 0.5, 0.5, 1), wall(3), -1, 0, 0));
    }

    @Test
    void itSlidesAlongAWall() {
        assertVec(1, 0, 1, resolve(box(1, 0.5, 0.5, 1), wall(3), 3, 0, 1));
    }

    @Test
    void itStopsInACorner() {
        List<Box> corner = all(wall(3), blocks(-6, -6, 3, 6, 6, 3));
        assertVec(1, 2, 1, resolve(box(1, 0.5, 1, 1), corner, 3, 2, 3));
    }

    @Test
    void itStopsInTheCornerOfARoom() {
        List<Box> room = all(all(wall(3), blocks(-6, -6, 3, 6, 6, 3)), blocks(-6, 3, -6, 6, 3, 6));
        assertVec(1, 1, 1, resolve(box(1, 1, 1, 1), room, 3, 3, 3));
    }

    @Test
    void aTurnedBoxStopsOnItsCorner() {
        Box turned = box(0, 0.5, 0.5, 1, 1, 1, new Quaterniond().rotationY(Math.toRadians(45)));
        assertVec(3 - Math.sqrt(2), 0, 0, resolve(turned, wall(3), 5, 0, 0));
    }

    @Test
    void aSlantingFaceTurnsTheMoveAlongIt() {
        // A face at 45 degrees across the way: the body goes along it, and what it loses is the part that went into the face.
        Box slope = box(5, 0, 0, 2, 5, 10, new Quaterniond().rotationY(Math.toRadians(45)));
        Vec3 allowed = resolve(box(0, 0, 0, 0.5), List.of(slope), 8, 0, 0);
        assertTrue(allowed.z != 0.0, "it did not turn: " + allowed);
        assertTrue(allowed.length() < 8, "it got longer: " + allowed);
        assertEquals(0.0, depth(moved(box(0, 0, 0, 0.5), allowed), List.of(slope)), EPS);
    }

    @Test
    void twoEdgesMeetAlongTheirCross() {
        // Askew sticks: only the cross of the two edges shows how far apart they are, as in BodyBoxesTest.
        Vector3d across = new Vector3d(1, -1, 1).normalize();
        Box stick = box(0, 0, 0, 5, 0.1, 0.1, new Quaterniond().rotationZ(Math.toRadians(45)));
        Box other = new Box("", new Vector3d(across).mul(0.5), new Vector3d(0.1, 0.1, 5), new Quaterniond().rotationX(Math.toRadians(-45)));
        Vec3 allowed = BodyBoxes.resolve(List.of(stick), List.of(other), new Vec3(across.x, across.y, across.z));
        assertTrue(BodyBoxes.push(moved(stick, allowed), other).isEmpty(), "it went in: " + allowed);
        assertTrue(BodyBoxes.push(moved(stick, allowed.scale(1.001)), other).isPresent(), "it stopped short: " + allowed);
        assertTrue(allowed.length() < 0.5);
    }

    @Test
    void aLongSlideDoesNotCatchOnTheJoinsOfAWall() {
        // Pressed into a flat wall, turned so only a corner touches, it must go along block after block at full speed.
        Box body = box(1.5, 0.5, -40, 1, 1, 1, new Quaterniond().rotationY(Math.toRadians(30)));
        List<Box> wall = blocks(3, -3, -48, 3, 3, 48);
        for (int tick = 0; tick < 100; tick++) {
            Vec3 allowed = resolve(body, wall, 0.2, 0, 0.7);
            assertEquals(0.7, allowed.z, EPS, "tick " + tick);
            body = moved(body, allowed);
        }
        assertEquals(0.0, depth(body, wall), "it went in");
    }

    @Test
    void aBoxInAWallCanBackOut() {
        assertVec(-2, 0, 0, resolve(box(2.5, 0.5, 0.5, 1), wall(3), -2, 0, 0));
    }

    @Test
    void aBoxInAWallCanGoAlongIt() {
        assertVec(0, 1, 2, resolve(box(2.5, -2, 0.5, 1), wall(3), 0, 1, 2));
    }

    @Test
    void aBoxInAWallCannotGoDeeper() {
        assertVec(0, 0, 0, resolve(box(2.5, 0.5, 0.5, 1), wall(3), 1, 0, 0));
        assertVec(0, 0, 0.5, resolve(box(2.5, 0.5, 0.5, 1), wall(3), 1, 0, 0.5));
    }

    /** A square tunnel along Z, two blocks wide inside, with a box half a block too big for it each way. */
    private static List<Box> tunnel(int from, int to) {
        return all(all(blocks(-2, -2, from, -2, 1, to), blocks(1, -2, from, 1, 1, to)), all(blocks(-1, -2, from, 0, -2, to), blocks(-1, 1, from, 0, 1, to)));
    }

    @Test
    void aBodyTooBigForATunnelCanStillGoAlongIt() {
        Box body = box(0, 0, 0, 1.5);
        List<Box> tunnel = tunnel(-40, 40);
        for (int tick = 0; tick < 40; tick++) {
            Vec3 allowed = resolve(body, tunnel, 0, 0, 0.8);
            assertVec(0, 0, 0.8, allowed);
            body = moved(body, allowed);
        }
    }

    @Test
    void aBodyTooBigForATunnelCannotPressIntoItsWalls() {
        Box body = box(0, 0, 0, 1.5);
        assertVec(0, 0, 0.3, resolve(body, tunnel(-10, 10), 1, 0, 0.3));
        assertVec(0, 0, 0.3, resolve(body, tunnel(-10, 10), -1, 0, 0.3));
        assertVec(0, 0, 0.3, resolve(body, tunnel(-10, 10), 0, 1, 0.3));
    }

    @Test
    void aBoxInsideAWallStillHitsTheNextOneOnlyAsDeepAsItWas() {
        // Half a block into both sides, it goes on to the plug at the end of the tunnel, half a block in, and no further.
        Box body = box(0, 0, 0, 1.5);
        List<Box> plugged = all(tunnel(-10, 10), blocks(-1, -1, 4, 0, 0, 4));
        assertVec(0, 0, 3, resolve(body, plugged, 0, 0, 8));
    }

    @Test
    void aHairInsideAWallOpensNoWayThrough() {
        // Rounding can leave a stop just inside the block, more than touching: the wall must hold.
        Box body = box(2 + HAIR, 0.5, 0.5, 1);
        assertTrue(depth(body, wall(3)) > 0.0, "the test wants a real overlap");
        assertVec(0, 0, 0, resolve(body, wall(3), 5, 0, 0));
    }

    @Test
    void aHairInsideAWallStillSlidesFreely() {
        assertVec(0, 0.4, 0.9, resolve(box(2 + HAIR, 0.5, 0.5, 1), wall(3), 0, 0.4, 0.9));
    }

    @Test
    void aBoxThatOnlyTouchesAWallStillSlidesAndStillStops() {
        // Inside by less than touching, as a stop can leave it: no block of the wall may catch it, and the wall still holds.
        Box body = box(2 + 5e-8, 0.5, 0.5, 1);
        assertEquals(0.0, depth(body, wall(3)), "the test wants it only touching");
        assertVec(0, 0.4, 0.9, resolve(body, wall(3), 0, 0.4, 0.9));
        assertVec(0, 0, 0, resolve(body, wall(3), 5, 0, 0));
    }

    @Test
    void aMoveAlmostAlongAWallItTouchesDoesNotJump() {
        // Closing so slowly that the time to the wall is huge, and before the start. Running the move backwards from there would meet
        // the block far behind the body on the way back.
        assertVec(0, 0, 8, resolve(box(2 + 5e-8, 0.5, 0.5, 1), all(wall(3), List.of(block(2, 0, -8))), 1e-10, 0, 8));
    }

    @Test
    void aMovePastACornerDoesNotStop() {
        // Diagonally past a block: it comes level with the block on one axis only after it has left it on the other.
        for (int i = 0; i < 40; i++) {
            double from = 2.2 + i * 0.01;
            Vec3 velocity = new Vec3(3, 0, 3);
            assertVec(3, 0, 3, BodyBoxes.resolve(List.of(box(0, 0.5, 0, 0.5)), List.of(Box.of(new AABB(from, 0, 0, from + 1, 1, 1))), velocity));
        }
    }

    @Test
    void aStoppedBodyDoesNotCreepInWhenFarFromTheOrigin() {
        // Near the world's edge the rounding of a position is at its worst.
        int far = 29_999_000;
        List<Box> wall = blocks(far + 3, -3, -3, far + 3, 3, 3);
        Box body = box(far, 0.5, 0.5, 1, 1, 1, new Quaterniond().rotationY(Math.toRadians(20)));
        for (int tick = 0; tick < 2000; tick++) {
            body = moved(body, resolve(body, wall, 0.73, 0, 0));
            assertTrue(depth(body, wall) == 0.0, "tick " + tick + ": " + depth(body, wall));
        }
        assertEquals(far + 3 - Math.cos(Math.toRadians(20)) - Math.sin(Math.toRadians(20)), body.middle().x(), EPS);
    }

    @Test
    void aBodyBuriedInRockCanFlyOutAlongItsLength() {
        Box body = box(0.5, 0.5, 0.5, 6, 1, 10, new Quaterniond());
        List<Box> rock = blocks(-12, -12, -16, 12, 12, 16);
        assertTrue(depth(body, rock) > 1.0, "the test wants it buried");
        for (int tick = 0; tick < 60; tick++) {
            body = moved(body, resolve(body, rock, 0, 0, 1));
        }
        assertEquals(0.0, depth(body, rock), "it got out");
    }

    @Test
    void aWedgeEndsTheMoveAndDoesNotPassThrough() {
        // Two faces closing on the way ahead, meeting near x = 10, so each slide runs straight into the other.
        Box left = box(4, 0, 2.4, 0.5, 5, 8, new Quaterniond().rotationY(Math.toRadians(-70)));
        Box right = box(4, 0, -2.4, 0.5, 5, 8, new Quaterniond().rotationY(Math.toRadians(70)));
        Box body = box(0, 0, 0, 0.5);
        Vec3 allowed = BodyBoxes.resolve(List.of(body), List.of(left, right), new Vec3(20, 0, 0.1));
        assertEquals(0.0, depth(moved(body, allowed), List.of(left, right)), EPS, "it went in");
        assertTrue(allowed.x > 4 && allowed.x < 11, "it should stop inside the wedge: " + allowed);
    }

    @Test
    void aTitanStopsWithItsNoseAtAWall() {
        // At the Titan's own scale, facing south (+Z): the saucer's front is the nose, and a wall stands a block past it.
        List<Box> body = BodyBoxes.place(BuiltinBodies.CHASSIS.get(BuiltinBodies.TITAN), id -> true, Vec3.ZERO, 0f, 0f, 8.0);
        double nose = body.stream().mapToDouble(box -> box.middle().z() + box.half().z()).max().orElseThrow();
        assertEquals(30.0, nose, 0.01);
        List<Box> wall = blocks(-30, 0, 31, 30, 30, 31);
        Vec3 allowed = BodyBoxes.resolve(body, wall, new Vec3(0.3, 0, 40));
        assertVec(0.3, 0, 31 - nose, allowed);
    }

    @Test
    void aRandomBodyNeverGoesDeeperOrFaster() {
        Random random = new Random(7);
        int stopped = 0;
        for (int round = 0; round < 1500; round++) {
            Quaterniond turn = BodyBoxes.turn(random.nextFloat() * 360f, random.nextFloat() * 180f - 90f);
            List<Box> body = new ArrayList<>();
            int count = 1 + random.nextInt(3);
            for (int i = 0; i < count; i++) {
                body.add(box(gauss(random, 2), gauss(random, 2), gauss(random, 2), 0.2 + random.nextDouble() * 2, 0.2 + random.nextDouble(), 0.2 + random.nextDouble() * 3, turn));
            }
            List<Box> blocks = new ArrayList<>();
            int clump = 1 + random.nextInt(60);
            for (int i = 0; i < clump; i++) {
                blocks.add(block((int) gauss(random, 5), (int) gauss(random, 5), (int) gauss(random, 5)));
            }
            for (int tick = 0; tick < 4; tick++) {
                Vec3 velocity = new Vec3(gauss(random, 1.5), gauss(random, 1.5), gauss(random, 1.5));
                Vec3 allowed = BodyBoxes.resolve(body, blocks, velocity);
                assertTrue(allowed.length() <= velocity.length() + EPS, "faster: " + velocity + " to " + allowed);
                if (allowed.distanceToSqr(velocity) > EPS) {
                    stopped++;
                }
                List<Box> next = new ArrayList<>();
                for (Box box : body) {
                    Box after = moved(box, allowed);
                    assertTrue(depth(after, blocks) <= depth(box, blocks) + EPS, "deeper, round " + round);
                    next.add(after);
                }
                body = next;
            }
        }
        assertTrue(stopped > 1000, "the test hardly met a block: " + stopped);
    }

    private static double gauss(Random random, double spread) {
        return random.nextGaussian() * spread;
    }

    @Test
    void oneHitLosesAllTheSpeedGoingIntoItEvenWithAGapLeft() {
        BodyBoxes.Moved moved = BodyBoxes.move(List.of(box(0, 0.5, 0.5, 1)), wall(3), new Vec3(3, 0, 0));
        assertVec(2, 0, 0, moved.move());
        assertVec(0, 0, 0, moved.speed());
    }

    @Test
    void aBodySlidingAlongAWallKeepsTheSpeedAlongIt() {
        BodyBoxes.Moved moved = BodyBoxes.move(List.of(box(0, 0.5, 0.5, 1)), wall(3), new Vec3(3, 0, 1));
        assertVec(0, 0, 1, moved.speed());
    }

    @Test
    void nothingInTheWayKeepsItAll() {
        Vec3 velocity = new Vec3(0.5, 0, 0);
        BodyBoxes.Moved moved = BodyBoxes.move(List.of(box(0, 0.5, 0.5, 1)), wall(9), velocity);
        assertSame(velocity, moved.move());
        assertSame(velocity, moved.speed());
    }

    @Test
    void theDeepestOverlapIsHowFarABoxIsIn() {
        assertEquals(0.0, BodyBoxes.deepest(List.of(box(0, 0.5, 0.5, 1)), wall(3)), 1e-9);
        assertEquals(0.25, BodyBoxes.deepest(List.of(box(2.25, 0.5, 0.5, 1)), wall(3)), 1e-9);
    }
}
