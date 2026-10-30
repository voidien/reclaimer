package com.voidreclaimer.ship;

import com.voidreclaimer.registry.ModEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * The invisible entity the pilot sits on, at the Helm. It has no gravity and no size to speak of; Sable keeps riders on a
 * moving ship. It remembers its Helm by block position (on a ship, the Helm's position is in the ship's own plot, far from
 * where the seat floats in the world, so it cannot be derived from the seat's position) and removes itself when the Helm
 * is gone or the pilot leaves.
 */
public class HelmSeat extends Entity {
    private static final EntityDataAccessor<BlockPos> HELM = SynchedEntityData.defineId(HelmSeat.class, EntityDataSerializers.BLOCK_POS);
    public HelmSeat(EntityType<?> type, Level level) {
        super(type, level);
        setNoGravity(true);
        noPhysics = true;
    }

    /** A seat for the Helm at {@code helm}, floating at {@code where} (the Helm's place in the world, which on a ship differs). */
    public static HelmSeat at(Level level, BlockPos helm, Vec3 where) {
        HelmSeat seat = new HelmSeat(ModEntities.HELM_SEAT.get(), level);
        seat.entityData.set(HELM, helm.immutable());
        seat.setPos(where.x, where.y + 0.35, where.z);
        return seat;
    }

    /** The Helm block this seat belongs to. */
    public BlockPos helmPos() {
        return entityData.get(HELM);
    }

    @Override
    public void tick() {
        super.tick();
        if (!level().isClientSide() && (getPassengers().isEmpty() || !(level().getBlockEntity(helmPos()) instanceof HelmBlockEntity))) {
            discard();
        }
    }

    @Override
    protected boolean canRide(Entity vehicle) {
        return false;
    }

    @Override
    protected boolean canAddPassenger(Entity passenger) {
        return getPassengers().isEmpty();
    }

    @Override
    public Vec3 getDismountLocationForPassenger(LivingEntity passenger) {
        BlockPos helm = helmPos();
        return new Vec3(helm.getX() + 0.5, helm.getY() + 1.0, helm.getZ() + 0.5);
    }

    @Override
    public boolean isPickable() {
        return false;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(HELM, BlockPos.ZERO);
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        NbtUtils.readBlockPos(tag, "helm").ifPresent(pos -> entityData.set(HELM, pos));
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        tag.put("helm", NbtUtils.writeBlockPos(helmPos()));
    }
}
