package com.voidreclaimer.body;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.voidreclaimer.api.body.Chassis;
import com.voidreclaimer.api.body.HitBox;
import com.voidreclaimer.api.body.SizeClass;
import com.voidreclaimer.api.body.Socket;
import com.voidreclaimer.body.BodyBoxes.Box;
import com.voidreclaimer.body.BodyHeading.Turn;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.function.Function;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaterniond;
import org.joml.Vector3d;
import org.junit.jupiter.api.Test;

class BodyHeadingTest {
    private static final double EPS = 1e-4;

    private static double degreesPerTick(int rank, float mass, float turnRate) {
        BodyStats stats = new BodyStats(BuiltinBodies.SHIP_SIZE, 1.0, rank, mass, 1f, 0, null, 1f, 0f, 0f, 1f, 0f, 0f, 0, 0f, 0f, turnRate);
        return BodyHeading.degreesPerTick(stats, BodyHeading.TURN_FACTOR, BodyHeading.SLOWEST_TURN, BodyHeading.FASTEST_TURN);
    }

    private static void assertHeading(double yaw, double pitch, BodyHeading heading) {
        assertEquals(yaw, heading.yaw(), EPS, "yaw of " + heading);
        assertEquals(pitch, heading.pitch(), EPS, "pitch of " + heading);
    }

    @Test
    void aDroneAndAMechFollowTheViewExactly() {
        assertEquals(Double.POSITIVE_INFINITY, degreesPerTick(0, 2.2f, 0.08f));
        assertEquals(Double.POSITIVE_INFINITY, degreesPerTick(1, 4.4f, 0.02f));
        BodyHeading view = new BodyHeading(10f, 5f).toward(7200.5f, -33.25f, Double.POSITIVE_INFINITY);
        assertEquals(7200.5f, view.yaw(), "not wrapped or eased");
        assertEquals(-33.25f, view.pitch());
    }

    @Test
    void theBasicBigBodiesTurnAtTheirNumbers() {
        // Masses and turn rates of `body preset`: the sections, a Mk I thruster and storage pod on every mount, and the core they need.
        assertEquals(44.8 / 20, degreesPerTick(2, 15.0f, 0.28f), 1e-5, "Ship");
        assertEquals(123.1 / 20, degreesPerTick(2, 7.8f, 0.4f), 1e-2, "Shuttle");
        assertEquals(15.1 / 20, degreesPerTick(3, 39.7f, 0.25f), 1e-2, "Titan");
    }

    @Test
    void weightSlowsATurnAndTurnPartsSpeedItUp() {
        assertEquals(degreesPerTick(2, 15f, 0.4f) / 2, degreesPerTick(2, 30f, 0.4f), 1e-6);
        assertEquals(degreesPerTick(2, 30f, 0.2f) * 2, degreesPerTick(2, 30f, 0.4f), 1e-6);
    }

    @Test
    void aBodyWithNothingToTurnItStillTurnsAndALightOneHasALimit() {
        assertEquals(BodyHeading.SLOWEST_TURN / 20, degreesPerTick(2, 15f, 0f), 1e-9);
        assertEquals(BodyHeading.FASTEST_TURN / 20, degreesPerTick(2, 0.5f, 1f), 1e-9);
    }

    @Test
    void aFarTurnGoesAtTheFullRate() {
        assertHeading(3, 0, new BodyHeading(0f, 0f).toward(90f, 0f, 3.0));
        assertHeading(0, -3, new BodyHeading(0f, 0f).toward(0f, -80f, 3.0));
    }

    @Test
    void yawAndPitchArriveTogether() {
        // The body's real turn in a tick is this diagonal, as yaw and the body's right axis are square to each other.
        BodyHeading next = new BodyHeading(0f, 0f).toward(40f, 30f, 3.0);
        assertHeading(2.4, 1.8, next);
    }

    @Test
    void itGoesTheShortWayRound() {
        assertHeading(173, 0, new BodyHeading(170f, 0f).toward(-170f, 0f, 3.0));
        assertHeading(-173, 0, new BodyHeading(-170f, 0f).toward(170f, 0f, 3.0));
        assertHeading(3603, 0, new BodyHeading(3600f, 0f).toward(90f, 0f, 3.0));
    }

