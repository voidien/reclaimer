package com.voidreclaimer.display;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

import java.util.List;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;

class HoloContentTest {
    private static HoloDisplay roundTrip(HoloDisplay display) {
        Tag saved = HoloDisplay.CODEC.encodeStart(NbtOps.INSTANCE, display).getOrThrow();
        return HoloDisplay.CODEC.parse(NbtOps.INSTANCE, saved).getOrThrow();
    }

    @Test
    void everyFieldSurvivesNbt() {
        HoloDisplay display = HoloDisplay.EMPTY.withLine(0, "first", HoloDisplay.NO_COLOR).withLine(2, "third", 0xFF0000)
                .withColor(0x00FF00).withScale(2.5f).withHeight(3f).withBillboard(false);
        assertEquals(display, roundTrip(display));
        HoloDisplay readout = display.withSource(ResourceLocation.fromNamespaceAndPath("some_addon", "gauge"));
        assertEquals(readout, roundTrip(readout), "an unknown source is kept, not dropped");
        assertEquals(HoloDisplay.Mode.READOUT, roundTrip(readout).mode());
    }

    @Test
    void anEmptySaveGivesTheDefaults() {
        assertEquals(HoloDisplay.EMPTY, HoloDisplay.CODEC.parse(NbtOps.INSTANCE, new CompoundTag()).getOrThrow());
    }

    @Test
    void aLineBeyondTheCapIsRefused() {
        HoloDisplay display = HoloDisplay.EMPTY.withLine(0, "kept", HoloDisplay.NO_COLOR);
        assertSame(display, display.withLine(HoloDisplay.MAX_LINES, "no", HoloDisplay.NO_COLOR));
        assertSame(display, display.withLine(-1, "no", HoloDisplay.NO_COLOR));
        assertEquals(List.of("kept"), display.texts());
    }

    @Test
    void writingOneLineKeepsTheOthers() {
        HoloDisplay display = HoloDisplay.EMPTY.withTexts(List.of("a", "b", "c")).withLine(1, "B", HoloDisplay.NO_COLOR);
        assertEquals(List.of("a", "B", "c"), display.texts());
    }

    @Test
    void writingTextOverAReadoutDropsTheReadoutsLines() {
        HoloDisplay readout = HoloDisplay.EMPTY.withSource(HoloDisplay.DEFAULT_SOURCE).withReadoutLines(List.of("r1", "r2", "r3"));
        HoloDisplay written = readout.withLine(1, "mine", HoloDisplay.NO_COLOR);
        assertEquals(HoloDisplay.Mode.TEXT, written.mode());
        assertEquals(List.of("", "mine"), written.texts());
    }

    @Test
    void choosingASourceSwitchesToReadoutAndReadoutLinesKeepTheMode() {
        HoloDisplay readout = HoloDisplay.EMPTY.withSource(HoloDisplay.DEFAULT_SOURCE);
        assertEquals(HoloDisplay.Mode.READOUT, readout.mode());
        assertEquals(HoloDisplay.Mode.READOUT, readout.withReadoutLines(List.of("x")).mode());
    }

    @Test
    void trailingBlankLinesAndExtraLinesAreDropped() {
        assertEquals(List.of("a"), HoloDisplay.EMPTY.withTexts(List.of("a", "", "")).texts());
        assertEquals(HoloDisplay.MAX_LINES, HoloDisplay.EMPTY.withTexts(java.util.Collections.nCopies(HoloDisplay.MAX_LINES + 4, "x")).lines().size());
    }

    @Test
    void scaleAndHeightAreHeldToTheirRanges() {
        assertEquals(HoloDisplay.MAX_SCALE, HoloDisplay.EMPTY.withScale(100f).scale());
        assertEquals(HoloDisplay.MIN_SCALE, HoloDisplay.EMPTY.withScale(0f).scale());
        assertEquals(HoloDisplay.MAX_HEIGHT, HoloDisplay.EMPTY.withHeight(100f).height());
        assertEquals(0f, HoloDisplay.EMPTY.withHeight(-3f).height());
    }

    @Test
    void aSavedLineTooLongIsCutOnLoad() {
        HoloDisplay.Line line = new HoloDisplay.Line("x".repeat(500), 0x1FF0000);
        assertEquals(HoloDisplay.MAX_CHARS, line.text().length());
        assertEquals(0xFF0000, line.color(), "only the colour bits are kept");
    }

    @Test
    void aModelAndABodySurviveNbt() {
        HoloDisplay model = HoloDisplay.EMPTY.withModel(ResourceLocation.fromNamespaceAndPath("some_addon", "entity/rover"));
        assertEquals(HoloDisplay.Mode.MODEL, model.mode());
        assertEquals("some_addon:entity/rover", roundTrip(model).subject());
        assertEquals(model, roundTrip(model));
        assertEquals(HoloDisplay.Mode.BODY, roundTrip(HoloDisplay.EMPTY.withBody()).mode());
    }

    @Test
    void theLastWriterWinsOverAModel() {
        HoloDisplay model = HoloDisplay.EMPTY.withLine(0, "old", HoloDisplay.NO_COLOR).withModel(ResourceLocation.fromNamespaceAndPath("a", "b"));
        assertEquals(List.of(), model.texts(), "a model replaces the text");
        HoloDisplay text = model.withLine(0, "new", HoloDisplay.NO_COLOR);
        assertEquals(HoloDisplay.Mode.TEXT, text.mode());
        assertEquals(HoloDisplay.Mode.READOUT, model.withSource(HoloDisplay.DEFAULT_SOURCE).mode());
    }
}
