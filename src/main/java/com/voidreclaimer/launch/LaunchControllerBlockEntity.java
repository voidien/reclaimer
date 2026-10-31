package com.voidreclaimer.launch;

import com.simibubi.create.content.kinetics.base.HorizontalKineticBlock;
import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import com.voidreclaimer.Config;
import com.voidreclaimer.api.event.MassDriverLaunchEvent;
import com.voidreclaimer.dyson.Dyson;
import com.voidreclaimer.machine.MassDriver;
import com.voidreclaimer.machine.MassDriverMath;
import com.voidreclaimer.network.LaunchStreakPayload;
import com.voidreclaimer.registry.ModBlockEntities;
import com.voidreclaimer.registry.ModSounds;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.Mth;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemStackHandler;
import net.neoforged.neoforge.network.PacketDistributor;
import software.bernie.geckolib.animatable.GeoBlockEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.PlayState;
import software.bernie.geckolib.util.GeckoLibUtil;

/**
 * The Launch Controller's block entity: the launch complex's payload store and its firing logic. Spun by a shaft while
 * assembled and loaded, it winds up ({@link MassDriverMath#launchTicks}) and fires one payload from the pad's gun into
 * the Dyson Sphere. Below the minimum speed, unassembled, or with the {@code massDriver} toggle off it does nothing.
 * Belts, funnels, arms and hoppers insert payloads from any side; nothing takes them out. The whole complex is drawn
 * from this block entity (GeckoLib, {@code geo/entity/mass_driver.geo.json}); the wind-up and the last launch are synced
 * every {@value #SYNC_TICKS} ticks for its code animation.
 */
public class LaunchControllerBlockEntity extends KineticBlockEntity implements GeoBlockEntity {
    public static final int SLOTS = 2;
    public static final int SLOT_LIMIT = 16;
    /** How often an assembled complex checks its ring and pad. */
    static final int CHECK_TICKS = 20;
    /** How often the wind-up is synced to the client while it builds. */
    static final int SYNC_TICKS = 10;

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
    private boolean assembled;
    private float progress;
    private int launches;
    private int checkIn;
    private int syncIn;
    /** Game time of the last launch, -1 before any; synced for the renderer's recoil. */
    private long lastLaunch = -1L;
    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    /** Client only: the wind-up as drawn, eased towards the synced value so the gun moves smoothly between updates. */
    private float shownProgress;
    private float shownAt;

    public LaunchControllerBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    public LaunchControllerBlockEntity(BlockPos pos, BlockState state) {
        this(ModBlockEntities.LAUNCH_CONTROLLER.get(), pos, state);
    }

    /** The way the pad lies from the controller. */
    public Direction facing() {
        return getBlockState().getValue(HorizontalKineticBlock.HORIZONTAL_FACING);
    }

    /** Whether the ring was complete and the pad filled the last time the complex was assembled. */
    public boolean assembled() {
        return assembled;
    }

    /** Wind-up of the next launch, 0 to 1. */
    public float progress() {
        return progress;
    }

    /** Payloads this complex has fired. */
    public int launches() {
        return launches;
    }

    /** Game time of the last launch, or -1 before any. */
    public long lastLaunch() {
        return lastLaunch;
    }

    /** Client only: the wind-up to draw, eased towards the last synced {@link #progress()}; a launch (progress back to 0) snaps. */
    public float shownProgress(float partialTick) {
        float now = (level == null ? 0L : level.getGameTime()) + partialTick;
        float dt = Mth.clamp(now - shownAt, 0f, 20f);
        shownAt = now;
        shownProgress = progress < shownProgress ? progress : Mth.lerp(Math.min(1f, dt * 0.15f), shownProgress, progress);
        return shownProgress;
    }

    /** What automation sees: payloads go in, nothing comes out. */
    public IItemHandler items() {
        return handler;
    }

    /** The raw inventory, for the controller itself, tests and drops. */
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

    void setAssembled(boolean value) {
        if (assembled == value) {
            return;
        }
        assembled = value;
        progress = 0f;
        checkIn = 0;
        setChanged();
        if (level != null) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        }
    }

    @Override
    public void tick() {
        super.tick();
        if (!(level instanceof ServerLevel server)) {
            return;
        }
        if (assembled && ++checkIn >= CHECK_TICKS) {
            checkIn = 0;
            LaunchComplex.check(this);
        }
        if (!assembled || !Config.isEnabled("massDriver")) {
            return;
        }
        int slot = firstPayloadSlot();
        float perTick = MassDriverMath.progressPerTick(getSpeed(), Config.MASS_DRIVER_MIN_RPM.get(), Config.MASS_DRIVER_FAST_RPM.get(),
                Config.MASS_DRIVER_SLOW_TICKS.get(), Config.MASS_DRIVER_FAST_TICKS.get());
        if (slot < 0 || perTick <= 0f) {
            return;
        }
        progress += perTick;
        if (++syncIn >= SYNC_TICKS) {
            syncIn = 0;
            sendData();
        }
        if (progress >= 1f) {
            progress = 0f;
            fire(server, slot);
        }
    }

    private void fire(ServerLevel server, int slot) {
        ItemStack payload = inventory.extractItem(slot, 1, false);
        if (payload.isEmpty()) {
            return;
        }
        boolean satellite = MassDriver.isSatellite(payload);
        boolean truss = MassDriver.isTruss(payload);
        MassDriverLaunchEvent event = new MassDriverLaunchEvent(server, worldPosition, payload.copy(), satellite ? 1 : 0, truss ? 1 : 0);
        NeoForge.EVENT_BUS.post(event);
        launches++;
        lastLaunch = server.getGameTime();
        setChanged();
        sendData();
        Vec3 muzzle = LaunchPad.muzzle(worldPosition, facing());
        server.playSound(null, muzzle.x, muzzle.y, muzzle.z, ModSounds.MASS_DRIVER_LAUNCH.get(), SoundSource.BLOCKS, 2f, 1f);
        server.sendParticles(ParticleTypes.CLOUD, muzzle.x, muzzle.y + 0.5, muzzle.z, 32, 0.4, 0.2, 0.4, 0.1);
        if (event.isCanceled()) {
            return;
        }
        if (event.getSatellites() > 0) {
            Dyson.launchSatellites(server.getServer(), event.getSatellites());
        }
        if (event.getTrusses() > 0) {
            Dyson.launchTrusses(server.getServer(), event.getTrusses());
        }
        PacketDistributor.sendToAllPlayers(new LaunchStreakPayload(BlockPos.containing(muzzle), truss && !satellite));
    }

    @Override
    protected void write(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
        super.write(tag, registries, clientPacket);
        tag.putBoolean("assembled", assembled);
        tag.putFloat("progress", progress);
        tag.putInt("launches", launches);
        tag.putLong("lastLaunch", lastLaunch);
        if (!clientPacket) {
            tag.put("inventory", inventory.serializeNBT(registries));
        }
    }

    @Override
    protected void read(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
        super.read(tag, registries, clientPacket);
        assembled = tag.getBoolean("assembled");
        progress = tag.getFloat("progress");
        launches = tag.getInt("launches");
        lastLaunch = tag.contains("lastLaunch") ? tag.getLong("lastLaunch") : -1L;
        if (!clientPacket) {
            inventory.deserializeNBT(registries, tag.getCompound("inventory"));
        }
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "launch_complex", 0, state -> PlayState.STOP));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
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
