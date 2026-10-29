package com.voidreclaimer.energy;

import com.voidreclaimer.Config;

/** Energy tuning in per-tick units, decoupled from the config so the simulation can be unit tested. */
public record EnergyRules(
        float capacity,
        float idleDrainPerTick,
        float sunChargePerTick,
        float emergencyChargePerTick,
        float reserveFraction,
        float restartFraction) {
    private static final float TICKS_PER_SECOND = 20f;

    /** Config rates with the body's core capacity and solar multiplier. */
    public static EnergyRules forBody(com.voidreclaimer.body.BodyStats body) {
        EnergyRules base = fromConfig();
        return new EnergyRules(body.energyCapacity(), base.idleDrainPerTick, base.sunChargePerTick * body.solarRate(),
                base.emergencyChargePerTick, base.reserveFraction, base.restartFraction);
    }

    public static EnergyRules fromConfig() {
        return new EnergyRules(
                Config.ENERGY_CAPACITY.get().floatValue(),
                Config.ENERGY_IDLE_DRAIN.get().floatValue() / TICKS_PER_SECOND,
                Config.ENERGY_SUN_CHARGE.get().floatValue() / TICKS_PER_SECOND,
                Config.ENERGY_EMERGENCY_CHARGE.get().floatValue() / TICKS_PER_SECOND,
                Config.ENERGY_RESERVE_FRACTION.get().floatValue(),
                Config.ENERGY_RESTART_FRACTION.get().floatValue());
    }
}
