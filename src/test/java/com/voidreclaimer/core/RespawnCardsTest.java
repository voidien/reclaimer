package com.voidreclaimer.core;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class RespawnCardsTest {
    @Test
    void pipsShowChargesOutOfTheMaximum() {
        assertEquals("●●○", RespawnCards.pips(2, 3));
        assertEquals("○○○", RespawnCards.pips(0, 3));
        assertEquals("●●●", RespawnCards.pips(3, 3));
    }

    @Test
    void pipsClampOutOfRangeValues() {
        assertEquals("●●●", RespawnCards.pips(9, 3));
        assertEquals("○○", RespawnCards.pips(-4, 2));
        assertEquals("", RespawnCards.pips(1, -1));
    }

    @Test
    void blocksIsTheRoundedStraightLineDistance() {
        assertEquals(5, RespawnCards.blocks(3, 4, 0));
        assertEquals(0, RespawnCards.blocks(0.2, 0.1, 0.1));
        assertEquals(173, RespawnCards.blocks(100, 100, 100));
    }
}
