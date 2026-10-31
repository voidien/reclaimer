package com.voidreclaimer.ship;

import java.util.ArrayList;
import java.util.List;
import org.joml.Vector3d;
import org.joml.Vector3dc;

/**
 * Turns what the pilot asks for into a throttle per thruster. Pure and config-free so it can be unit-tested.
 *
 * <p>Everything is in the ship's own frame: {@code forward}, {@code right} and {@code up} are the Helm's axes, the
 * centre of mass is where the ship turns about. A thruster pushes along its {@code push} direction at its {@code at}
 * position, so it adds force along {@code push} and torque {@code (at - centre) x push}. The pilot asks for a force
 * direction and a torque direction (each component -1, 0 or 1 along the Helm's axes); a thruster is opened in
 * proportion to how well its own force and torque serve that, and shut if it works against either. A translation key
 * alone does not care about the torque it causes and a rotation key alone not about the force (the holds, or the
 * pilot's other hand, deal with that); when both are asked for at once only thrusters that serve both, or serve one
 * without hurting the other, open.
 */
public final class ThrustAllocator {
    /** A thruster of the ship: where it is, which way it pushes (ship frame, unit vector), and how hard it can at full throttle. */
    public record Thruster(Vector3dc at, Vector3dc push, double maxForce) {}

    /** The ship's axes in its own frame. */
    public record Frame(Vector3dc forward, Vector3dc right, Vector3dc up, Vector3dc centreOfMass) {}

    /** Any component of a thruster's contribution below this (as a fraction of the best possible) is treated as none. */
    static final double DEAD_ZONE = 0.05;

    private ThrustAllocator() {}

    /** The throttles (0 to 1) for {@code thrusters}, in order, that best give the pilot's keys in {@code frame}. */
    public static double[] throttles(List<Thruster> thrusters, Frame frame, PilotInput input) {
        Vector3d force = wanted(frame, input.surge(), input.sway(), input.heave());
        // Rotation about an axis follows the right-hand rule: roll right is a torque about -forward, nose up about -right,
        // yaw left about +up.
        Vector3d torque = wanted(frame, -input.roll(), -input.pitch(), input.yaw());
        return throttles(thrusters, frame, force, torque);
    }

    /**
     * The throttles for a request given as vectors: a wanted force and a wanted torque in the ship frame, each of length
     * 0 to 1 (longer ones, the keys' sums, are cut to 1). The length is the gain: the holds ask for the opposite of the
     * ship's velocity and spin with a length that shrinks as the ship slows, so they ease off instead of overshooting.
     */
    public static double[] throttles(List<Thruster> thrusters, Frame frame, Vector3dc wantedForce, Vector3dc wantedTorque) {
        Vector3d force = new Vector3d(wantedForce);
        Vector3d torque = new Vector3d(wantedTorque);
        double[] out = new double[thrusters.size()];
        if (force.lengthSquared() == 0.0 && torque.lengthSquared() == 0.0) {
            return out;
        }
        if (force.lengthSquared() > 1.0) {
            force.normalize();
        }
        if (torque.lengthSquared() > 1.0) {
            torque.normalize();
        }
        for (int i = 0; i < thrusters.size(); i++) {
            Thruster t = thrusters.get(i);
            double along = force.lengthSquared() > 0.0 ? t.push().dot(force) : 0.0;
            Vector3d lever = new Vector3d(t.at()).sub(frame.centreOfMass());
            Vector3d itsTorque = lever.cross(new Vector3d(t.push()), new Vector3d());
            double turning = torque.lengthSquared() > 0.0 && itsTorque.lengthSquared() > 0.0
                    ? itsTorque.normalize().dot(torque) : 0.0;
            // Helping one request and hurting the other cancels out: such a thruster stays shut rather than fighting the
            // other request (the holds would otherwise open a jet that brakes drift while feeding the spin).
            double want = along + turning;
            out[i] = want > DEAD_ZONE ? Math.min(1.0, want) : 0.0;
        }
        return out;
    }

