package com.voidreclaimer.machine;

import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import com.voidreclaimer.Config;
import com.voidreclaimer.registry.ModBlockEntities;
import com.voidreclaimer.registry.ModFluids;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;

/**
 * The Electrolyzer: spun by a shaft, it splits Water from its input tank into Oxygen Gas and Hydrogen Gas in two output
 * tanks. Pipes fill the water tank and drain the gases (see {@link #fluids()}). Does nothing below the minimum RPM, with no
 * water, with full output tanks, or when the {@code electrolyzer} toggle is off.
 */
public class ElectrolyzerBlockEntity extends KineticBlockEntity {
    private final FluidTank water = new FluidTank(Config.ELECTROLYZER_TANK.getDefault(), stack -> stack.is(Fluids.WATER));
    private final FluidTank oxygen = new FluidTank(Config.ELECTROLYZER_TANK.getDefault(), stack -> stack.is(ModFluids.OXYGEN_GAS.source().get()));
    private final FluidTank hydrogen = new FluidTank(Config.ELECTROLYZER_TANK.getDefault(), stack -> stack.is(ModFluids.HYDROGEN_GAS.source().get()));
    private final IFluidHandler handler = new Handler();
    private float progress;

    public ElectrolyzerBlockEntity(net.minecraft.world.level.block.entity.BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    public ElectrolyzerBlockEntity(BlockPos pos, BlockState state) {
        this(ModBlockEntities.ELECTROLYZER.get(), pos, state);
    }

    /** What pipes see: water goes in, the gases come out. */
    /** Progress toward the next batch, 0 to 1 (the share of the configured cycle done). */
    public float progress() {
        int cycle = Config.MACHINE_CYCLE_TICKS.get();
        return cycle <= 0 ? 0f : Math.min(1f, Math.max(0f, progress / cycle));
    }

    public IFluidHandler fluids() {
        return handler;
    }

    public FluidTank waterTank() {
        return water;
    }

    public FluidTank oxygenTank() {
        return oxygen;
    }

    public FluidTank hydrogenTank() {
        return hydrogen;
    }

    @Override
    public void tick() {
        super.tick();
        if (level == null || level.isClientSide || !Config.isEnabled("electrolyzer")) {
            return;
        }
        applyCapacity();
        float perTick = MachineMath.progressPerTick(getSpeed(), Config.MACHINE_MIN_RPM.get());
        int waterUse = Config.ELECTROLYZER_WATER.get();
        int oxygenOut = Config.ELECTROLYZER_OXYGEN.get();
        int hydrogenOut = Config.ELECTROLYZER_HYDROGEN.get();
        boolean canRun = perTick > 0f && water.getFluidAmount() >= waterUse
                && oxygen.getSpace() >= oxygenOut && hydrogen.getSpace() >= hydrogenOut;
        if (!canRun) {
            return;
        }
        progress += perTick;
        if (progress >= Config.MACHINE_CYCLE_TICKS.get()) {
            progress -= Config.MACHINE_CYCLE_TICKS.get();
            water.drain(waterUse, IFluidHandler.FluidAction.EXECUTE);
            oxygen.fill(new FluidStack(ModFluids.OXYGEN_GAS.source().get(), oxygenOut), IFluidHandler.FluidAction.EXECUTE);
            hydrogen.fill(new FluidStack(ModFluids.HYDROGEN_GAS.source().get(), hydrogenOut), IFluidHandler.FluidAction.EXECUTE);
            setChanged();
        }
    }

    /** The tank size is a config value, which isn't loaded when the block entity is constructed. */
    private void applyCapacity() {
        int capacity = Config.ELECTROLYZER_TANK.get();
        if (water.getCapacity() != capacity) {
            water.setCapacity(capacity);
            oxygen.setCapacity(capacity);
            hydrogen.setCapacity(capacity);
        }
    }

    @Override
    protected void write(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
        super.write(tag, registries, clientPacket);
        tag.put("water", water.writeToNBT(registries, new CompoundTag()));
        tag.put("oxygen", oxygen.writeToNBT(registries, new CompoundTag()));
        tag.put("hydrogen", hydrogen.writeToNBT(registries, new CompoundTag()));
        tag.putFloat("progress", progress);
    }

    @Override
    protected void read(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
        super.read(tag, registries, clientPacket);
        water.readFromNBT(registries, tag.getCompound("water"));
        oxygen.readFromNBT(registries, tag.getCompound("oxygen"));
        hydrogen.readFromNBT(registries, tag.getCompound("hydrogen"));
        progress = tag.getFloat("progress");
    }

    /** Tank 0 takes water only; tanks 1 and 2 give out gas only. */
    private final class Handler implements IFluidHandler {
        @Override
        public int getTanks() {
            return 3;
        }

        private FluidTank tank(int index) {
            return index == 0 ? water : index == 1 ? oxygen : hydrogen;
        }

        @Override
        public FluidStack getFluidInTank(int tank) {
            return tank(tank).getFluid();
        }

        @Override
        public int getTankCapacity(int tank) {
            return tank(tank).getCapacity();
        }

        @Override
        public boolean isFluidValid(int tank, FluidStack stack) {
            return tank == 0 && water.isFluidValid(stack);
        }

        @Override
        public int fill(FluidStack resource, FluidAction action) {
            int filled = water.fill(resource, action);
            if (filled > 0 && action.execute()) {
                setChanged();
            }
            return filled;
        }

        @Override
        public FluidStack drain(FluidStack resource, FluidAction action) {
            FluidStack drained = oxygen.drain(resource, action);
            if (drained.isEmpty()) {
                drained = hydrogen.drain(resource, action);
            }
            if (!drained.isEmpty() && action.execute()) {
                setChanged();
            }
            return drained;
        }

        @Override
        public FluidStack drain(int maxDrain, FluidAction action) {
            FluidStack drained = hydrogen.drain(maxDrain, action);
            if (drained.isEmpty()) {
                drained = oxygen.drain(maxDrain, action);
            }
            if (!drained.isEmpty() && action.execute()) {
                setChanged();
            }
            return drained;
        }
    }
}
