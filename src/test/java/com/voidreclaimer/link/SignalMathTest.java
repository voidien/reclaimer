package com.voidreclaimer.link;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;

class SignalMathTest {
    @Test
    void aLostSignalCountsDownThenEnds() {
        assertEquals(5, SignalMath.countdown(0, 5), "the first second starts it");
        assertEquals(4, SignalMath.countdown(5, 5));
        assertEquals(0, SignalMath.countdown(1, 5), "the last second ends the link");
        assertEquals(0, SignalMath.countdown(0, 0), "no grace ends it at once");
    }

    private static final double EXACT = 1e-9;
    private static final SignalMath RULES = new SignalMath(1000, 2000, List.of(10.0, 200.0, 3000.0, 40000.0), 0.25, 1.5, 0.1);

    @Test
    void eachDysonTierAddsItsOwnRangeOnTopOfTheOnesBelow() {
        assertEquals(0, RULES.dysonBonus(0), EXACT);
        assertEquals(10, RULES.dysonBonus(1), EXACT);
        assertEquals(210, RULES.dysonBonus(2), EXACT);
        assertEquals(3210, RULES.dysonBonus(3), EXACT);
        assertEquals(43210, RULES.dysonBonus(4), EXACT);
    }

    @Test
    void aTierPastTheLastAddsNothingMore() {
        assertEquals(43210, RULES.dysonBonus(9), EXACT);
        assertEquals(0, RULES.dysonBonus(-1), EXACT);
    }

    @Test
    void seatsAndRelaysStartFromTheirOwnRangeAndBothGainTheBonus() {
        assertEquals(1000, RULES.seatRange(0, false), EXACT);
        assertEquals(1210, RULES.seatRange(2, false), EXACT);
        assertEquals(2000, RULES.relayRange(0), EXACT);
        assertEquals(2210, RULES.relayRange(2), EXACT);
    }

    @Test
    void aStationSeatMultipliesItsWholeRangeBonusIncluded() {
        assertEquals(1500, RULES.seatRange(0, true), EXACT);
        assertEquals(1815, RULES.seatRange(2, true), EXACT);
    }

    @Test
    void aStationSeatStartsStaticLater() {
        assertEquals(0.25, RULES.staticBand(false), EXACT);
        assertEquals(0.1, RULES.staticBand(true), EXACT);
    }

    @Test
    void strengthIsFullUntilTheBandThenFallsInAStraightLine() {
        assertEquals(1, SignalMath.strength(0, 100, 0.25), EXACT);
        assertEquals(1, SignalMath.strength(75, 100, 0.25), EXACT);
        assertEquals(0.5, SignalMath.strength(87.5, 100, 0.25), EXACT);
        assertEquals(0.2, SignalMath.strength(95, 100, 0.25), EXACT);
    }

    @Test
    void strengthIsNothingFromTheEdgeOutward() {
        assertEquals(0, SignalMath.strength(100, 100, 0.25), EXACT);
        assertEquals(0, SignalMath.strength(101, 100, 0.25), EXACT);
    }

    @Test
    void aNarrowerBandIsCleanFartherOut() {
        assertTrue(SignalMath.strength(85, 100, 0.1) > SignalMath.strength(85, 100, 0.25));
        assertEquals(1, SignalMath.strength(85, 100, 0.1), EXACT);
    }

    @Test
    void noBandMeansAHardEdge() {
        assertEquals(1, SignalMath.strength(100, 100, 0), EXACT);
        assertEquals(0, SignalMath.strength(100.5, 100, 0), EXACT);
    }

    @Test
    void aWholeBandFadesFromTheCentre() {
        assertEquals(0.5, SignalMath.strength(50, 100, 1), EXACT);
    }

    @Test
    void aNodeWithNoRangeReachesNothing() {
        assertEquals(0, SignalMath.strength(0, 0, 0.25), EXACT);
        assertEquals(0, SignalMath.strength(0, 0, 0), EXACT);
        assertEquals(0, SignalMath.strength(0, -5, 0), EXACT);
    }

    @Test
    void staticIsTheInverseOfStrengthAndStaysInBounds() {
        assertEquals(0, SignalMath.staticLevel(1), EXACT);
        assertEquals(0.75, SignalMath.staticLevel(0.25), EXACT);
        assertEquals(1, SignalMath.staticLevel(0), EXACT);
        assertEquals(0, SignalMath.staticLevel(1.5), EXACT);
        assertEquals(1, SignalMath.staticLevel(-1), EXACT);
    }

    @Test
    void barsGoOutOneAtATimeAndTheLastOnlyWhenTheSignalIsLost() {
        assertEquals(SignalMath.BARS, SignalMath.bars(1.0));
        assertEquals(SignalMath.BARS, SignalMath.bars(0.76));
        assertEquals(3, SignalMath.bars(0.75));
        assertEquals(2, SignalMath.bars(0.5));
        assertEquals(1, SignalMath.bars(0.25));
        assertEquals(1, SignalMath.bars(0.01));
        assertEquals(0, SignalMath.bars(0.0));
    }

    @Test
    void aStormThinsAStrongSignalAndTakesAWeakOne() {
        assertEquals(0.6, SignalMath.underStorm(1.0, 0.4), EXACT);
        assertEquals(0.0, SignalMath.underStorm(0.3, 0.4), EXACT);
        assertEquals(0.3, SignalMath.underStorm(0.3, 0.0), EXACT);
        assertEquals(0.3, SignalMath.underStorm(0.3, -1.0), EXACT);
    }
}
