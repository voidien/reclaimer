package com.voidreclaimer.replicator;

import com.voidreclaimer.dust.DustData;
import com.voidreclaimer.dust.DustKind;

/**
 * The player state milestone criteria look at. Kept to plain values so the criteria stay unit-testable.
 *
 * @param dust         current dust reserve
 * @param blocksPlaced blocks the player has placed so far (by hand or with the Nanite Swarm)
 */
public record MilestoneContext(DustData dust, int blocksPlaced) {
    public static MilestoneContext of(DustData dust) {
        return new MilestoneContext(dust, 0);
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
