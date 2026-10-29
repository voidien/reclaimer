package com.voidreclaimer.replicator;

import com.voidreclaimer.dust.DustData;
import com.voidreclaimer.dust.DustKind;

/**
 * The player state milestone criteria look at. Kept to plain values so the criteria stay unit-testable.
 *
 * @param dust         current dust reserve
 * @param blocksPlaced blocks the player has placed so far (by hand or with the Nanite Swarm)
 * @param dustCapacity storage per dust kind
 */
public record MilestoneContext(DustData dust, int blocksPlaced, float dustCapacity) {
    public static MilestoneContext of(DustData dust) {
        return new MilestoneContext(dust, 0, Float.MAX_VALUE);
    }

    public MilestoneContext(DustData dust, int blocksPlaced) {
        this(dust, blocksPlaced, Float.MAX_VALUE);
    }

    /** Whether any one kind of dust is at capacity. */
    public boolean anyKindFull() {
        for (DustKind kind : DustKind.values()) {
            if (dust.get(kind) >= dustCapacity - 1.0E-3f) {
                return true;
            }
        }
        return false;
    }

    /** How many dust kinds the reserve holds any of. */
    public int kindsHeld() {
        int kinds = 0;
        for (DustKind kind : DustKind.values()) {
            if (dust.get(kind) > 0f) {
                kinds++;
            }
        }
        return kinds;
    }
}
