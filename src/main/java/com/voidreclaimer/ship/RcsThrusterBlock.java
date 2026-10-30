package com.voidreclaimer.ship;

import com.voidreclaimer.registry.ModBlockEntities;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

/** The RCS Thruster placed: the Ion Thruster's block with a small jet for fine rotation. */
public class RcsThrusterBlock extends IonThrusterBlock {
    public RcsThrusterBlock(Properties properties) {
        super(properties);
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new RcsThrusterBlockEntity(pos, state);
    }

    @SuppressWarnings("unchecked")
    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (level.isClientSide() || type != ModBlockEntities.RCS_THRUSTER.get()) {
            return null;
        }
        return (BlockEntityTicker<T>) (BlockEntityTicker<RcsThrusterBlockEntity>) ElectricThrusterBlockEntity::serverTick;
    }
}