    /** Which of the six pushes the ship can make at all: {forward, back, right, left, up, down} as the best force fraction. */
    public static double[] translationCoverage(List<Thruster> thrusters, Frame frame) {
        Vector3dc[] dirs = {frame.forward(), new Vector3d(frame.forward()).negate(), frame.right(), new Vector3d(frame.right()).negate(),
                frame.up(), new Vector3d(frame.up()).negate()};
        double[] out = new double[6];
        for (int d = 0; d < 6; d++) {
            for (Thruster t : thrusters) {
                out[d] = Math.max(out[d], t.push().dot(dirs[d]));
            }
        }
        return out;
    }

    /** Which of the six turns the ship can make: {roll right, roll left, nose up, nose down, yaw left, yaw right}. */
    public static double[] rotationCoverage(List<Thruster> thrusters, Frame frame) {
        Vector3dc[] axes = {new Vector3d(frame.forward()).negate(), frame.forward(), new Vector3d(frame.right()).negate(), frame.right(),
                frame.up(), new Vector3d(frame.up()).negate()};
        double[] out = new double[6];
        for (int a = 0; a < 6; a++) {
            for (Thruster t : thrusters) {
                Vector3d itsTorque = new Vector3d(t.at()).sub(frame.centreOfMass()).cross(new Vector3d(t.push()));
                if (itsTorque.lengthSquared() > 0.0) {
                    out[a] = Math.max(out[a], itsTorque.normalize().dot(axes[a]));
                }
            }
        }
        return out;
    }

    /**
     * What the holds ask for when the pilot leaves an axis alone: the opposite of the ship's velocity (the dampener) and of
     * its spin (the RCS lock), in the ship frame, but only along the axes the pilot is not using, so a key always wins over
     * the hold on its own axis. Speeds below {@code deadband} count as stopped; from there the request grows with the
     * speed up to full at {@code ramp} (blocks or radians per second), so a hold eases off as the ship comes to rest
     * rather than hammering it back and forth across the deadband. Returns {force, torque}.
     */
    public static Vector3d[] holdRequest(Frame frame, PilotInput keys, Vector3dc velocityShipFrame, Vector3dc spinShipFrame,
                                         boolean dampener, boolean rcsLock, double deadband, double ramp) {
        Vector3d force = new Vector3d();
        Vector3d torque = new Vector3d();
        if (dampener) {
            force.add(counter(frame.forward(), velocityShipFrame, keys.surge(), deadband, ramp))
                 .add(counter(frame.right(), velocityShipFrame, keys.sway(), deadband, ramp))
                 .add(counter(frame.up(), velocityShipFrame, keys.heave(), deadband, ramp));
        }
        if (rcsLock) {
            // Spin about an axis: the pilot's roll key owns the forward axis, pitch the right axis, yaw the up axis.
            torque.add(counter(frame.forward(), spinShipFrame, keys.roll(), deadband, ramp))
                  .add(counter(frame.right(), spinShipFrame, keys.pitch(), deadband, ramp))
                  .add(counter(frame.up(), spinShipFrame, keys.yaw(), deadband, ramp));
        }
        return new Vector3d[] {force, torque};
    }

    /** The push against {@code motion}'s component along {@code axis}, unless the pilot holds that axis or it is already still. */
    private static Vector3d counter(Vector3dc axis, Vector3dc motion, int keyHeld, double deadband, double ramp) {
        double along = motion.dot(axis);
        if (keyHeld != 0 || Math.abs(along) < deadband) {
            return new Vector3d();
        }
        return new Vector3d(axis).mul(-Math.max(-1.0, Math.min(1.0, along / ramp)));
    }

    private static Vector3d wanted(Frame frame, int forward, int right, int up) {
        return new Vector3d(frame.forward()).mul(forward).add(new Vector3d(frame.right()).mul(right)).add(new Vector3d(frame.up()).mul(up));
    }

    /** Helper for tests and the Helm: a unit vector. */
    public static Vector3d unit(double x, double y, double z) {
        return new Vector3d(x, y, z).normalize();
    }

    public static List<Thruster> none() {
        return new ArrayList<>();
    }
}
