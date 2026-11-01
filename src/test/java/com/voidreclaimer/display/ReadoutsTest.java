package com.voidreclaimer.display;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Locale;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;

class ReadoutsTest {
    private static void fits(List<String> lines) {
        assertTrue(lines.size() <= HoloDisplay.MAX_LINES, "too many lines: " + lines);
        for (String line : lines) {
            assertTrue(line.length() <= HoloDisplay.MAX_CHARS, "too long: " + line);
        }
    }

    @Test
    void anUnknownKindShowsNothing() {
        assertEquals(List.of(), Readouts.lines(null, null, null));
    }

    @Test
    void numbersDoNotFollowTheLocale() {
        Locale before = Locale.getDefault();
        Locale.setDefault(Locale.GERMANY);
        try {
            assertEquals("1234.5", Readouts.number(1234.5));
        } finally {
            Locale.setDefault(before);
        }
    }

    @Test
    void percentRoundsAndHoldsItsRange() {
        assertEquals("0%", Readouts.percent(-1));
        assertEquals("50%", Readouts.percent(0.504));
        assertEquals("100%", Readouts.percent(3));
    }

    @Test
    void theClockShowsDayAndTime() {
        assertEquals(List.of("MISSION CLOCK", "Day 0  00:00"), Readouts.clock(new Readouts.Clock(0)));
        assertEquals(List.of("MISSION CLOCK", "Day 2  06:30"), Readouts.clock(new Readouts.Clock(2 * 24_000 + 6_500)));
        fits(Readouts.clock(new Readouts.Clock(Long.MAX_VALUE)));
    }

    @Test
    void nextWalksTheKindsInIdOrderAndWrapsRound() {
        ResourceLocation a = ResourceLocation.parse("void_reclaimer:a");
        ResourceLocation b = ResourceLocation.parse("void_reclaimer:b");
        ResourceLocation c = ResourceLocation.parse("void_reclaimer:c");
        List<ResourceLocation> ids = List.of(c, a, b);
        assertEquals(b, Readouts.next(ids, a));
        assertEquals(c, Readouts.next(ids, b));
        assertEquals(a, Readouts.next(ids, c), "wraps round");
        assertEquals(a, Readouts.next(ids, ResourceLocation.parse("void_reclaimer:gone")), "an unknown id starts from the first");
    }
}
