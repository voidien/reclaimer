package com.voidreclaimer.link;

import com.voidreclaimer.body.BodyData;
import com.voidreclaimer.body.Bodies;
import com.voidreclaimer.registry.ModAttachments;
import com.voidreclaimer.registry.ModEntities;
import java.util.List;
import java.util.Optional;
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
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.player.Player;
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
    /** Blocks ahead per unit of scale, with one to spare: long bodies such as the Shuttle reach far past their hit box. */
    private static final double AHEAD = 4.0;
    private static final int STALE_CHECK_TICKS = 20;
    private static final org.slf4j.Logger LOGGER = com.mojang.logging.LogUtils.getLogger();

    /** Which link this copy belongs to: after a crash, a copy with an old epoch knows it is stale. */
    private long epoch;
    @Nullable
    private UUID owner;
    /** The contents, as {@code Inventory.save} writes them: kept as saved, so that one bad item cannot lose the rest. */
    private ListTag items = new ListTag();
    /** How far a wreck moves each tick in space, for good: nothing out there stops it. */
    private Vec3 drift = Vec3.ZERO;
    private int selected;
    /** Effects as the game saves them, kept as they were: a body nobody is in does not wear them down. */
    private ListTag effects = new ListTag();

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
        if (role == HostRole.SHELL) {
            Links.note(host);
        }
        return host;
    }

    /**
     * A shell back in the world after a link: the same entity id, so a stale copy of it on disk is replaced, and the link's epoch.
     * A copy that can be seen goes first. The level can still refuse it, for one in a chunk that is not ready, which is empty here:
     * the caller holds the body in the outbox until then.
     */
    public static Optional<BodyHost> restore(ServerLevel level, LinkState.Place at, UUID id, long epoch, BodySnapshot body, @Nullable UUID owner) {
        if (level.getEntity(id) instanceof BodyHost stale) {
            stale.discard();
        }
        BodyHost host = new BodyHost(ModEntities.BODY_HOST.get(), level);
        host.setUUID(id);
        host.setPos(at.position());
        host.setYRot(at.yaw());
        host.setXRot(at.pitch());
        host.setYBodyRot(at.yaw());
        host.owner = owner;
        host.epoch = epoch;
        body.applyTo(host);
        return level.addFreshEntity(host) ? Optional.of(host) : Optional.empty();
    }

    /** A new shell of {@code owner}'s in their view, far enough off for its size. Debug only: nothing in survival makes one yet. */
    public static BodyHost shellAhead(ServerPlayer owner, BodyData body) {
        Vec3 eyes = owner.getEyePosition();
        BodyHost shell = spawn(owner.serverLevel(), eyes, HostRole.SHELL, body, owner.getUUID());
        shell.setPos(eyes.add(owner.getLookAngle().scale(AHEAD * (1.0 + shell.getScale()))));
        Links.note(shell);
        return shell;
    }

    /** A wreck in space drifts, and its owner's marker follows it. Where something holds it up, it stays. */
    private void drift() {
        if (com.voidreclaimer.space.ZeroGravity.isZeroG(level(), blockPosition())) {
            move(net.minecraft.world.entity.MoverType.SELF, drift);
        }
        if (tickCount % STALE_CHECK_TICKS == 0 && owner != null && level().getServer().getPlayerList().getPlayer(owner) instanceof ServerPlayer player) {
            com.voidreclaimer.core.DeathMarker.follow(player, getUUID(), net.minecraft.core.GlobalPos.of(level().dimension(), blockPosition()));
        }
    }

    /** Sets a wreck drifting at {@code speed} blocks a tick. */
    void setDrift(Vec3 speed) {
        drift = speed;
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

    @Override
    public void tick() {
        super.tick();
        if (!level().isClientSide && role() == HostRole.WRECK && drift.lengthSqr() > 0.0) {
            drift();
        }
        if (!level().isClientSide && tickCount % STALE_CHECK_TICKS == 0 && role() != HostRole.MIRROR) {
            if (Links.replaced(this)) {
                LOGGER.info("Shell {} was an old copy and has been removed", getUUID());
                Outboxes.lookAgain(getUUID());
                discard();
            } else if (Links.stale(this)) {
                Links.noteOldCopy(this);
            }
        }
    }

    @Override
    public void remove(RemovalReason reason) {
        if (!level().isClientSide && reason == RemovalReason.KILLED && role() != HostRole.MIRROR) {
            Links.destroyed(this);
        }
        super.remove(reason);
    }

    /** Only a command can reach a host, by a damage that nothing resists: a shell then leaves a wreck, not items on the floor. */
    @Override
    public void die(DamageSource source) {
        if (!level().isClientSide && role() == HostRole.SHELL) {
            Wrecks.make(this);
        } else {
            super.die(source);
        }
    }

    @Override
    public boolean isInvulnerableTo(DamageSource source) {
        return !source.is(DamageTypeTags.BYPASSES_INVULNERABILITY);
    }

    @Override
    public InteractionResult interact(Player player, InteractionHand hand) {
        boolean scrap = role() == HostRole.SHELL && player.isSecondaryUseActive();
        if (hand != InteractionHand.MAIN_HAND || !scrap && role() != HostRole.WRECK) {
            return InteractionResult.PASS;
        }
        if (player instanceof ServerPlayer server && !Links.stale(this)) {
            if (scrap) {
                Wrecks.scrap(server, this);
            } else {
                Wrecks.salvage(server, this);
            }
        }
        return InteractionResult.sidedSuccess(level().isClientSide);
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

    public int selected() {
        return selected;
    }

    public void setSelected(int selected) {
        this.selected = selected;
    }

    public ListTag effects() {
        return effects.copy();
    }

    public void setEffects(ListTag effects) {
        this.effects = effects.copy();
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
        tag.putInt("selected", selected);
        tag.put("held_effects", effects.copy());
        if (drift.lengthSqr() > 0.0) {
            tag.put("drift", newDoubleList(drift.x, drift.y, drift.z));
        }
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        setRole(HostRole.parse(tag.getString("role")));
        epoch = tag.getLong("epoch");
        owner = tag.hasUUID("owner") ? tag.getUUID("owner") : null;
        items = tag.getList("items", Tag.TAG_COMPOUND).copy();
        selected = tag.getInt("selected");
        effects = tag.getList("held_effects", Tag.TAG_COMPOUND).copy();
        ListTag way = tag.getList("drift", Tag.TAG_DOUBLE);
        drift = way.size() == 3 ? new Vec3(way.getDouble(0), way.getDouble(1), way.getDouble(2)) : Vec3.ZERO;
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