    @Test
    void theLastOfATurnEasesIn() {
        assertHeading(0.6, 0, new BodyHeading(0f, 0f).toward(4f, 0f, 3.0));
        assertHeading(1.2, 0, new BodyHeading(0f, 0f).toward(8f, 0f, 3.0));
    }

    @Test
    void aTurnArrivesExactlyAndNeverPastTheView() {
        BodyHeading heading = new BodyHeading(0f, 0f);
        for (int tick = 0; tick < 200; tick++) {
            BodyHeading next = heading.toward(50f, -20f, 3.0);
            assertTrue(next.yaw() >= heading.yaw() && next.yaw() <= 50f, "yaw went past: " + next);
            heading = next;
        }
        assertEquals(new BodyHeading(50f, -20f), heading);
    }

    @Test
    void aSmallTurnHasNoJerkAtTheEnd() {
        assertEquals(new BodyHeading(1.01f, 0f), new BodyHeading(1f, 0f).toward(1.01f, 0f, 3.0));
    }

    // ---- among blocks -----------------------------------------------------------------------------------------

    private static final Chassis LONG_BODY = new Chassis(BuiltinBodies.SHIP_SIZE, "root", List.of(
            new Socket("root", List.of(), false, Optional.empty(), Vec3.ZERO, "body").withBoxes(new HitBox(new Vec3(0, 0.5, 5), new Vec3(1, 1, 12)))));

    /** One box 12 long and 1 across, its feet at the origin, reaching 11 ahead and 1 behind: a long body's nose swings far. */
    private static final Function<BodyHeading, List<Box>> BODY = heading -> BodyBoxes.place(LONG_BODY, id -> true, Vec3.ZERO, heading.yaw(), heading.pitch(), 1.0);

    private static Box block(int x, int y, int z) {
        return Box.of(new AABB(x, y, z, x + 1, y + 1, z + 1));
    }

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

    private static List<Box> shifted(List<Box> boxes, Vec3 by) {
        return boxes.stream().map(box -> new Box(box.socket(), new Vector3d(box.middle()).add(by.x, by.y, by.z), box.half(), box.turn())).toList();
    }

    private static double depth(List<Box> boxes, List<Box> blocks) {
        return boxes.stream().flatMap(box -> blocks.stream().map(block -> BodyBoxes.push(box, block)))
                .mapToDouble(push -> push.map(Vector3d::length).orElse(0.0)).max().orElse(0.0);
    }

    /** At yaw 0 the nose is toward +Z, and more yaw swings it toward -X: a wall down the body's side, its face at {@code x + 1}. */
    private static List<Box> wallBeside(int x) {
        return blocks(x, -4, -4, x, 4, 16);
    }

    @Test
    void nothingInTheWayIsAWholeTurnAndNoPush() {
        BodyHeading next = new BodyHeading(2f, 1f);
        Turn turn = new BodyHeading(0f, 0f).turn(next, BODY, blocks(30, 0, 0, 30, 0, 0));
        assertEquals(next, turn.heading());
        assertEquals(Vec3.ZERO, turn.push());
    }

    @Test
    void aBodyWithNoBoxesTurnsThroughAnything() {
        BodyHeading next = new BodyHeading(40f, 0f);
        assertEquals(next, new BodyHeading(0f, 0f).turn(next, heading -> List.of(), blocks(-2, -2, -2, 2, 2, 2)).heading());
    }

    @Test
    void noTurnIsNoTurn() {
        BodyHeading here = new BodyHeading(5f, 6f);
        assertEquals(new Turn(here, Vec3.ZERO), here.turn(here, BODY, blocks(-2, -2, -2, 2, 2, 2)));
    }

    @Test
    void aTurnIntoAWallPushesTheBodyAway() {
        // The side of the body is half a block from a wall, and 3 degrees swings the nose a little past that: the body moves off.
        List<Box> wall = wallBeside(-2);
        BodyHeading start = new BodyHeading(0f, 0f);
        assertEquals(0.0, depth(BODY.apply(start), wall), "it starts clear");
        Turn turn = start.turn(new BodyHeading(3f, 0f), BODY, wall);
        assertEquals(new BodyHeading(3f, 0f), turn.heading(), "the whole turn");
        assertTrue(turn.push().x > 0.0 && turn.push().length() <= BodyBoxes.MOST_PUSH, "pushed away from the wall: " + turn.push());
        assertEquals(0.0, depth(shifted(BODY.apply(turn.heading()), turn.push()), wall), 1e-6, "and clear of it");
    }

