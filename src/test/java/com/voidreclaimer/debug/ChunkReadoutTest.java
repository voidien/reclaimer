package com.voidreclaimer.debug;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class ChunkReadoutTest {
    @Test
    void theGamesDebugTextComesOutPlain() {
        assertEquals("31 St: 12minecraft:full Ch: 12minecraft:full 3ENTITY_TICKING",
                ChunkReadout.plain("31\nSt: §1"  + "12minecraft:full§r\nCh: §112minecraft:full§r\n§33ENTITY_TICKING§r"));
    }
}
