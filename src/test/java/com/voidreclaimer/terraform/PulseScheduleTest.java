package com.voidreclaimer.terraform;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

class PulseScheduleTest {
    @Test
    void everyRingIsCoveredExactlyOnceInOrderAndTheWaveEndsAtTheRadius() {
        for (int[] cfg : new int[][] {{128, 300}, {20, 40}, {5, 1}, {0, 10}, {128, 20}, {3, 1000}}) {
            PulseSchedule schedule = new PulseSchedule(cfg[0], cfg[1]);
            assertEquals(-1, schedule.reached(0));
            assertEquals(schedule.radius(), schedule.reached(schedule.durationTicks()), "wave ends at the radius " + cfg[0] + "/" + cfg[1]);
            List<Integer> rings = new ArrayList<>();
            for (int tick = 1; tick <= schedule.durationTicks(); tick++) {
                schedule.forNewRings(tick, rings::add);
            }
            assertEquals(schedule.radius() + 1, rings.size(), "ring count " + cfg[0] + "/" + cfg[1]);
            for (int i = 0; i < rings.size(); i++) {
                assertEquals(i, rings.get(i));
            }
        }
    }

    @Test
    void theWaveNeverSkipsAheadOfTime() {
        PulseSchedule schedule = new PulseSchedule(128, 300);
        int previous = -1;
        for (int tick = 0; tick <= 300; tick++) {
            int reached = schedule.reached(tick);
            assertTrue(reached >= previous && reached - previous <= 1, "tick " + tick);
            previous = reached;
        }
        assertFalse(schedule.isDone(299));
        assertTrue(schedule.isDone(300));
    }

    @Test
    void ringsAreSquareShellsThatTileTheArea() {
        Set<Long> seen = new HashSet<>();
        int radius = 12;
        for (int d = 0; d <= radius; d++) {
            final int ring = d;
            int[] count = {0};
            PulseSchedule.forRing(d, (dx, dz) -> {
                assertEquals(ring, Math.max(Math.abs(dx), Math.abs(dz)));
                assertTrue(seen.add(((long) dx << 32) ^ (dz & 0xFFFFFFFFL)), "duplicate " + dx + "," + dz);
                count[0]++;
            });
            assertEquals(d == 0 ? 1 : 8 * d, count[0]);
        }
        assertEquals((2 * radius + 1) * (2 * radius + 1), seen.size());
    }
}
