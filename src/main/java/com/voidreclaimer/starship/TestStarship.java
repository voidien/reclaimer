package com.voidreclaimer.starship;

import com.voidreclaimer.registry.ModEntities;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;

/**
 * A placeholder starship for testing models and animations in game: drawn with GeckoLib at the Ship size class's
 * scale, it drifts round its anchor ({@link StarshipDrift}) and loops {@code gear_cycle}; a right-click plays
 * {@code ignite_prep} once. It has no gravity, no collision with blocks, no drops, and breaks with one hit.
 * The model, texture and animations come from {@code tools/art/rd_generate.py --promote-as entity/starship}.
 */
public class TestStarship extends Entity implements GeoEntity {
    /** Length of {@code ignite_prep} (2.4 s). */
    public static final int IGNITE_TICKS = 48;

    private static final EntityDataAccessor<Vector3f> ANCHOR = SynchedEntityData.defineId(TestStarship.class, EntityDataSerializers.VECTOR3);
    private static final EntityDataAccessor<Integer> IGNITING = SynchedEntityData.defineId(TestStarship.class, EntityDataSerializers.INT);

    private static final RawAnimation GEAR_CYCLE = RawAnimation.begin().thenLoop("animation.void_reclaimer.starship.gear_cycle");
    private static final RawAnimation IGNITE_PREP = RawAnimation.begin().thenPlay("animation.void_reclaimer.starship.ignite_prep");

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    private boolean anchored;

    public TestStarship(EntityType<? extends TestStarship> type, Level level) {
        super(type, level);
        noPhysics = true;
        setNoGravity(true);
    }

    /** Spawns a ship anchored at {@code pos}. */
    public static TestStarship spawn(ServerLevel level, Vec3 pos) {
        TestStarship ship = new TestStarship(ModEntities.TEST_STARSHIP.get(), level);
        ship.setAnchor(pos);
        ship.moveToDrift();
        level.addFreshEntity(ship);
        return ship;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(ANCHOR, new Vector3f());
        builder.define(IGNITING, 0);
    }

    public Vec3 anchor() {
        Vector3f a = entityData.get(ANCHOR);
        return new Vec3(a.x, a.y, a.z);
    }

    public void setAnchor(Vec3 pos) {
        entityData.set(ANCHOR, pos.toVector3f());
        anchored = true;
    }

    public boolean isIgniting() {
        return entityData.get(IGNITING) > 0;
    }

    /** Per-ship offset into the drift cycle, so that several ships don't move in step. */
    public double phase() {
        return (getUUID().getLeastSignificantBits() >>> 40) / (double) (1L << 24);
    }

    @Override
    public void onSyncedDataUpdated(EntityDataAccessor<?> key) {
        super.onSyncedDataUpdated(key);
        if (key == ANCHOR && level().isClientSide) {
            anchored = true;
        }
    }

    @Override
    public void tick() {
        super.tick();
        if (!anchored) {
            if (level().isClientSide) {
                return; // the anchor hasn't arrived yet; stay where the spawn packet put the ship
            }
            setAnchor(position()); // summoned with /summon: drift round where it appeared
        }
        moveToDrift();
        if (!level().isClientSide && isIgniting()) {
            entityData.set(IGNITING, entityData.get(IGNITING) - 1);
        }
    }

    private void moveToDrift() {
        double time = level().getGameTime();
        Vec3 pos = anchor().add(StarshipDrift.offset(time, phase()));
        setPos(pos.x, pos.y, pos.z);
        setYRot(StarshipDrift.yaw(time, phase()));
    }

    /** The client works the position out itself; server movement packets would only make it stutter. */
    @Override
    public void lerpTo(double x, double y, double z, float yRot, float xRot, int steps) {}

    @Override
    public InteractionResult interact(Player player, InteractionHand hand) {
        if (!level().isClientSide && !isIgniting()) {
            entityData.set(IGNITING, IGNITE_TICKS);
        }
        return InteractionResult.sidedSuccess(level().isClientSide);
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (isInvulnerableTo(source)) {
            return false;
        }
        if (!level().isClientSide && !isRemoved()) {
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
        if (tag.contains("anchor_x")) {
            setAnchor(new Vec3(tag.getDouble("anchor_x"), tag.getDouble("anchor_y"), tag.getDouble("anchor_z")));
        }
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        Vec3 a = anchor();
        tag.putDouble("anchor_x", a.x);
        tag.putDouble("anchor_y", a.y);
        tag.putDouble("anchor_z", a.z);
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "main", 4,
                state -> state.setAndContinue(isIgniting() ? IGNITE_PREP : GEAR_CYCLE)));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }
}
