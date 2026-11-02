package com.voidreclaimer.weather;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Optional;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;

class WeatherClockTest {
    private static final ResourceLocation FLARE = ResourceLocation.fromNamespaceAndPath("test", "flare");
    private static final ResourceLocation ION = ResourceLocation.fromNamespaceAndPath("test", "ion");
    private static final WeatherClock.Candidate FLARES = new WeatherClock.Candidate(FLARE, 1, 3, 10, 10);
    private static final WeatherClock.Candidate IONS = new WeatherClock.Candidate(ION, 3, 0, 5, 9);
    private static final WeatherClock.Dice ZERO = bound -> 0;

    private static WeatherState tick(WeatherState state, List<WeatherClock.Candidate> candidates) {
        return WeatherClock.second(state, candidates, () -> 100, ZERO);
    }

    @Test
    void aNewWorldPlansItsFirstStorm() {
        assertEquals(100, tick(WeatherState.NONE, List.of()).nextIn());
    }

    @Test
    void theGapCountsDownAndAStormIsPicked() {
        WeatherState waiting = new WeatherState(Optional.empty(), 0, 0, 0f, 1);
        WeatherState next = tick(waiting, List.of(FLARES));
        assertEquals(0, next.nextIn());
        WeatherState coming = tick(next, List.of(FLARES));
        assertEquals(Optional.of(FLARE), coming.weather());
        assertTrue(coming.warning());
        assertEquals(3, coming.warningLeft());
    }

    @Test
    void withNothingThatCanComeItWaitsAnotherGap() {
        WeatherState due = new WeatherState(Optional.empty(), 0, 0, 0f, 0);
        WeatherState next = tick(due, List.of());
        assertTrue(next.weather().isEmpty());
        assertEquals(100, next.nextIn());
    }

    @Test
    void aWarningRunsOutThenTheStormRunsItsLengthThenItPlansTheNext() {
        WeatherState state = WeatherClock.start(FLARES, ZERO);
        for (int second = 0; second < 3; second++) {
            assertTrue(state.warning(), "second " + second);
            assertFalse(state.active());
            state = tick(state, List.of());
        }
        int active = 0;
        while (state.active()) {
            active++;
            state = tick(state, List.of());
        }
        assertEquals(10, active);
        assertTrue(state.weather().isEmpty());
        assertEquals(100, state.nextIn());
    }

    @Test
    void weightDecidesWhichComes() {
        WeatherState due = new WeatherState(Optional.empty(), 0, 0, 0f, 0);
        assertEquals(Optional.of(FLARE), WeatherClock.second(due, List.of(FLARES, IONS), () -> 100, bound -> 0).weather());
        assertEquals(Optional.of(ION), WeatherClock.second(due, List.of(FLARES, IONS), () -> 100, bound -> bound == 4 ? 1 : 0).weather());
        assertEquals(Optional.of(ION), WeatherClock.second(due, List.of(FLARES, IONS), () -> 100, bound -> bound == 4 ? 3 : 0).weather());
    }

    @Test
    void aWeightOfNothingNeverComes() {
        WeatherState due = new WeatherState(Optional.empty(), 0, 0, 0f, 0);
        assertTrue(tick(due, List.of(new WeatherClock.Candidate(FLARE, 0, 3, 10, 10))).weather().isEmpty());
    }

    @Test
    void lengthAndStrengthStayInTheirRange() {
        WeatherState weakest = WeatherClock.start(IONS, bound -> 0);
        WeatherState strongest = WeatherClock.start(IONS, bound -> bound - 1);
        assertEquals(5, weakest.activeLeft());
        assertEquals(9, strongest.activeLeft());
        assertEquals(0.5f, weakest.strength(), 1e-6f);
        assertEquals(1.0f, strongest.strength(), 1e-6f);
    }
}
