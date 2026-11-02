package com.voidreclaimer.body;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class CrashRulesTest {
    private static final double SAFE = 5.0;

    @Test
    void underTheSafeSpeedNothingHappens() {
        assertEquals(0.0, CrashRules.healthShare(5.0, 30.0, SAFE, 1.0));
        assertEquals(-1.0, CrashRules.hardness(5.0, 40.0, SAFE));
        assertEquals(0, CrashRules.blocks(5.0, 40.0, SAFE, 32));
    }

    @Test
    void aHeadOnHitAtTopSpeedCanDestroyABody() {
        assertEquals(1.0, CrashRules.healthShare(30.0, 30.0, SAFE, 1.0), 1e-9, "a Ship");
        assertEquals(1.0, CrashRules.healthShare(10.0, 10.0, SAFE, 1.0), 1e-9, "a Drone");
        assertEquals(0.5, CrashRules.healthShare(10.0, 10.0, SAFE, 0.5), 1e-9, "the config can soften it");
    }

    @Test
    void itGrowsWithTheSquareOfTheSpeedOverTheSafeOne() {
        assertEquals(0.25, CrashRules.healthShare(17.5, 30.0, SAFE, 1.0), 1e-9);
        assertTrue(CrashRules.healthShare(40.0, 30.0, SAFE, 1.0) > 1.0, "past top speed");
    }

    @Test
    void aBodyWithNoSpeedToSpeakOfIsNotMadeOfGlass() {
        assertEquals(0.04, CrashRules.healthShare(6.0, 0.0, SAFE, 1.0), 1e-9);
        assertEquals(0.04, CrashRules.healthShare(6.0, 6.0, SAFE, 1.0), 1e-9);
    }

    @Test
    void weakBlocksGoFirstAndAnythingGoesAtHighEnoughSpeed() {
        assertTrue(CrashRules.hardness(10.0, 15.0, SAFE) < 0.5, "a slow Ship breaks glass at most");
        assertTrue(CrashRules.hardness(30.0, 15.0, SAFE) > 5.0, "a Ship at top speed breaks an iron block");
        assertTrue(CrashRules.hardness(30.0, 15.0, SAFE) < 50.0, "and not obsidian");
        assertTrue(CrashRules.hardness(100.0, 40.0, SAFE) > 50.0, "anything goes in the end");
    }

    @Test
    void weightCounts() {
        assertTrue(CrashRules.hardness(20.0, 40.0, SAFE) > CrashRules.hardness(20.0, 10.0, SAFE));
        assertTrue(CrashRules.blocks(20.0, 40.0, SAFE, 100) > CrashRules.blocks(20.0, 10.0, SAFE, 100));
    }

    @Test
    void onlySpeedLostAlongTheWayCounts() {
        assertEquals(18.0, CrashRules.speedLost(0, 0, -18, 0, 0, 0), 1e-9, "stopped dead");
        assertEquals(24.0, CrashRules.speedLost(0, 0, -18, 0, 0, 6), 1e-9, "bounced");
        assertEquals(0.0, CrashRules.speedLost(0, 0, -18, 0, 0, -30), 1e-9, "sped up");
        assertEquals(18.0, CrashRules.speedLost(0, 0, -18, 18, 0, 0), 1e-9, "turned square");
        assertEquals(0.0, CrashRules.speedLost(0, 0, 0, 5, 5, 5), 1e-9, "from rest");
    }

    @Test
    void aHitNeverBreaksMoreThanTheLimit() {
        assertEquals(32, CrashRules.blocks(500.0, 40.0, SAFE, 32));
    }
}
