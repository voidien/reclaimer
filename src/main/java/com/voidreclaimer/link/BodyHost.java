package com.voidreclaimer.link;

import com.voidreclaimer.body.BodyData;
import com.voidreclaimer.body.Bodies;
import com.voidreclaimer.registry.ModAttachments;
import com.voidreclaimer.registry.ModEntities;
import java.util.List;
import java.util.UUID;
import javax.annotation.Nullable;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * A body with nobody in it (see {@link HostRole}). The body itself is the {@code BODY} attachment, as on a player, so
 * {@link Bodies} reads it the same way. It floats where it is put, so losing the signal on ruined Earth cannot drop it, and it
 * stays in its dimension: a shell only crosses a portal with the player in it.
 */
public class BodyHost extends LivingEntity {
    private static final EntityDataAccessor<Integer> ROLE = SynchedEntityData.defineId(BodyHost.class, EntityDataSerializers.INT);
    private static final double AHEAD = 6.0;

    /** Which link this copy belongs to: after a crash, a copy with an old epoch knows it is stale. */
    private long epoch;
    @Nullable
    private UUID owner;
    /** The contents, as {@code Inventory.save} writes them: kept as saved, so that one bad item cannot lose the rest. */
    private ListTag items = new ListTag();

    public BodyHost(EntityType<? extends BodyHost> type, Level level) {
        super(type, level);
        setNoGravity(true);
    }

    public static AttributeSupplier.Builder attributes() {
        return LivingEntity.createLivingAttributes();
    }

    /** A new host in {@code level} at full health, wearing {@code body}. */
    public static BodyHost spawn(ServerLevel level, Vec3 where, HostRole role, BodyData body, @Nullable UUID owner) {
        BodyHost host = new BodyHost(ModEntities.BODY_HOST.get(), level);
        host.setPos(where);
        host.setRole(role);
        host.owner = owner;
        host.wear(body);
        host.setHealth(host.getMaxHealth());
        level.addFreshEntity(host);
        return host;
    }

    /** A new shell of {@code owner}'s in their view, far enough off for its size. Debug only: nothing in survival makes one yet. */
    public static BodyHost shellAhead(ServerPlayer owner, BodyData body) {
        Vec3 where = owner.getEyePosition().add(owner.getLookAngle().scale(AHEAD));
        return spawn(owner.serverLevel(), where, HostRole.SHELL, body, owner.getUUID());
    }

    /** Puts {@code body} on this host: its size, health limit and reach follow. */
    public void wear(BodyData body) {
        setData(ModAttachments.BODY, body);
        if (isAddedToLevel()) {
            syncData(ModAttachments.BODY);
        }
        Bodies.apply(this);
        refreshDimensions();
    }

    /** The scale is a modifier that is not saved, so a host loaded from disk has to put it back. */
    @Override
    public void onAddedToLevel() {
        super.onAddedToLevel();
        if (!level().isClientSide) {
            Bodies.apply(this);
            refreshDimensions();
        }
    }

    public HostRole role() {
        return HostRole.values()[entityData.get(ROLE)];
    }

    public void setRole(HostRole role) {
        entityData.set(ROLE, role.ordinal());
    }

    public long epoch() {
        return epoch;
    }

    public void setEpoch(long epoch) {
        this.epoch = epoch;
    }

    @Nullable
    public UUID owner() {
        return owner;
    }

    public ListTag items() {
        return items.copy();
    }

    public void setItems(ListTag items) {
        this.items = items.copy();
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(ROLE, HostRole.SHELL.ordinal());
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putString("role", role().id());
        tag.putLong("epoch", epoch);
        if (owner != null) {
            tag.putUUID("owner", owner);
        }
        tag.put("items", items.copy());
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        setRole(HostRole.parse(tag.getString("role")));
        epoch = tag.getLong("epoch");
        owner = tag.hasUUID("owner") ? tag.getUUID("owner") : null;
        items = tag.getList("items", Tag.TAG_COMPOUND).copy();
    }

    @Override
    public boolean shouldBeSaved() {
        return role() != HostRole.MIRROR && super.shouldBeSaved();
    }

    @Override
    public boolean canUsePortal(boolean allowPassengers) {
        return false;
    }

    @Override
    public boolean canChangeDimensions(Level oldLevel, Level newLevel) {
        return false;
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    public Iterable<ItemStack> getArmorSlots() {
        return List.of();
    }

    @Override
    public ItemStack getItemBySlot(EquipmentSlot slot) {
        return ItemStack.EMPTY;
    }

    @Override
    public void setItemSlot(EquipmentSlot slot, ItemStack stack) {}

    @Override
    public HumanoidArm getMainArm() {
        return HumanoidArm.RIGHT;
    }
}
