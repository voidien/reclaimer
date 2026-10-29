package com.voidreclaimer.machine;

import com.voidreclaimer.registry.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemStackHandler;

/**
 * The Payload Chamber holds the payloads waiting for the Mass Driver below it. Belts, funnels, arms and hoppers insert
 * (payload items only); only the driver takes them out.
 */
public class PayloadChamberBlockEntity extends BlockEntity {
    public static final int SLOTS = 2;
    public static final int SLOT_LIMIT = 16;

    private final ItemStackHandler inventory = new ItemStackHandler(SLOTS) {
        @Override
        public int getSlotLimit(int slot) {
            return SLOT_LIMIT;
        }

        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            return MassDriver.isPayload(stack);
        }

        @Override
        protected void onContentsChanged(int slot) {
            setChanged();
        }
    };
    private final IItemHandler handler = new Handler();

    public PayloadChamberBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.PAYLOAD_CHAMBER.get(), pos, state);
    }

    /** What automation sees: payloads go in, nothing comes out. */
    public IItemHandler items() {
        return handler;
    }

    /** The raw inventory, for the driver, tests and drops. */
    public ItemStackHandler inventory() {
        return inventory;
    }

    /** The first slot that holds a payload, or -1. */
    public int firstPayloadSlot() {
        for (int i = 0; i < SLOTS; i++) {
            if (!inventory.getStackInSlot(i).isEmpty()) {
                return i;
            }
        }
        return -1;
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.put("inventory", inventory.serializeNBT(registries));
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        inventory.deserializeNBT(registries, tag.getCompound("inventory"));
    }

    private final class Handler implements IItemHandler {
        @Override
        public int getSlots() {
            return SLOTS;
        }

        @Override
        public ItemStack getStackInSlot(int slot) {
            return inventory.getStackInSlot(slot);
        }

        @Override
        public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
            return inventory.insertItem(slot, stack, simulate);
        }

        @Override
        public ItemStack extractItem(int slot, int amount, boolean simulate) {
            return ItemStack.EMPTY;
        }

        @Override
        public int getSlotLimit(int slot) {
            return SLOT_LIMIT;
        }

        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            return inventory.isItemValid(slot, stack);
        }
    }
}