    @Test
    void aWallOfManyBlocksPushesOnce() {
        // Each block of the wall the nose is in asks for the same push. Added up they would be many times too much.
        List<Box> wall = wallBeside(-2);
        BodyHeading next = new BodyHeading(3f, 0f);
        Vec3 push = new BodyHeading(0f, 0f).turn(next, BODY, wall).push();
        assertTrue(push.length() > 0.0);
        assertEquals(depth(BODY.apply(next), wall), push.length(), 1e-6);
    }

    @Test
    void aTurnTheLimitRefusesIsDoneInPart() {
        // 7 degrees swings the nose well past the wall, more than a tick's push. Half of that turn only just reaches it.
        List<Box> wall = blocks(-2, -4, 6, -2, 4, 16);
        BodyHeading start = new BodyHeading(0f, 0f);
        BodyHeading next = new BodyHeading(7f, 0f);
        assertTrue(depth(BODY.apply(next), wall) > BodyBoxes.MOST_PUSH, "the test wants more than one push");
        assertTrue(BodyBoxes.turnPush(BODY.apply(start), BODY.apply(next), wall).isEmpty());
        Turn turn = start.turn(next, BODY, wall);
        assertEquals(3.5f, turn.heading().yaw(), 1e-5);
        assertTrue(turn.push().x > 0.0 && turn.push().length() <= BodyBoxes.MOST_PUSH, "a push: " + turn.push());
        assertEquals(0.0, depth(shifted(BODY.apply(turn.heading()), turn.push()), wall), 1e-6);
    }

    /** The body with its side flush against walls: x 0 to 1, in a slot just as wide. */
    private static final Function<BodyHeading, List<Box>> IN_SLOT = heading -> BodyBoxes.place(LONG_BODY, id -> true, new Vec3(0.5, 0, 0), heading.yaw(), heading.pitch(), 1.0);

    @Test
    void aBodyWedgedBetweenTwoWallsKeepsItsHeading() {
        // Turning sends the nose into one wall and the tail into the other, and each push only drives the body into the other wall.
        List<Box> slot = new ArrayList<>(blocks(-1, -4, -4, -1, 4, 16));
        slot.addAll(blocks(1, -4, -4, 1, 4, 16));
        BodyHeading start = new BodyHeading(0f, 0f);
        assertEquals(0.0, depth(IN_SLOT.apply(start), slot), "it starts clear");
        assertEquals(new Turn(start, Vec3.ZERO), start.turn(new BodyHeading(3f, 0f), IN_SLOT, slot));
        assertEquals(new Turn(start, Vec3.ZERO), start.turn(new BodyHeading(-3f, 0f), IN_SLOT, slot));
    }

    @Test
    void aBodyInASlotCanStillTurnWhenOneWallEnds() {
        // The same slot with the wall by the tail gone: now the push has somewhere to go.
        List<Box> slot = blocks(-1, -4, 2, -1, 4, 16);
        Turn turn = new BodyHeading(0f, 0f).turn(new BodyHeading(1f, 0f), IN_SLOT, slot);
        assertEquals(1f, turn.heading().yaw(), 1e-5);
        assertTrue(turn.push().x > 0.0, "pushed off the wall: " + turn);
    }

    @Test
    void aPushIsNotAllowedIntoABlockJustOutOfReach() {
        // Nothing is near the far side of the body yet, but the push off the wall would carry it in.
        List<Box> blocks = new ArrayList<>(blocks(-1, -4, 2, -1, 4, 16));
        blocks.add(Box.of(new AABB(1.1, -4, -4, 2, 4, 16)));
        Turn turn = new BodyHeading(0f, 0f).turn(new BodyHeading(1f, 0f), IN_SLOT, blocks);
        assertEquals(0.0, depth(shifted(IN_SLOT.apply(turn.heading()), turn.push()), blocks), 1e-6, "pushed in: " + turn);
    }

