package com.voidreclaimer.body;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import com.voidreclaimer.api.body.Chassis;
import com.voidreclaimer.api.body.HitBox;
import com.voidreclaimer.api.body.Socket;
import com.voidreclaimer.body.BodyBoxes.Box;
import com.voidreclaimer.flight.FlightPhysics;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.Set;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaterniond;
import org.joml.Vector3d;
import org.junit.jupiter.api.Test;

class BodyBoxesTest {
    @org.junit.jupiter.api.Test
    void aBoxHasItsEightCornersWhereItsSidesAre() {
        var box = BodyBoxes.Box.of(new net.minecraft.world.phys.AABB(1, 2, 3, 3, 6, 9));
        var corners = BodyBoxes.corners(box);
        org.junit.jupiter.api.Assertions.assertEquals(8, corners.size());
        org.junit.jupiter.api.Assertions.assertEquals(8L, corners.stream().distinct().count(), "two corners fell on the same place");
        org.junit.jupiter.api.Assertions.assertEquals(new net.minecraft.world.phys.Vec3(1, 2, 3), corners.get(0));
        org.junit.jupiter.api.Assertions.assertEquals(new net.minecraft.world.phys.Vec3(3, 6, 9), corners.get(7));
    }

    private static final double EPS = 1e-9;
    private static final Vec3 FEET = new Vec3(100, 64, -30);

    private static Chassis chassis(Socket... sockets) {
        return new Chassis(BuiltinBodies.SHIP_SIZE, "root", List.of(sockets));
    }

    private static Socket socket(String id, HitBox... boxes) {
        return new Socket(id, List.of(), false, Optional.empty(), Vec3.ZERO, "body").withBoxes(boxes);
    }

    private static HitBox box(double right, double up, double forward) {
        return new HitBox(new Vec3(right, up, forward), new Vec3(1, 1, 1));
    }

    private static Box only(Chassis chassis, float yaw, float pitch, double scale) {
        return BodyBoxes.place(chassis, id -> true, FEET, yaw, pitch, scale).get(0);
    }

    private static void assertVec(double x, double y, double z, org.joml.Vector3dc actual) {
        assertEquals(x, actual.x(), EPS, "x of " + actual);
        assertEquals(y, actual.y(), EPS, "y of " + actual);
        assertEquals(z, actual.z(), EPS, "z of " + actual);
    }

    private static Box cube(double x, double y, double z) {
        return Box.of(new AABB(x, y, z, x + 1, y + 1, z + 1));
    }

    /** A box at the origin, turned about the Y axis. */
    private static Box turned(double degrees, double half) {
        return new Box("", new Vector3d(), new Vector3d(half, half, half), new Quaterniond().rotationY(Math.toRadians(degrees)));
    }

    @Test
    void aBoxAheadOfTheBodyIsAlongTheView() {
        Chassis ahead = chassis(socket("nose", box(0, 0, 1)));
        for (float yaw : new float[] {0f, 37f, 90f, -120f, 180f}) {
            for (float pitch : new float[] {0f, 25f, -60f, 89f}) {
                Vec3 view = FlightPhysics.viewVector(pitch, yaw);
                Vector3d seen = new Vector3d(only(ahead, yaw, pitch, 1.0).middle()).sub(FEET.x, FEET.y, FEET.z);
                assertEquals(view.x, seen.x, 1e-3, yaw + "," + pitch);
                assertEquals(view.y, seen.y, 1e-3, yaw + "," + pitch);
                assertEquals(view.z, seen.z, 1e-3, yaw + "," + pitch);
            }
        }
    }

    @Test
    void rightAndUpAreThePilots() {
        // Yaw 0 looks south, so the pilot's right is west.
        assertVec(FEET.x - 1, FEET.y, FEET.z, only(chassis(socket("wing", box(1, 0, 0))), 0f, 0f, 1.0).middle());
        assertVec(FEET.x, FEET.y + 1, FEET.z, only(chassis(socket("top", box(0, 1, 0))), 0f, 0f, 1.0).middle());
    }

    @Test
    void theScaleGrowsWhereABoxIsAndHowBigItIs() {
        Chassis tail = chassis(socket("tail", new HitBox(new Vec3(0, 0, -1), new Vec3(2, 3, 4))));
        Box box = only(tail, 0f, 0f, 8.0);
        assertVec(FEET.x, FEET.y, FEET.z - 8, box.middle());
        assertVec(8, 12, 16, box.half());
    }

