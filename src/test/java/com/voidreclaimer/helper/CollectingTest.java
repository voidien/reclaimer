package com.voidreclaimer.helper;

import static com.voidreclaimer.helper.Collecting.Action;
import static com.voidreclaimer.helper.Collecting.Phase;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class CollectingTest {
    @Test
    void aCollectorGathersUntilFullThenBringsTheDustToItsOwner() {
        assertEquals(Phase.TO_POINT, Collecting.after(Phase.IDLE, Action.NONE, true, false, false, true, false, false));
        assertEquals(Phase.TO_POINT, Collecting.after(Phase.TO_POINT, Action.NONE, true, false, false, true, false, false), "still flying");
        assertEquals(Phase.GATHERING, Collecting.after(Phase.TO_POINT, Action.NONE, true, false, false, true, true, false), "at the point: gather");
        assertEquals(Action.GATHER, Collecting.actionOn(Phase.GATHERING, false));
        assertEquals(Phase.GATHERING, Collecting.after(Phase.GATHERING, Action.GATHER, true, false, true, true, true, false), "not full yet");
        assertEquals(Phase.TO_OWNER, Collecting.after(Phase.GATHERING, Action.GATHER, true, true, true, true, true, false), "full: off to the owner");
        assertEquals(Action.NONE, Collecting.actionOn(Phase.TO_OWNER, false));
        assertEquals(Action.DELIVER, Collecting.actionOn(Phase.TO_OWNER, true));
        assertEquals(Phase.TO_POINT, Collecting.after(Phase.TO_OWNER, Action.DELIVER, true, false, false, true, true, false), "handed over: back to the point");
    }

    @Test
    void anOwnerOutOfReachOrFullMeansWaitingThenTryingAgain() {
        assertEquals(Phase.WAITING, Collecting.after(Phase.GATHERING, Action.GATHER, true, true, true, false, true, false), "full, owner away");
        assertEquals(Phase.WAITING, Collecting.after(Phase.TO_OWNER, Action.NONE, true, true, true, false, false, false), "the owner left on the way");
        assertEquals(Phase.WAITING, Collecting.after(Phase.TO_OWNER, Action.DELIVER, true, true, true, true, true, false), "the owner could not take it all");
        assertEquals(Phase.WAITING, Collecting.after(Phase.WAITING, Action.NONE, true, true, true, true, false, false), "the wait is not over");
        assertEquals(Phase.TO_OWNER, Collecting.after(Phase.WAITING, Action.NONE, true, true, true, true, false, true), "wait over, owner near: deliver");
        assertEquals(Phase.WAITING, Collecting.after(Phase.WAITING, Action.NONE, true, true, true, false, false, true), "wait over, owner still away");
        assertEquals(Phase.TO_POINT, Collecting.after(Phase.WAITING, Action.NONE, true, false, true, false, false, true), "room left: gather more");
    }

    @Test
    void withoutACollectPointTheDroneIdles() {
        for (Phase phase : Phase.values()) {
            assertEquals(Phase.IDLE, Collecting.after(phase, Action.NONE, false, true, true, true, true, true), "unlinked from " + phase);
        }
        assertEquals(Phase.TO_OWNER, Collecting.after(Phase.IDLE, Action.NONE, true, true, true, true, false, false), "linked again while full: deliver first");
        assertEquals(Action.NONE, Collecting.actionOn(Phase.IDLE, true));
        assertTrue(Collecting.moving(Phase.TO_POINT) && Collecting.moving(Phase.TO_OWNER));
        assertFalse(Collecting.moving(Phase.GATHERING) || Collecting.moving(Phase.WAITING) || Collecting.moving(Phase.IDLE));
    }
}
