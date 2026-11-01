package com.voidreclaimer.client.render.holo;

import java.util.Map;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.ColorResolver;
import net.minecraft.world.level.EmptyBlockGetter;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LightChunk;
import net.minecraft.world.level.chunk.LightChunkGetter;
import net.minecraft.world.level.lighting.LevelLightEngine;
import net.minecraft.world.level.material.FluidState;

/** A ship's blocks as a block model needs to see them, copied, so that it can be meshed away from the render thread. */
record HoloShipRegion(Map<BlockPos, BlockState> blocks) implements BlockAndTintGetter {
    /** A hologram is drawn at full brightness, so a mesh needs no light: this engine has neither layer and always reads dark. */
    private static final LevelLightEngine NO_LIGHT = new LevelLightEngine(new LightChunkGetter() {
        @Nullable
        @Override
        public LightChunk getChunkForLighting(int chunkX, int chunkZ) {
            return null;
        }

        @Override
        public BlockGetter getLevel() {
            return EmptyBlockGetter.INSTANCE;
        }
    }, false, false);

    @Override
    public BlockState getBlockState(BlockPos pos) {
        return blocks.getOrDefault(pos, Blocks.AIR.defaultBlockState());
    }

    @Override
    public FluidState getFluidState(BlockPos pos) {
        return getBlockState(pos).getFluidState();
    }

    @Nullable
    @Override
    public BlockEntity getBlockEntity(BlockPos pos) {
        return null;
    }

    @Override
    public float getShade(Direction direction, boolean shade) {
        return 1f;
    }

    @Override
    public LevelLightEngine getLightEngine() {
        return NO_LIGHT;
    }

    @Override
    public int getBlockTint(BlockPos pos, ColorResolver resolver) {
        return -1;
    }

    @Override
    public int getHeight() {
        return 4064;
    }

    @Override
    public int getMinBuildHeight() {
        return -2032;
    }
}
