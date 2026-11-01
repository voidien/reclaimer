package com.voidreclaimer.link;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.voidreclaimer.energy.EnergyData;
import org.junit.jupiter.api.Test;

class SeatChargeTest {
    private static final EnergyData LOW = new EnergyData(10f, 100f, false);

    @Test
    void aSecondGivesTheRate() {
        assertEquals(10.5f, SeatCharge.after(LOW, 20L, 0.5).energy(), 0.001f);
    }

    @Test
    void itStopsAtTheCapacity() {
        assertEquals(100f, SeatCharge.after(LOW, 1_000_000L, 0.5).energy(), 0.001f);
    }

    @Test
    void aFullBodyStaysAsItIs() {
        EnergyData over = new EnergyData(120f, 100f, false);
        assertEquals(over, SeatCharge.after(over, 20L, 0.5));
    }

    @Test
    void aClockThatWentBackGivesNothing() {
        assertEquals(LOW, SeatCharge.after(LOW, -5L, 0.5));
    }

    @Test
    void lowPowerIsTheTickersToLift() {
        assertEquals(true, SeatCharge.after(new EnergyData(0f, 100f, true), 20L, 0.5).lowPower());
    }
}
