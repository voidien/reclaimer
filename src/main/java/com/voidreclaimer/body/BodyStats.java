package com.voidreclaimer.body;

import com.voidreclaimer.flight.FlightStats;
import java.util.List;
import javax.annotation.Nullable;

/**
 * Everything the body's parts add up to. Pure: computed from part stats only, so it can be unit tested.
 *
 * <p>Mass is a soft limit: over the frame's budget, acceleration, braking and top speed drop and energy use rises,
 * the more the further over the budget the body is.
 *
 * @param size           size class (scale)
 * @param mass           total mass of frame and parts
 * @param massBudget     the frame's budget
 * @param coreTier       highest part tier the core allows
 * @param flight         handling after the overload penalty
 * @param energyUse      multiplier on thruster and dampener energy costs
 * @param energyCapacity energy capacity from the core
 * @param solarRate      sunlight charge multiplier from the core
 * @param maxHealth      max health (half-hearts) from the frame
 * @param reach          extra interaction range from the frame
 * @param dustIntake     dust collection multiplier from the frame
 * @param rows           main-inventory rows available (0..3, beyond the hotbar)
 * @param dustCapacity   extra dust storage per kind from storage parts
 */
public record BodyStats(SizeClass size, float mass, float massBudget, int coreTier, FlightStats flight, float energyUse,
                        float energyCapacity, float solarRate, float maxHealth, float reach, float dustIntake, int rows,
                        float dustCapacity) {
    public static final int MAX_ROWS = 3;
    /** Handling lost per unit of overload (mass/budget - 1). */
    static final double HANDLING_PENALTY = 2.0;
    static final double SPEED_PENALTY = 1.0;
    static final double ENERGY_PENALTY = 2.0;

    /** Fallbacks for a body with no frame or core (never happens in play; keeps the game running if data is broken). */
    private static final PartStats.Frame NO_FRAME = new PartStats.Frame(SizeClass.DRONE, 1f, 20f, 0f, 1f, 1, java.util.Map.of());
    private static final float NO_CORE_CAPACITY = 100f;

    public static BodyStats compute(@Nullable PartStats frame, List<PartStats> parts) {
        PartStats.Frame frameSpec = frame == null ? NO_FRAME : frame.frame().orElse(NO_FRAME);
        float mass = frame == null ? 0f : frame.mass();
        float thrust = 0f;
        float brake = 0f;
        float topSpeed = 0f;
        float energyCapacity = 0f;
        float solarRate = 0f;
        int coreTier = 0;
        boolean hasCore = false;
        int rows = frameSpec.baseRows();
        float dustCapacity = 0f;
        for (PartStats part : parts) {
            mass += part.mass();
            if (part.thruster().isPresent()) {
                PartStats.Thruster t = part.thruster().get();
                thrust += t.thrust();
                brake += t.brake();
                topSpeed = Math.max(topSpeed, t.topSpeed());
            }
            if (part.core().isPresent()) {
                hasCore = true;
                energyCapacity = part.core().get().energyCapacity();
                solarRate = part.core().get().solarRate();
                coreTier = part.tier();
            }
            if (part.storage().isPresent()) {
                rows += part.storage().get().rows();
                dustCapacity += part.storage().get().dustCapacity();
            }
        }
        if (!hasCore) {
            energyCapacity = NO_CORE_CAPACITY;
            solarRate = 1f;
        }
        mass = Math.max(mass, 0.01f);
        double overload = overload(mass, frameSpec.massBudget());
        double handling = 1.0 / (1.0 + HANDLING_PENALTY * overload);
        double speedFactor = 1.0 / (1.0 + SPEED_PENALTY * overload);
        FlightStats flight = new FlightStats(mass, thrust * handling, brake * handling,
                topSpeed * speedFactor / FlightStats.TICKS_PER_SECOND);
        return new BodyStats(frameSpec.size(), mass, frameSpec.massBudget(), coreTier, flight,
                (float) (1.0 + ENERGY_PENALTY * overload), energyCapacity, solarRate, frameSpec.maxHealth(), frameSpec.reach(),
                frameSpec.dustIntake(), Math.min(MAX_ROWS, rows), dustCapacity);
    }

    /** How far over the budget: 0 within budget, 0.5 at 150% of it. */
    public static double overload(float mass, float budget) {
        return budget <= 0f ? 0.0 : Math.max(0.0, mass / budget - 1.0);
    }

    public boolean overloaded() {
        return mass > massBudget;
    }

    /** Main-inventory slots usable, hotbar included (18..36). */
    public int inventorySlots() {
        return 9 + 9 * rows;
    }
}
