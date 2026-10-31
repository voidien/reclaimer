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

    @Test
    void wearTintsFromWhiteThroughAmberToRed() {
        assertEquals(0xFFFFFFFF, com.voidreclaimer.body.Wear.tint(1f), "a new section is drawn as it is");
        int half = com.voidreclaimer.body.Wear.tint(com.voidreclaimer.body.Wear.FULL_EFFECT);
        assertTrue((half >> 8 & 0xFF) > (half & 0xFF), "amber: more green than blue");
        int spent = com.voidreclaimer.body.Wear.tint(0f);
        assertEquals(0xFFFF0000, spent, "spent is pure red");
    }

    @Test
    void theTextShowsEvenASmallChange() {
        assertEquals("10 > 14", StatDelta.text(new StatDelta.Row("a", 10.0, 14.0, true)));
        assertEquals("5.0 > 5.2", StatDelta.text(new StatDelta.Row("a", 5.0, 5.2, false)));
        assertEquals("5.00 > 5.05", StatDelta.text(new StatDelta.Row("a", 5.0, 5.05, false)));
        assertEquals("0.030 > 0.029", StatDelta.text(new StatDelta.Row("a", 0.03, 0.029, false)));
    }
}
