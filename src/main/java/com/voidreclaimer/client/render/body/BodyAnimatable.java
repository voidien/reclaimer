package com.voidreclaimer.client.render.body;

import com.voidreclaimer.energy.Energy;
import com.voidreclaimer.registry.ModAttachments;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import software.bernie.geckolib.animatable.GeoReplacedEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.AnimationState;
import software.bernie.geckolib.animation.PlayState;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.cache.GeckoLibCache;
import software.bernie.geckolib.loading.object.BakedAnimations;
import software.bernie.geckolib.constant.DataTickets;
import software.bernie.geckolib.util.GeckoLibUtil;

/**
 * The single animatable GeckoLib uses for every body (one instance cache entry per player). Its controller plays the rig's
 * {@code idle}, {@code thrust}, {@code brake} or {@code low_power} animation, picked by {@link BodyLook#pose}, and
 * {@code module_work} while the player swings (using a tool).
 */
public final class BodyAnimatable implements GeoReplacedEntity {
    public static final BodyAnimatable INSTANCE = new BodyAnimatable();

    /** A pose is kept this many ticks, so a one-tick wobble in speed doesn't flick the animation back and forth. */
    private static final int HOLD_TICKS = 4;

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    private final Map<Integer, Motion> motions = new HashMap<>();

    private BodyAnimatable() {}

    /** Per-player memory of the last speed and the pose being held. */
    private static final class Motion {
        int tick = Integer.MIN_VALUE;
        double speed;
        double previousSpeed;
        BodyLook.Pose pose = BodyLook.Pose.IDLE;
        int heldUntil;
    }

    @Override
    public EntityType<?> getReplacingEntityType() {
        return EntityType.PLAYER;
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "body", 6, this::play));
    }

    private PlayState play(AnimationState<BodyAnimatable> state) {
        Entity entity = state.getData(DataTickets.ENTITY);
        if (!(entity instanceof Player player)) {
            return PlayState.STOP;
        }
        BodyRenderer.Frame frame = BodyRenderer.frame();
        if (frame == null) {
            return PlayState.STOP;
        }
        BodyLook.Pose pose = pose(player);
        List<String> keys = new ArrayList<>();
        if (player.swinging && pose != BodyLook.Pose.LOW_POWER) {
            keys.add(BodyLook.MODULE_WORK);
        }
        keys.addAll(BodyLook.fallbacks(pose));
        ResourceLocation file = frame.animationResource();
        for (String key : keys) {
            String name = frame.spec() != null ? frame.spec().animation(key) : BodyLook.animationName(frame.rig(), key);
            if (name != null && hasAnimation(file, name)) {
                return state.setAndContinue(RawAnimation.begin().thenLoop(name));
            }
        }
        return PlayState.STOP;
    }

    private static boolean hasAnimation(ResourceLocation file, String name) {
        BakedAnimations animations = GeckoLibCache.getBakedAnimations().get(file);
        return animations != null && animations.getAnimation(name) != null;
    }

    private BodyLook.Pose pose(Player player) {
        Motion motion = motions.computeIfAbsent(player.getId(), id -> new Motion());
        if (motion.tick != player.tickCount) {
            Vec3 step = player.position().subtract(player.xo, player.yo, player.zo);
            motion.previousSpeed = motion.speed;
            motion.speed = step.length();
            motion.tick = player.tickCount;
            float yaw = player.getYRot();
            BodyLook.Pose now = BodyLook.pose(motion.speed, motion.previousSpeed, !Energy.canPowerSystems(player),
                    BodyLook.forward(step.x, step.z, yaw), BodyLook.lateral(step.x, step.z, yaw), thrusting(player));
            if (now != motion.pose && (player.tickCount >= motion.heldUntil || now == BodyLook.Pose.LOW_POWER)) {
                motion.pose = now;
                motion.heldUntil = player.tickCount + HOLD_TICKS;
            } else if (now == motion.pose) {
                motion.heldUntil = player.tickCount + HOLD_TICKS;
            }
        }
        return motion.pose;
    }

    /** The local player's held keys; other players' input isn't known here, so their steady motion counts as thrust. */
    private static boolean thrusting(Player player) {
        return !player.isLocalPlayer() || player.getData(ModAttachments.FLIGHT_INPUT).isThrusting();
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }

    @Override
    public double getTick(Object entity) {
        return entity instanceof Entity e ? e.tickCount : 0;
    }
}
