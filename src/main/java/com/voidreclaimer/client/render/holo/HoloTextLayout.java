package com.voidreclaimer.client.render.holo;

import com.voidreclaimer.display.HoloDisplay;
import java.util.ArrayList;
import java.util.List;
import java.util.function.ToIntFunction;

/**
 * Where a projector's lines go on its panel, in font pixels from the panel's top left corner (pure: the font is passed as a
 * width function). Blank lines keep their place; a panel is as wide as its longest line plus the padding.
 *
 * @param width  panel width
 * @param height panel height
 * @param rows   the lines that have text
 */
public record HoloTextLayout(int width, int height, List<Row> rows) {
    public static final int PADDING = 3;
    public static final int LINE_HEIGHT = 10;

    /** One line of text: where it starts and its colour, 0xRRGGBB. */
    public record Row(String text, int x, int y, int color) {}

    public static HoloTextLayout of(HoloDisplay display, ToIntFunction<String> width) {
        List<Row> rows = new ArrayList<>();
        int widest = 0;
        int y = PADDING;
        for (HoloDisplay.Line line : display.lines()) {
            if (!line.text().isEmpty()) {
                widest = Math.max(widest, width.applyAsInt(line.text()));
                rows.add(new Row(line.text(), PADDING, y, line.color() == HoloDisplay.NO_COLOR ? display.color() : line.color()));
            }
            y += LINE_HEIGHT;
        }
        return new HoloTextLayout(widest + 2 * PADDING, display.lines().size() * LINE_HEIGHT + 2 * PADDING, List.copyOf(rows));
    }
}
