package com.voidreclaimer.body.assembly;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class AssemblyCameraTest {
    @Test
    void draggingTurnsTheViewAndPitchStopsAtTheLimit() {
        AssemblyCamera camera = new AssemblyCamera(0f, 0f);
        camera.drag(100, 0);
        assertEquals(100 * AssemblyCamera.SENSITIVITY, camera.yaw(), 1e-3);
        camera.drag(0, -10);
        assertEquals(10 * AssemblyCamera.SENSITIVITY, camera.pitch(), 1e-3, "dragging up tips the view up");
        camera.drag(0, -100_000);
        assertEquals(AssemblyCamera.PITCH_LIMIT, camera.pitch(), 1e-3);
        camera.drag(0, 100_000);
        assertEquals(-AssemblyCamera.PITCH_LIMIT, camera.pitch(), 1e-3);
    }

    @Test
    void scrollingZoomsWithinItsLimits() {
        AssemblyCamera camera = new AssemblyCamera(0f, 0f);
        camera.scroll(1);
        assertEquals(AssemblyCamera.ZOOM_STEP, camera.zoom(), 1e-4);
        camera.scroll(-1);
        assertEquals(1f, camera.zoom(), 1e-4, "a notch back undoes a notch in");
        camera.scroll(1000);
        assertEquals(AssemblyCamera.MAX_ZOOM, camera.zoom(), 1e-4);
        camera.scroll(-1000);
        assertEquals(AssemblyCamera.MIN_ZOOM, camera.zoom(), 1e-4);
    }

    @Test
    void theFocusEasesToItsTargetWhateverTheFrameRate() {
        AssemblyCamera slow = new AssemblyCamera(0f, 0f);
        AssemblyCamera fast = new AssemblyCamera(0f, 0f);
        slow.focusOn(10f, -4f, 2f);
        fast.focusOn(10f, -4f, 2f);
        slow.advance(0.1f);
        for (int i = 0; i < 10; i++) {
            fast.advance(0.01f);
        }
        assertEquals(slow.focusX(), fast.focusX(), 1e-3, "ten short frames equal one long one");
        assertTrue(slow.focusX() > 0f && slow.focusX() < 10f, "part of the way after 0.1 s: " + slow.focusX());
        slow.advance(5f);
        assertEquals(10f, slow.focusX(), 1e-2);
        assertEquals(-4f, slow.focusY(), 1e-2);
        slow.focusCentre();
        slow.advance(5f);
        assertEquals(0f, slow.focusX(), 1e-2, "back to the body's centre");
    }

    @Test
    void theGridKeepsItsLinesApartAtAnyZoom() {
        assertEquals(1, AssemblyCamera.gridStep(40, 12), "close up: a line a block");
        assertEquals(2, AssemblyCamera.gridStep(8, 12));
        assertEquals(16, AssemblyCamera.gridStep(1, 12), "a Titan fills the view: wide lines");
        assertTrue(AssemblyCamera.gridStep(1.0E-4, 12) <= 1024, "never loops forever on a tiny scale");
    }

    @Test
    void explodingEasesOutAndBackAndSettlesExactly() {
        AssemblyCamera camera = new AssemblyCamera(0f, 0f);
        assertEquals(0f, camera.explode());
        camera.toggleExploded();
        assertTrue(camera.exploded());
        camera.advance(0.1f);
        assertTrue(camera.explode() > 0f && camera.explode() < 1f, "on its way: " + camera.explode());
        camera.advance(5f);
        assertEquals(1f, camera.explode(), "settled exactly, so the body can hold still");
        camera.toggleExploded();
        camera.advance(5f);
        assertEquals(0f, camera.explode());
    }

    @Test
    void aSectionSpreadsAlongTheLineFromItsAnchor() {
        float[] offset = AssemblyCamera.spread(new float[] {10f, 4f, 0f}, new float[] {2f, 4f, 0f}, 0.5f);
        assertEquals(4f, offset[0], 1e-5, "half way along");
        assertEquals(0f, offset[1], 1e-5, "level with its anchor, so it does not rise");
        assertEquals(0f, offset[2], 1e-5);
    }
}
