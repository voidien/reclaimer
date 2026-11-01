package com.voidreclaimer.display;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import javax.annotation.Nullable;
import net.minecraft.util.StringUtil;
import net.minecraft.world.item.DyeColor;

/** Pure text rules of a Holo Projector: cleaning, cutting, wrapping and colour names. */
public final class HoloText {
    private HoloText() {}

    /** One line as a projector stores it: no control characters, {@link HoloDisplay#MAX_CHARS} at most. */
    public static String clean(String text) {
        String clean = StringUtil.filterText(text);
        return clean.length() > HoloDisplay.MAX_CHARS ? clean.substring(0, HoloDisplay.MAX_CHARS) : clean;
    }

    /**
     * Breaks text into lines: at newlines, then at spaces so that no line is over {@link HoloDisplay#MAX_CHARS}, a word
     * longer than a line being cut. Stops at {@link HoloDisplay#MAX_LINES} lines.
     */
    public static List<String> wrap(String text) {
        List<String> lines = new ArrayList<>();
        for (String paragraph : text.split("\n", -1)) {
            String rest = StringUtil.filterText(paragraph).stripTrailing();
            while (rest.length() > HoloDisplay.MAX_CHARS) {
                int space = rest.lastIndexOf(' ', HoloDisplay.MAX_CHARS);
                int cut = space > 0 ? space : HoloDisplay.MAX_CHARS;
                lines.add(rest.substring(0, cut).stripTrailing());
                rest = rest.substring(cut).stripLeading();
            }
            lines.add(rest);
        }
        return lines.size() > HoloDisplay.MAX_LINES ? List.copyOf(lines.subList(0, HoloDisplay.MAX_LINES)) : List.copyOf(lines);
    }

    /**
     * A colour from a program: a number 0xRRGGBB, a hex string ({@code #7FDFFF}, {@code 0x7FDFFF} or {@code 7FDFFF}) or a
     * dye name ({@code cyan}). Null when it is none of these.
     */
    @Nullable
    public static Integer color(@Nullable Object value) {
        if (value instanceof Number number) {
            double d = number.doubleValue();
            return d >= 0 && d <= 0xFFFFFF && d == Math.rint(d) ? (int) d : null;
        }
        if (!(value instanceof String text)) {
            return null;
        }
        String name = text.strip().toLowerCase(Locale.ROOT);
        DyeColor dye = DyeColor.byName(name, null);
        if (dye != null) {
            return dye.getTextColor();
        }
        String hex = name.startsWith("#") ? name.substring(1) : name.startsWith("0x") ? name.substring(2) : name;
        if (hex.length() != 6) {
            return null;
        }
        try {
            return Integer.parseInt(hex, 16);
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
