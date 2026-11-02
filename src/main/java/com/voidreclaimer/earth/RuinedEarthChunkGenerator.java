package com.voidreclaimer.earth;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.voidreclaimer.registry.ModBlocks;
import com.voidreclaimer.space.BarrenChunkGenerator;
import java.util.concurrent.CompletableFuture;
import net.minecraft.core.HolderLookup;
import net.minecraft.world.level.LevelHeightAccessor;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.chunk.ChunkGeneratorStructureState;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.level.levelgen.blending.Blender;
import net.minecraft.world.level.levelgen.flat.FlatLevelGeneratorSettings;
import net.minecraft.world.level.levelgen.structure.StructureSet;

/**
 * The ruined Earth generator: a flat-generator subclass (like the asteroid one) that fills the columns of the
 * {@link RuinedTerrain} planet with stone under a few layers of Ashen Slag, and leaves everything outside the planet
 * void. The world seed arrives in {@link #createState}.
 */
public final class RuinedEarthChunkGenerator extends BarrenChunkGenerator {
    public static final MapCodec<RuinedEarthChunkGenerator> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            FlatLevelGeneratorSettings.CODEC.fieldOf("settings").forGetter(BarrenChunkGenerator::settings)
    ).apply(instance, instance.stable(RuinedEarthChunkGenerator::new)));

    /** Layers of slag on top of the stone. */
    public static final int SLAG_DEPTH = 3;

    private volatile long seed;

    public RuinedEarthChunkGenerator(FlatLevelGeneratorSettings settings) {
        super(settings);
    }

    @Override
    public ChunkGeneratorStructureState createState(HolderLookup<StructureSet> structureSetLookup, RandomState randomState, long seed) {
        this.seed = seed;
        return super.createState(structureSetLookup, randomState, seed);
    }

    @Override
    protected MapCodec<? extends ChunkGenerator> codec() {
        return CODEC;
    }

    @Override
    public CompletableFuture<ChunkAccess> fillFromNoise(Blender blender, RandomState randomState, StructureManager structureManager,
                                                        ChunkAccess chunk) {
        return super.fillFromNoise(blender, randomState, structureManager, chunk).thenApply(filled -> {
            fillTerrain(seed, filled);
            return filled;
        });
    }

    @Override
    public int getBaseHeight(int x, int z, Heightmap.Types type, LevelHeightAccessor level, RandomState randomState) {
        return RuinedTerrain.onPlanet(x, z) ? RuinedTerrain.surfaceY(seed, x, z) + 1 : level.getMinBuildHeight();
    }

    /**
     * Fills the planet columns of {@code chunk}, which is new and has no light yet. Blocks go straight into their sections, as vanilla's
     * noise generator does: {@code chunk.setBlockState} also updates two heightmaps a block, and here only the top of a column counts.
     * Public for tests.
     */
    public static void fillTerrain(long seed, ChunkAccess chunk) {
        int minY = chunk.getMinBuildHeight();
        BlockState bedrock = Blocks.BEDROCK.defaultBlockState();
        BlockState stone = Blocks.STONE.defaultBlockState();
        BlockState slag = ModBlocks.ASHEN_SLAG.get().defaultBlockState();
        Heightmap oceanFloor = chunk.getOrCreateHeightmapUnprimed(Heightmap.Types.OCEAN_FLOOR_WG);
        Heightmap surface = chunk.getOrCreateHeightmapUnprimed(Heightmap.Types.WORLD_SURFACE_WG);
        for (int dx = 0; dx < 16; dx++) {
            for (int dz = 0; dz < 16; dz++) {
                int x = chunk.getPos().getMinBlockX() + dx;
                int z = chunk.getPos().getMinBlockZ() + dz;
                if (!RuinedTerrain.onPlanet(x, z)) {
                    continue;
                }
                int top = Math.min(RuinedTerrain.surfaceY(seed, x, z), chunk.getMaxBuildHeight() - 1);
                for (int y = minY; y <= top; y++) {
                    BlockState state = y == minY ? bedrock : y > top - SLAG_DEPTH ? slag : stone;
                    LevelChunkSection section = chunk.getSection(chunk.getSectionIndex(y));
                    section.setBlockState(dx, y & 15, dz, state, false);
                }
                oceanFloor.update(dx, top, dz, slag);
                surface.update(dx, top, dz, slag);
            }
        }
    }
}
