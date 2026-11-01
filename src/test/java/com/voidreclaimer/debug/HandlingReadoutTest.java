package com.voidreclaimer.debug;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class HandlingReadoutTest {
    private static final HandlingReadout.Row SHIP = new HandlingReadout.Row("Ship", 15, 20, 30, 6, 12, 12);

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
        HandlingReadout.Row none = new HandlingReadout.Row("Bare", 1, 2, 10, 0, 0, 0);
        assertEquals(0.0, none.lengthsPerSecond());
        assertEquals(0.0, none.secondsToTop());
        assertEquals(0.0, none.stoppingDistance());
    }

    @Test
    void anOverloadedBodySaysSo() {
        assertTrue(HandlingReadout.line(new HandlingReadout.Row("Mech", 5.5, 5, 14, 14, 20, 2)).contains("5.5 of 5.0 OVER"));
        assertFalse(HandlingReadout.line(SHIP).contains("OVER"));
    }
}
