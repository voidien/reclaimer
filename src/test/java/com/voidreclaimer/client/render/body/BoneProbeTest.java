package com.voidreclaimer.client.render.body;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.joml.Vector3f;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

class BoneProbeTest {
    @AfterEach
    void stop() {
        BoneProbe.capture(false);
    }

    @Test
    void withoutCapturingNothingIsRecorded() {
        BoneProbe.capture(false);
        BoneProbe.begin();
        assertFalse(BoneProbe.recording(), "drawing a body costs nothing while no one is looking");
        BoneProbe.end();
        assertTrue(BoneProbe.latest().isEmpty());
    }

    @Test
    void aFrameReplacesTheOneBeforeOnlyWhenItEnds() {
        BoneProbe.capture(true);
        BoneProbe.begin();
        BoneProbe.add("head", BoneProbe.Kind.SOCKET, true, new Vector3f(1, 2, 3), "head", null);
        BoneProbe.end();
        assertEquals(1, BoneProbe.latest().size());
        BoneProbe.begin();
        BoneProbe.add("a", BoneProbe.Kind.MOUNT, false, new Vector3f(), "head", null);
        BoneProbe.add("b", BoneProbe.Kind.MOUNT, false, new Vector3f(), "head", null);
        assertEquals("head", BoneProbe.latest().get(0).name(), "a screen mid-frame still sees a whole frame");
        BoneProbe.end();
        assertEquals(2, BoneProbe.latest().size());
        BoneProbe.capture(false);
        assertTrue(BoneProbe.latest().isEmpty(), "turning it off forgets the points");
    }
}
