package com.voidreclaimer.ship;

import java.util.List;
import org.joml.Matrix3dc;
import org.joml.Vector3d;
import org.joml.Vector3dc;

/**
 * Steers a ship with the mouse: turns what the pilot does into a wanted torque for {@link ThrustAllocator}. Pure and config-free so
 * it can be unit-tested, and it knows nothing of the Helm, which ticks it.
 *
 * <p>Two ways of steering share this interface, so they can be compared on the same ship. {@link Direct} is Space Engineers' way:
 * moving the mouse asks for a turn at that speed, and a still mouse asks the ship to stop. {@link Aimed} swings the nose toward
 * where the pilot looks. Both work through the ship's real spin, so both have inertia.
 *
 * <p>Everything is in the ship's own frame. Rotation follows the right-hand rule, as {@code lever x push} does in
 * {@link ThrustAllocator}: spin about {@code right} raises the nose, about {@code up} turns it left, about {@code forward} rolls
 * right. The torque returned has one part per axis, each -1 to 1 of what the thrusters can do, and a key held on an axis beats the
 * mouse on it. It covers stopping as well, so it replaces the RCS lock's torque rather than adding to it.
 */
public interface AimController {
    /** Seconds between two requests: the server's tick. A turn must be stopped by requests this far apart. */
    double TICK_SECONDS = 1.0 / 20.0;

    /** The wanted torque for this tick, in the ship frame. */
    Vector3d torque(ThrustAllocator.Frame frame, Request request);

    /**
     * What the pilot and the ship are doing.
     *
     * @param keys       the pilot's keys
     * @param mouseRight degrees the view moved right this tick, for {@link Direct}
     * @param mouseUp    degrees the view moved up this tick, for {@link Direct}
     * @param look       where the pilot looks, a unit vector in the ship frame, for {@link Aimed}
     * @param spin       the ship's spin in the ship frame, radians per second
     * @param holdRoll   whether a roll with no key held is stopped: the RCS lock
     * @param authority  what the thrusters can do
     */
    record Request(PilotInput keys, double mouseRight, double mouseUp, Vector3dc look, Vector3dc spin, boolean holdRoll, Authority authority) {}

    /**
     * How fast the thrusters can change the ship's spin about each of its axes, in radians per second squared. Each is the weaker
     * of the two ways round: that is what has to stop a turn, whichever way it goes.
     */
    record Authority(double pitch, double yaw, double roll) {
        /** {@code inverseInertia} is in the ship frame, about the centre of mass. */
        public static Authority of(List<ThrustAllocator.Thruster> thrusters, ThrustAllocator.Frame frame, Matrix3dc inverseInertia) {
            return new Authority(about(frame.right(), thrusters, frame, inverseInertia), about(frame.up(), thrusters, frame, inverseInertia),
                    about(frame.forward(), thrusters, frame, inverseInertia));
        }

        private static double about(Vector3dc axis, List<ThrustAllocator.Thruster> thrusters, ThrustAllocator.Frame frame, Matrix3dc inverseInertia) {
            double weakest = Math.min(torqueAbout(axis, thrusters, frame), torqueAbout(new Vector3d(axis).negate(), thrusters, frame));
            return weakest * inverseInertia.transform(new Vector3d(axis)).dot(axis);
        }

        /** The torque the allocator really gets along {@code axis} when asked for all of it: it opens each jet only as far as it helps. */
        private static double torqueAbout(Vector3dc axis, List<ThrustAllocator.Thruster> thrusters, ThrustAllocator.Frame frame) {
            double[] throttles = ThrustAllocator.throttles(thrusters, frame, new Vector3d(), axis);
            double total = 0.0;
            for (int i = 0; i < throttles.length; i++) {
                ThrustAllocator.Thruster thruster = thrusters.get(i);
                Vector3d lever = new Vector3d(thruster.at()).sub(frame.centreOfMass());
                total += throttles[i] * thruster.maxForce() * lever.cross(thruster.push(), new Vector3d()).dot(axis);
            }
            return total;
        }
    }

