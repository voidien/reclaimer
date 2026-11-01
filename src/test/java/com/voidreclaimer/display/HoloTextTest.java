package com.voidreclaimer.display;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import net.minecraft.world.item.DyeColor;
import org.junit.jupiter.api.Test;

class HoloTextTest {
    @Test
    void cleanCutsAtTheLineLengthAndDropsControlCharacters() {
        assertEquals(HoloDisplay.MAX_CHARS, HoloText.clean("x".repeat(HoloDisplay.MAX_CHARS + 20)).length());
        assertEquals("ab", HoloText.clean("a\u0007b"));
        assertEquals("plain", HoloText.clean("plain"));
    }

    @Test
    void wrapBreaksAtSpacesWithinTheLineLength() {
        String word = "w".repeat(30);
        List<String> lines = HoloText.wrap(word + " " + word);
        assertEquals(List.of(word, word), lines);
    }

    @Test
    void wrapCutsAWordLongerThanALine() {
        List<String> lines = HoloText.wrap("x".repeat(HoloDisplay.MAX_CHARS + 5));
        assertEquals(2, lines.size());
        assertEquals(HoloDisplay.MAX_CHARS, lines.get(0).length());
        assertEquals("xxxxx", lines.get(1));
    }

    @Test
    void wrapKeepsNewlinesAndStopsAtTheLineCap() {
        assertEquals(List.of("a", "", "b"), HoloText.wrap("a\n\nb"));
        List<String> many = HoloText.wrap("x\n".repeat(HoloDisplay.MAX_LINES + 5));
        assertEquals(HoloDisplay.MAX_LINES, many.size());
    }

    @Test
    void everyWrappedLineFits() {
        for (String line : HoloText.wrap("the quick brown fox ".repeat(30))) {
            assertTrue(line.length() <= HoloDisplay.MAX_CHARS, line);
        }
    }

    @Test
    void coloursReadNumbersHexAndDyeNames() {
        assertEquals(0x123456, HoloText.color(0x123456));
        assertEquals(0x123456, HoloText.color((double) 0x123456), "Lua numbers are doubles");
        assertEquals(0xABCDEF, HoloText.color("#abcdef"));
        assertEquals(0xABCDEF, HoloText.color("0xABCDEF"));
        assertEquals(0xABCDEF, HoloText.color("abcdef"));
        assertEquals(DyeColor.CYAN.getTextColor(), HoloText.color("cyan"));
        assertEquals(DyeColor.RED.getTextColor(), HoloText.color(" RED "));
    }

    @Test
    void anythingElseIsNoColour() {
        assertNull(HoloText.color(null));
        assertNull(HoloText.color("not a colour"));
        assertNull(HoloText.color("#12345"));
        assertNull(HoloText.color(-1));
        assertNull(HoloText.color(0x1000000));
        assertNull(HoloText.color(1.5));
        assertNull(HoloText.color(true));
    }
}
