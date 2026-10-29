package com.voidreclaimer.machine;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class MachineMathTest {
    @Test
    void runsOnlyAtOrAboveTheMinimumInEitherDirection() {
        assertFalse(MachineMath.runs(15f, 16));
        assertTrue(MachineMath.runs(16f, 16));
        assertTrue(MachineMath.runs(-64f, 16));
        assertFalse(MachineMath.runs(0f, 16));
    }

    @Test
    void progressIsProportionalToSpeed() {
        assertEquals(1f, MachineMath.progressPerTick(64f, 16));
        assertEquals(2f, MachineMath.progressPerTick(128f, 16));
        assertEquals(1f, MachineMath.progressPerTick(-64f, 16));
        assertEquals(0f, MachineMath.progressPerTick(8f, 16));
    }

    @Test
    void cycleTicksShrinkWithSpeedAndRoundUp() {
        assertEquals(60, MachineMath.cycleTicks(64f, 16, 60));
        assertEquals(30, MachineMath.cycleTicks(128f, 16, 60));
        assertEquals(240, MachineMath.cycleTicks(16f, 16, 60));
        assertEquals(60, MachineMath.cycleTicks(-64f, 16, 60));
        assertEquals(-1, MachineMath.cycleTicks(10f, 16, 60));
        assertEquals(21, MachineMath.cycleTicks(192f, 16, 60 + 3)); // 63 / 3 = 21
    }
}
