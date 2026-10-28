package com.voidreclaimer.flight;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.Test;

class FlightPhysicsTest {
    private static final double EPS = 1.0E-6;
    /** Starter drone defaults: accel 0.03, brake 0.04, top speed 0.5 blocks/tick. */
    private static final FlightStats STARTER = new FlightStats(1.0, 0.03, 0.04, 0.5);
    private static final Vec3 FORWARD = new Vec3(0, 0, 1);

    @Test
    void thrustAcceleratesByThrustOverMass() {
        Vec3 v = FlightPhysics.step(Vec3.ZERO, FORWARD, true, true, STARTER);
        assertEquals(0.03, v.z, EPS);

        FlightStats heavy = new FlightStats(3.0, 0.03, 0.04, 0.5);
        Vec3 slow = FlightPhysics.step(Vec3.ZERO, FORWARD, true, true, heavy);
        assertEquals(0.01, slow.z, EPS);
    }

    @Test
    void thrustDirectionIsNormalized() {
        Vec3 v = FlightPhysics.step(Vec3.ZERO, new Vec3(3, 0, 4), true, true, STARTER);
        assertEquals(0.03, v.length(), EPS);
    }

    @Test
    void speedIsHardCapped() {
        Vec3 v = Vec3.ZERO;
        for (int i = 0; i < 200; i++) {
            v = FlightPhysics.step(v, FORWARD, true, true, STARTER);
        }
        assertEquals(0.5, v.length(), EPS);
    }

    @Test
    void noDragWhenCoastingWithDampenerOff() {
        Vec3 start = new Vec3(0.2, -0.1, 0.3);
        Vec3 v = start;
        for (int i = 0; i < 100; i++) {
            v = FlightPhysics.step(v, Vec3.ZERO, true, false, STARTER);
        }
        assertEquals(start, v);
    }

    @Test
    void dampenerBrakesToExactlyZero() {
        Vec3 v = new Vec3(0, 0, 0.5);
        int ticks = 0;
        while (v.lengthSqr() > 0 && ticks < 100) {
            v = FlightPhysics.step(v, Vec3.ZERO, true, true, STARTER);
            ticks++;
        }
        assertEquals(Vec3.ZERO, v);
        // 0.5 / 0.04 = 12.5 -> 13 ticks
        assertEquals(13, ticks);
    }

    @Test
    void lowPowerDisablesThrustAndDampener() {
        Vec3 drift = new Vec3(0.1, 0, 0);
        assertEquals(drift, FlightPhysics.step(drift, FORWARD, false, true, STARTER));
        assertEquals(drift, FlightPhysics.step(drift, Vec3.ZERO, false, true, STARTER));
    }

    @Test
    void lowPowerStillCapsSpeed() {
        Vec3 tooFast = new Vec3(0, 0, 2.0);
        assertEquals(0.5, FlightPhysics.step(tooFast, Vec3.ZERO, false, false, STARTER).length(), EPS);
    }

    @Test
    void thrustDirectionIsViewRelative() {
        FlightInput forward = FlightInput.of(true, false, false, false, false, false);
        FlightInput right = FlightInput.of(false, false, false, true, false, false);
        FlightInput up = FlightInput.of(false, false, false, false, true, false);

        // Yaw 0, pitch 0 faces +Z (south); the player's right is -X (west); up is +Y.
        assertVec(new Vec3(0, 0, 1), FlightPhysics.thrustDirection(0f, 0f, forward));
        assertVec(new Vec3(-1, 0, 0), FlightPhysics.thrustDirection(0f, 0f, right));
        assertVec(new Vec3(0, 1, 0), FlightPhysics.thrustDirection(0f, 0f, up));

        // Looking straight up (pitch -90): forward thrust goes up, "up" thrust goes backward (-Z).
        assertVec(new Vec3(0, 1, 0), FlightPhysics.thrustDirection(-90f, 0f, forward));
        assertVec(new Vec3(0, 0, -1), FlightPhysics.thrustDirection(-90f, 0f, up));

        // Strafe stays horizontal regardless of pitch.
        assertEquals(0.0, FlightPhysics.thrustDirection(45f, 30f, right).y, EPS);
    }

    @Test
    void opposingKeysCancel() {
        FlightInput both = FlightInput.of(true, true, false, false, false, false);
        assertEquals(0.0, FlightPhysics.thrustDirection(0f, 0f, both).length(), EPS);
        assertTrue(!both.isThrusting());
    }

    @Test
    void speedLimitCheck() {
        Vec3 start = Vec3.ZERO;
        // 20 ticks at the 0.5 b/t cap with tolerance 1.5 and slack 1 allows 16 blocks.
        assertTrue(!FlightPhysics.exceedsSpeedLimit(start, new Vec3(10, 0, 0), 20, 0.5, 1.5, 1.0));
        assertTrue(!FlightPhysics.exceedsSpeedLimit(start, new Vec3(16, 0, 0), 20, 0.5, 1.5, 1.0));
        assertTrue(FlightPhysics.exceedsSpeedLimit(start, new Vec3(16.5, 0, 0), 20, 0.5, 1.5, 1.0));
    }

    private static void assertVec(Vec3 expected, Vec3 actual) {
        assertEquals(expected.x, actual.x, EPS, "x");
        assertEquals(expected.y, actual.y, EPS, "y");
        assertEquals(expected.z, actual.z, EPS, "z");
    }
}
