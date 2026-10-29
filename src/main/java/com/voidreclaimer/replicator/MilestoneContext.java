package com.voidreclaimer.replicator;

import com.voidreclaimer.dust.DustData;

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
        return dust.anyKindFull(dustCapacity);
    }

    /** How many dust kinds the reserve holds any of. */
    public int kindsHeld() {
        return dust.kindsHeld();
    }
}
