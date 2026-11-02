package com.voidreclaimer.link;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class RelayPowerTest {
    private static final int PER_TICK = 20;
    private static final int BUFFER = 2000;

    @Test
    void aLitRelayRunsWhileItHasATicksEnergy() {
        assertTrue(RelayPower.runs(20, true, PER_TICK, BUFFER));
        assertFalse(RelayPower.runs(19, true, PER_TICK, BUFFER));
    }

    @Test
    void aDarkRelayWaitsForASecondOfEnergy() {
        assertFalse(RelayPower.runs(399, false, PER_TICK, BUFFER));
        assertTrue(RelayPower.runs(400, false, PER_TICK, BUFFER));
    }

    @Test
    void aSmallBufferCanStillRestart() {
        assertTrue(RelayPower.runs(100, false, PER_TICK, 100));
        assertFalse(RelayPower.runs(99, false, PER_TICK, 100));
    }

    @Test
    void aBufferBelowOneTicksUseRunsOnWhatItHas() {
        assertTrue(RelayPower.runs(10, true, PER_TICK, 10));
        assertEquals(0, RelayPower.after(10, PER_TICK));
    }

    @Test
    void aFreeRelayAlwaysRuns() {
        assertTrue(RelayPower.runs(0, false, 0, BUFFER));
        assertEquals(0, RelayPower.after(0, 0));
    }

    @Test
    void runningSpendsOneTicksUse() {
        assertEquals(380, RelayPower.after(400, PER_TICK));
    }
}