    /**
     * Moving the mouse asks for a turn at that speed, and a still mouse asks the ship to stop, so the ship stops by itself.
     *
     * @param sensitivity how much of the view's speed the ship is asked for
     * @param maxRate     the fastest turn asked for, radians per second
     * @param stopSeconds the longest a turn may take to stop: a ship with weak thrusters for its size turns no faster than that allows
     */
    record Direct(double sensitivity, double maxRate, double stopSeconds) implements AimController {
        public static final Direct DEFAULT = new Direct(1.0, Math.toRadians(90.0), 2.0);

        @Override
        public Vector3d torque(ThrustAllocator.Frame frame, Request request) {
            return steer(frame, request, rate(request.mouseUp(), request.authority().pitch()), rate(-request.mouseRight(), request.authority().yaw()));
        }

        private double rate(double degreesThisTick, double authority) {
            double limit = Math.min(maxRate, authority * stopSeconds);
            return Math.max(-limit, Math.min(limit, Math.toRadians(degreesThisTick) / TICK_SECONDS * sensitivity));
        }
    }

    /**
     * Swings the nose toward where the pilot looks. It turns no faster than it can stop by the time it gets there, and eases over
     * the last of the way instead of snapping onto the target.
     *
     * @param maxRate the fastest turn asked for, radians per second
     * @param settle  turning speed per radian still to go, per second: the ease near the target
     */
    record Aimed(double maxRate, double settle) implements AimController {
        public static final Aimed DEFAULT = new Aimed(Math.toRadians(90.0), 3.0);

        @Override
        public Vector3d torque(ThrustAllocator.Frame frame, Request request) {
            Vector3d turn = turnToward(frame, request.look());
            return steer(frame, request, rate(turn.dot(frame.right()), request.authority().pitch()), rate(turn.dot(frame.up()), request.authority().yaw()));
        }

        private double rate(double radians, double authority) {
            double left = Math.abs(radians);
            return Math.copySign(Math.min(Math.min(maxRate, settle * left), stoppableRate(left, authority)), radians);
        }

        /** The rotation that takes {@code forward} onto {@code look}: its axis, as long as its angle in radians. */
        private static Vector3d turnToward(ThrustAllocator.Frame frame, Vector3dc look) {
            Vector3d axis = frame.forward().cross(look, new Vector3d());
            double sine = axis.length();
            double angle = Math.atan2(sine, frame.forward().dot(look));
            if (sine > 1.0E-9) {
                return axis.mul(angle / sine);
            }
            // Dead ahead nothing is wanted; dead astern every way round is as good, so turn left.
            return new Vector3d(frame.up()).mul(angle);
        }
    }

    /**
     * The fastest spin that can still stop within {@code radians}, in radians per second, at {@code authority} radians per second
     * squared: the root of {@code rate^2 / (2 * authority) + rate * TICK_SECONDS = radians}. The second term is the tick: a request
     * only acts on the tick after it, and the spin must be gone by then.
     */
    static double stoppableRate(double radians, double authority) {
        if (authority <= 0.0) {
            return 0.0;
        }
        return authority * (Math.sqrt(TICK_SECONDS * TICK_SECONDS + 2.0 * radians / authority) - TICK_SECONDS);
    }

    /** Asks for {@code noseUp} and {@code turnLeft}, radians per second, and no roll; a key held on an axis has it instead. */
    private static Vector3d steer(ThrustAllocator.Frame frame, Request request, double noseUp, double turnLeft) {
        Vector3dc spin = request.spin();
        Authority authority = request.authority();
        PilotInput keys = request.keys();
        Vector3d torque = new Vector3d();
        torque.add(axis(frame.right(), keys.pitch(), spin, noseUp, authority.pitch()));
        torque.add(axis(frame.up(), keys.yaw(), spin, turnLeft, authority.yaw()));
        if (keys.roll() != 0 || request.holdRoll()) {
            torque.add(axis(frame.forward(), keys.roll(), spin, 0.0, authority.roll()));
        }
        return torque;
    }

    /** One axis: all of the torque for a key, otherwise as much of it as closes the gap to {@code wanted} in one tick. */
    private static Vector3d axis(Vector3dc axis, int key, Vector3dc spin, double wanted, double authority) {
        if (key != 0) {
            return new Vector3d(axis).mul(key);
        }
        if (authority <= 0.0) {
            return new Vector3d();
        }
        double share = (wanted - spin.dot(axis)) / (authority * TICK_SECONDS);
        return new Vector3d(axis).mul(Math.max(-1.0, Math.min(1.0, share)));
    }
}
