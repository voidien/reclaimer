package com.voidreclaimer.terraform;

import com.voidreclaimer.api.terraform.TerraformProfile;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.QuartPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.levelgen.Heightmap;

/**
 * The world edits of a Genesis Pulse: rewriting a column's blocks by a {@link TerraformProfile}, and rewriting the biome
 * of a whole square area and resending its chunks like {@code /fillbiome} does (so Sodium rebuilds them from the
 * ordinary packets).
 */
public final class Terraforming {
    /** How many blocks below the surface a pulse rewrites. */
    public static final int DEPTH = 5;

    private Terraforming() {}

    /** Rewrites the top {@link #DEPTH} blocks of the column at (x, z); returns how many blocks changed. */
    public static int swapColumn(ServerLevel level, TerraformProfile profile, int x, int z) {
        int top = level.getHeight(Heightmap.Types.WORLD_SURFACE, x, z) - 1;
        int bottom = Math.max(level.getMinBuildHeight(), top - DEPTH + 1);
        int changed = 0;
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        for (int y = top; y >= bottom; y--) {
            BlockState state = level.getBlockState(pos.set(x, y, z));
            if (state.isAir()) {
                continue;
            }
            boolean exposed = level.getBlockState(pos.set(x, y + 1, z)).isAir();
            Optional<BlockState> replacement = profile.replacement(state, exposed);
            if (replacement.isPresent() && !replacement.get().equals(state)) {
                level.setBlock(pos.set(x, y, z), replacement.get(), Block.UPDATE_CLIENTS);
                changed++;
            }
        }
        return changed;
    }

    /**
     * Sets the biome of every cell whose center lies within {@code radius} blocks (square) of {@code center}, in every
     * chunk the area touches, then resends those chunks. Returns how many chunks were resent.
     */
    public static int rewriteBiomes(ServerLevel level, BlockPos center, int radius, Holder<net.minecraft.world.level.biome.Biome> biome) {
        int minX = center.getX() - radius;
        int maxX = center.getX() + radius;
        int minZ = center.getZ() - radius;
        int maxZ = center.getZ() + radius;
        List<ChunkAccess> chunks = new ArrayList<>();
        for (int chunkX = Mth.floorDiv(minX, 16); chunkX <= Mth.floorDiv(maxX, 16); chunkX++) {
            for (int chunkZ = Mth.floorDiv(minZ, 16); chunkZ <= Mth.floorDiv(maxZ, 16); chunkZ++) {
                ChunkAccess chunk = level.getChunk(chunkX, chunkZ);
                chunk.fillBiomesFromNoise((quartX, quartY, quartZ, sampler) -> {
                    int blockX = QuartPos.toBlock(quartX) + 2;
                    int blockZ = QuartPos.toBlock(quartZ) + 2;
                    boolean inside = blockX >= minX && blockX <= maxX && blockZ >= minZ && blockZ <= maxZ;
                    return inside ? biome : chunk.getNoiseBiome(quartX, quartY, quartZ);
                }, level.getChunkSource().randomState().sampler());
                chunk.setUnsaved(true);
                chunks.add(chunk);
            }
        }
        level.getChunkSource().chunkMap.resendBiomesForChunks(chunks);
        return chunks.size();
    }
}
