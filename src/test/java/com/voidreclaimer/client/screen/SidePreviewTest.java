package com.voidreclaimer.client.screen;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class SidePreviewTest {
    /** The Replicator panel is 320 wide: this is the margin beside it on a screen of {@code width} GUI pixels. */
    private static int margin(int width) {
        return (width - 320) / 2;
    }

    @Test
    void thereIsRoomOnlyWhenTheScreenLeavesIt() {
        assertFalse(SidePreview.fits(margin(427)), "1280 x 720 at GUI scale 3 has no room");
        assertTrue(SidePreview.fits(margin(640)), "1280 x 720 at GUI scale 2 has room");
        assertTrue(SidePreview.fits(margin(960)), "1920 x 1080 at GUI scale 2 has room");
    }

    @Test
    void onlyAPressOnTheBoxTurnsIt() {
        SidePreview preview = new SidePreview();
        assertTrue(preview.press(160, 60, 300, 100, 100), "on the box");
        assertTrue(preview.drag(5, 5), "a drag from the box turns it");
        preview.release();
        assertFalse(preview.drag(5, 5), "a drag after letting go");
        assertFalse(preview.press(160, 60, 300, 200, 100), "on the panel");
        assertFalse(preview.drag(5, 5), "a drag from the panel");
        assertFalse(preview.press(53, 60, 300, 20, 100), "with no room there is no box");
    }

    @Test
    void theBoxStaysOnScreenAndLeavesAWideMarginAlone() {
        assertEquals(4, SidePreview.leftEdge(SidePreview.MIN_MARGIN), "a narrow margin is used from its edge");
        assertEquals(236, SidePreview.leftEdge(400), "a wide margin keeps the box beside the panel");
    }
}
