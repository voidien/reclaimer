package com.voidreclaimer.helper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class HelperBatteryTest {
    private static final float LOW = 0.1f, FLAT = 0.25f, CAPACITY = 2400f;

    @Test
    void aBatteryAboveLowGivesFullPower() {
        assertEquals(1f, HelperBattery.power(1f, LOW, FLAT), 1e-6);
        assertEquals(1f, HelperBattery.power(LOW, LOW, FLAT), 1e-6);
    }

    @Test
    void aFlatBatteryStillRunsAtTheFlatShare() {
        assertEquals(FLAT, HelperBattery.power(0f, LOW, FLAT), 1e-6);
        assertEquals(FLAT, HelperBattery.power(-1f, LOW, FLAT), 1e-6, "a share below zero counts as flat");
    }

    @Test
    void powerFadesWithoutASteps() {
        assertEquals((1f + FLAT) / 2f, HelperBattery.power(LOW / 2f, LOW, FLAT), 1e-6);
        float last = HelperBattery.power(0f, LOW, FLAT);
        for (int i = 1; i <= 100; i++) {
            float now = HelperBattery.power(LOW * i / 100f, LOW, FLAT);
            assertTrue(now >= last, "power fell at step " + i);
            assertTrue(now - last < 0.01f, "a step at " + i + ": " + last + " to " + now);
            last = now;
        }
    }

    @Test
    void gainAddsSunlightAndTheCore() {
        assertEquals(0f, HelperBattery.gain(0f, false, 1.2f, 4f), 1e-6, "dark, no core");
        assertEquals(0.8f, HelperBattery.gain(2f / 3f, false, 1.2f, 4f), 1e-5, "deep space's dim sun");
        assertEquals(4f, HelperBattery.gain(0f, true, 1.2f, 4f), 1e-6, "a core in the dark");
        assertEquals(4.8f, HelperBattery.gain(2f / 3f, true, 1.2f, 4f), 1e-5, "both");
    }

    @Test
    void aTickMovesTheShareByGainLessDrainOverCapacityAndStaysInRange() {
        assertEquals(0.5f + 1f / CAPACITY, HelperBattery.next(0.5f, 2f, 1f, CAPACITY), 1e-6);
        assertEquals(1f, HelperBattery.next(1f, 4f, 0f, CAPACITY), 1e-6, "never over full");
        assertEquals(0f, HelperBattery.next(0f, 0f, 1f, CAPACITY), 1e-6, "never under flat");
    }

    @Test
    void aDroneFlyingInTheDarkRunsFlatAfterTheBatterysLengthAndNeverStops() {
        float share = 1f;
        int ticks = 0;
        while (share > 0f && ticks < 100_000) {
            float power = HelperBattery.power(share, LOW, FLAT);
            share = HelperBattery.next(share, 0f, power, CAPACITY);
            ticks++;
        }
        assertTrue(ticks >= 2400 && ticks < 4000, "flat after " + ticks + " ticks");
        assertEquals(FLAT, HelperBattery.power(share, LOW, FLAT), 1e-6);
    }

    @Test
    void aDroneLivingOnTheSunSettlesAtASteadySpeedWithoutStuttering() {
        float share = 0f, gain = 0.8f, last = 0f;
        for (int i = 0; i < 20_000; i++) {
            float power = HelperBattery.power(share, LOW, FLAT);
            share = HelperBattery.next(share, gain, power, CAPACITY);
            if (i > 15_000) {
                assertTrue(Math.abs(power - last) < 0.001f, "stutter at tick " + i + ": " + last + " to " + power);
            }
            last = power;
        }
        assertEquals(0.8f, last, 0.01f, "it settles where flight costs what the sun gives");
    }
}
