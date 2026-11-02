package com.voidreclaimer.debug;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class HandlingReadoutTest {
    private static final HandlingReadout.Row SHIP = new HandlingReadout.Row("Ship", 15, 20, 30, 6, 12, 12, 45);

    @Test
    void speedIsMeasuredInTheBodysOwnLengths() {
        assertEquals(2.5, SHIP.lengthsPerSecond(), 1e-9);
    }

    @Test
    void timeToTopAndStoppingDistanceComeFromTheRates() {
        assertEquals(5.0, SHIP.secondsToTop(), 1e-9);
        assertEquals(37.5, SHIP.stoppingDistance(), 1e-9, "30 m/s braking at 12: v squared over twice the rate");
    }

    @Test
    void aBodyWithNoThrustOrModelReadsZeroNotInfinity() {
        HandlingReadout.Row none = new HandlingReadout.Row("Bare", 1, 2, 10, 0, 0, 0, Double.POSITIVE_INFINITY);
        assertEquals(0.0, none.lengthsPerSecond());
        assertEquals(0.0, none.secondsToTop());
        assertEquals(0.0, none.stoppingDistance());
    }

    @Test
    void anOverloadedBodySaysSo() {
        assertTrue(HandlingReadout.line(new HandlingReadout.Row("Mech", 5.5, 5, 14, 14, 20, 2, Double.POSITIVE_INFINITY)).contains("5.5 of 5.0 OVER"));
        assertFalse(HandlingReadout.line(SHIP).contains("OVER"));
    }

    @Test
    void aBigBodyShowsItsHalfTurnAndASmallOneTurnsAtOnce() {
        assertEquals(4.0, SHIP.secondsToHalfTurn(), 1e-9);
        assertTrue(HandlingReadout.line(SHIP).contains("45 deg/s, half turn 4.0 s"), HandlingReadout.line(SHIP));
        HandlingReadout.Row drone = new HandlingReadout.Row("Drone", 1, 2, 10, 1, 1, 1, Double.POSITIVE_INFINITY);
        assertEquals(0.0, drone.secondsToHalfTurn(), 1e-9);
        assertTrue(HandlingReadout.line(drone).contains("turns at once"), HandlingReadout.line(drone));
    }
}
