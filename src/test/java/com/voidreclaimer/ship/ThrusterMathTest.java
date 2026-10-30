package com.voidreclaimer.ship;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class ThrusterMathTest {
    private static final double EPS = 1.0E-9;

    @Test
    void redstoneSignalIsTheThrottle() {
        assertEquals(0.0, ThrusterMath.throttle(0), EPS);
        assertEquals(1.0, ThrusterMath.throttle(15), EPS);
        assertEquals(1.0 / 3.0, ThrusterMath.throttle(5), EPS);
        assertEquals(1.0, ThrusterMath.throttle(99), EPS, "clamped high");
        assertEquals(0.0, ThrusterMath.throttle(-1), EPS, "clamped low");
    }

    @Test
    void forceScalesWithFuelThrottleAndSpin() {
        assertEquals(400.0, ThrusterMath.chemicalForce(400, 1, 1, 1), EPS);
        assertEquals(200.0, ThrusterMath.chemicalForce(400, 0.5, 1, 1), EPS);
        assertEquals(100.0, ThrusterMath.chemicalForce(400, 1, 0.25, 1), EPS);
        assertEquals(200.0, ThrusterMath.chemicalForce(400, 1, 1, 0.5), EPS);
    }

    @Test
    void aFasterShaftDoesNotPushHarderThanFullSpin() {
        assertEquals(400.0, ThrusterMath.chemicalForce(400, 1, 1, 4.0), EPS);
        assertEquals(2.0, ThrusterMath.burn(2, 1, 4.0), EPS, "nor does it burn more");
    }

    @Test
    void noThrottleOrNoSpinMeansNoForceAndNoBurn() {
        assertEquals(0.0, ThrusterMath.chemicalForce(400, 1, 0, 1), EPS);
        assertEquals(0.0, ThrusterMath.chemicalForce(400, 1, 1, 0), EPS);
        assertEquals(0.0, ThrusterMath.burn(2, 0, 1), EPS);
        assertEquals(0.0, ThrusterMath.burn(2, 1, 0), EPS);
    }

    @Test
    void burnFollowsTheThrottle() {
        assertTrue(ThrusterMath.burn(2, 0.5, 1) < ThrusterMath.burn(2, 1, 1));
        assertEquals(1.0, ThrusterMath.burn(2, 0.5, 1), EPS);
    }
}
