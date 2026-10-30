package com.voidreclaimer.ship;

import com.voidreclaimer.Config;
import com.voidreclaimer.registry.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;

/** An RCS Thruster: a small electric jet. Placed away from the centre of mass, it turns the ship. */
public class RcsThrusterBlockEntity extends ElectricThrusterBlockEntity {
    public RcsThrusterBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.RCS_THRUSTER.get(), pos, state);
    }

    @Override
    protected boolean enabled() {
        return Config.isEnabled("rcsPush");
    }

    @Override
    protected double baseForce() {
        return Config.RCS_THRUST.get();
    }

    @Override
    protected int fePerTick() {
        return Config.RCS_FE_PER_TICK.get();
    }

    @Override
    protected int feBuffer() {
        return Config.RCS_FE_BUFFER.get();
    }
}
