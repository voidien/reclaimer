package com.voidreclaimer.api.milestone;

import com.voidreclaimer.dust.DustData;
import java.util.Set;
import net.minecraft.resources.ResourceLocation;

/**
 * The player state milestone criteria look at. Kept to plain values so criteria stay unit-testable.
 *
 * @param dust         current dust reserve
 * @param blocksPlaced blocks the player has placed so far (by hand or with the Nanite Swarm)
 * @param dustCapacity storage per dust kind
 * @param dysonTier    current Dyson Sphere skybox tier (0 to 4), the same for every player
 * @param itemsMade    ids of the items the player has held at some point (only those some milestone asks for are recorded)
 */
public record MilestoneContext(DustData dust, int blocksPlaced, float dustCapacity, int dysonTier, Set<ResourceLocation> itemsMade) {
    public MilestoneContext {
        itemsMade = Set.copyOf(itemsMade);
    }

    public static MilestoneContext of(DustData dust) {
        return new MilestoneContext(dust, 0, Float.MAX_VALUE, 0, Set.of());
    }

    public MilestoneContext(DustData dust, int blocksPlaced) {
        this(dust, blocksPlaced, Float.MAX_VALUE, 0, Set.of());
    }

    public MilestoneContext(DustData dust, int blocksPlaced, float dustCapacity) {
        this(dust, blocksPlaced, dustCapacity, 0, Set.of());
    }

    public MilestoneContext(DustData dust, int blocksPlaced, float dustCapacity, int dysonTier) {
        this(dust, blocksPlaced, dustCapacity, dysonTier, Set.of());
    }
}
