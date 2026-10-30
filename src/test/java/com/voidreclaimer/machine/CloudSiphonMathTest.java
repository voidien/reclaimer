package com.voidreclaimer.machine;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class CloudSiphonMathTest {
    @Test
    void thinDustMakesNothing() {
        assertEquals(0f, CloudSiphonMath.itemsPerSecond(0.39, 1f, 0.4, 0.5), 1e-6f);
        assertEquals(0f, CloudSiphonMath.itemsPerSecond(0.0, 1f, 0.0, 0.5), 1e-6f);
    }

    @Test
    void outputScalesWithDensityAndPower() {
        assertEquals(0.5f, CloudSiphonMath.itemsPerSecond(1.0, 1f, 0.4, 0.5), 1e-6f);
        assertEquals(0.25f, CloudSiphonMath.itemsPerSecond(0.5, 1f, 0.4, 0.5), 1e-6f);
        assertEquals(1.0f, CloudSiphonMath.itemsPerSecond(1.0, 2f, 0.4, 0.5), 1e-6f);
    }

    @Test
    void noPowerMakesNothing() {
        assertEquals(0f, CloudSiphonMath.itemsPerSecond(1.0, 0f, 0.4, 0.5), 1e-6f);
        assertEquals(0f, CloudSiphonMath.itemsPerSecond(1.0, -1f, 0.4, 0.5), 1e-6f);
    }

    @Test
    void theMinimumDensityItselfWorks() {
        assertEquals(0.2f, CloudSiphonMath.itemsPerSecond(0.4, 1f, 0.4, 0.5), 1e-6f);
    }
}
