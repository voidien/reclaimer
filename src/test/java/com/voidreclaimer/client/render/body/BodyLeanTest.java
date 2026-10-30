package com.voidreclaimer.client.render.body;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class BodyLeanTest {
    private static final BodyRigSpec.Lean LEAN = new BodyRigSpec.Lean(20f, 15f, 0.4f);

    @Test
    void flyingForwardLeansForwardBackwardTipsBackAndStrafingBanks() {
        assertEquals(10f, BodyLean.target(0.2, 0.0, LEAN).pitch(), 1e-4f);
        assertEquals(-10f, BodyLean.target(-0.2, 0.0, LEAN).pitch(), 1e-4f);
        assertEquals(7.5f, BodyLean.target(0.0, 0.2, LEAN).roll(), 1e-4f);
        assertEquals(-7.5f, BodyLean.target(0.0, -0.2, LEAN).roll(), 1e-4f);
    }

    @Test
    void leanStopsAtTheConfiguredAngleAndIsOffByDefault() {
        assertEquals(20f, BodyLean.target(5.0, 0.0, LEAN).pitch(), 1e-4f);
        assertEquals(-15f, BodyLean.target(0.0, -5.0, LEAN).roll(), 1e-4f);
        assertEquals(BodyLean.Angles.ZERO, BodyLean.target(1.0, 1.0, BodyRigSpec.Lean.NONE));
        assertTrue(BodyRigSpec.Lean.NONE.isNone());
    }

    @Test
    void anglesEaseTowardTheTargetAndBackToZero() {
        BodyLean.Angles angles = BodyLean.Angles.ZERO;
        BodyLean.Angles target = BodyLean.target(0.4, 0.0, LEAN);
        BodyLean.Angles first = BodyLean.ease(angles, target);
        assertTrue(first.pitch() > 0f && first.pitch() < 20f, "one tick moves only part of the way");
        for (int i = 0; i < 40; i++) {
            angles = BodyLean.ease(angles, target);
        }
        assertEquals(20f, angles.pitch(), 0.05f);
        for (int i = 0; i < 40; i++) {
            angles = BodyLean.ease(angles, BodyLean.Angles.ZERO);
        }
        assertEquals(0f, angles.pitch(), 0.05f);
    }

    @Test
    void stateAdvancesOncePerTickAndBlendsBetweenTicks() {
        BodyLean.State state = new BodyLean.State();
        state.update(1, 100, 0.4, 0.0, LEAN);
        state.update(1, 100, 0.4, 0.0, LEAN);
        float once = state.at(1, 1f).pitch();
        assertEquals(20f * BodyLean.EASE, once, 1e-4f, "a repeated call in the same tick must not advance again");
        assertEquals(once / 2f, state.at(1, 0.5f).pitch(), 1e-4f);
        assertEquals(BodyLean.Angles.ZERO, state.at(2, 1f), "unknown players don't lean");
    }
}
