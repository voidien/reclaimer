package com.voidreclaimer.mixin;

import net.minecraft.core.Holder;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.level.chunk.PalettedContainerRO;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/** A section's biomes, for a chunk that has only one and need not write it 64 times ({@code BarrenChunkGenerator}). */
@Mixin(LevelChunkSection.class)
public interface LevelChunkSectionAccessor {
    @Accessor("biomes")
    void void_reclaimer$setBiomes(PalettedContainerRO<Holder<Biome>> biomes);
}
