package com.voidreclaimer.client.hud;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class DustMeterFadeTest {
    @Test
    void staysBrightWhileDustChangesAndForTheHoldTime() {
        assertEquals(1f, DustMeterFade.alpha(0, false));
        assertEquals(1f, DustMeterFade.alpha(DustMeterFade.HOLD_TICKS, false));
    }

    @Test
    void fadesToTheMinimumAndNoFurther() {
        float midway = DustMeterFade.alpha(DustMeterFade.HOLD_TICKS + DustMeterFade.FADE_TICKS / 2, false);
        assertTrue(midway < 1f && midway > DustMeterFade.MIN_ALPHA, "halfway through the fade: " + midway);
        assertEquals(DustMeterFade.MIN_ALPHA, DustMeterFade.alpha(DustMeterFade.HOLD_TICKS + DustMeterFade.FADE_TICKS, false), 1e-6f);
        assertEquals(DustMeterFade.MIN_ALPHA, DustMeterFade.alpha(1_000_000, false), 1e-6f);
    }

    @Test
    void stayBrightInACloudOrWithAScreenOpen() {
        assertEquals(1f, DustMeterFade.alpha(1_000_000, true));
    }
}
