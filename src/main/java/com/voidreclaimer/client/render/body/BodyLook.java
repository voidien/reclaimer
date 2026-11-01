package com.voidreclaimer.client.render.body;

import com.voidreclaimer.api.body.Chassis;
import com.voidreclaimer.body.BodyData;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import javax.annotation.Nullable;
import net.minecraft.resources.ResourceKey;
import org.joml.Quaternionf;
import software.bernie.geckolib.cache.object.GeoBone;

/**
 * Pure rules of how a body is drawn, kept free of rendering classes so they can be unit-tested: which art rig a chassis uses,
 * the bone names parts attach to, which animation plays, and how a casing material is painted into a body texture.
 */
public final class BodyLook {
    /** The body rigs the art drop provides (geo/body/NAME.geo.json). */
    public static final Set<String> RIGS = Set.of("drone", "mech", "ship", "titan", "shuttle");
    public static final String DEFAULT_RIG = "drone";

    /** Side of the casing swatch in the top-left corner of every body and section texture. */
    public static final int SWATCH = 16;

    private BodyLook() {}

    /** The rig for a chassis: its id's path when the art has one (drone, mech, ship), else the Drone rig. */
    public static String rig(@Nullable ResourceKey<Chassis> chassis) {
        if (chassis != null) {
            String path = chassis.location().getPath();
            if (RIGS.contains(path)) {
                return path;
            }
        }
        return DEFAULT_RIG;
    }

    /** Whether the rig turns with the look pitch (flyers nose up and down; the upright Mech only turns). */
    public static boolean pitches(String rig) {
        return !"mech".equals(rig);
    }

    /** Name of the bone a module sits on in the body rig: {@code mount_core_1} on the core, {@code head__mount_sensor_1} elsewhere. */
    public static String rigMountBone(String coreSocket, BodyData.Mount mount) {
        String local = sectionMountBone(mount);
        return mount.socket().equals(coreSocket) ? local : mount.socket() + "__" + local;
    }

    /** Name of the same mount inside a section's own model: {@code mount_thruster_1}. Indexes in names count from 1. */
    public static String sectionMountBone(BodyData.Mount mount) {
        return "mount_" + mount.kind().getSerializedName() + "_" + (mount.index() + 1);
    }

    /** True for the cap bones that plug an empty socket ({@code cap_head}). They are hidden once the socket has a section. */
    public static boolean isCap(String bone) {
        return bone.startsWith("cap_");
    }

    /** Rig bone that holds a socket's section ({@code socket id = bone name}). */
    public static String socketBone(String socket) {
        return socket;
    }

    /** What the body is doing, for the animation controller. {@link #animation()} is the pose key in rig files and specs. */
    public enum Pose {
        IDLE("idle"), THRUST("thrust"), BRAKE("brake"), LOW_POWER("low_power"), BACK("back"),
        STRAFE_LEFT("strafe_left"), STRAFE_RIGHT("strafe_right");

        private final String animation;

        Pose(String animation) {
            this.animation = animation;
        }

        public String animation() {
            return animation;
        }
    }

    /** Pose key of the idle variant (the Mech's impatient fidget): played in place of {@code idle} on every other {@link #IDLE_VARIANT_TICKS} of standing still. */
    public static final String IDLE_LONG = "idle_long";
    public static final long IDLE_VARIANT_TICKS = 400;

    /** Whether a body that has been idle for {@code idleTicks} shows its idle variant: the plain idle for 20 s, the variant for 20 s, and so on. */
    public static boolean idleVariant(long idleTicks) {
        return idleTicks >= IDLE_VARIANT_TICKS && (idleTicks / IDLE_VARIANT_TICKS) % 2 == 1;
    }

    /** Pose key of the tool animation, played while the player swings. */
    public static final String MODULE_WORK = "module_work";

    /**
     * Pose keys to try, in order, for a pose a rig may not have an animation for: sideways and backward flight fall back to
     * thrust, everything falls back to idle.
     */
    public static List<String> fallbacks(Pose pose) {
        List<String> keys = new ArrayList<>();
        keys.add(pose.animation());
        if (pose == Pose.STRAFE_LEFT || pose == Pose.STRAFE_RIGHT || pose == Pose.BACK) {
            keys.add(Pose.THRUST.animation());
        }
        if (pose != Pose.IDLE) {
            keys.add(Pose.IDLE.animation());
        }
        return keys;
    }

    /** The pose keys a layer looks for, in order: the tool animation while swinging (not in low power), then the pose and its fallbacks. */
    public static List<String> layerKeys(boolean swinging, Pose pose) {
        List<String> keys = new ArrayList<>();
        if (swinging && pose != Pose.LOW_POWER) {
            keys.add(MODULE_WORK);
        }
        keys.addAll(fallbacks(pose));
        return keys;
    }

