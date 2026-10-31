package com.voidreclaimer.helper;

import com.voidreclaimer.Config;
import com.voidreclaimer.registry.ModDataComponents;
import com.voidreclaimer.registry.ModEntities;
import com.voidreclaimer.registry.ModItems;
import java.util.Optional;
import java.util.UUID;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemHandlerHelper;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;

/**
 * A helper drone: a small flying worker drawn with the mining probe model. Its first job is the ferry
 * ({@link Ferry}): it carries items from its source inventory to its destination inventory, up to
 * {@code helper.carry} at a time, flying straight at {@code helper.speed} blocks per tick and hovering a block above each
 * end. It has no gravity and flies through blocks (placeholder steering). Sneak-use with an empty hand takes it back as
 * an item with its links; a hit breaks it into that item as well. Right-click reports what it is doing.
 */
public class HelperDrone extends Entity implements GeoEntity {
    private static final EntityDataAccessor<Byte> PHASE = SynchedEntityData.defineId(HelperDrone.class, EntityDataSerializers.BYTE);
    private static final RawAnimation IDLE = RawAnimation.begin().thenLoop("animation.void_reclaimer.mining_probe.idle");
    private static final RawAnimation THRUST = RawAnimation.begin().thenLoop("animation.void_reclaimer.mining_probe.thrust");
    /** The drone hovers this far above the block it works on. */
    public static final double HOVER = 1.0;

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    private HelperLinks links = HelperLinks.EMPTY;
    private ItemStack carried = ItemStack.EMPTY;
    private int wait;
    @Nullable
    private UUID owner;

    public HelperDrone(EntityType<? extends HelperDrone> type, Level level) {
        super(type, level);
        noPhysics = true;
        setNoGravity(true);
    }

    /** Spawns a drone at {@code pos} with {@code links}, owned by {@code owner} (may be null). */
    public static HelperDrone spawn(ServerLevel level, Vec3 pos, HelperLinks links, @Nullable UUID owner) {
        HelperDrone drone = new HelperDrone(ModEntities.HELPER_DRONE.get(), level);
        drone.setPos(pos.x, pos.y, pos.z);
        drone.links = links;
        drone.owner = owner;
        drone.setPhase(Ferry.after(Ferry.Phase.IDLE, Ferry.Action.NONE, links.complete(), false, false));
        level.addFreshEntity(drone);
        return drone;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(PHASE, (byte) Ferry.Phase.IDLE.ordinal());
    }

    public Ferry.Phase phase() {
        return Ferry.Phase.values()[entityData.get(PHASE)];
    }

    private void setPhase(Ferry.Phase phase) {
        entityData.set(PHASE, (byte) phase.ordinal());
    }

    public HelperLinks links() {
        return links;
    }

    public ItemStack carried() {
        return carried;
    }

    /** For tests: what the drone holds. */
    public void setCarried(ItemStack stack) {
        carried = stack;
    }

    @Nullable
    public UUID owner() {
        return owner;
    }

    /** Where the drone is heading in its current phase, or empty when it is idle or waiting. */
    public Optional<Vec3> target() {
        return switch (phase()) {
            case TO_SOURCE -> links.source().map(HelperDrone::above);
            case TO_DESTINATION -> links.destination().map(HelperDrone::above);
            default -> Optional.empty();
        };
    }

    private static Vec3 above(BlockPos pos) {
        return Vec3.atCenterOf(pos).add(0, HOVER, 0);
    }

    @Override
    public void tick() {
        super.tick();
        if (!(level() instanceof ServerLevel server)) {
            return;
        }
        Ferry.Phase phase = phase();
        Optional<Vec3> target = target();
        boolean arrived = false;
        if (target.isPresent()) {
            Vec3 next = HelperSteering.step(position(), target.get(), Config.HELPER_SPEED.get());
            setYRot(HelperSteering.yaw(position(), target.get(), getYRot()));
            setPos(next.x, next.y, next.z);
            arrived = HelperSteering.arrived(next, target.get(), Config.HELPER_REACH.get());
        }
        Ferry.Action action = Ferry.actionOn(phase, arrived);
        switch (action) {
            case LOAD -> links.source().ifPresent(pos -> load(server, pos));
            case UNLOAD -> links.destination().ifPresent(pos -> unload(server, pos));
            default -> { }
        }
        boolean waitOver = phase == Ferry.Phase.WAITING && --wait <= 0;
        Ferry.Phase next = Ferry.after(phase, action, links.complete(), !carried.isEmpty(), waitOver);
        if (next == Ferry.Phase.WAITING && phase != Ferry.Phase.WAITING) {
            wait = Config.HELPER_PAUSE.get();
        }
        if (next != phase) {
            setPhase(next);
        }
    }

