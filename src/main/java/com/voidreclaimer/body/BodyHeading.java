package com.voidreclaimer.body;

import com.voidreclaimer.flight.FlightStats;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

/**
 * Where a body points, in Minecraft's degrees. The view turns at once and a big body follows it, as fast as its weight lets it: the
 * view asks and the heading answers. A smaller body's heading is its view. Pure, so it can be unit tested.
 */
public record BodyHeading(float yaw, float pitch) {
    /**
     * Degrees a second for each unit of turn rate per unit of mass. A basic Ship turns at 45, a basic Shuttle at 123 and a basic Titan at
     * 15: half turns of 4, 1.5 and 12 seconds, the owner's numbers. Weight scales it, so a loaded body turns slower than an empty one.
     */
    public static final double TURN_FACTOR = 2400.0;
    /** A body with nothing to help it turn still turns, or it could only ever face one way. Degrees a second. */
    public static final double SLOWEST_TURN = 6.0;
    /** Degrees a second, so a big body stripped of its weight still doesn't whip round. */
    public static final double FASTEST_TURN = 240.0;
    /** The smallest size class that turns with weight, the Ship's. */
    private static final int HEAVY_RANK = 2;
    /** A turn eases in over its last few degrees, each tick going this share of what is left: the 3 a second the Helm's aim uses. */
    private static final double EASE = 0.15;
    /** Degrees from the view at which a turn is over. */
    private static final double ARRIVED = 0.02;
    /** How many times a turn is halved to fit, when the whole of it needs more of a push than a tick allows. */
    private static final int HALVINGS = 3;

    /**
     * Where a turn got to.
     *
     * @param heading where the body points now
     * @param push    how far that moved it, in blocks: a move, not a speed
     */
    public record Turn(BodyHeading heading, Vec3 push) {}

    /** The most a body can turn in a tick, in degrees. A body too small to turn with weight has no limit. */
    public static double degreesPerTick(BodyStats stats, double factor, double slowest, double fastest) {
        if (stats.sizeRank() < HEAVY_RANK) {
            return Double.POSITIVE_INFINITY;
        }
        return Mth.clamp(factor * stats.turnRate() / stats.mass(), slowest, fastest) / FlightStats.TICKS_PER_SECOND;
    }

    /** The next heading on the way to the view, the short way round, with yaw and pitch arriving together. */
    public BodyHeading toward(float viewYaw, float viewPitch, double degreesPerTick) {
        float turnYaw = Mth.wrapDegrees(viewYaw - yaw);
        float turnPitch = viewPitch - pitch;
        double left = Math.hypot(turnYaw, turnPitch);
        if (Double.isInfinite(degreesPerTick) || left <= ARRIVED) {
            return new BodyHeading(viewYaw, viewPitch);
        }
        return partWay(turnYaw, turnPitch, Math.min(degreesPerTick, left * EASE) / left);
    }

    /** {@code to}, or as far toward it as {@code degrees} goes, the short way round. */
    public BodyHeading atMost(BodyHeading to, double degrees) {
        float turnYaw = Mth.wrapDegrees(to.yaw - yaw);
        float turnPitch = to.pitch - pitch;
        double left = Math.hypot(turnYaw, turnPitch);
        return left <= degrees ? to : partWay(turnYaw, turnPitch, degrees / left);
    }

    private BodyHeading partWay(float turnYaw, float turnPitch, double share) {
        return new BodyHeading((float) (yaw + turnYaw * share), (float) (pitch + turnPitch * share));
    }

    /**
     * Turns as far toward {@code next} as the blocks allow. A long body swings its nose much further than it turns, so a turn the push
     * limit refuses may fit when it is halved: a heavy body nudging off a wall turns more slowly, and does not stop dead. If no share of
     * it fits the heading stays, and the view keeps going.
     *
     * @param next   where the body would go with nothing in the way
     * @param boxes  the body's boxes at a heading, in the same order each time
     * @param blocks all that can stop it, out to {@link BodyBoxes#MOST_PUSH} from the boxes at {@code next}
     */
    public Turn turn(BodyHeading next, Function<BodyHeading, List<BodyBoxes.Box>> boxes, List<BodyBoxes.Box> blocks) {
        if (equals(next)) {
            return new Turn(this, Vec3.ZERO);
        }
        List<BodyBoxes.Box> now = boxes.apply(this);
        float turnYaw = Mth.wrapDegrees(next.yaw - yaw);
        float turnPitch = next.pitch - pitch;
        double share = 1.0;
        for (int i = 0; i <= HALVINGS; i++) {
            BodyHeading heading = i == 0 ? next : partWay(turnYaw, turnPitch, share);
            Optional<Vec3> push = BodyBoxes.turnPush(now, boxes.apply(heading), blocks);
            if (push.isPresent()) {
                return new Turn(heading, push.get());
            }
            share /= 2.0;
        }
        return new Turn(this, Vec3.ZERO);
    }
}
