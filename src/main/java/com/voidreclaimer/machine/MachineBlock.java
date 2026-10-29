package com.voidreclaimer.machine;

import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import com.simibubi.create.content.kinetics.base.RotatedPillarKineticBlock;
import com.simibubi.create.foundation.block.IBE;
import java.util.function.Supplier;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

/**
 * A kinetic machine block with a shaft through two opposite faces (the {@code axis} property, set when placed). The
 * machine itself lives in the block entity; stress impact is registered in {@link MachineSetup}.
 */
public class MachineBlock<T extends KineticBlockEntity> extends RotatedPillarKineticBlock implements IBE<T> {
    private final Class<T> entityClass;
    private final Supplier<BlockEntityType<? extends T>> entityType;

    public MachineBlock(Properties properties, Class<T> entityClass, Supplier<BlockEntityType<? extends T>> entityType) {
        super(properties);
        this.entityClass = entityClass;
        this.entityType = entityType;
    }

    @Override
    public boolean hasShaftTowards(LevelReader level, BlockPos pos, BlockState state, Direction face) {
        return face.getAxis() == state.getValue(AXIS);
    }

    @Override
    public Direction.Axis getRotationAxis(BlockState state) {
        return state.getValue(AXIS);
    }

    @Override
    public Class<T> getBlockEntityClass() {
        return entityClass;
    }

    @Override
    public BlockEntityType<? extends T> getBlockEntityType() {
        return entityType.get();
    }
}