    /** Takes up to {@code helper.carry} items of the first non-empty slot of the inventory at {@code pos}. */
    private void load(ServerLevel server, BlockPos pos) {
        IItemHandler handler = server.getCapability(Capabilities.ItemHandler.BLOCK, pos, null);
        if (handler == null) {
            return;
        }
        int carry = Config.HELPER_CARRY.get();
        for (int slot = 0; slot < handler.getSlots() && carried.getCount() < carry; slot++) {
            ItemStack inSlot = handler.getStackInSlot(slot);
            if (inSlot.isEmpty() || (!carried.isEmpty() && !ItemStack.isSameItemSameComponents(carried, inSlot))) {
                continue;
            }
            ItemStack taken = handler.extractItem(slot, carry - carried.getCount(), false);
            if (taken.isEmpty()) {
                continue;
            }
            if (carried.isEmpty()) {
                carried = taken;
            } else {
                carried.grow(taken.getCount());
            }
        }
    }

    /** Gives what it carries to the inventory at {@code pos}; what does not fit stays aboard. */
    private void unload(ServerLevel server, BlockPos pos) {
        IItemHandler handler = server.getCapability(Capabilities.ItemHandler.BLOCK, pos, null);
        if (handler == null || carried.isEmpty()) {
            return;
        }
        carried = ItemHandlerHelper.insertItem(handler, carried, false);
    }

    @Override
    public InteractionResult interact(Player player, InteractionHand hand) {
        if (level().isClientSide) {
            return InteractionResult.SUCCESS;
        }
        if (player.isShiftKeyDown() && player.getItemInHand(hand).isEmpty()) {
            ItemStack item = toItem();
            if (!player.getInventory().add(item)) {
                player.drop(item, false);
            }
            dropCarried();
            discard();
            player.displayClientMessage(Component.translatable("message.void_reclaimer.helper.picked_up"), true);
            return InteractionResult.CONSUME;
        }
        player.displayClientMessage(status(), true);
        return InteractionResult.CONSUME;
    }

    /** One line about the job: the phase, what is carried, and the links. */
    public Component status() {
        String phase = phase().name().toLowerCase(java.util.Locale.ROOT);
        Component load = carried.isEmpty() ? Component.translatable("message.void_reclaimer.helper.empty")
                : Component.translatable("message.void_reclaimer.helper.carrying", carried.getCount(), carried.getHoverName());
        return Component.translatable("message.void_reclaimer.helper.status." + phase, load);
    }

    /** The drone as an item, with its links. */
    public ItemStack toItem() {
        ItemStack item = new ItemStack(ModItems.HELPER_DRONE.get());
        item.set(ModDataComponents.HELPER_LINKS.get(), links);
        return item;
    }

    private void dropCarried() {
        if (!carried.isEmpty()) {
            level().addFreshEntity(new ItemEntity(level(), getX(), getY(), getZ(), carried.copy()));
            carried = ItemStack.EMPTY;
        }
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (isInvulnerableTo(source)) {
            return false;
        }
        if (!level().isClientSide && !isRemoved()) {
            level().addFreshEntity(new ItemEntity(level(), getX(), getY(), getZ(), toItem()));
            dropCarried();
            discard();
        }
        return true;
    }

    @Override
    public boolean isPickable() {
        return !isRemoved();
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        links = tag.contains("links") ? HelperLinks.CODEC.parse(NbtOps.INSTANCE, tag.get("links")).result().orElse(HelperLinks.EMPTY) : HelperLinks.EMPTY;
        carried = tag.contains("carried") ? ItemStack.parseOptional(registryAccess(), tag.getCompound("carried")) : ItemStack.EMPTY;
        wait = tag.getInt("wait");
        owner = tag.hasUUID("owner") ? tag.getUUID("owner") : null;
        int phase = tag.getInt("phase");
        setPhase(phase >= 0 && phase < Ferry.Phase.values().length ? Ferry.Phase.values()[phase] : Ferry.Phase.IDLE);
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        HelperLinks.CODEC.encodeStart(NbtOps.INSTANCE, links).result().ifPresent(encoded -> tag.put("links", (Tag) encoded));
        if (!carried.isEmpty()) {
            tag.put("carried", carried.save(registryAccess()));
        }
        tag.putInt("wait", wait);
        if (owner != null) {
            tag.putUUID("owner", owner);
        }
        tag.putInt("phase", phase().ordinal());
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "main", 4,
                state -> state.setAndContinue(Ferry.moving(phase()) ? THRUST : IDLE)));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }
}
