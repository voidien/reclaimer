package com.voidreclaimer.client.hud;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class HealthBarMathTest {
    @Test
    void biggerBodiesGetLongerTallerBarsUpToTheHotbarWidth() {
        assertEquals(HealthBarMath.MIN_WIDTH, HealthBarMath.width(20f));
        assertEquals(HealthBarMath.MIN_HEIGHT, HealthBarMath.height(20f));
        assertTrue(HealthBarMath.width(140f) > HealthBarMath.width(60f));
        assertTrue(HealthBarMath.width(400f) > HealthBarMath.width(140f));
        assertEquals(HealthBarMath.MAX_WIDTH, HealthBarMath.width(1000f));
        assertEquals(HealthBarMath.MAX_WIDTH, HealthBarMath.width(50_000f), "capped at the hotbar width");
        assertEquals(HealthBarMath.MAX_HEIGHT, HealthBarMath.height(50_000f));
    }

    @Test
    void oneNotchPerHundredHealthWhenThereIsRoom() {
        assertTrue(HealthBarMath.notches(20f, 60).isEmpty(), "a single segment has no notch");
        assertEquals(3, HealthBarMath.notches(400f, 150).size());
        assertEquals(java.util.List.of(50, 100), HealthBarMath.notches(300f, 150));
        assertTrue(HealthBarMath.notches(100_000f, 182).isEmpty(), "too many segments are left out, not drawn as a comb");
    }

    @Test
    void fillIsProportionalAndNeverHidesRemainingHealth() {
        assertEquals(0, HealthBarMath.fill(0f, 100f, 100));
        assertEquals(1, HealthBarMath.fill(0.1f, 1000f, 60));
        assertEquals(50, HealthBarMath.fill(50f, 100f, 100));
        assertEquals(100, HealthBarMath.fill(500f, 100f, 100), "overfull health clamps");
    }
}
