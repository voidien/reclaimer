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

    @Test
    void theHelmSaysWhenItIsNotAboard() {
        assertEquals(List.of("HELM", "Not aboard a ship"), Readouts.helm(new Readouts.Helm(false, 0, 0, 0, 0, 0, 0, 0, 0, 0)));
    }

    @Test
    void theHelmShowsSpeedAttitudeAndSupplies() {
        assertEquals(List.of("HELM", "Speed 12.3 b/s", "Heading 90  Pitch -5  Roll 2", "Thrusters 6", "Fuel 500 / 1000 mB", "Energy 40 / 80 FE"),
                Readouts.helm(new Readouts.Helm(true, 6, 12.34, 90.2, -4.6, 2.4, 500, 1000, 40, 80)));
    }

    @Test
    void aSpireShowsItsPulseOnlyWhileItRuns() {
        assertEquals("Idle", Readouts.spire(new Readouts.Spire(false, 0.5, 2, 128)).get(1));
        assertEquals("Pulse 50%", Readouts.spire(new Readouts.Spire(true, 0.5, 2, 128)).get(1));
    }

    @Test
    void theLaunchComplexSaysWhyItIsNotWindingUp() {
        assertEquals(List.of("LAUNCH COMPLEX", "Not assembled"), Readouts.launch(new Readouts.Launch(false, 0.5, 3, 64, true)));
        assertEquals("No payload", Readouts.launch(new Readouts.Launch(true, 0.5, 3, 64, false)).get(1));
        assertEquals("Wind-up 50%", Readouts.launch(new Readouts.Launch(true, 0.5, 3, -64, true)).get(1));
        assertEquals("Speed 64.0 RPM", Readouts.launch(new Readouts.Launch(true, 0.5, 3, -64, true)).get(2));
    }

    @Test
    void aDeviceShowsOnlyWhatItHas() {
        assertEquals(List.of("Cell", "Energy 5 / 10 FE"), Readouts.device(new Readouts.Device("Cell", 5, 10, "", 0, -1)));
        assertEquals(List.of("Tank", "Water 5 / 10 mB"), Readouts.device(new Readouts.Device("Tank", 0, -1, "Water", 5, 10)));
        assertEquals(3, Readouts.device(new Readouts.Device("Both", 5, 10, "Water", 5, 10)).size());
    }

    @Test
    void theCloudReadoutSaysClearSpaceOutsideACloud() {
        assertEquals(List.of("DUST CLOUD", "Clear space"), Readouts.cloud(new Readouts.Cloud("", 0)));
        assertEquals(List.of("DUST CLOUD", "Kind ice", "Density 40%"), Readouts.cloud(new Readouts.Cloud("ice", 0.4)));
    }

    /** The biggest numbers a readout can meet must still fit a projector. */
    @Test
    void everyReadoutFitsTheCaps() {
        double big = 1e7;
        int most = Integer.MAX_VALUE;
        String name = "a name of twenty four ch";
        fits(Readouts.helm(new Readouts.Helm(true, most, big, 359, -90, -180, big, big, most, most)));
        fits(Readouts.thruster(new Readouts.Thruster(1, big, "Energy " + most + " / " + most + " FE")));
        fits(Readouts.siphon(new Readouts.Siphon(1, 1, big, most)));
        fits(Readouts.electrolyzer(new Readouts.Electrolyzer(1, big, most, most, most, most)));
        fits(Readouts.spinner(new Readouts.Spinner(1, big, 64, 64)));
        fits(Readouts.launch(new Readouts.Launch(true, 1, most, big, true)));
        fits(Readouts.dyson(new Readouts.Dyson(1L << 50, 1L << 50, 1L << 50, 4, 1)));
        fits(Readouts.spire(new Readouts.Spire(true, 1, most, most)));
        fits(Readouts.core(new Readouts.Core(name, 3, 3, 1)));
        fits(Readouts.body(new Readouts.Body(name, 1e6, 1e6, 1e6, 1e6, true)));
        fits(Readouts.cloud(new Readouts.Cloud(name, 1)));
        fits(Readouts.device(new Readouts.Device(name, most, most, "Hydrogen Gas", most, most)));
    }
}
