package com.voidreclaimer.machine;

import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import com.voidreclaimer.Config;
import com.voidreclaimer.registry.ModBlockEntities;
import com.voidreclaimer.registry.ModMaterials;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemStackHandler;

/**
 * The Spinner: spun by a shaft, it draws High-Purity Graphite from its input slot and spins it into Carbon Thread in its
 * output slot. Hoppers, funnels and belts see {@link #items()}: graphite goes in, thread comes out.
 */
public class SpinnerBlockEntity extends KineticBlockEntity {
    private static final int INPUT = 0;
    private static final int OUTPUT = 1;

    private final ItemStackHandler inventory = new ItemStackHandler(2);
    private final IItemHandler handler = new Handler();
    private float progress;

    public SpinnerBlockEntity(net.minecraft.world.level.block.entity.BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    public SpinnerBlockEntity(BlockPos pos, BlockState state) {
        this(ModBlockEntities.SPINNER.get(), pos, state);
    }

    /** What automation sees: graphite goes in, thread comes out. */
    /** Progress toward the next thread, 0 to 1 (the share of the configured cycle done). */
    public float progress() {
        int cycle = Config.MACHINE_CYCLE_TICKS.get();
        return cycle <= 0 ? 0f : Math.min(1f, Math.max(0f, progress / cycle));
    }

    public IItemHandler items() {
        return handler;
    }

    /** The raw inventory (slot 0 input, slot 1 output), for tests and drops. */
    public ItemStackHandler inventory() {
        return inventory;
    }

    private static ItemStack thread(int count) {
        return new ItemStack(ModMaterials.CARBON_THREAD.get(), count);
    }

    @Override
    public void tick() {
        super.tick();
        if (level == null || level.isClientSide || !Config.isEnabled("spinner")) {
            return;
        }
        float perTick = MachineMath.progressPerTick(getSpeed(), Config.MACHINE_MIN_RPM.get());
        int threads = Config.SPINNER_THREADS.get();
        ItemStack input = inventory.getStackInSlot(INPUT);
        ItemStack output = inventory.getStackInSlot(OUTPUT);
        boolean canRun = perTick > 0f && input.is(ModMaterials.HIGH_PURITY_GRAPHITE.get())
                && (output.isEmpty() || output.is(ModMaterials.CARBON_THREAD.get()) && output.getCount() + threads <= output.getMaxStackSize());
        if (!canRun) {
            return;
        }
        progress += perTick;
        if (progress >= Config.MACHINE_CYCLE_TICKS.get()) {
            progress -= Config.MACHINE_CYCLE_TICKS.get();
            inventory.extractItem(INPUT, 1, false);
            inventory.insertItem(OUTPUT, thread(threads), false);
            setChanged();
        }
    }

    @Override
    protected void write(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
        super.write(tag, registries, clientPacket);
        tag.put("inventory", inventory.serializeNBT(registries));
        tag.putFloat("progress", progress);
    }

    @Override
    protected void read(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
        super.read(tag, registries, clientPacket);
        inventory.deserializeNBT(registries, tag.getCompound("inventory"));
        progress = tag.getFloat("progress");
    }

    /** Slot 0 takes graphite only and can't be taken from; slot 1 gives thread only and can't be filled. */
    private final class Handler implements IItemHandler {
        @Override
        public int getSlots() {
            return 2;
        }

        @Override
        public ItemStack getStackInSlot(int slot) {
            return inventory.getStackInSlot(slot);
        }

        @Override
        public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
            return slot == INPUT && isItemValid(slot, stack) ? inventory.insertItem(INPUT, stack, simulate) : stack;
        }

        @Override
        public ItemStack extractItem(int slot, int amount, boolean simulate) {
            return slot == OUTPUT ? inventory.extractItem(OUTPUT, amount, simulate) : ItemStack.EMPTY;
        }

        @Override
        public int getSlotLimit(int slot) {
            return inventory.getSlotLimit(slot);
        }

        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            return slot == INPUT && stack.is(ModMaterials.HIGH_PURITY_GRAPHITE.get());
        }
    }
}
