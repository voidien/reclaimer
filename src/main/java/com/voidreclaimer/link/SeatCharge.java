package com.voidreclaimer.link;

import com.voidreclaimer.energy.EnergyData;

/** What an Uplink Seat gives the main body while the mind is away. Pure, for tests. */
public final class SeatCharge {
    private static final float TICKS_PER_SECOND = 20f;

    private SeatCharge() {}

    /** {@code energy} after {@code ticks} in a seat at {@code perSecond}. It never lowers a charge, and a clock that went back gives nothing. */
    public static EnergyData after(EnergyData energy, long ticks, double perSecond) {
        if (ticks <= 0 || energy.energy() >= energy.capacity()) {
            return energy;
        }
        float gained = (float) (perSecond * ticks / TICKS_PER_SECOND);
        return new EnergyData(Math.min(energy.capacity(), energy.energy() + gained), energy.capacity(), energy.lowPower());
    }
}
