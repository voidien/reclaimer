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

    @Test
    void cloudDragSlowsDriftEvenWithDampenerOff() {
        FlightPhysics.CloudDrag dense = new FlightPhysics.CloudDrag(1.0, 0.04, 0.6);
        Vec3 v = new Vec3(0, 0, 0.1);
        Vec3 next = FlightPhysics.step(v, Vec3.ZERO, true, false, STARTER, dense);
        assertEquals(0.096, next.z, EPS);
        Vec3 unpowered = FlightPhysics.step(v, Vec3.ZERO, false, false, STARTER, dense);
        assertEquals(0.096, unpowered.z, EPS);
    }

    @Test
    void cloudLowersTopSpeedSmoothly() {
        FlightPhysics.CloudDrag dense = new FlightPhysics.CloudDrag(1.0, 0.04, 0.6);
        // Entering the core at full speed bleeds off at most 8% per tick on top of drag, never snapping.
        Vec3 entering = FlightPhysics.step(new Vec3(0, 0, 0.5), Vec3.ZERO, true, false, STARTER, dense);
        assertTrue(entering.z < 0.5 && entering.z > 0.5 * 0.96 * 0.92 - EPS, "z=" + entering.z);

        Vec3 v = Vec3.ZERO;
        for (int i = 0; i < 400; i++) {
            v = FlightPhysics.step(v, FORWARD, true, true, STARTER, dense);
        }
        // Top speed in the densest core is at most 40% of the normal 0.5 blocks/tick.
        assertTrue(v.length() <= 0.2 + EPS, "core top speed " + v.length());
        assertTrue(v.length() > 0.1, "thrusters still make headway: " + v.length());
    }

    @Test
    void noCloudMeansNoDrag() {
        Vec3 v = new Vec3(0.3, 0, 0);
        assertEquals(v, FlightPhysics.step(v, Vec3.ZERO, true, false, STARTER, FlightPhysics.CloudDrag.NONE));
    }

    private static void assertVec(Vec3 expected, Vec3 actual) {
        assertEquals(expected.x, actual.x, EPS, "x");
        assertEquals(expected.y, actual.y, EPS, "y");
        assertEquals(expected.z, actual.z, EPS, "z");
    }

    /** Hover tests: a weight of 0.048, chosen to sit between the WEAK and STRONG test bodies (not the real default weight). */
    private static final double WEIGHT = 0.048;
    /** Accel 0.075: more than WEIGHT, so it can hover with 0.027 left to steer. */
    private static final FlightStats STRONG = new FlightStats(1.0, 0.075, 0.04, 0.5);
    /** Accel 0.03: less than WEIGHT, so it cannot hold itself up. */
    private static final FlightStats WEAK = new FlightStats(1.0, 0.03, 0.04, 0.5);

    private static Vec3 run(Vec3 velocity, Vec3 thrust, boolean powered, boolean dampener, FlightStats stats, int ticks) {
        for (int i = 0; i < ticks; i++) {
            velocity = FlightPhysics.step(velocity, thrust, powered, dampener, stats, FlightPhysics.CloudDrag.NONE, WEIGHT);
        }
        return velocity;
    }

    @Test
    void zeroWeightIsExactlyTheWeightlessStep() {
        Vec3 start = new Vec3(0.2, -0.1, 0.3);
        for (Vec3 thrust : new Vec3[] {Vec3.ZERO, FORWARD, new Vec3(0, 1, 0)}) {
            for (boolean dampener : new boolean[] {true, false}) {
                assertEquals(FlightPhysics.step(start, thrust, true, dampener, STARTER),
                        FlightPhysics.step(start, thrust, true, dampener, STARTER, FlightPhysics.CloudDrag.NONE, 0.0));
            }
        }
    }

    @Test
    void dampenerHoldsAltitudeWhenThrustCoversWeight() {
        Vec3 v = run(Vec3.ZERO, Vec3.ZERO, true, true, STRONG, 100);
        assertEquals(0.0, v.y, EPS);
        assertEquals(0.0, v.x, EPS);
    }

    @Test
    void weightComesFirstAndOnlyTheRemainderSteers() {
        Vec3 v = FlightPhysics.step(Vec3.ZERO, FORWARD, true, true, STRONG, FlightPhysics.CloudDrag.NONE, WEIGHT);
        assertEquals(0.075 - WEIGHT, v.z, EPS);
        assertEquals(0.0, v.y, EPS, "weight is still cancelled while steering");
    }

    @Test
    void aBodyThatCannotCoverItsWeightFallsSlowlyAndDoesNotSteer() {
        Vec3 coasting = run(Vec3.ZERO, Vec3.ZERO, true, true, WEAK, 100);
        assertEquals(-(WEIGHT - 0.03), coasting.y, EPS, "falls at the unsupported weight, not at full gravity");
        assertTrue(coasting.y > -WEIGHT, "partial lift slows the fall");
        Vec3 steering = run(Vec3.ZERO, FORWARD, true, true, WEAK, 20);
        assertEquals(0.0, steering.z, EPS, "no thrust is left to steer");
    }

    @Test
    void dampenerOffOrNoPowerFallsUnderFullWeight() {
        assertEquals(-WEIGHT * 10, run(Vec3.ZERO, Vec3.ZERO, true, false, STRONG, 10).y, EPS);
        assertEquals(-WEIGHT * 10, run(Vec3.ZERO, Vec3.ZERO, false, true, STRONG, 10).y, EPS);
    }

    @Test
    void upThrustClimbsAtTheSteeringRemainder() {
        Vec3 v = run(Vec3.ZERO, new Vec3(0, 1, 0), true, true, STRONG, 4);
        assertEquals(4 * (0.075 - WEIGHT), v.y, EPS);
    }

    @Test
    void hoverShareIsTheShareOfThrustSpentOnWeight() {
        assertEquals(WEIGHT / 0.075, FlightPhysics.hoverShare(STRONG, WEIGHT, true, true), EPS);
        assertEquals(1.0, FlightPhysics.hoverShare(WEAK, WEIGHT, true, true), EPS, "a weak body spends everything");
        assertEquals(0.0, FlightPhysics.hoverShare(STRONG, WEIGHT, false, true), EPS, "no dampener, no station-keeping");
        assertEquals(0.0, FlightPhysics.hoverShare(STRONG, WEIGHT, true, false), EPS, "no power");
        assertEquals(0.0, FlightPhysics.hoverShare(STRONG, 0.0, true, true), EPS, "zero-G costs nothing");
        // A heavier body of the same thrust pays a bigger share.
        FlightStats heavy = new FlightStats(1.5, 0.075, 0.04, 0.5);
        assertTrue(FlightPhysics.hoverShare(heavy, WEIGHT, true, true) > FlightPhysics.hoverShare(STRONG, WEIGHT, true, true));
    }
}
