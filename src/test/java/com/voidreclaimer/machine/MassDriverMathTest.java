package com.voidreclaimer.machine;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class MassDriverMathTest {
    // Defaults: minimum 64 RPM, fastest at 256 RPM, 400 ticks (20 s) slow, 60 ticks (3 s) fast.
    private static int ticks(float rpm) {
        return MassDriverMath.launchTicks(rpm, 64, 256, 400, 60);
    }

    @Test
    void tooSlowNeverLaunches() {
        assertEquals(-1, ticks(63.9f));
        assertEquals(-1, ticks(0f));
        assertEquals(0f, MassDriverMath.progressPerTick(10f, 64, 256, 400, 60));
    }

    @Test
    void slowestSpeedTakesTwentySeconds() {
        assertEquals(400, ticks(64f));
    }

    @Test
    void fastestSpeedTakesThreeSecondsAndNoFasterBeyond() {
        assertEquals(60, ticks(256f));
        assertEquals(60, ticks(512f));
    }

    @Test
    void speedingUpShortensTheWindUpMonotonically() {
        int previous = Integer.MAX_VALUE;
        for (float rpm = 64f; rpm <= 256f; rpm += 8f) {
            int now = ticks(rpm);
            assertTrue(now <= previous, "rpm " + rpm);
            previous = now;
        }
        assertEquals(230, ticks(160f));
    }

    @Test
    void reverseRotationLaunchesToo() {
        assertEquals(ticks(128f), ticks(-128f));
    }

    @Test
    void progressPerTickIsTheInverseOfTheInterval() {
        assertEquals(1f / 400f, MassDriverMath.progressPerTick(64f, 64, 256, 400, 60), 1e-6f);
    }

    @Test
    void receiverIsLinearInTrussesAndCapsAtTheShell() {
        assertEquals(0f, MassDriverMath.receiverFraction(0, 2000));
        assertEquals(0.5f, MassDriverMath.receiverFraction(1000, 2000), 1e-6f);
        assertEquals(1f, MassDriverMath.receiverFraction(5000, 2000));
        assertEquals(0f, MassDriverMath.receiverFraction(10, 0));
    }

    @Test
    void completedShellGivesTheFullOutput() {
        float perRpm = MassDriverMath.receiverCapacityPerRpm(2000, 2000, 262_144.0, 128);
        assertEquals(262_144f, perRpm * 128f, 0.5f);
        assertEquals(131_072f, MassDriverMath.receiverCapacityPerRpm(1000, 2000, 262_144.0, 128) * 128f, 0.5f);
        assertEquals(0f, MassDriverMath.receiverCapacityPerRpm(1000, 2000, 262_144.0, 0));
    }
}
