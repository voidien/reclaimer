package com.voidreclaimer.dyson;

/**
 * Config-free Dyson Sphere math: skybox tiers and the 0-1 values the sky is drawn from.
 *
 * <ul>
 *   <li>Tier 0: nothing launched.</li>
 *   <li>Tier 1: 1-{@value #SWARM_TIER_1_MAX} satellites. Tier 2: {@value #SWARM_TIER_1_MAX}+1 and more.</li>
 *   <li>Tier 3: at least {@code latticeStart} truss modules. Tier 4: {@code shellTotal} truss modules, the shell is complete.</li>
 * </ul>
 * Truss tiers take over from the satellite tiers.
 */
public final class DysonTiers {
    public static final int SWARM_TIER_1_MAX = 50;
    /** Satellites at which the swarm is drawn at full density. */
    public static final int SWARM_FULL = 500;

    /** The owner's numbers (2026-10-29): the lattice appears after 50 trusses, the shell is complete at 2000. The server config overrides both. */
    public static final long DEFAULT_LATTICE_START = 50;
    public static final long DEFAULT_SHELL_TOTAL = 2000;

    /** Sunlight left when the lattice first appears ("about 50% occluded"), and with the swarm at its densest. */
    private static final float LATTICE_SUNLIGHT = 0.5f;
    private static final float DENSE_SWARM_SUNLIGHT = 0.85f;

    private DysonTiers() {}

    public static int tier(long satellites, long trusses, long latticeStart, long shellTotal) {
        long start = Math.max(1, latticeStart);
        long total = Math.max(start, shellTotal);
        if (trusses >= total) {
            return 4;
        }
        if (trusses >= start) {
            return 3;
        }
        if (satellites > SWARM_TIER_1_MAX) {
            return 2;
        }
        return satellites > 0 ? 1 : 0;
    }

    public static float swarmDensity(long satellites) {
        return clamp01(satellites / (float) SWARM_FULL);
    }

    public static float shellProgress(long trusses, long shellTotal) {
        return shellTotal <= 0 ? 0f : clamp01(trusses / (float) shellTotal);
    }

    /**
     * Fraction of the star's light that still arrives, for the sky. Falls from 1 with the swarm, to 0.5 as the lattice
     * starts, and to 0 when the shell is complete. Not used for solar charging yet: the star going dark would also
     * cut off the AI's power, which needs the Dyson power beam first.
     */
    public static float sunlight(int tier, float shellProgress) {
        return switch (tier) {
            case 0, 1 -> 1f;
            case 2 -> DENSE_SWARM_SUNLIGHT;
            case 3 -> LATTICE_SUNLIGHT * (1f - shellProgress);
            default -> 0f;
        };
    }

    private static float clamp01(float value) {
        return Math.max(0f, Math.min(1f, value));
    }
}
