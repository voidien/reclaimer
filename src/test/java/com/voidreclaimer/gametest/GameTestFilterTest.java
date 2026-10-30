package com.voidreclaimer.gametest;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GameTestFilterTest {
    @Test
    void matchesClassOrMethodIgnoringCase() {
        assertTrue(GameTestFilter.matches("zerogmining", "ZeroGMiningGameTests", "handMining"));
        assertTrue(GameTestFilter.matches("HANDMINING", "ZeroGMiningGameTests", "handMining"));
        assertFalse(GameTestFilter.matches("flight", "ZeroGMiningGameTests", "handMining"));
    }

    @Test
    void commaSeparatedTermsAreAlternatives() {
        assertTrue(GameTestFilter.matches("flight, zerog", "ZeroGMiningGameTests", "handMining"));
        assertFalse(GameTestFilter.matches(" , ", "ZeroGMiningGameTests", "handMining"));
    }
}
