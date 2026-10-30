package com.voidreclaimer.machine;

import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import com.voidreclaimer.Config;
import com.voidreclaimer.api.event.CollectorPowerEvent;
import com.voidreclaimer.dust.DustField;
import com.voidreclaimer.dust.RawItems;
import com.voidreclaimer.registry.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.energy.IEnergyStorage;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemStackHandler;

/**
 * The Cloud Siphon: set in a dense dust cloud and turned by a shaft, it condenses the cloud's dust into raw items in its
 * output slots. Output scales with the local density (nothing below the configured minimum) and with {@link #power()}, which
 * the shaft sets and addons can change through {@link CollectorPowerEvent}. Hoppers, funnels and belts take items out.
 */
public class CloudSiphonBlockEntity extends KineticBlockEntity {
    public static final int SLOTS = 9;
    private static final int SAMPLE_TICKS = 20;

    private final ItemStackHandler inventory = new ItemStackHandler(SLOTS);
    private final IItemHandler handler = new Handler();
    private float progress;
    private int sampleTimer;
    private double density;
    private Item item;
    private float power;
    private int stored;
    private final IEnergyStorage energy = new Energy();

    public CloudSiphonBlockEntity(net.minecraft.world.level.block.entity.BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    public CloudSiphonBlockEntity(BlockPos pos, BlockState state) {
        this(ModBlockEntities.CLOUD_SIPHON.get(), pos, state);
    }

    /** What automation sees: items come out, nothing goes in. */
    public IItemHandler items() {
        return handler;
    }

    /**
     * Forge Energy input, from any energy mod (Crafts & Additions, New Age and so on): the buffer tops the shaft's power up
     * to full while the siphon is working, so a siphon with no shaft runs on electricity alone. Nothing can be taken out.
     */
    public IEnergyStorage energy() {
        return energy;
    }

    /** FE in the buffer. */
    public int storedEnergy() {
        return stored;
    }

    /** The raw inventory, for tests and drops. */
    public ItemStackHandler inventory() {
        return inventory;
    }

    /** Cloud density at the block as of the last sample (0 outside a cloud). */
    public double density() {
        return density;
    }

    /** Power as of the last sample: the shaft's, after {@link CollectorPowerEvent}. */
    public float power() {
        return power;
    }

    @Override
    public void tick() {
        super.tick();
        if (level == null || level.isClientSide || !Config.isEnabled("cloudSiphon")) {
            return;
        }
        if (sampleTimer-- <= 0) {
            sampleTimer = SAMPLE_TICKS - 1;
            sample();
        }
        float running = power;
        if (item != null && density >= Config.SIPHON_MIN_DENSITY.get()) {
            // Only a siphon that is actually working draws energy.
            CloudSiphonMath.EnergyUse use = CloudSiphonMath.topUp(power, stored, Config.SIPHON_FE_PER_TICK.get());
            running = use.power();
            if (use.drained() > 0) {
                stored -= use.drained();
                setChanged();
            }
        }
        float perSecond = CloudSiphonMath.itemsPerSecond(density, running, Config.SIPHON_MIN_DENSITY.get(), Config.SIPHON_RATE.get());
        if (item == null || perSecond <= 0f) {
            return;
        }
        progress += perSecond / 20f;
        int whole = (int) Math.floor(progress);
        if (whole <= 0) {
            return;
        }
        ItemStack left = new ItemStack(item, whole);
        for (int slot = 0; slot < SLOTS && !left.isEmpty(); slot++) {
            left = inventory.insertItem(slot, left, false);
        }
        progress -= whole - left.getCount();
        // A full output pauses the siphon: it doesn't bank more than the one item it couldn't place.
        progress = Math.min(progress, 1f);
        setChanged();
    }

    private void sample() {
        DustField field = DustField.of(level);
        DustField.Sample sample = field == null ? DustField.Sample.NONE : field.sample(Vec3.atCenterOf(worldPosition));
        density = sample.inCloud() ? sample.density() : 0.0;
        item = sample.inCloud() ? RawItems.forKind(sample.cloud().kind()) : null;
        CollectorPowerEvent event = new CollectorPowerEvent(this, MachineMath.progressPerTick(getSpeed(), Config.MACHINE_MIN_RPM.get()));
        NeoForge.EVENT_BUS.post(event);
        power = event.getPower();
    }

    @Override
    protected void write(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
        super.write(tag, registries, clientPacket);
        tag.put("inventory", inventory.serializeNBT(registries));
        tag.putFloat("progress", progress);
        tag.putInt("energy", stored);
    }

    @Override
    protected void read(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
        super.read(tag, registries, clientPacket);
        inventory.deserializeNBT(registries, tag.getCompound("inventory"));
        progress = tag.getFloat("progress");
        stored = Math.max(0, tag.getInt("energy"));
    }

    /** Takes energy up to the configured buffer; gives none back. Off when the per-tick draw is 0. */
    private final class Energy implements IEnergyStorage {
        @Override
        public int receiveEnergy(int maxReceive, boolean simulate) {
            if (Config.SIPHON_FE_PER_TICK.get() <= 0) {
                return 0;
            }
            int accepted = Math.max(0, Math.min(maxReceive, Config.SIPHON_FE_BUFFER.get() - stored));
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
            return Config.SIPHON_FE_PER_TICK.get() <= 0 ? 0 : Config.SIPHON_FE_BUFFER.get();
        }

        @Override
        public boolean canExtract() {
            return false;
        }

        @Override
        public boolean canReceive() {
            return Config.SIPHON_FE_PER_TICK.get() > 0;
        }
    }

    /** Items can be taken out of every slot and none can be put in. */
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
            return stack;
        }

        @Override
        public ItemStack extractItem(int slot, int amount, boolean simulate) {
            return inventory.extractItem(slot, amount, simulate);
        }

        @Override
        public int getSlotLimit(int slot) {
            return inventory.getSlotLimit(slot);
        }

        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            return false;
        }
    }
}
