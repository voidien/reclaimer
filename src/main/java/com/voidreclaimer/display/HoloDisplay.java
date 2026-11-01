package com.voidreclaimer.display;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.voidreclaimer.VoidReclaimer;
import com.voidreclaimer.registry.ModCodecs;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.util.StringRepresentable;

/**
 * What a Holo Projector shows. Immutable: every change makes a new value, so a projector can tell whether anything changed.
 * The last writer wins: a program's text switches to {@link Mode#TEXT}, choosing a source switches to {@link Mode#READOUT}.
 *
 * @param lines     up to {@link #MAX_LINES} lines; trailing blank lines are dropped
 * @param color     colour of lines that have none of their own, 0xRRGGBB
 * @param mode      what the projector is showing
 * @param source    the readout kind (registry id; an unknown id is kept and shows nothing)
 * @param scale     size of the picture, {@link #MIN_SCALE} to {@link #MAX_SCALE}
 * @param height    blocks from the top of the projector to the picture, 0 to {@link #MAX_HEIGHT}
 * @param billboard whether the picture turns to the viewer
 * @param subject   what {@link Mode#MODEL} shows: a GeckoLib model id; unused by the other modes
 */
public record HoloDisplay(List<Line> lines, int color, Mode mode, ResourceLocation source, float scale, float height, boolean billboard, String subject) {
    public static final int MAX_LINES = 8;
    public static final int MAX_CHARS = 48;
    public static final int DEFAULT_COLOR = 0x7FDFFF;
    /** A line with this colour uses the display's. */
    public static final int NO_COLOR = -1;
    public static final float MIN_SCALE = 0.25f;
    public static final float MAX_SCALE = 4f;
    public static final float MAX_HEIGHT = 4f;
    public static final ResourceLocation DEFAULT_SOURCE = VoidReclaimer.id("clock");
    public static final HoloDisplay EMPTY = new HoloDisplay(List.of(), DEFAULT_COLOR, Mode.TEXT, DEFAULT_SOURCE, 1f, 0.5f, true, "");

    /** What the projector is showing. Saved by name, so new modes keep old saves readable. */
    public enum Mode implements StringRepresentable {
        TEXT, READOUT, MODEL, BODY;

        public static final Codec<Mode> CODEC = StringRepresentable.fromEnum(Mode::values);

        @Override
        public String getSerializedName() {
            return name().toLowerCase(Locale.ROOT);
        }
    }

    /** One line of text and its colour ({@link #NO_COLOR} for the display's). */
    public record Line(String text, int color) {
        public static final Line BLANK = new Line("", NO_COLOR);
        public static final Codec<Line> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Codec.STRING.optionalFieldOf("text", "").forGetter(Line::text),
                Codec.INT.optionalFieldOf("color", NO_COLOR).forGetter(Line::color)
        ).apply(instance, Line::new));

        public Line {
            text = HoloText.clean(text);
            color = color < 0 ? NO_COLOR : color & 0xFFFFFF;
        }
    }

    public static final Codec<HoloDisplay> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Line.CODEC.listOf().lenientOptionalFieldOf("lines", List.of()).forGetter(HoloDisplay::lines),
            Codec.INT.optionalFieldOf("color", DEFAULT_COLOR).forGetter(HoloDisplay::color),
            Mode.CODEC.lenientOptionalFieldOf("mode", Mode.TEXT).forGetter(HoloDisplay::mode),
            ModCodecs.ID.lenientOptionalFieldOf("source", DEFAULT_SOURCE).forGetter(HoloDisplay::source),
            Codec.FLOAT.optionalFieldOf("scale", 1f).forGetter(HoloDisplay::scale),
            Codec.FLOAT.optionalFieldOf("height", 0.5f).forGetter(HoloDisplay::height),
            Codec.BOOL.optionalFieldOf("billboard", true).forGetter(HoloDisplay::billboard),
            Codec.STRING.optionalFieldOf("subject", "").forGetter(HoloDisplay::subject)
    ).apply(instance, HoloDisplay::new));

    public HoloDisplay {
        lines = trim(lines);
        color &= 0xFFFFFF;
        scale = Mth.clamp(scale, MIN_SCALE, MAX_SCALE);
        height = Mth.clamp(height, 0f, MAX_HEIGHT);
    }

    private static List<Line> trim(List<Line> lines) {
        int end = Math.min(lines.size(), MAX_LINES);
        while (end > 0 && lines.get(end - 1).text().isEmpty()) {
            end--;
        }
        return List.copyOf(lines.subList(0, end));
    }

    /** Whether a line number (from 0) is one a projector can hold. */
    public static boolean fits(int index) {
        return index >= 0 && index < MAX_LINES;
    }

    /** The text of a line, empty past the last one. */
    public String text(int index) {
        return index >= 0 && index < lines.size() ? lines.get(index).text() : "";
    }

    public List<String> texts() {
        return lines.stream().map(Line::text).toList();
    }

    /** With one line written, as text (a readout's lines are dropped first); a line beyond the cap changes nothing. */
    public HoloDisplay withLine(int index, String text, int lineColor) {
        if (!fits(index)) {
            return this;
        }
        List<Line> next = new ArrayList<>(mode == Mode.TEXT ? lines : List.of());
        while (next.size() <= index) {
            next.add(Line.BLANK);
        }
        next.set(index, new Line(text, lineColor));
        return new HoloDisplay(next, color, Mode.TEXT, source, scale, height, billboard, subject);
    }

    /** With all the text replaced. */
    public HoloDisplay withTexts(List<String> texts) {
        return new HoloDisplay(texts.stream().map(text -> new Line(text, NO_COLOR)).toList(), color, Mode.TEXT, source, scale, height, billboard, subject);
    }

    /** The same picture with other lines: for a client's own message, nothing saved. */
    public HoloDisplay withLines(List<String> texts) {
        return withReadoutLines(texts);
    }

    /** With the readout's newest lines; the mode stays what it was. */
    public HoloDisplay withReadoutLines(List<String> texts) {
        return new HoloDisplay(texts.stream().map(text -> new Line(text, NO_COLOR)).toList(), color, mode, source, scale, height, billboard, subject);
    }

    public HoloDisplay withColor(int value) {
        return new HoloDisplay(lines, value, mode, source, scale, height, billboard, subject);
    }

    /** Showing a readout from now on. */
    public HoloDisplay withSource(ResourceLocation id) {
        return new HoloDisplay(lines, color, Mode.READOUT, id, scale, height, billboard, subject);
    }

    /** Showing a GeckoLib model from now on. The server cannot tell whether it exists: a client without it says so. */
    public HoloDisplay withModel(ResourceLocation id) {
        return new HoloDisplay(List.of(), color, Mode.MODEL, source, scale, height, billboard, id.toString());
    }

    /** Showing the viewer's own body from now on. */
    public HoloDisplay withBody() {
        return new HoloDisplay(List.of(), color, Mode.BODY, source, scale, height, billboard, "");
    }

    public HoloDisplay withScale(float value) {
        return new HoloDisplay(lines, color, mode, source, value, height, billboard, subject);
    }

    public HoloDisplay withHeight(float value) {
        return new HoloDisplay(lines, color, mode, source, scale, value, billboard, subject);
    }

    public HoloDisplay withBillboard(boolean value) {
        return new HoloDisplay(lines, color, mode, source, scale, height, value, subject);
    }
}
