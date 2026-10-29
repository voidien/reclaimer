package com.voidreclaimer.client.render.body;

import com.voidreclaimer.energy.Energy;
import java.util.HashMap;
import java.util.Map;
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
        String rig = BodyRenderer.currentRig();
        BodyLook.Pose pose = pose(player);
        String name = player.swinging && pose != BodyLook.Pose.LOW_POWER ? "module_work" : pose.animation();
        return state.setAndContinue(RawAnimation.begin().thenLoop(BodyLook.animationName(rig, name)));
    }

    private BodyLook.Pose pose(Player player) {
        Motion motion = motions.computeIfAbsent(player.getId(), id -> new Motion());
        if (motion.tick != player.tickCount) {
            Vec3 step = player.position().subtract(player.xo, player.yo, player.zo);
            motion.previousSpeed = motion.speed;
            motion.speed = step.length();
            motion.tick = player.tickCount;
            BodyLook.Pose now = BodyLook.pose(motion.speed, motion.previousSpeed, !Energy.canPowerSystems(player));
            if (now != motion.pose && (player.tickCount >= motion.heldUntil || now == BodyLook.Pose.LOW_POWER)) {
                motion.pose = now;
                motion.heldUntil = player.tickCount + HOLD_TICKS;
            } else if (now == motion.pose) {
                motion.heldUntil = player.tickCount + HOLD_TICKS;
            }
        }
        return motion.pose;
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
