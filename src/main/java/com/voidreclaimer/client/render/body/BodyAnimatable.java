package com.voidreclaimer.client.render.body;

import com.voidreclaimer.energy.Energy;
import com.voidreclaimer.link.BodyHost;
import com.voidreclaimer.registry.ModAttachments;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
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
 * The single animatable GeckoLib uses for every body (one instance cache entry per entity). Its controller plays the rig's
 * {@code idle}, {@code thrust}, {@code brake} or {@code low_power} animation, picked by {@link BodyLook#pose}, and
 * {@code module_work} while the wearer swings (using a tool).
 */
public final class BodyAnimatable implements GeoReplacedEntity {
    public static final BodyAnimatable INSTANCE = new BodyAnimatable();

    /** A pose is kept this many ticks, so a one-tick wobble in speed doesn't flick the animation back and forth. */
    private static final int HOLD_TICKS = 4;

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    private final Map<Integer, Motion> motions = new HashMap<>();

    private BodyAnimatable() {}

    /** Per-entity memory of the last speed and the pose being held. */
    private static final class Motion {
        int tick = Integer.MIN_VALUE;
        double speed;
        double previousSpeed;
        BodyLook.Pose pose = BodyLook.Pose.IDLE;
        int heldUntil;
        /** Game tick the current idle began, or -1 while not idle. */
        int idleSince = -1;
    }

    @Override
    public EntityType<?> getReplacingEntityType() {
        return EntityType.PLAYER;
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "body", 6, this::play));
        // Layers added after the main controller overwrite the bones they animate; the rest stays with the main animation.
        for (int i = 0; i < BodyRigSpec.MAX_LAYERS; i++) {
            int index = i;
            controllers.add(new AnimationController<>(this, "layer" + i, 6, state -> playLayer(index, state)));
        }
    }

    private PlayState playLayer(int index, AnimationState<BodyAnimatable> state) {
        Entity entity = state.getData(DataTickets.ENTITY);
        BodyRenderer.Frame frame = BodyRenderer.frame();
        if (!(entity instanceof LivingEntity wearer) || frame == null || frame.spec() == null || BodyRenderer.exploding()) {
            return PlayState.STOP;
        }
        BodyLook.Pose pose = pose(wearer);
        ResourceLocation file = frame.animationResource();
        for (String key : BodyLook.layerKeys(wearer.swinging, pose)) {
            String name = frame.spec().layerAnimation(index, key);
            if (name != null && hasAnimation(file, name)) {
                return state.setAndContinue(RawAnimation.begin().thenLoop(name));
            }
        }
        return PlayState.STOP;
    }

    private PlayState play(AnimationState<BodyAnimatable> state) {
        Entity entity = state.getData(DataTickets.ENTITY);
        if (!(entity instanceof LivingEntity wearer)) {
            return PlayState.STOP;
        }
        BodyRenderer.Frame frame = BodyRenderer.frame();
        if (frame == null || BodyRenderer.exploding()) {
            return PlayState.STOP;
        }
        BodyLook.Pose pose = pose(wearer);
        List<String> keys = new ArrayList<>();
        boolean layered = frame.spec() != null && frame.spec().hasLayerFor(BodyLook.MODULE_WORK);
        if (wearer.swinging && pose != BodyLook.Pose.LOW_POWER && !layered) {
            keys.add(BodyLook.MODULE_WORK);
        }
        if (pose == BodyLook.Pose.IDLE && BodyLook.idleVariant(idleTicks(wearer))) {
            keys.add(BodyLook.IDLE_LONG);
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

    private BodyLook.Pose pose(LivingEntity wearer) {
        Motion motion = motions.computeIfAbsent(wearer.getId(), id -> new Motion());
        if (motion.tick != wearer.tickCount) {
            Vec3 step = wearer.position().subtract(wearer.xo, wearer.yo, wearer.zo);
            motion.previousSpeed = motion.speed;
            motion.speed = step.length();
            motion.tick = wearer.tickCount;
            float yaw = wearer instanceof net.minecraft.world.entity.player.Player player ? com.voidreclaimer.body.BodyTurning.of(player).yaw() : wearer.getYRot();
            BodyLook.Pose now = BodyLook.pose(motion.speed, motion.previousSpeed, lowPower(wearer),
                    BodyLook.forward(step.x, step.z, yaw), BodyLook.lateral(step.x, step.z, yaw), thrusting(wearer));
            if (now != motion.pose && (wearer.tickCount >= motion.heldUntil || now == BodyLook.Pose.LOW_POWER)) {
                motion.pose = now;
                motion.heldUntil = wearer.tickCount + HOLD_TICKS;
            } else if (now == motion.pose) {
                motion.heldUntil = wearer.tickCount + HOLD_TICKS;
            }
        }
        if (motion.pose != BodyLook.Pose.IDLE) {
            motion.idleSince = -1;
        } else if (motion.idleSince < 0) {
            motion.idleSince = wearer.tickCount;
        }
        return motion.pose;
    }

    /** How long the body has been idle, in ticks (0 when it is not). */
    private long idleTicks(LivingEntity wearer) {
        Motion motion = motions.get(wearer.getId());
        return motion == null || motion.idleSince < 0 ? 0 : Math.max(0, wearer.tickCount - motion.idleSince);
    }

    /** A player on their energy; a host with nobody in it is asleep, unless it stands for a player who is only away. */
    private static boolean lowPower(LivingEntity wearer) {
        if (wearer instanceof BodyHost host) {
            return host.role().dormant();
        }
        return wearer instanceof Player player && !Energy.canPowerSystems(player);
    }

    /** The local player's held keys; other players' input isn't known here, so their steady motion counts as thrust. Hosts have no keys. */
    private static boolean thrusting(LivingEntity wearer) {
        return wearer instanceof Player player && (!player.isLocalPlayer() || player.getData(ModAttachments.FLIGHT_INPUT).isThrusting());
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
