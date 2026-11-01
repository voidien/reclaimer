package com.voidreclaimer.debug;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class HelmTraceTest {
    /** A pulse of asking on yaw, and a ship whose spin rises {@code delay} ticks after each tick of it. */
    private static HelmTrace shipLaggingBy(int delay) {
        HelmTrace trace = new HelmTrace();
        double[] asked = new double[40];
        for (int i = 8; i < 14; i++) {
            asked[i] = 1.0;
        }
        double spin = 0;
        for (int i = 0; i < 40; i++) {
            spin += i >= delay ? asked[i - delay] * 10.0 : 0.0;
            trace.record(new HelmTrace.Sample(0f, 0f, new double[] {0, asked[i], 0}, new double[] {0, spin, 0}));
        }
        return trace;
    }

    @Test
    void itFindsHowLongTheShipTakesToAnswer() {
        assertEquals(1, shipLaggingBy(1).lag());
        assertEquals(2, shipLaggingBy(2).lag());
        assertEquals(4, shipLaggingBy(4).lag());
    }

    @Test
    void aStillShipHasNoLagToMeasure() {
        HelmTrace trace = new HelmTrace();
        trace.record(new HelmTrace.Sample(0f, 0f, new double[] {0, 1, 0}, new double[] {0, 0, 0}));
        assertEquals(-1, trace.lag());
    }

    @Test
    void theLinesSayWhatWasAskedAndWhatHappened() {
        HelmTrace trace = shipLaggingBy(2);
        trace.setup(new double[] {90, 120, 400}, "mouse");
        String text = String.join("\n", trace.lines());
        assertTrue(text.contains("Steering: mouse"), text);
        assertTrue(text.contains("yaw 120"), text);
        assertTrue(text.contains("about 2 ticks"), text);
    }
}
