package com.voidreclaimer.ship;

import com.voidreclaimer.Config;
import com.voidreclaimer.registry.ModBlockEntities;
import dev.ryanhcode.sable.api.block.BlockEntitySubLevelActor;
import dev.ryanhcode.sable.api.physics.handle.RigidBodyHandle;
import dev.ryanhcode.sable.sublevel.ServerSubLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.energy.IEnergyStorage;

/**
 * An Ion Thruster: while redstone holds the throttle open and its buffer has Forge Energy, it pushes the ship it is built
 * into, through Sable's physics tick. The energy comes from any energy mod, through the standard capability. A short
 * supply gives a proportional push. The server tick decides the force; the physics tick only reads it.
 */
public class IonThrusterBlockEntity extends BlockEntity implements BlockEntitySubLevelActor {
    private final IEnergyStorage energy = new Energy();
    private int stored;
    private volatile double force;

    public IonThrusterBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.ION_THRUSTER.get(), pos, state);
    }

    /** Forge Energy input; nothing can be taken out. */
    public IEnergyStorage energy() {
        return energy;
    }

    public int storedEnergy() {
        return stored;
    }

    /** The force it pushes with right now (0 when not firing). */
    public double force() {
        return force;
    }

    /** Redstone throttle, 0 to 1. */
    public double throttle() {
        return level == null ? 0.0 : ThrusterMath.throttle(level.getBestNeighborSignal(worldPosition));
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, IonThrusterBlockEntity thruster) {
        double throttle = thruster.throttle();
        if (!Config.isEnabled("ionThruster") || throttle <= 0.0) {
            thruster.force = 0.0;
            return;
        }
        ThrusterMath.IonDraw draw = ThrusterMath.ionDraw(Config.ION_THRUST.get(), Config.ION_FE_PER_TICK.get(), throttle, thruster.stored);
        thruster.force = draw.force();
        if (draw.energy() > 0) {
            thruster.stored -= draw.energy();
            thruster.setChanged();
        }
    }

    @Override
    public void sable$physicsTick(ServerSubLevel subLevel, RigidBodyHandle handle, double dt) {
        double current = force;
        if (current > 0.0) {
            ThrusterPush.apply(subLevel, worldPosition, getBlockState().getValue(IonThrusterBlock.FACING).getOpposite(), current, dt);
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putInt("energy", stored);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        stored = Math.max(0, tag.getInt("energy"));
    }

    /** Takes energy up to the configured buffer; gives none back. */
    private final class Energy implements IEnergyStorage {
        @Override
        public int receiveEnergy(int maxReceive, boolean simulate) {
            int accepted = Math.max(0, Math.min(maxReceive, Config.ION_FE_BUFFER.get() - stored));
            if (!simulate && accepted > 0) {
                stored += accepted;
                setChanged();
            }
            return accepted;
        }

        @Override
        public int extractEnergy(int maxExtract, boolean simulate) {
            return 0;
        }

        @Override
        public int getEnergyStored() {
            return stored;
        }

        @Override
        public int getMaxEnergyStored() {
            return Config.ION_FE_BUFFER.get();
        }

        @Override
        public boolean canExtract() {
            return false;
        }

        @Override
        public boolean canReceive() {
            return true;
        }
    }
}
