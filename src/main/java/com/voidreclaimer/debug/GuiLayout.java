package com.voidreclaimer.debug;

import java.util.ArrayList;
import java.util.List;

/** Finds clickable things on a screen that sit on top of each other, pure: one of the two can never be clicked. */
public final class GuiLayout {
    /** A clickable rectangle in GUI pixels, with a name to report it by. */
    public record Rect(String name, int x, int y, int width, int height) {
        public boolean overlaps(Rect other) {
            return x < other.x + other.width && other.x < x + width && y < other.y + other.height && other.y < y + height;
        }

        /** Whether a click at this point lands here. */
        public boolean contains(double px, double py) {
            return px >= x && px < x + width && py >= y && py < y + height;
        }
    }

    /** A pair of rectangles that overlap. */
    public record Overlap(Rect first, Rect second) {}

    private GuiLayout() {}

    /** Every overlapping pair; rectangles with no area never overlap anything. */
    public static List<Overlap> overlaps(List<Rect> rects) {
        List<Overlap> found = new ArrayList<>();
        for (int i = 0; i < rects.size(); i++) {
            for (int j = i + 1; j < rects.size(); j++) {
                Rect a = rects.get(i);
                Rect b = rects.get(j);
                if (a.width() > 0 && a.height() > 0 && b.width() > 0 && b.height() > 0 && a.overlaps(b)) {
                    found.add(new Overlap(a, b));
                }
            }
        }
        return found;
    }
}
