package com.voidreclaimer.space;

import com.voidreclaimer.VoidReclaimer;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.dimension.DimensionType;

/** Keys for the datapack-defined deep space dimension (see data/void_reclaimer/dimension*). */
public final class ModDimensions {
    public static final ResourceKey<Level> DEEP_SPACE =
            ResourceKey.create(Registries.DIMENSION, VoidReclaimer.id("deep_space"));
    public static final ResourceKey<DimensionType> DEEP_SPACE_TYPE =
            ResourceKey.create(Registries.DIMENSION_TYPE, VoidReclaimer.id("deep_space"));
    public static final ResourceKey<Biome> DEEP_SPACE_BIOME =
            ResourceKey.create(Registries.BIOME, VoidReclaimer.id("deep_space"));

    private ModDimensions() {}

    public static boolean isDeepSpace(Level level) {
        return level.dimension() == DEEP_SPACE;
    }
}
