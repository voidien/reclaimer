package com.voidreclaimer.flight;

import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

/** Pure thruster-flight math, independent of entities and config so it can be unit tested. */
public final class FlightPhysics {
    private FlightPhysics() {}

    /**
     * One tick of thruster physics.
     *
     * @param velocity  current velocity in blocks/tick
     * @param thrust    un-normalized thrust direction (zero when no key is held)
     * @param powered   false in low power mode: no thrust, no dampener, pure drift
     * @param dampener  inertial dampener enabled
     * @param stats     body stats
     * @return the new velocity, never faster than {@link FlightStats#maxSpeed()}
     */
    public static Vec3 step(Vec3 velocity, Vec3 thrust, boolean powered, boolean dampener, FlightStats stats) {
        return step(velocity, thrust, powered, dampener, stats, CloudDrag.NONE);
    }

    /**
     * One tick of thruster physics including dust cloud drag. Drag is physical, so it also applies with the
     * dampener off and in low power mode: inside a cloud velocity decays, and speeds above the cloud's reduced top
     * speed bleed off smoothly instead of snapping.
     */
    public static Vec3 step(Vec3 velocity, Vec3 thrust, boolean powered, boolean dampener, FlightStats stats, CloudDrag drag) {
        return step(velocity, thrust, powered, dampener, stats, drag, 0.0);
    }

    /**
     * One tick of thruster physics under gravity, as on ruined Earth.
     *
     * <p>{@code weight} is the downward acceleration (blocks/tick²) the body must out-thrust; 0 is zero-G and gives
     * exactly the weightless step. With the dampener on, weight comes first: thrust is spent cancelling it (up to the
     * body's whole thrust, see {@link #weightSupport}) and only the remainder steers. A body that cannot cover its
     * weight still falls, but slower, and does not steer. With the dampener off, or without power, the weight pulls in
     * full. Braking happens before weight is added, so a falling body slows to the fall speed its unsupported weight
     * allows instead of being stopped dead.
     */
    public static Vec3 step(Vec3 velocity, Vec3 thrust, boolean powered, boolean dampener, FlightStats stats, CloudDrag drag,
                            double weight) {
        return step(velocity, thrust, powered, dampener, stats, drag, weight, Vec3.ZERO);
    }

    /**
     * Per tick, a body in wind moves this fraction of the way from its own horizontal velocity to the wind's: a free
     * body is carried at wind speed after a few seconds, and a dampener whose brake is stronger than the strongest
     * push ({@code WIND_COUPLING} times the wind speed) holds still. Ships use the same coupling.
     */
    public static final double WIND_COUPLING = 0.02;

    /**
     * One tick of thruster physics under gravity and in wind ({@code wind} is the air's horizontal velocity in
     * blocks/tick; zero anywhere but ruined Earth). The wind acts after thrust and braking, before the cloud and the
     * top speed, so a key or the dampener always has the last word over the drift.
     */
    public static Vec3 step(Vec3 velocity, Vec3 thrust, boolean powered, boolean dampener, FlightStats stats, CloudDrag drag,
                            double weight, Vec3 wind) {
        double startSpeed = velocity.length();
        double support = weightSupport(stats, weight, dampener, powered);
        if (powered) {
            if (thrust.lengthSqr() > 1.0E-6) {
                double steering = stats.acceleration() - support;
                if (steering > 0.0) {
                    velocity = velocity.add(thrust.normalize().scale(steering));
                }
            } else if (dampener) {
                velocity = brake(velocity, stats.brakeDeceleration());
            }
        }
        if (weight > 0.0) {
            velocity = velocity.add(0.0, -(weight - support), 0.0);
        }
        if (wind.x != 0.0 || wind.z != 0.0) {
            velocity = velocity.add(windDrift(velocity, wind));
        }
        if (drag.density() > 0.0) {
            velocity = velocity.scale(1.0 - drag.dragPerTick() * drag.density());
            double cloudMax = stats.maxSpeed() * (1.0 - drag.speedPenalty() * drag.density());
            double speed = velocity.length();
            // Thrust can't push past the cloud's top speed; arriving faster bleeds off gradually.
            double allowed = Math.max(cloudMax, startSpeed * (1.0 - CLOUD_OVERSPEED_BLEED));
            if (speed > allowed) {
                velocity = velocity.scale(allowed / speed);
            }
        }
        return capSpeed(velocity, stats.maxSpeed());
    }

