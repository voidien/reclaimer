package com.voidreclaimer.client.camera;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class OrbitStateTest {
    @Test
    void mouseMovementOrbitsOnlyWhileHeld() {
        OrbitState state = new OrbitState();
        state.tick(false, 100, 100);
        assertFalse(state.offset(), "movement without the key does nothing");
        state.tick(true, 100, 40);
        assertEquals(100 * OrbitState.SENSITIVITY, state.yaw(1f), 1e-4f);
        assertEquals(40 * OrbitState.SENSITIVITY, state.pitch(1f), 1e-4f);
        assertTrue(state.offset());
    }

    @Test
    void pitchIsClampedAndYawWraps() {
        OrbitState state = new OrbitState();
        state.tick(true, 0, 1_000_000);
        assertEquals(OrbitState.MAX_PITCH, state.pitch(1f), 1e-4f);
        state.tick(true, 0, -2_000_000);
        assertEquals(-OrbitState.MAX_PITCH, state.pitch(1f), 1e-4f);
        OrbitState turned = new OrbitState();
        turned.tick(true, 400 / OrbitState.SENSITIVITY, 0);
        assertEquals(40f, turned.yaw(1f), 1e-2f, "400 degrees is 40");
    }

    @Test
    void theCameraEasesBackBehindTheBodyAfterRelease() {
        OrbitState state = new OrbitState();
        state.tick(true, 600, 200);
        for (int i = 0; i < 60; i++) {
            state.tick(false, 0, 0);
        }
        assertFalse(state.offset());
        assertEquals(0f, state.yaw(1f));
    }

    @Test
    void zoomIsClampedAndScrollingInShortensTheDistance() {
        OrbitState state = new OrbitState();
        state.scroll(1);
        assertTrue(state.zoom() < 1f, "scrolling up zooms in");
        state.scroll(-1);
        assertEquals(1f, state.zoom(), 1e-4f);
        state.scroll(1000);
        assertEquals(OrbitState.MIN_ZOOM, state.zoom(), 1e-4f);
        state.scroll(-1000);
        assertEquals(OrbitState.MAX_ZOOM, state.zoom(), 1e-4f);
        state.reset();
        assertEquals(1f, state.zoom());
    }

    @Test
    void holdingTheCameraPastHalfWayRoundDoesNotSpinTheBlend() {
        OrbitState state = new OrbitState();
        state.tick(true, 179 / OrbitState.SENSITIVITY, 0);
        state.tick(true, 2 / OrbitState.SENSITIVITY, 0);
        assertEquals(-179f, state.yaw(1f), 1e-2f, "the yaw wraps");
        assertEquals(180f, Math.abs(state.yaw(0.5f)), 1e-2f, "half a tick in, the blend is between the two sides, not across the circle");
    }

    @Test
    void theReturnTakesTheShortWayRound() {
        OrbitState state = new OrbitState();
        state.tick(true, 170 / OrbitState.SENSITIVITY, 0);
        state.tick(true, 20 / OrbitState.SENSITIVITY, 0); // 190 degrees wraps to -170
        state.tick(false, 0, 0);
        float blended = state.yaw(0.5f);
        assertTrue(Math.abs(blended) > 100f, "blending between ticks must not swing through zero: " + blended);
    }
}
