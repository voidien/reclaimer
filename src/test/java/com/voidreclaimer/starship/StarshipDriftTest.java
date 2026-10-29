package com.voidreclaimer.starship;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.Test;

class StarshipDriftTest {
    @Test
    void driftStaysCloseToTheAnchor() {
        for (long t = 0; t < 5000; t += 7) {
            Vec3 o = StarshipDrift.offset(t, 0.3);
            assertTrue(Math.hypot(o.x, o.z) <= StarshipDrift.RADIUS + 1e-9, "left the circle at " + t);
            assertTrue(Math.abs(o.y) <= StarshipDrift.BOB + 1e-9, "bobbed too far at " + t);
        }
    }

    @Test
    void driftMovesSlowlyAndSmoothly() {
        double longest = 0;
        for (long t = 0; t < 2400; t++) {
            longest = Math.max(longest, StarshipDrift.offset(t + 1, 0.7).distanceTo(StarshipDrift.offset(t, 0.7)));
        }
        assertTrue(longest > 0.01, "the ship does not drift");
        assertTrue(longest < 0.1, "the ship jumps " + longest + " blocks in a tick");
        assertEquals(0, StarshipDrift.offset(0, 0.25).distanceTo(StarshipDrift.offset(StarshipDrift.CIRCLE_TICKS * StarshipDrift.BOB_TICKS, 0.25)), 1e-6,
                "the drift is periodic");
    }

    @Test
    void yawTurnsSteadilyAndWraps() {
        assertEquals(StarshipDrift.TURN_PER_TICK, StarshipDrift.yaw(1, 0) - StarshipDrift.yaw(0, 0), 1e-4);
        assertEquals(90f, StarshipDrift.yaw(0, 0.25), 1e-4);
        for (long t = 0; t < 10_000; t += 13) {
            float yaw = StarshipDrift.yaw(t, 0.9);
            assertTrue(yaw >= 0 && yaw < 360, "yaw " + yaw + " at " + t);
        }
    }

    @Test
    void phasesKeepShipsApart() {
        assertTrue(StarshipDrift.offset(100, 0.0).distanceTo(StarshipDrift.offset(100, 0.5)) > 1, "two phases drift in step");
    }
}