    /** The change of velocity the wind makes this tick: a fraction of the horizontal difference between the air and the body. */
    public static Vec3 windDrift(Vec3 velocity, Vec3 wind) {
        return new Vec3((wind.x - velocity.x) * WIND_COUPLING, 0.0, (wind.z - velocity.z) * WIND_COUPLING);
    }

    /**
     * Acceleration (blocks/tick²) the thrusters spend cancelling {@code weight} this tick: all of it, or all the thrust
     * there is when that is less. Nothing without the dampener (the body is not holding station) or without power.
     */
    public static double weightSupport(FlightStats stats, double weight, boolean dampener, boolean powered) {
        if (!powered || !dampener || weight <= 0.0) {
            return 0.0;
        }
        return Math.min(stats.acceleration(), weight);
    }

    /** The share of the body's thrust spent holding up its weight, 0 to 1: what hovering costs in energy. */
    public static double hoverShare(FlightStats stats, double weight, boolean dampener, boolean powered) {
        double acceleration = stats.acceleration();
        return acceleration <= 0.0 ? 0.0 : weightSupport(stats, weight, dampener, powered) / acceleration;
    }

    /** Max fraction of speed shed per tick when above a cloud's reduced top speed. */
    static final double CLOUD_OVERSPEED_BLEED = 0.08;

    /**
     * Dust cloud drag at a position.
     *
     * @param density      cloud density 0..1 (0 outside clouds)
     * @param dragPerTick  fraction of velocity lost per tick at full density
     * @param speedPenalty top speed reduction at full density
     */
    public record CloudDrag(double density, double dragPerTick, double speedPenalty) {
        public static final CloudDrag NONE = new CloudDrag(0.0, 0.0, 0.0);
    }

    /**
     * View-relative thrust direction: forward/back along the look vector, strafe along the view's right vector
     * (horizontal, since there is no roll) and up/down along the view's up vector.
     */
    public static Vec3 thrustDirection(float xRot, float yRot, FlightInput input) {
        Vec3 forward = viewVector(xRot, yRot);
        Vec3 up = viewVector(xRot - 90f, yRot);
        Vec3 right = forward.cross(up);
        return forward.scale(input.forwardAxis())
                .add(right.scale(input.strafeAxis()))
                .add(up.scale(input.verticalAxis()));
    }

    /** Same as {@code Entity.calculateViewVector}: yaw 0 looks toward +Z, positive pitch looks down. */
    public static Vec3 viewVector(float xRot, float yRot) {
        float pitch = xRot * Mth.DEG_TO_RAD;
        float yaw = -yRot * Mth.DEG_TO_RAD;
        float cosYaw = Mth.cos(yaw);
        float sinYaw = Mth.sin(yaw);
        float cosPitch = Mth.cos(pitch);
        float sinPitch = Mth.sin(pitch);
        return new Vec3(sinYaw * cosPitch, -sinPitch, cosYaw * cosPitch);
    }

    public static Vec3 brake(Vec3 velocity, double deceleration) {
        double speed = velocity.length();
        if (speed <= deceleration) {
            return Vec3.ZERO;
        }
        return velocity.scale((speed - deceleration) / speed);
    }

    public static Vec3 capSpeed(Vec3 velocity, double maxSpeed) {
        if (velocity.lengthSqr() > maxSpeed * maxSpeed) {
            return velocity.normalize().scale(maxSpeed);
        }
        return velocity;
    }

    /** Whether moving from {@code start} to {@code end} over {@code ticks} ticks breaks the speed limit. */
    public static boolean exceedsSpeedLimit(Vec3 start, Vec3 end, int ticks, double maxSpeed, double tolerance, double slack) {
        return start.distanceTo(end) > maxSpeed * ticks * tolerance + slack;
    }
}
