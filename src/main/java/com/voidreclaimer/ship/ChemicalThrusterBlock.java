package com.voidreclaimer.ship;

import com.simibubi.create.content.kinetics.base.DirectionalKineticBlock;
import com.simibubi.create.foundation.block.IBE;
import com.voidreclaimer.registry.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

/**
 * The Chemical Thruster: its nozzle points along {@code facing} and pushes the ship the other way. The shaft goes in at
 * the back. Redstone is the throttle. The machine itself is in {@link ChemicalThrusterBlockEntity}.
 */
public class ChemicalThrusterBlock extends DirectionalKineticBlock implements IBE<ChemicalThrusterBlockEntity> {
    public ChemicalThrusterBlock(Properties properties) {
        super(properties);
    }

    @Override
    public boolean hasShaftTowards(LevelReader level, BlockPos pos, BlockState state, Direction face) {
        return face == state.getValue(FACING).getOpposite();
    }

    @Override
    public Direction.Axis getRotationAxis(BlockState state) {
        return state.getValue(FACING).getAxis();
    }

    @Override
    public Class<ChemicalThrusterBlockEntity> getBlockEntityClass() {
        return ChemicalThrusterBlockEntity.class;
    }

    @Override
    public BlockEntityType<? extends ChemicalThrusterBlockEntity> getBlockEntityType() {
        return ModBlockEntities.CHEMICAL_THRUSTER.get();
    }
}
