package com.voidreclaimer.body;

import com.voidreclaimer.api.body.ModuleStats;
import com.voidreclaimer.api.body.SectionStats;
import com.voidreclaimer.api.body.SizeClass;
import com.voidreclaimer.flight.FlightStats;
import java.util.List;
import net.minecraft.resources.ResourceKey;

/**
 * Everything the body's sections and modules add up to. Pure: computed from their stats and the size class only, so it
 * can be unit tested.
 *
 * <p>Sections add mass, mass budget, health, reach, dust intake, inventory rows, stability and turn rate. Modules add
 * mass, and by kind: the core sets energy and the tier limit, thrusters add up (the best one sets top speed), storage
 * adds rows and dust capacity.
 *
 * <p>Mass is a soft limit: over the budget, acceleration, braking and top speed drop and energy use rises, the more
 * the further over the budget the body is.
 *
 * @param size           size class id
 * @param scale          body scale ({@code generic.scale})
 * @param sizeRank       size class rank, for modules with a minimum size
 * @param mass           total mass of sections and modules
 * @param massBudget     summed section budgets
 * @param coreTier       highest tier the core module allows (0 without a core)
 * @param flight         handling after the overload penalty
 * @param energyUse      multiplier on thruster and dampener energy costs
 * @param energyCapacity energy capacity from the core
 * @param solarRate      sunlight charge multiplier from the core
 * @param maxHealth      max health (half-hearts)
 * @param reach          extra interaction range
 * @param dustIntake     dust collection multiplier
 * @param rows           main-inventory rows available (0..3, beyond the hotbar)
 * @param dustCapacity   extra dust storage per kind from storage modules
 * @param stability      footing on solid ground
 * @param turnRate       rotation help
 */
public record BodyStats(ResourceKey<SizeClass> size, double scale, int sizeRank, float mass, float massBudget, int coreTier,
                        FlightStats flight, float energyUse, float energyCapacity, float solarRate, float maxHealth,
                        float reach, float dustIntake, int rows, float dustCapacity, float stability, float turnRate) {
    public static final int MAX_ROWS = 3;
    /** Handling lost per unit of overload (mass/budget - 1). */
    static final double HANDLING_PENALTY = 2.0;
    static final double SPEED_PENALTY = 1.0;
    static final double ENERGY_PENALTY = 2.0;
    /** Fallbacks when data is broken (no core module, no sections); never happens in normal play. */
    static final float NO_CORE_CAPACITY = 100f;
    static final float MIN_HEALTH = 2f;
    static final float MIN_BUDGET = 0.1f;

    /** Stats of an installed section or module with its effectiveness (1, or less for a worn section, see {@link Wear}). */
    public record Installed<T>(T stats, float efficiency) {}

    /** All parts at full effectiveness. */
    public static BodyStats compute(ResourceKey<SizeClass> sizeKey, SizeClass size, List<SectionStats> sections,
                                    List<ModuleStats> modules) {
        return computeWorn(sizeKey, size, sections.stream().map(s -> new Installed<>(s, 1f)).toList(),
                modules.stream().map(m -> new Installed<>(m, 1f)).toList());
    }

    /**
     * Sections' contributions (except mass and rows) scale with their effectiveness; so do the thrusters mounted on
     * them (a module's effectiveness is its section's).
     */
    public static BodyStats computeWorn(ResourceKey<SizeClass> sizeKey, SizeClass size, List<Installed<SectionStats>> sections,
                                        List<Installed<ModuleStats>> modules) {
        float mass = 0f;
        float budget = 0f;
        float health = 0f;
        float reach = 0f;
        float intake = 0f;
        int rows = 0;
        float stability = 0f;
        float turnRate = 0f;
        for (Installed<SectionStats> installed : sections) {
            SectionStats section = installed.stats();
            float e = installed.efficiency();
            mass += section.mass();
            // A worn-out section still carries and gathers: a body that couldn't lift itself or take in dust would be stranded in all but name.
            float carrying = Math.max(e, 1f - Wear.MAX_LOSS);
            budget += section.massBudget() * carrying;
            health += section.maxHealth() * e;
            reach += section.reach() * e;
            intake += section.dustIntake() * carrying;
            rows += section.baseRows();
            stability += section.stability() * e;
            turnRate += section.turnRate() * e;
        }
        float thrust = 0f;
        float brake = 0f;
        float topSpeed = 0f;
        float energyCapacity = NO_CORE_CAPACITY;
        float solarRate = 1f;
        int coreTier = 0;
        float dustCapacity = 0f;
        // Wear leaves the last thrusters a little life, but a save can hold them at nothing, and then the body limps on what it has.
        boolean limping = modules.stream().noneMatch(installed -> installed.efficiency() > 0f && installed.stats().thruster().isPresent());
        for (Installed<ModuleStats> installed : modules) {
            ModuleStats module = installed.stats();
            float e = limping && module.thruster().isPresent() ? 1f - Wear.MAX_LOSS : installed.efficiency();
            mass += module.mass();
            if (module.thruster().isPresent()) {
                ModuleStats.Thruster t = module.thruster().get();
                thrust += t.thrust() * e;
                brake += t.brake() * e;
                topSpeed = Math.max(topSpeed, t.topSpeed() * e);
            }
            if (module.core().isPresent()) {
                energyCapacity = module.core().get().energyCapacity();
                solarRate = module.core().get().solarRate();
                coreTier = module.tier();
            }
            if (module.storage().isPresent()) {
                rows += module.storage().get().rows();
                dustCapacity += module.storage().get().dustCapacity();
            }
            if (module.plating().isPresent()) {
                health += module.plating().get().health() * e;
            }
        }
        mass = Math.max(mass, 0.01f);
        budget = Math.max(budget, MIN_BUDGET);
        double overload = overload(mass, budget);
        double handling = 1.0 / (1.0 + HANDLING_PENALTY * overload);
        double speedFactor = 1.0 / (1.0 + SPEED_PENALTY * overload);
        double bySize = Math.min(topSpeed * size.speedFactor(), size.maxSpeed());
        FlightStats flight = new FlightStats(mass, thrust * handling, brake * handling,
                bySize * speedFactor / FlightStats.TICKS_PER_SECOND);
        return new BodyStats(sizeKey, size.scale(), size.rank(), mass, budget, coreTier, flight,
                (float) (1.0 + ENERGY_PENALTY * overload), energyCapacity, solarRate, Math.max(MIN_HEALTH, health), reach,
                intake, Math.min(MAX_ROWS, rows), (float) (dustCapacity * size.storageFactor()), stability, turnRate);
    }

    /** How far over the budget: 0 within budget, 0.5 at 150% of it. */
    public static double overload(float mass, float budget) {
        return budget <= 0f ? 0.0 : Math.max(0.0, mass / budget - 1.0);
    }

    public boolean overloaded() {
        return mass > massBudget;
    }

    /** Main-inventory slots usable, hotbar included (9..36). */
    public int inventorySlots() {
        return 9 + 9 * rows;
    }
}