    @Test
    void aBoxTurnsWithTheBody() {
        Box box = only(chassis(socket("nose", box(0, 0, 1))), 90f, 0f, 1.0);
        Vector3d along = box.turn().transform(new Vector3d(0, 0, -1));
        assertVec(-1, 0, 0, along);
    }

    @Test
    void aMissingSectionLosesItsBoxes() {
        Chassis two = chassis(socket("left", box(-1, 0, 0), box(-2, 0, 0)), socket("right", box(1, 0, 0)));
        assertEquals(3, BodyBoxes.place(two, id -> true, FEET, 0f, 0f, 1.0).size());
        List<Box> kept = BodyBoxes.place(two, Set.of("right")::contains, FEET, 0f, 0f, 1.0);
        assertEquals(1, kept.size());
        assertEquals("right", kept.get(0).socket());
    }

    @Test
    void boxesApartDoNotPush() {
        assertTrue(BodyBoxes.push(cube(0, 0, 0), cube(3, 0, 0)).isEmpty());
    }

    @Test
    void boxesThatOnlyTouchDoNotPush() {
        assertTrue(BodyBoxes.push(cube(0, 0, 0), cube(1, 0, 0)).isEmpty());
    }

    @Test
    void anOverlapPushesTheShortWayOut() {
        assertVec(-0.25, 0, 0, BodyBoxes.push(cube(0, 0, 0), cube(0.75, 0.1, 0.1)).orElseThrow());
        assertVec(0, 0.25, 0, BodyBoxes.push(cube(0, 0.75, 0), cube(0.1, 0, 0.1)).orElseThrow());
    }

    @Test
    void aBlockOnATurnedBoxCorner() {
        // A box of half size 1 turned 45 degrees has a corner at sqrt 2 along X, into a block that starts at 1.2.
        assertVec(1.2 - Math.sqrt(2), 0, 0, BodyBoxes.push(turned(45, 1), cube(1.2, -0.5, -0.5)).orElseThrow());
    }

    @Test
    void aGapOnlyAFaceOfTheTurnedBoxShows() {
        // The block overlaps that box's reach along X, Y and Z, but its nearest corner is past the box's slanting face.
        assertTrue(BodyBoxes.push(turned(45, 1), cube(0.95, -0.5, 0.95)).isEmpty());
        assertTrue(BodyBoxes.push(cube(0.95, -0.5, 0.95), turned(45, 1)).isEmpty(), "whichever of the two is moving");
    }

    @Test
    void aGapOnlyOneBoxsOwnFaceShows() {
        // A cube just off the slightly tilted box's top face: only that face's axis has a gap, whichever of the two is moving.
        Quaterniond tilt = new Quaterniond().rotationXYZ(0.1, 0.1, 0.1);
        Vector3d top = tilt.transform(new Vector3d(0, 1, 0));
        double reach = 0.5 * (Math.abs(top.x) + Math.abs(top.y) + Math.abs(top.z));
        Box tilted = new Box("", new Vector3d(), new Vector3d(1, 1, 1), tilt);
        Box off = new Box("", new Vector3d(top).mul(1 + reach + 0.05), new Vector3d(0.5, 0.5, 0.5), new Quaterniond());
        Box on = new Box("", new Vector3d(top).mul(1 + reach - 0.05), new Vector3d(0.5, 0.5, 0.5), new Quaterniond());
        assertTrue(BodyBoxes.push(off, tilted).isEmpty());
        assertTrue(BodyBoxes.push(tilted, off).isEmpty());
        assertTrue(BodyBoxes.push(on, tilted).isPresent(), "a little closer, they overlap");
    }

    @Test
    void aGapOnlyACrossOfTwoEdgesShows() {
        // Two long thin sticks passing each other askew. Along either stick's own axes they overlap; the gap is along the cross of the two.
        Quaterniond one = new Quaterniond().rotationZ(Math.toRadians(45));
        Quaterniond other = new Quaterniond().rotationX(Math.toRadians(-45));
        Vector3d across = new Vector3d(1, -1, 1).normalize();
        Box stick = new Box("", new Vector3d(), new Vector3d(5, 0.1, 0.1), one);
        assertTrue(BodyBoxes.push(stick, new Box("", new Vector3d(across).mul(0.5), new Vector3d(0.1, 0.1, 5), other)).isEmpty(), "they pass with a gap");
        assertTrue(BodyBoxes.push(stick, new Box("", new Vector3d(across).mul(0.2), new Vector3d(0.1, 0.1, 5), other)).isPresent(), "closer, they touch");
    }

