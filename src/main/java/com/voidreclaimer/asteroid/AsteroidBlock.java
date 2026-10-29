package com.voidreclaimer.asteroid;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;

/** Asteroid rock of one dust kind. Only the Mechanical Drill and the Matter Harvester can break it (see {@link Drilling}). */
public class AsteroidBlock extends Block {
    private final ResourceLocation dustKind;

    public AsteroidBlock(ResourceLocation dustKind, Properties properties) {
        super(properties);
        this.dustKind = dustKind;
    }

    /** The dust kind this rock is made of, and refunds when drilled. */
    public ResourceLocation dustKind() {
        return dustKind;
    }
}