    /** Change in speed (blocks per tick, per tick) that counts as thrusting or braking; below it the body just coasts. */
    public static final double ACCEL_THRESHOLD = 0.002;
    /** Speed (blocks per tick) below which braking has nothing left to slow. */
    public static final double REST_SPEED = 0.01;
    /** How much of the motion must be sideways or backward for the strafe and back poses. */
    public static final double DIRECTION_SHARE = 0.6;

    /** Part of a step ({@code dx, dz}) that points where the body faces ({@code yaw} in degrees); negative when moving backward. */
    public static double forward(double dx, double dz, float yaw) {
        double r = Math.toRadians(yaw);
        return -Math.sin(r) * dx + Math.cos(r) * dz;
    }

    /** Part of a step that points to the body's right; negative when moving left. */
    public static double lateral(double dx, double dz, float yaw) {
        double r = Math.toRadians(yaw);
        return -Math.cos(r) * dx - Math.sin(r) * dz;
    }

    /**
     * Picks the pose from how the body moves. Low power wins over everything; mostly sideways motion is a strafe, mostly
     * backward motion is flying back; otherwise speeding up is thrust, slowing down is braking, and a steady drift (or
     * standing still) is idle.
     *
     * @param forward the motion's part along the facing, per tick
     * @param lateral the motion's part to the right, per tick
     * @param thrusting whether the body is under thrust (a thrust key is held, or unknown); a steady cruise under thrust
     *                  keeps the thrust pose instead of dropping to idle once the speed stops changing
     */
    public static Pose pose(double speed, double previousSpeed, boolean lowPower, double forward, double lateral, boolean thrusting) {
        if (lowPower) {
            return Pose.LOW_POWER;
        }
        if (speed > REST_SPEED) {
            if (Math.abs(lateral) > DIRECTION_SHARE * speed) {
                return lateral > 0 ? Pose.STRAFE_RIGHT : Pose.STRAFE_LEFT;
            }
            if (-forward > DIRECTION_SHARE * speed) {
                return Pose.BACK;
            }
        }
        double change = speed - previousSpeed;
        if (change > ACCEL_THRESHOLD) {
            return Pose.THRUST;
        }
        if (change < -ACCEL_THRESHOLD && previousSpeed > REST_SPEED) {
            return Pose.BRAKE;
        }
        if (thrusting && speed > REST_SPEED) {
            return Pose.THRUST;
        }
        return Pose.IDLE;
    }

    /** {@link #pose(double, double, boolean, double, double, boolean)} with no thrust key held. */
    public static Pose pose(double speed, double previousSpeed, boolean lowPower, double forward, double lateral) {
        return pose(speed, previousSpeed, lowPower, forward, lateral, false);
    }

    /** {@link #pose(double, double, boolean, double, double, boolean)} for a body moving straight ahead, with no thrust key held. */
    public static Pose pose(double speed, double previousSpeed, boolean lowPower) {
        return pose(speed, previousSpeed, lowPower, speed, 0.0, false);
    }

    /** Animation id inside a body rig's animation file: {@code animation.void_reclaimer.<rig>.<state>}. */
    public static String animationName(String rig, String state) {
        return "animation.void_reclaimer." + rig + "." + state;
    }

    /**
     * Paints {@code swatch} (a square casing texture, any power-of-two side) over the casing swatch area of {@code base}: the
     * top-left {@link #SWATCH} square of an ARGB image {@code width} wide. A swatch of another size is sampled to fit. Returns a
     * new array; the inputs are not changed.
     */
    public static int[] paintCasing(int[] base, int width, int[] swatch, int swatchSide) {
        int[] out = base.clone();
        if (swatchSide <= 0 || swatch.length < swatchSide * swatchSide || width < SWATCH || base.length < width * SWATCH) {
            return out;
        }
        for (int y = 0; y < SWATCH; y++) {
            for (int x = 0; x < SWATCH; x++) {
                int sx = x * swatchSide / SWATCH;
                int sy = y * swatchSide / SWATCH;
                out[y * width + x] = swatch[sy * swatchSide + sx];
            }
        }
        return out;
    }

    /**
     * Hides a bone's own cubes and leaves its children drawn: a Mech's {@code root} is its pelvis and also carries its chest. GeckoLib's
     * {@code setHidden} hides the children too, so the second call is what makes this work.
     */
    public static void hideAlone(GeoBone bone, boolean hidden) {
        bone.setHidden(hidden);
        bone.setChildrenHidden(false);
    }

    /**
     * The model's turn: to where the body looks, and down by {@code pitch}. The rig's own {@code rigYaw} comes first, so its nose is at
     * local -Z when the pitch is applied, whichever way the model was built. Positive pitch looks down, as vanilla's does.
     */
    public static Quaternionf turn(float yaw, float pitch, float rigYaw) {
        return new Quaternionf()
                .rotationY((float) Math.toRadians(180f - yaw))
                .rotateX((float) Math.toRadians(-pitch))
                .rotateY((float) Math.toRadians(rigYaw));
    }
}