    @Test
    void aPushAlwaysFreesTheBox() {
        Random random = new Random(42);
        int pushed = 0;
        for (int i = 0; i < 5000; i++) {
            Box first = random(random);
            Box second = random(random);
            Optional<Vector3d> push = BodyBoxes.push(first, second);
            if (push.isPresent()) {
                pushed++;
                Vector3d moved = new Vector3d(first.middle()).add(new Vector3d(push.get()).mul(1.000001));
                assertTrue(BodyBoxes.push(new Box("", moved, first.half(), first.turn()), second).isEmpty(), "still inside after " + push.get());
            }
        }
        assertTrue(pushed > 500, "the test hardly met an overlap: " + pushed);
    }

    private static Box random(Random random) {
        Quaterniond turn = new Quaterniond().rotationXYZ(random.nextDouble() * 6, random.nextDouble() * 6, random.nextDouble() * 6);
        return new Box("", new Vector3d(random.nextDouble() * 4, random.nextDouble() * 4, random.nextDouble() * 4),
                new Vector3d(0.3 + random.nextDouble() * 2, 0.3 + random.nextDouble() * 2, 0.3 + random.nextDouble() * 2), turn);
    }

    @Test
    void theBigBodiesHaveTheirBoxesAndTheOthersNone() {
        assertEquals(19, boxes(BuiltinBodies.TITAN));
        assertEquals(14, boxes(BuiltinBodies.SHUTTLE));
        assertEquals(29, boxes(BuiltinBodies.SHIP));
        assertEquals(0, boxes(BuiltinBodies.DRONE));
        assertEquals(0, boxes(BuiltinBodies.MECH));
    }

    private static int boxes(net.minecraft.resources.ResourceKey<Chassis> key) {
        return BuiltinBodies.CHASSIS.get(key).sockets().stream().mapToInt(socket -> socket.boxes().size()).sum();
    }

    @Test
    void aLeftSectionMirrorsItsRight() {
        for (Chassis chassis : List.of(BuiltinBodies.CHASSIS.get(BuiltinBodies.TITAN), BuiltinBodies.CHASSIS.get(BuiltinBodies.SHUTTLE))) {
            for (Socket socket : chassis.sockets()) {
                if (socket.id().contains("_l") && !socket.boxes().isEmpty()) {
                    Socket right = chassis.socket(socket.id().replace("_l", "_r"));
                    assertEquals(socket.boxes().size(), right.boxes().size(), socket.id());
                    for (int i = 0; i < socket.boxes().size(); i++) {
                        HitBox left = socket.boxes().get(i);
                        assertTrue(left.at().x < 0.0, socket.id() + " should be on the pilot's left");
                        assertEquals(left.at().multiply(-1, 1, 1), right.boxes().get(i).at(), socket.id());
                        assertEquals(left.size(), right.boxes().get(i).size(), socket.id());
                    }
                }
            }
        }
    }

    @Test
    void aSocketKeepsItsBoxesWhenMirrored() {
        assertEquals(1, socket("wing", box(1, 0, 0)).mirrored().boxes().size());
    }

    @Test
    void aBoxReadsFromJsonAndASizeOfNothingIsRefused() {
        var good = JsonParser.parseString("{\"at\": [0, 1.5, 2], \"size\": [1, 2, 3]}");
        HitBox box = HitBox.CODEC.parse(JsonOps.INSTANCE, good).getOrThrow();
        assertEquals(new Vec3(0, 1.5, 2), box.at());
        assertEquals(new Vec3(1, 2, 3), box.size());
        assertTrue(HitBox.CODEC.parse(JsonOps.INSTANCE, JsonParser.parseString("{\"at\": [0, 0, 0], \"size\": [1, 0, 3]}")).isError());
    }

    @Test
    void aSocketWithNoBoxesWritesNone() {
        var json = Socket.CODEC.encodeStart(JsonOps.INSTANCE, socket("plain")).getOrThrow().getAsJsonObject();
        assertFalse(json.has("boxes"), "a chassis file should not change for a body with no boxes");
        Socket boxed = socket("wing", box(1, 0, 0));
        var back = Socket.CODEC.parse(JsonOps.INSTANCE, Socket.CODEC.encodeStart(JsonOps.INSTANCE, boxed).getOrThrow()).getOrThrow();
        assertEquals(boxed.boxes(), back.boxes());
    }
}
