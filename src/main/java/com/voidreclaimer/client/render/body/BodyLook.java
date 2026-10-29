package com.voidreclaimer.client.render.body;

import com.voidreclaimer.api.body.Chassis;
import com.voidreclaimer.body.BodyData;
import java.util.Set;
import javax.annotation.Nullable;
import net.minecraft.resources.ResourceKey;

/**
 * Pure rules of how a body is drawn, kept free of rendering classes so they can be unit-tested: which art rig a chassis uses,
 * the bone names parts attach to, which animation plays, and how a casing material is painted into a body texture.
 */
public final class BodyLook {
    /** The body rigs the art drop provides (geo/body/NAME.geo.json). */
    public static final Set<String> RIGS = Set.of("drone", "mech", "ship");
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

    /** What the body is doing, for the animation controller. */
    public enum Pose {
        IDLE("idle"), THRUST("thrust"), BRAKE("brake"), LOW_POWER("low_power");

        private final String animation;

        Pose(String animation) {
            this.animation = animation;
        }

        public String animation() {
            return animation;
        }
    }

    /** Change in speed (blocks per tick, per tick) that counts as thrusting or braking; below it the body just coasts. */
    public static final double ACCEL_THRESHOLD = 0.002;
    /** Speed (blocks per tick) below which braking has nothing left to slow. */
    public static final double REST_SPEED = 0.01;

    /**
     * Picks the pose from how the speed changed since the last tick. Low power wins over everything; speeding up is thrust,
     * slowing down is braking, and a steady drift (or standing still) is idle.
     */
    public static Pose pose(double speed, double previousSpeed, boolean lowPower) {
        if (lowPower) {
            return Pose.LOW_POWER;
        }
        double change = speed - previousSpeed;
        if (change > ACCEL_THRESHOLD) {
            return Pose.THRUST;
        }
        if (change < -ACCEL_THRESHOLD && previousSpeed > REST_SPEED) {
            return Pose.BRAKE;
        }
        return Pose.IDLE;
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
}
