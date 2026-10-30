package com.voidreclaimer.ship;

import com.voidreclaimer.Config;
import com.voidreclaimer.registry.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;

/** An Ion Thruster: the main electric engine. */
public class IonThrusterBlockEntity extends ElectricThrusterBlockEntity {
    public IonThrusterBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.ION_THRUSTER.get(), pos, state);
    }

    @Override
    protected boolean enabled() {
        return Config.isEnabled("ionThruster");
    }

    @Override
    protected double baseForce() {
        return Config.ION_THRUST.get();
    }

    @Override
    protected int fePerTick() {
        return Config.ION_FE_PER_TICK.get();
    }

    @Override
    protected int feBuffer() {
        return Config.ION_FE_BUFFER.get();
    }
}
