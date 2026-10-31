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
import javax.annotation.Nullable;
import net.minecraft.world.phys.Vec3;

/**
 * The invisible entity the pilot sits on, at the Helm. It has no gravity and no size to speak of. It lives in the
 * world, where the pilot's camera is, and on a ship it moves itself every tick to where its Helm is in the world (the
 * Helm's own position is in the ship's plot, far away; an entity kept in the plot would put the pilot's view there). It
 * remembers its Helm by block position and removes itself when the Helm is gone or the pilot leaves.
 */
public class HelmSeat extends Entity {
    private static final EntityDataAccessor<BlockPos> HELM = SynchedEntityData.defineId(HelmSeat.class, EntityDataSerializers.BLOCK_POS);
    /** How far above the Helm's bottom the seat floats. */
    static final double SEAT_HEIGHT = 0.35;
    public HelmSeat(EntityType<?> type, Level level) {
        super(type, level);
        setNoGravity(true);
        noPhysics = true;
    }

    /**
     * Where the Helm at {@code helm} (plot coordinates on a ship) is in the world right now: the bottom centre of its
     * block. Null when the Helm's position is in the plots but no ship is there any more (the ship was removed): such a
     * Helm is nowhere in the world, and a seat must not go to the raw plot position, which would drag the pilot into the
     * plot, millions of blocks away.
     */
    @Nullable
    public static Vec3 worldSpot(Level level, BlockPos helm) {
        Vec3 centre = Vec3.atBottomCenterOf(helm);
        dev.ryanhcode.sable.sublevel.SubLevel ship = Ships.shipAt(level, helm);
        if (ship != null) {
            return ship.logicalPose().transformPosition(centre);
        }
        return Ships.inPlots(level, helm) ? null : centre;
    }

    /** A seat for the Helm at {@code helm}, floating at {@code where} (the Helm's place in the world, see {@link #worldSpot}). */
    public static HelmSeat at(Level level, BlockPos helm, Vec3 where) {
        HelmSeat seat = new HelmSeat(ModEntities.HELM_SEAT.get(), level);
        seat.entityData.set(HELM, helm.immutable());
        seat.setPos(where.x, where.y + SEAT_HEIGHT, where.z);
        HelmSeats.track(seat);
        return seat;
    }

    /** The Helm block this seat belongs to. */
    public BlockPos helmPos() {
        return entityData.get(HELM);
    }

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide()) {
            return;
        }
        follow();
    }

    /**
     * Keeps the seat at its Helm, or removes it when the Helm or its ship is gone. Called from the seat's own tick and
     * every server tick from {@link HelmSeats}, since a seat whose chunk is not entity-ticking does not tick on its own.
     */
    public void follow() {
        if (isRemoved()) {
            return;
        }
        if (getPassengers().isEmpty() || !(level().getBlockEntity(helmPos()) instanceof HelmBlockEntity)) {
            discard();
            return;
        }
        // Ride the ship: Sable carries entities that stand on a ship, but not one with no physics, so the seat keeps
        // itself at the Helm (and vanilla keeps the pilot on the seat). A Helm whose ship is gone has no place in the
        // world: the seat goes, and the pilot is left where they are.
        Vec3 spot = worldSpot(level(), helmPos());
        if (spot == null) {
            discard();
            return;
        }
        spot = spot.add(0, SEAT_HEIGHT, 0);
        if (spot.distanceToSqr(position()) > 1.0e-8) {
            setPos(spot.x, spot.y, spot.z);
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
        HelmSeats.track(this);
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        tag.put("helm", NbtUtils.writeBlockPos(helmPos()));
    }
}
