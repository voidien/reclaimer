package com.voidreclaimer.api.milestone;

import com.voidreclaimer.dust.DustData;
import java.util.Set;
import net.minecraft.resources.ResourceLocation;

/**
 * The player state milestone criteria look at. Kept to plain values so criteria stay unit-testable.
 *
 * @param dust         current dust reserve
 * @param collected    dust gathered over the whole game, per kind (clouds and drilling; not gifts or refunds)
 * @param blocksPlaced blocks the player has placed so far (by hand or with the Nanite Swarm)
 * @param dustCapacity storage per dust kind
 * @param dysonTier    current Dyson Sphere skybox tier (0 to 4), the same for every player
 * @param itemsMade    ids of the items the player has held at some point (only those some milestone asks for are recorded)
 * @param dimensionsVisited ids of the dimensions the player has set foot in
 */
public record MilestoneContext(DustData dust, DustData collected, int blocksPlaced, float dustCapacity, int dysonTier,
                               Set<ResourceLocation> itemsMade, Set<ResourceLocation> dimensionsVisited) {
    public MilestoneContext {
        itemsMade = Set.copyOf(itemsMade);
        dimensionsVisited = Set.copyOf(dimensionsVisited);
    }

    public MilestoneContext(DustData dust, DustData collected, int blocksPlaced, float dustCapacity, int dysonTier, Set<ResourceLocation> itemsMade) {
        this(dust, collected, blocksPlaced, dustCapacity, dysonTier, itemsMade, Set.of());
    }

    /** The shorthands below treat everything held as collected, which is what a test of one criterion wants. */
    public static MilestoneContext of(DustData dust) {
        return new MilestoneContext(dust, dust, 0, Float.MAX_VALUE, 0, Set.of());
    }

    public MilestoneContext(DustData dust, int blocksPlaced) {
        this(dust, dust, blocksPlaced, Float.MAX_VALUE, 0, Set.of());
    }

    public MilestoneContext(DustData dust, int blocksPlaced, float dustCapacity) {
        this(dust, dust, blocksPlaced, dustCapacity, 0, Set.of());
    }

    public MilestoneContext(DustData dust, int blocksPlaced, float dustCapacity, int dysonTier) {
        this(dust, dust, blocksPlaced, dustCapacity, dysonTier, Set.of());
    }

    public MilestoneContext(DustData dust, int blocksPlaced, float dustCapacity, int dysonTier, Set<ResourceLocation> itemsMade) {
        this(dust, dust, blocksPlaced, dustCapacity, dysonTier, itemsMade);
    }
}
