package com.voidreclaimer.space;

import com.google.common.base.Suppliers;
import com.voidreclaimer.mixin.LevelChunkSectionAccessor;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.IdMap;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.level.chunk.PalettedContainer;
import net.minecraft.world.level.levelgen.FlatLevelSource;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.level.levelgen.blending.Blender;
import net.minecraft.world.level.levelgen.flat.FlatLevelGeneratorSettings;

/**
 * A flat generator with one biome and usually nothing to decorate, as deep space and ruined Earth are. For every section vanilla starts
 * from plains, writes the real biome into all 64 cells, which grows the palette, and later reads them all back to find features to
 * place. With deep space's 254 sections that was over a third of what making a chunk cost, and it left a useless plains entry in every
 * saved section. Here each section gets its one biome outright. A dimension that turns on lakes, features or structures still gets
 * vanilla's decoration pass.
 */
public abstract class BarrenChunkGenerator extends FlatLevelSource {
    private final Supplier<Boolean> decorates = Suppliers.memoize(this::findDecoration);

    protected BarrenChunkGenerator(FlatLevelGeneratorSettings settings) {
        super(settings);
    }

    @Override
    public CompletableFuture<ChunkAccess> createBiomes(RandomState randomState, Blender blender, StructureManager structureManager,
                                                       ChunkAccess chunk) {
        IdMap<Holder<Biome>> ids = structureManager.registryAccess().registryOrThrow(Registries.BIOME).asHolderIdMap();
        Holder<Biome> biome = settings().getBiome();
        for (LevelChunkSection section : chunk.getSections()) {
            ((LevelChunkSectionAccessor) section).void_reclaimer$setBiomes(new PalettedContainer<>(ids, biome, PalettedContainer.Strategy.SECTION_BIOMES));
        }
        return CompletableFuture.completedFuture(chunk);
    }

    @Override
    public void applyBiomeDecoration(WorldGenLevel level, ChunkAccess chunk, StructureManager structureManager) {
        if (decorates()) {
            super.applyBiomeDecoration(level, chunk, structureManager);
        }
    }

    /** Whether these settings have anything to place. No structure overrides at all means every structure set, as vanilla reads it. */
    public boolean decorates() {
        return decorates.get();
    }

    private boolean findDecoration() {
        boolean structures = settings().structureOverrides().map(sets -> sets.size() > 0).orElse(true);
        return structures || getBiomeGenerationSettings(settings().getBiome()).features().stream().anyMatch(BarrenChunkGenerator::hasAny);
    }

    private static boolean hasAny(HolderSet<?> features) {
        return features.size() > 0;
    }
}
