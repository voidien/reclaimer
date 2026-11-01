package com.voidreclaimer.debug;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.Test;

class GateTimerTest {
    private static final List<GateTimer.Gate> COURSE = List.of(new GateTimer.Gate(0, 0, 10, 4), new GateTimer.Gate(0, 0, 20, 4), new GateTimer.Gate(0, 0, 30, 4));

    /** Flies straight down the line at x, one block a tick from z 9, and returns the last event. */
    private static GateTimer.Event fly(GateTimer timer, double x, double fromZ, double toZ) {
        GateTimer.Event last = null;
        long tick = (long) fromZ;
        for (double z = fromZ; z <= toZ; z++) {
            GateTimer.Event event = timer.update(tick++, new Vec3(x, 0, z));
            last = event != null ? event : last;
        }
        return last;
    }

    @Test
    void theFirstGateStartsTheClockAndTheLastStopsIt() {
        GateTimer timer = new GateTimer(COURSE);
        assertEquals(GateTimer.Kind.STARTED, fly(timer, 0, 8, 12).kind());
        assertTrue(timer.running());
        GateTimer.Event finish = fly(timer, 0, 13, 31);
        assertEquals(GateTimer.Kind.FINISHED, finish.kind());
        assertEquals(1.0, finish.seconds(), 1e-9, "20 blocks at 20 blocks a second");
        assertEquals(20.0, finish.speed(), 1e-9);
        assertEquals(1.0, finish.best(), 1e-9);
        assertFalse(timer.running());
    }

    @Test
    void aSplitIsCountedAtTheCrossingNotTheTick() {
        GateTimer timer = new GateTimer(COURSE);
        timer.update(0, new Vec3(0, 0, 9.5));
        timer.update(1, new Vec3(0, 0, 10.5));
        timer.update(2, new Vec3(0, 0, 19.75));
        GateTimer.Event gate = timer.update(3, new Vec3(0, 0, 20.75));
        assertEquals(1.75 / 20.0, gate.seconds(), 1e-9, "from half way through tick 1 to a quarter of the way through tick 3");
    }

    @Test
    void oneUnevenTickDoesNotDoubleTheSpeed() {
        GateTimer timer = new GateTimer(List.of(new GateTimer.Gate(0, 0, 10, 4)));
        double[] places = {4, 5, 6, 7, 8, 9, 11};
        GateTimer.Event event = null;
        for (int i = 0; i < places.length; i++) {
            event = timer.update(i, new Vec3(0, 0, places[i]));
        }
        assertEquals(24.0, event.speed(), 1e-9, "6 blocks over 5 ticks, though the last tick moved 2");
    }

    @Test
    void goingRoundTheGateEndsTheRun() {
        GateTimer timer = new GateTimer(COURSE);
        fly(timer, 0, 8, 12);
        GateTimer.Event missed = fly(timer, 9, 13, 21);
        assertEquals(GateTimer.Kind.MISSED, missed.kind());
        assertEquals(2, missed.gate());
        assertFalse(timer.running());
    }

    @Test
    void theFirstGateRestartsARunAtAnyTime() {
        GateTimer timer = new GateTimer(COURSE);
        fly(timer, 0, 8, 22);
        assertEquals(GateTimer.Kind.STARTED, fly(timer, 0, 5, 11).kind());
    }

    @Test
    void flyingTheWrongWayOrPastTheStartTimesNothing() {
        GateTimer timer = new GateTimer(COURSE);
        assertNull(timer.update(0, new Vec3(0, 0, 40)));
        assertNull(fly(timer, 0, 41, 45));
        assertNull(timer.update(50, new Vec3(0, 0, 25)), "backwards through gate 3");
        assertNull(fly(timer, 9, 8, 12), "outside the first gate");
        assertFalse(timer.running());
    }
}
