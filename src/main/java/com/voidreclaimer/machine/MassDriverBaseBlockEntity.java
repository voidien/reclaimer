package com.voidreclaimer.machine;

import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import com.voidreclaimer.registry.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

/**
 * The Kinetic Drive Base of the old three-block Mass Driver. Inert since the Launch Complex replaced it: it still turns
 * with the shaft and remembers its old count, but it launches nothing. Kept so old worlds load and the blocks can be
 * picked up.
 */
public class MassDriverBaseBlockEntity extends KineticBlockEntity {
    private float progress;
    private int launches;

    public MassDriverBaseBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    public MassDriverBaseBlockEntity(BlockPos pos, BlockState state) {
        this(ModBlockEntities.MASS_DRIVER_BASE.get(), pos, state);
    }

    /** Wind-up of the next launch, 0 to 1. */
    public float progress() {
        return progress;
    }

    /** Payloads this driver has fired. */
    public int launches() {
        return launches;
    }

    @Override
    public void tick() {
        super.tick();
    }

    @Override
    protected void write(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
        super.write(tag, registries, clientPacket);
        tag.putFloat("progress", progress);
        tag.putInt("launches", launches);
    }

    @Override
    protected void read(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
        super.read(tag, registries, clientPacket);
        progress = tag.getFloat("progress");
        launches = tag.getInt("launches");
    }
}
