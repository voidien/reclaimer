package com.voidreclaimer.machine;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class CloudSiphonEnergyTest {
    private static final double EPS = 1.0E-5;

    @Test
    void energyAloneRunsItAtFullPowerAndCostsTheConfiguredDraw() {
        CloudSiphonMath.EnergyUse use = CloudSiphonMath.topUp(0f, 1000, 40);
        assertEquals(1f, use.power(), EPS);
        assertEquals(40, use.drained());
    }

    @Test
    void aShaftThatAlreadyGivesFullPowerUsesNoEnergy() {
        assertEquals(0, CloudSiphonMath.topUp(1f, 1000, 40).drained());
        assertEquals(0, CloudSiphonMath.topUp(1.7f, 1000, 40).drained());
        assertEquals(1.7f, CloudSiphonMath.topUp(1.7f, 1000, 40).power(), EPS, "a fast shaft keeps its own power");
    }

    @Test
    void energyOnlyFillsTheGapLeftByASlowShaft() {
        CloudSiphonMath.EnergyUse use = CloudSiphonMath.topUp(0.75f, 1000, 40);
        assertEquals(1f, use.power(), EPS);
        assertEquals(10, use.drained(), "a quarter of the full draw");
    }

    @Test
    void aShortBufferGivesProportionalPower() {
        CloudSiphonMath.EnergyUse use = CloudSiphonMath.topUp(0f, 10, 40);
        assertEquals(0.25f, use.power(), EPS);
        assertEquals(10, use.drained());
        assertEquals(0f, CloudSiphonMath.topUp(0f, 0, 40).power(), EPS);
        assertEquals(0, CloudSiphonMath.topUp(0f, 0, 40).drained());
    }

    @Test
    void zeroDrawTurnsTheEnergyInputOff() {
        CloudSiphonMath.EnergyUse use = CloudSiphonMath.topUp(0.5f, 1000, 0);
        assertEquals(0.5f, use.power(), EPS);
        assertEquals(0, use.drained());
        assertTrue(CloudSiphonMath.topUp(-1f, 1000, 0).power() == 0f, "negative power counts as zero");
    }
}
