package com.voidreclaimer.earth;

import com.voidreclaimer.VoidReclaimer;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.dimension.DimensionType;

/** Keys for the datapack-defined ruined Earth dimension (see data/void_reclaimer/dimension*). */
public final class RuinedEarth {
    public static final ResourceKey<Level> DIMENSION =
            ResourceKey.create(Registries.DIMENSION, VoidReclaimer.id("ruined_earth"));
    public static final ResourceKey<DimensionType> TYPE =
            ResourceKey.create(Registries.DIMENSION_TYPE, VoidReclaimer.id("ruined_earth"));
    public static final ResourceKey<Biome> BIOME =
            ResourceKey.create(Registries.BIOME, VoidReclaimer.id("ruined_earth"));

    private RuinedEarth() {}

    public static boolean is(Level level) {
        return level.dimension() == DIMENSION;
    }
}
