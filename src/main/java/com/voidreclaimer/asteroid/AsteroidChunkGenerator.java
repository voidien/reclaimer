package com.voidreclaimer.asteroid;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.voidreclaimer.Config;
import com.voidreclaimer.registry.ModBlocks;
import com.voidreclaimer.space.BarrenChunkGenerator;
import java.util.concurrent.CompletableFuture;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.chunk.ChunkGeneratorStructureState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.level.levelgen.blending.Blender;
import net.minecraft.world.level.levelgen.flat.FlatLevelGeneratorSettings;
import net.minecraft.world.level.levelgen.structure.StructureSet;
import net.minecraft.world.phys.Vec3;

/**
 * The deep space generator: the flat void generator with the asteroids of an {@link AsteroidField} carved in. Giants of
 * hundreds of blocks work because nothing here is a structure (structures only reach about 128 blocks from their start
 * chunk): each chunk asks the field about its own cell and places the blocks of the bodies that cross it.
 *
 * <p>The world seed arrives in {@link #createState}, which runs when the level is created, before any chunk is made.
 * Sodium is not involved: these are ordinary blocks in ordinary chunks.
 */
public final class AsteroidChunkGenerator extends BarrenChunkGenerator {
    public static final MapCodec<AsteroidChunkGenerator> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            FlatLevelGeneratorSettings.CODEC.fieldOf("settings").forGetter(BarrenChunkGenerator::settings)
    ).apply(instance, instance.stable(AsteroidChunkGenerator::new)));

    /** Clear space around the deep space spawn, from the nearest edge of any body. */
    public static final double SPAWN_CLEARANCE = 300;

    private volatile long seed;
    @Nullable
    private volatile AsteroidField field;

    public AsteroidChunkGenerator(FlatLevelGeneratorSettings settings) {
        super(settings);
    }

    @Override
    public ChunkGeneratorStructureState createState(HolderLookup<StructureSet> structureSetLookup, RandomState randomState, long seed) {
        this.seed = seed;
        this.field = null;
        return super.createState(structureSetLookup, randomState, seed);
    }

    @Override
    protected MapCodec<? extends ChunkGenerator> codec() {
        return CODEC;
    }

    private AsteroidField field() {
        Vec3 spawn = new Vec3(Config.SPAWN_X.get() + 0.5, Config.SPAWN_Y.get(), Config.SPAWN_Z.get() + 0.5);
        AsteroidField current = field;
        if (current == null || !current.spawn().equals(spawn)) {
            current = new AsteroidField(seed, spawn, SPAWN_CLEARANCE);
            field = current;
        }
        return current;
    }

    @Override
    public CompletableFuture<ChunkAccess> fillFromNoise(Blender blender, RandomState randomState, StructureManager structureManager,
                                                        ChunkAccess chunk) {
        return super.fillFromNoise(blender, randomState, structureManager, chunk).thenApply(filled -> {
            fillAsteroids(field(), filled);
            return filled;
        });
    }

    /** Places the blocks of every body that crosses {@code chunk}. Public for tests. */
    public static void fillAsteroids(AsteroidField field, ChunkAccess chunk) {
        int minX = chunk.getPos().getMinBlockX();
        int minZ = chunk.getPos().getMinBlockZ();
        int minY = chunk.getMinBuildHeight();
        int maxY = chunk.getMaxBuildHeight() - 1;
        int cx = Math.floorDiv(minX, AsteroidField.CELL);
        int cz = Math.floorDiv(minZ, AsteroidField.CELL);
        Heightmap oceanFloor = chunk.getOrCreateHeightmapUnprimed(Heightmap.Types.OCEAN_FLOOR_WG);
        Heightmap surface = chunk.getOrCreateHeightmapUnprimed(Heightmap.Types.WORLD_SURFACE_WG);
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        for (int cy = Math.floorDiv(minY, AsteroidField.CELL); cy <= Math.floorDiv(maxY, AsteroidField.CELL); cy++) {
            for (AsteroidBody body : field.bodiesIn(cx, cy, cz)) {
                double extent = body.maxExtent();
                int fromX = Math.max(minX, Mth.floor(body.center().x - extent));
                int toX = Math.min(minX + 15, Mth.ceil(body.center().x + extent));
                int fromZ = Math.max(minZ, Mth.floor(body.center().z - extent));
                int toZ = Math.min(minZ + 15, Mth.ceil(body.center().z + extent));
                int fromY = Math.max(minY, Mth.floor(body.center().y - extent));
                int toY = Math.min(maxY, Mth.ceil(body.center().y + extent));
                if (fromX > toX || fromY > toY || fromZ > toZ) {
                    continue;
                }
                BlockState state = blockFor(body.kind());
                for (int x = fromX; x <= toX; x++) {
                    for (int z = fromZ; z <= toZ; z++) {
                        for (int y = fromY; y <= toY; y++) {
                            if (body.contains(x + 0.5, y + 0.5, z + 0.5)) {
                                chunk.setBlockState(pos.set(x, y, z), state, false);
                                oceanFloor.update(x & 15, y, z & 15, state);
                                surface.update(x & 15, y, z & 15, state);
                            }
                        }
                    }
                }
            }
        }
    }

    /** The asteroid block of a dust kind. */
    public static BlockState blockFor(ResourceLocation kind) {
        return ModBlocks.asteroidBlock(kind).defaultBlockState();
    }
}
