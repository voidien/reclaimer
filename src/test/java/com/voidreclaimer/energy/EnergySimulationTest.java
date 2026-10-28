package com.voidreclaimer.energy;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class EnergySimulationTest {
    private static final float EPS = 1.0E-4f;
    /** Config defaults: capacity 100, idle 0.2/s, sun 1.5/s, emergency 0.1/s, reserve 10%, restart 5%. */
    private static final EnergyRules DEFAULTS = new EnergyRules(100f, 0.01f, 0.075f, 0.005f, 0.10f, 0.05f);

    @Test
    void idleDrainAboveReserveInDarkness() {
        EnergyData after = EnergyTicker.simulate(new EnergyData(50f, 100f, false), 0f, DEFAULTS);
        assertEquals(49.99f, after.energy(), EPS);
    }

    @Test
    void sunChargeScalesWithExposure() {
        EnergyData full = EnergyTicker.simulate(new EnergyData(50f, 100f, false), 1f, DEFAULTS);
        EnergyData deepSpace = EnergyTicker.simulate(new EnergyData(50f, 100f, false), 10f / 15f, DEFAULTS);
        assertEquals(50f + 0.075f - 0.01f, full.energy(), EPS);
        assertEquals(50f + 0.05f - 0.01f, deepSpace.energy(), EPS);
    }

    @Test
    void emergencyTrickleBelowReserveNeverStrandsInDarkness() {
        EnergyData data = new EnergyData(10f, 100f, false);
        for (int i = 0; i < 20 * 60 * 10; i++) {
            data = EnergyTicker.simulate(data, 0f, DEFAULTS);
            assertTrue(data.energy() > 9f, "energy must hover at the reserve, not drain to zero");
        }
    }

    @Test
    void lowPowerRecoversOnlyAtRestartThreshold() {
        // Energy.consume() sets lowPower when draining to zero; the simulation keeps it until 5%.
        EnergyData data = EnergyTicker.simulate(new EnergyData(0f, 100f, true), 0f, DEFAULTS);
        assertTrue(data.lowPower());
        assertTrue(data.energy() > 0f);

        int ticks = 0;
        while (data.lowPower() && ticks < 100_000) {
            data = EnergyTicker.simulate(data, 0f, DEFAULTS);
            ticks++;
        }
        assertFalse(data.lowPower());
        assertTrue(data.energy() >= 5f);
        // 5 energy at 0.005/tick of emergency trickle = 1000 ticks (50 s) in total darkness.
        assertEquals(1000, ticks, 2);
    }

    @Test
    void neverExceedsCapacityAndCapacityFollowsRules() {
        EnergyData data = EnergyTicker.simulate(new EnergyData(150f, 150f, false), 1f, DEFAULTS);
        assertEquals(100f, data.energy(), EPS);
        assertEquals(100f, data.capacity(), EPS);
    }

    @Test
    void halfIconsRoundUp() {
        assertEquals(20, new EnergyData(100f, 100f, false).halfIcons());
        assertEquals(1, new EnergyData(0.1f, 100f, false).halfIcons());
        assertEquals(0, new EnergyData(0f, 100f, true).halfIcons());
    }
}
