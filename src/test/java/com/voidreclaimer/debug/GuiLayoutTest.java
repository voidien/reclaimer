package com.voidreclaimer.debug;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;

class GuiLayoutTest {
    private static GuiLayout.Rect rect(String name, int x, int y, int w, int h) {
        return new GuiLayout.Rect(name, x, y, w, h);
    }

    @Test
    void theTwoButtonsThatHidEachOtherAreCaught() {
        // The real case: the assembly view's button placed over the Replicator's Inventory button.
        GuiLayout.Rect inventory = rect("Inventory", 313, 4, 56, 14);
        GuiLayout.Rect assembly = rect("3D view", 307, 5, 60, 13);
        List<GuiLayout.Overlap> found = GuiLayout.overlaps(List.of(inventory, assembly));
        assertEquals(1, found.size());
        assertEquals(inventory, found.get(0).first());
    }

    @Test
    void neighboursThatOnlyTouchDoNotOverlap() {
        assertTrue(GuiLayout.overlaps(List.of(rect("a", 0, 0, 10, 10), rect("b", 10, 0, 10, 10), rect("c", 0, 10, 10, 10))).isEmpty());
    }

    @Test
    void anEmptyRectangleOverlapsNothing() {
        assertTrue(GuiLayout.overlaps(List.of(rect("a", 0, 0, 10, 10), rect("empty", 5, 5, 0, 0))).isEmpty());
    }

    @Test
    void aClickLandsInsideTheTopAndLeftEdgesOnly() {
        GuiLayout.Rect a = rect("a", 10, 20, 5, 5);
        assertTrue(a.contains(10, 20));
        assertTrue(!a.contains(15, 20), "the right edge is the next pixel over");
        assertTrue(!a.contains(10, 25));
    }
}
