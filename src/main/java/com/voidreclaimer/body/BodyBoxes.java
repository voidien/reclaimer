package com.voidreclaimer.body;

import com.voidreclaimer.api.body.Chassis;
import com.voidreclaimer.api.body.HitBox;
import com.voidreclaimer.api.body.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaterniond;
import org.joml.Quaterniondc;
import org.joml.Vector3d;
import org.joml.Vector3dc;

/**
 * Where a big body's boxes are in the world, and whether two boxes overlap. Pure, so it can be unit tested. Nothing moves or stops a
 * body yet: flight does not read this.
 *
 * <p>A body's own axes are right +X, up +Y and forward -Z, turned about its feet, as {@code BodyLook.turn} turns the model. A box
 * stays aligned with the body, so a world block or anything else that does not turn is a box with no turn at all.
 */
public final class BodyBoxes {
    /** Overlaps smaller than this do not count, so two boxes that only touch do not push. */
    private static final double TOUCHING = 1.0E-7;
    /** A cross of two nearly parallel axes says nothing, and its length is too small to divide by. */
    private static final double PARALLEL = 1.0E-10;
    private static final Quaterniondc NO_TURN = new Quaterniond();

    /**
     * A box in the world.
     *
     * @param socket the socket it belongs to, empty for anything that is not a body
     * @param middle its middle
     * @param half   half its size along its own axes
     * @param turn   its own axes to the world's
     */
    public record Box(String socket, Vector3dc middle, Vector3dc half, Quaterniondc turn) {
        /** A box that does not turn, such as a block. */
        public static Box of(AABB aabb) {
            Vec3 middle = aabb.getCenter();
            return new Box("", new Vector3d(middle.x, middle.y, middle.z),
                    new Vector3d(aabb.getXsize() / 2.0, aabb.getYsize() / 2.0, aabb.getZsize() / 2.0), NO_TURN);
        }

        private Vector3d[] axes() {
            return new Vector3d[] {turn.transform(new Vector3d(1, 0, 0)), turn.transform(new Vector3d(0, 1, 0)), turn.transform(new Vector3d(0, 0, 1))};
        }

        /** How far the box reaches along {@code axis} from its middle. */
        private double reach(Vector3d[] axes, Vector3dc axis) {
            return Math.abs(axes[0].dot(axis)) * half.x() + Math.abs(axes[1].dot(axis)) * half.y() + Math.abs(axes[2].dot(axis)) * half.z();
        }
    }

    private BodyBoxes() {}

    /** A body's own axes to the world's, for a body looking along {@code yaw} and {@code pitch}, in Minecraft's degrees. */
    public static Quaterniond turn(float yaw, float pitch) {
        return new Quaterniond().rotationY(Math.toRadians(180.0 - yaw)).rotateX(Math.toRadians(-pitch));
    }

    /** The boxes of the sections {@code fitted} says are there, for a body with its feet at {@code feet}. */
    public static List<Box> place(Chassis chassis, Predicate<String> fitted, Vec3 feet, float yaw, float pitch, double scale) {
        Quaterniondc turn = turn(yaw, pitch);
        List<Box> boxes = new ArrayList<>();
        for (Socket socket : chassis.sockets()) {
            if (fitted.test(socket.id())) {
                for (HitBox box : socket.boxes()) {
                    Vector3d middle = turn.transform(new Vector3d(box.at().x, box.at().y, -box.at().z)).mul(scale).add(feet.x, feet.y, feet.z);
                    Vector3d half = new Vector3d(box.size().x, box.size().y, box.size().z).mul(scale / 2.0);
                    boxes.add(new Box(socket.id(), middle, half, turn));
                }
            }
        }
        return boxes;
    }

    /**
     * The shortest move that takes {@code moving} out of {@code fixed}, or nothing when they do not overlap. Two boxes are apart when
     * some axis has a gap between them, and only 15 axes need looking at: the three of each box, and the crosses of those.
     */
    public static Optional<Vector3d> push(Box moving, Box fixed) {
        Vector3d[] ours = moving.axes();
        Vector3d[] theirs = fixed.axes();
        Vector3d apart = new Vector3d(fixed.middle()).sub(moving.middle());
        Vector3d best = null;
        double least = Double.MAX_VALUE;
        for (Vector3d axis : candidates(ours, theirs)) {
            double along = apart.dot(axis);
            double overlap = moving.reach(ours, axis) + fixed.reach(theirs, axis) - Math.abs(along);
            if (overlap <= TOUCHING) {
                return Optional.empty();
            }
            if (overlap < least) {
                least = overlap;
                best = new Vector3d(axis).mul(along > 0.0 ? -overlap : overlap);
            }
        }
        return Optional.ofNullable(best);
    }

    private static List<Vector3d> candidates(Vector3d[] ours, Vector3d[] theirs) {
        List<Vector3d> axes = new ArrayList<>(List.of(ours));
        axes.addAll(List.of(theirs));
        for (Vector3d our : ours) {
            for (Vector3d their : theirs) {
                Vector3d cross = our.cross(their, new Vector3d());
                if (cross.lengthSquared() > PARALLEL) {
                    axes.add(cross.normalize());
                }
            }
        }
        return axes;
    }
}
