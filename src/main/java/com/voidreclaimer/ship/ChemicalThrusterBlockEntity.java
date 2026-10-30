package com.voidreclaimer.ship;

import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import com.voidreclaimer.Config;
import com.voidreclaimer.api.ship.ThrusterFuel;
import com.voidreclaimer.machine.MachineMath;
import com.voidreclaimer.registry.ModBlockEntities;
import com.voidreclaimer.registry.ModDataMaps;
import dev.ryanhcode.sable.api.block.BlockEntitySubLevelActor;
import dev.ryanhcode.sable.api.physics.force.ForceGroups;
import dev.ryanhcode.sable.api.physics.handle.RigidBodyHandle;
import dev.ryanhcode.sable.sublevel.ServerSubLevel;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;
import org.joml.Vector3d;

/**
 * A Chemical Thruster: while redstone holds the throttle open, the shaft turns and the tank holds a fuel from the
 * {@code thruster_fuel} data map, it burns fuel and pushes the ship it is built into, through Sable's physics tick. The
 * force acts at the block, so one placed off-centre also turns the ship. The server tick decides the force; the physics
 * tick only reads it.
 */
public class ChemicalThrusterBlockEntity extends KineticBlockEntity implements BlockEntitySubLevelActor {
    private final FluidTank tank = new FluidTank(Config.THRUSTER_TANK.getDefault(), stack -> fuelOf(stack) != null);
    /** Fractions of a mB already burned but not yet taken from the tank. */
    private double burnDebt;
    private volatile double force;

    public ChemicalThrusterBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    public ChemicalThrusterBlockEntity(BlockPos pos, BlockState state) {
        this(ModBlockEntities.CHEMICAL_THRUSTER.get(), pos, state);
    }

    @Nullable
    private static ThrusterFuel fuelOf(FluidStack stack) {
        return stack.isEmpty() ? null : stack.getFluidHolder().getData(ModDataMaps.THRUSTER_FUEL);
    }

    /** What pipes see: only fuels go in. */
    public IFluidHandler fluids() {
        return tank;
    }

    public FluidTank tank() {
        return tank;
    }

    /** The force it pushes with right now (0 when not firing). */
    public double force() {
        return force;
    }

    /** Redstone throttle, 0 to 1. */
    public double throttle() {
        return level == null ? 0.0 : ThrusterMath.throttle(level.getBestNeighborSignal(worldPosition));
    }

    /** The way the ship is pushed: away from the nozzle. */
    public Direction pushDirection() {
        return getBlockState().getValue(ChemicalThrusterBlock.FACING).getOpposite();
    }

    @Override
    public void tick() {
        super.tick();
        if (level == null || level.isClientSide) {
            return;
        }
        if (tank.getCapacity() != Config.THRUSTER_TANK.get()) {
            tank.setCapacity(Config.THRUSTER_TANK.get());
        }
        ThrusterFuel fuel = fuelOf(tank.getFluid());
        double throttle = throttle();
        double spin = MachineMath.progressPerTick(getSpeed(), Config.MACHINE_MIN_RPM.get());
        if (!Config.isEnabled("chemicalThruster") || fuel == null || throttle <= 0.0 || spin <= 0.0) {
            force = 0.0;
            return;
        }
        burnDebt += ThrusterMath.burn(fuel.burn(), throttle, spin);
        int whole = (int) Math.floor(burnDebt);
        if (whole > 0) {
            burnDebt -= tank.drain(whole, IFluidHandler.FluidAction.EXECUTE).getAmount();
            setChanged();
        }
        force = tank.isEmpty() ? 0.0 : ThrusterMath.chemicalForce(Config.CHEMICAL_THRUST.get(), fuel.thrust(), throttle, spin);
    }

    @Override
    public void sable$physicsTick(ServerSubLevel subLevel, RigidBodyHandle handle, double dt) {
        double current = force;
        if (current <= 0.0) {
            return;
        }
        Direction push = pushDirection();
        Vector3d impulse = new Vector3d(push.getStepX(), push.getStepY(), push.getStepZ()).mul(current * dt);
        Vector3d at = new Vector3d(worldPosition.getX() + 0.5, worldPosition.getY() + 0.5, worldPosition.getZ() + 0.5);
        subLevel.getOrCreateQueuedForceGroup(ForceGroups.PROPULSION.get()).applyAndRecordPointForce(at, impulse);
    }

    @Override
    protected void write(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
        super.write(tag, registries, clientPacket);
        tag.put("tank", tank.writeToNBT(registries, new CompoundTag()));
        tag.putDouble("burn_debt", burnDebt);
    }

    @Override
    protected void read(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
        super.read(tag, registries, clientPacket);
        tank.readFromNBT(registries, tag.getCompound("tank"));
        burnDebt = tag.getDouble("burn_debt");
    }
}
