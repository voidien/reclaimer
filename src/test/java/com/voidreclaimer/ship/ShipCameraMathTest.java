package com.voidreclaimer.ship;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.joml.Quaterniond;
import org.joml.Vector3d;
import org.junit.jupiter.api.Test;

class ShipCameraMathTest {
    private static final float EPS = 1e-3f;
    /** Minecraft's yaw: south 0, west 90, north 180, east -90. */
    private static final float NORTH = 180f, SOUTH = 0f, EAST = -90f;
    private static final Quaterniond ROLL_RIGHT_30 = new Quaterniond().rotateAxis(Math.toRadians(30), 0, 0, -1);
    private static final Quaterniond NOSE_UP_20 = new Quaterniond().rotateAxis(Math.toRadians(20), 1, 0, 0);

    @Test
    void aLevelShipNeedsNoRollWhicheverWayThePilotLooks() {
        for (float yaw : new float[] {NORTH, SOUTH, EAST, 37f}) {
            for (float pitch : new float[] {0f, 30f, -45f}) {
                assertEquals(0f, ShipCameraMath.roll(new Quaterniond(), yaw, pitch, 9f), EPS);
            }
        }
    }

    @Test
    void lookingAheadTheCameraRollsWithTheShip() {
        // The ship (facing north) rolls right 30 degrees: the pilot looking north rolls right with it.
        assertEquals(30f, ShipCameraMath.roll(ROLL_RIGHT_30, NORTH, 0f, 0f), EPS);
        // Looking back over the stern, the same roll is to the left.
        assertEquals(-30f, ShipCameraMath.roll(ROLL_RIGHT_30, SOUTH, 0f, 0f), EPS);
    }

    @Test
    void aPitchedShipRollsTheViewAcrossTheBeam() {
        // Nose up tips the ship's up aft (south). Looking north or south that is no roll; looking east, south is to the
        // right, so the view rolls right by the pitch.
        assertEquals(0f, ShipCameraMath.roll(NOSE_UP_20, NORTH, 0f, 0f), EPS);
        assertEquals(20f, ShipCameraMath.roll(NOSE_UP_20, EAST, 0f, 0f), EPS);
    }

    @Test
    void lookingStraightAlongTheShipsUpKeepsTheLastRoll() {
        // A level ship's up is straight up; a pilot looking straight up has no horizon to roll against.
        assertEquals(7f, ShipCameraMath.roll(new Quaterniond(), NORTH, -90f, 7f), EPS);
    }

    @Test
    void theViewFollowsTheNose() {
        assertEquals(0f, ShipCameraMath.yaw(new Vector3d(0, 0, 1)), 1e-3f, "south");
        assertEquals(90f, Math.abs(ShipCameraMath.yaw(new Vector3d(-1, 0, 0))), 1e-3f);
        assertEquals(90f, ShipCameraMath.yaw(new Vector3d(-1, 0, 0)), 1e-3f, "west");
        assertEquals(-90f, ShipCameraMath.yaw(new Vector3d(1, 0, 0)), 1e-3f, "east");
        assertEquals(-45f, ShipCameraMath.pitch(new Vector3d(0, 1, 1)), 1e-3f, "up is negative");
        assertEquals(45f, ShipCameraMath.pitch(new Vector3d(1, -1, 0)), 1e-3f, "down is positive");
    }
}
