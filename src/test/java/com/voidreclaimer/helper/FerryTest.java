package com.voidreclaimer.helper;

import static com.voidreclaimer.helper.Ferry.Action;
import static com.voidreclaimer.helper.Ferry.Phase;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class FerryTest {
    @Test
    void aLinkedDroneGoesToTheSourceLoadsAndDelivers() {
        assertEquals(Phase.TO_SOURCE, Ferry.after(Phase.IDLE, Action.NONE, true, false, false));
        assertEquals(Action.NONE, Ferry.actionOn(Phase.TO_SOURCE, false), "nothing happens on the way");
        assertEquals(Phase.TO_SOURCE, Ferry.after(Phase.TO_SOURCE, Action.NONE, true, false, false));
        assertEquals(Action.LOAD, Ferry.actionOn(Phase.TO_SOURCE, true));
        assertEquals(Phase.TO_DESTINATION, Ferry.after(Phase.TO_SOURCE, Action.LOAD, true, true, false), "loaded: off to deliver");
        assertEquals(Action.UNLOAD, Ferry.actionOn(Phase.TO_DESTINATION, true));
        assertEquals(Phase.TO_SOURCE, Ferry.after(Phase.TO_DESTINATION, Action.UNLOAD, true, false, false), "delivered: back for more");
    }

    @Test
    void nothingToTakeOrNoRoomMeansWaitingThenTryingAgain() {
        assertEquals(Phase.WAITING, Ferry.after(Phase.TO_SOURCE, Action.LOAD, true, false, false), "empty source");
        assertEquals(Phase.WAITING, Ferry.after(Phase.TO_DESTINATION, Action.UNLOAD, true, true, false), "full destination");
        assertEquals(Phase.WAITING, Ferry.after(Phase.WAITING, Action.NONE, true, true, false));
        assertEquals(Phase.TO_DESTINATION, Ferry.after(Phase.WAITING, Action.NONE, true, true, true), "still carrying: try the destination again");
        assertEquals(Phase.TO_SOURCE, Ferry.after(Phase.WAITING, Action.NONE, true, false, true), "empty-handed: back to the source");
        assertEquals(Action.NONE, Ferry.actionOn(Phase.WAITING, true));
    }

    @Test
    void withoutBothLinksTheDroneIdles() {
        for (Phase phase : Phase.values()) {
            assertEquals(Phase.IDLE, Ferry.after(phase, Action.NONE, false, true, true), "unlinked from " + phase);
        }
        assertEquals(Action.NONE, Ferry.actionOn(Phase.IDLE, true));
        assertEquals(Phase.TO_DESTINATION, Ferry.after(Phase.IDLE, Action.NONE, true, true, false), "linked again while carrying: deliver first");
        assertTrue(Ferry.moving(Phase.TO_SOURCE) && Ferry.moving(Phase.TO_DESTINATION));
        assertFalse(Ferry.moving(Phase.IDLE) || Ferry.moving(Phase.WAITING));
    }
}
