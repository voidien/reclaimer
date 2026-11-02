package com.voidreclaimer.body;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class WearEfficiencyTest {
    @Test
    void aSectionWorksInFullDownToHalf() {
        assertEquals(1f, Wear.efficiency(1f));
        assertEquals(1f, Wear.efficiency(Wear.FULL_EFFECT));
    }

    @Test
    void belowHalfItFadesAndNeverMuch() {
        assertTrue(Wear.efficiency(0.25f) < 1f);
        assertTrue(Wear.efficiency(0.01f) >= 1f - Wear.MAX_LOSS);
    }

    @Test
    void wornRightOutItGivesNothingUntilRepaired() {
        assertEquals(0f, Wear.efficiency(0f));
        assertEquals(0f, Wear.efficiency(-1f));
    }
}
