package com.voidreclaimer.body.assembly;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.voidreclaimer.body.BodyStats;
import com.voidreclaimer.body.BuiltinBodies;
import com.voidreclaimer.flight.FlightStats;
import java.util.List;
import org.junit.jupiter.api.Test;

class StatDeltaTest {
    private static BodyStats stats(float mass, double thrust, float health) {
        return new BodyStats(BuiltinBodies.DRONE_SIZE, 0.4, 0, mass, 2f, 1, new FlightStats(mass, thrust, 0.04, 0.5), 1f, 100f, 1f, health, 0f, 1f, 1, 0f, 0f, 0f);
    }

    @Test
    void aBodyComparedWithItselfHasNoRows() {
        assertTrue(StatDelta.between(stats(1f, 0.03, 12f), stats(1f, 0.03, 12f)).isEmpty());
    }

    @Test
    void moreOfAGoodStatIsBetterAndMoreMassIsNot() {
        List<StatDelta.Row> rows = StatDelta.between(stats(1f, 0.03, 12f), stats(2f, 0.03, 20f));
        StatDelta.Row mass = rows.stream().filter(r -> r.key().equals("mass")).findFirst().orElseThrow();
        assertFalse(mass.better(), "heavier is worse");
        StatDelta.Row health = rows.stream().filter(r -> r.key().equals("health")).findFirst().orElseThrow();
        assertTrue(health.better());
        assertEquals(6.0, health.before(), 1e-6, "health shows in hearts, as on the Body tab");
        assertEquals(10.0, health.after(), 1e-6);
    }

    @Test
    void accelerationFollowsMassAndThrustTogether() {
        List<StatDelta.Row> rows = StatDelta.between(stats(1f, 0.03, 12f), stats(2f, 0.03, 12f));
        assertTrue(rows.stream().anyMatch(r -> r.key().equals("acceleration") && !r.better()), "the same thrust moves more mass slower: " + rows);
    }

    @Test
    void lessOfAGoodStatIsWorse() {
        List<StatDelta.Row> rows = StatDelta.between(stats(1f, 0.03, 20f), stats(1f, 0.03, 12f));
        assertFalse(rows.get(0).better());
    }
}
