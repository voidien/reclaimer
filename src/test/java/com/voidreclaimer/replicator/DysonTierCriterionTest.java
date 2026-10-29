package com.voidreclaimer.replicator;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.voidreclaimer.api.milestone.MilestoneContext;
import com.voidreclaimer.dust.DustData;
import org.junit.jupiter.api.Test;

class DysonTierCriterionTest {
    private static MilestoneContext atTier(int tier) {
        return new MilestoneContext(DustData.EMPTY, 0, Float.MAX_VALUE, tier);
    }

    @Test
    void reachedFromItsTierUpward() {
        MilestoneCriteria.DysonTier two = new MilestoneCriteria.DysonTier(2);
        assertFalse(two.isReached(atTier(1)));
        assertTrue(two.isReached(atTier(2)));
        assertTrue(two.isReached(atTier(4)));
    }

    @Test
    void olderContextsNeverReachDysonMilestones() {
        assertFalse(new MilestoneCriteria.DysonTier(1).isReached(MilestoneContext.of(DustData.EMPTY)));
    }

    @Test
    void builtinDysonMilestonesAreOrderedAfterTheStarterOnes() {
        int lastStarter = Milestones.BUILTIN.get(Milestones.EXPANSION).order();
        for (var key : java.util.List.of(Milestones.DYSON_TIER_1, Milestones.DYSON_TIER_2, Milestones.DYSON_TIER_3, Milestones.DYSON_TIER_4)) {
            assertTrue(Milestones.BUILTIN.get(key).order() > lastStarter, key.location().toString());
        }
    }
}
