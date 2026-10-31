package com.voidreclaimer.helper;

import static com.voidreclaimer.helper.Mining.Action;
import static com.voidreclaimer.helper.Mining.Phase;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class MiningTest {
    @Test
    void aMinerFliesToTheRockDigsItAndDelivers() {
        assertEquals(Phase.TO_ROCK, Mining.after(Phase.IDLE, Action.NONE, true, true, false, false, false), "a rock in reach: off to it");
        assertEquals(Phase.TO_ROCK, Mining.after(Phase.TO_ROCK, Action.NONE, true, true, false, false, false), "still flying");
        assertEquals(Phase.DIGGING, Mining.after(Phase.TO_ROCK, Action.NONE, true, true, true, false, false), "beside it: dig");
        assertEquals(Action.DIG, Mining.actionOn(Phase.DIGGING, true, false));
        assertEquals(Phase.DIGGING, Mining.after(Phase.DIGGING, Action.DIG, true, true, true, false, false));
        assertEquals(Action.BREAK, Mining.actionOn(Phase.DIGGING, true, true), "the dig is done: break the rock");
        assertEquals(Phase.TO_DESTINATION, Mining.after(Phase.DIGGING, Action.BREAK, true, true, true, true, false), "carrying the rock: deliver");
        assertEquals(Action.UNLOAD, Mining.actionOn(Phase.TO_DESTINATION, true, false));
        assertEquals(Phase.TO_ROCK, Mining.after(Phase.TO_DESTINATION, Action.UNLOAD, true, true, true, false, false), "delivered, another rock waits");
        assertEquals(Phase.WAITING, Mining.after(Phase.TO_DESTINATION, Action.UNLOAD, true, false, true, false, false), "delivered, no rock left");
    }

    @Test
    void noRockOrNoRoomMeansWaitingThenLookingAgain() {
        assertEquals(Phase.WAITING, Mining.after(Phase.IDLE, Action.NONE, true, false, false, false, false), "no rock in reach");
        assertEquals(Phase.WAITING, Mining.after(Phase.TO_ROCK, Action.NONE, true, false, false, false, false), "the rock vanished on the way");
        assertEquals(Phase.WAITING, Mining.after(Phase.DIGGING, Action.DIG, true, false, true, false, false), "the rock vanished under the drill");
        assertEquals(Phase.WAITING, Mining.after(Phase.TO_DESTINATION, Action.UNLOAD, true, true, true, true, false), "no room: still carrying");
        assertEquals(Phase.WAITING, Mining.after(Phase.WAITING, Action.NONE, true, true, false, false, false), "the wait is not over");
        assertEquals(Phase.TO_DESTINATION, Mining.after(Phase.WAITING, Action.NONE, true, true, false, true, true), "wait over, carrying: deliver first");
        assertEquals(Phase.TO_ROCK, Mining.after(Phase.WAITING, Action.NONE, true, true, false, false, true), "wait over, a rock: go");
        assertEquals(Phase.WAITING, Mining.after(Phase.WAITING, Action.NONE, true, false, false, false, true), "wait over, still no rock: wait more");
        assertEquals(Action.NONE, Mining.actionOn(Phase.WAITING, true, true));
    }

    @Test
    void withoutLinksTheMinerIdles() {
        for (Phase phase : Phase.values()) {
            assertEquals(Phase.IDLE, Mining.after(phase, Action.NONE, false, true, true, true, true), "unlinked from " + phase);
        }
        assertEquals(Phase.TO_DESTINATION, Mining.after(Phase.IDLE, Action.NONE, true, false, false, true, false), "linked again while carrying: deliver first");
        assertTrue(Mining.moving(Phase.TO_ROCK) && Mining.moving(Phase.TO_DESTINATION));
        assertFalse(Mining.moving(Phase.DIGGING) || Mining.moving(Phase.WAITING) || Mining.moving(Phase.IDLE));
    }
}