    @Test
    void aTurnCutShortStaysOnTheShortWayRound() {
        // Across the seam at 180: the 6 degrees are refused, and half of them must not be half of the 354 the long way.
        List<Box> wall = blocks(1, -4, -16, 1, 4, -6);
        Turn turn = new BodyHeading(178f, 0f).turn(new BodyHeading(-176f, 0f), BODY, wall);
        assertEquals(181f, turn.heading().yaw(), 1e-4);
    }

    @Test
    void twoWallsInACornerAddUp() {
        // A push off one wall and another off the wall square to it: each is needed, and neither undoes the other.
        Box inCorner = new Box("", new Vector3d(0, 0, 0), new Vector3d(1, 1, 1), new Quaterniond());
        Box wider = new Box("", new Vector3d(0, 0, 0), new Vector3d(1.1, 1.1, 1), new Quaterniond());
        List<Box> corner = new ArrayList<>(blocks(1, -6, -6, 1, 6, 6));
        corner.addAll(blocks(-6, 1, -6, 6, 1, 6));
        Vec3 push = BodyBoxes.turnPush(List.of(inCorner), List.of(wider), corner).orElseThrow();
        assertEquals(-0.1, push.x, 1e-6);
        assertEquals(-0.1, push.y, 1e-6);
    }

    /** Solid rock past x = -1, for the nose to be buried in. */
    private static List<Box> rock() {
        return blocks(-6, -4, 6, -2, 4, 16);
    }

    @Test
    void aBodyInAWallCanTurnOutOfIt() {
        BodyHeading stuck = new BodyHeading(8f, 0f);
        assertTrue(depth(BODY.apply(stuck), rock()) > 0.3, "the test wants the nose well in");
        Turn turn = stuck.turn(new BodyHeading(5f, 0f), BODY, rock());
        assertEquals(new BodyHeading(5f, 0f), turn.heading());
        assertEquals(Vec3.ZERO, turn.push(), "turning out needs no push");
    }

    @Test
    void aBodyInAWallCannotJustTurnDeeper() {
        BodyHeading stuck = new BodyHeading(8f, 0f);
        Turn turn = stuck.turn(new BodyHeading(9f, 0f), BODY, rock());
        assertTrue(turn.push().length() > 0.0, "it must be pushed for going deeper: " + turn);
        assertEquals(depth(BODY.apply(stuck), rock()), depth(shifted(BODY.apply(turn.heading()), turn.push()), rock()), 1e-6, "no deeper than it was");
    }

    @Test
    void aHairInsideAWallIsNoLicenceToTurnIn() {
        List<Box> wall = wallBeside(-2);
        Vec3 flush = new Vec3(-0.5 - 1e-6, 0, 0);
        List<Box> now = shifted(BODY.apply(new BodyHeading(0f, 0f)), flush);
        assertTrue(depth(now, wall) > 0.0, "the test wants a real overlap");
        Vec3 push = BodyBoxes.turnPush(now, shifted(BODY.apply(new BodyHeading(1f, 0f)), flush), wall).orElseThrow();
        assertTrue(push.x > 0.1, "the hair gave it room: " + push);
    }

    @Test
    void aRandomTurnNeverEndsDeeperOrPushesTooFar() {
        Random random = new Random(11);
        int pushed = 0;
        for (int round = 0; round < 2000; round++) {
            List<Box> blocks = new ArrayList<>();
            int clump = 1 + random.nextInt(50);
            for (int i = 0; i < clump; i++) {
                blocks.add(block((int) (random.nextGaussian() * 6), (int) (random.nextGaussian() * 4), (int) (random.nextGaussian() * 7)));
            }
            BodyHeading start = new BodyHeading(random.nextFloat() * 360f, random.nextFloat() * 120f - 60f);
            BodyHeading next = start.toward(random.nextFloat() * 360f, random.nextFloat() * 120f - 60f, 1.0 + random.nextDouble() * 4.0);
            Turn turn = start.turn(next, BODY, blocks);
            assertTrue(turn.push().length() <= BodyBoxes.MOST_PUSH + 1e-9, "too far: " + turn);
            double was = depth(BODY.apply(start), blocks);
            assertTrue(depth(shifted(BODY.apply(turn.heading()), turn.push()), blocks) <= was + 1e-6, "deeper, round " + round);
            if (turn.push().lengthSqr() > 0.0) {
                pushed++;
            }
        }
        assertTrue(pushed > 100, "the test hardly met a block: " + pushed);
    }
}
