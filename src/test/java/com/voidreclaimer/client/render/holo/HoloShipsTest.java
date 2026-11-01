package com.voidreclaimer.client.render.holo;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class HoloShipsTest {
    @Test
    void aShipNeverLookedAtIsDueAtOnce() {
        assertTrue(HoloShips.due(HoloShips.NEVER, 0));
        assertTrue(HoloShips.due(HoloShips.NEVER, 1_000_000L));
    }

    @Test
    void aShipLookedAtWaitsItsTurn() {
        assertFalse(HoloShips.due(100, 139));
        assertTrue(HoloShips.due(100, 140));
    }
}
