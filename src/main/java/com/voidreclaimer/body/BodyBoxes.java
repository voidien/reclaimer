package com.voidreclaimer.body;

import com.voidreclaimer.api.body.Chassis;
import com.voidreclaimer.api.body.HitBox;
import com.voidreclaimer.api.body.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;
import javax.annotation.Nullable;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaterniond;
import org.joml.Quaterniondc;
import org.joml.Vector3d;
import org.joml.Vector3dc;

/**
 * Where a big body's boxes are in the world, whether two boxes overlap, how far a body can move among blocks, and whether it can turn
 * among them. Pure, so it can be unit tested.
 *
 * <p>A body's own axes are right +X, up +Y and forward -Z, turned about its feet, as {@code BodyLook.turn} turns the model. A box
 * stays aligned with the body, so a world block or anything else that does not turn is a box with no turn at all.
 */
public final class BodyBoxes {
    /** Overlaps smaller than this do not count, so two boxes that only touch do not push. */
    private static final double TOUCHING = 1.0E-7;
    /** A cross of two nearly parallel axes says nothing, and its length is too small to divide by. */
    private static final double PARALLEL = 1.0E-10;
    /** The longest step {@link #resolve} takes at a time. */
    private static final double SUB_STEP = 0.5;
    /** How many times one tick's move can lose part of itself to what it hit. After that the rest is dropped, so a move always ends. */
    private static final int SLIDES = 8;
    /** A move or a speed along an axis below this is no move. */
    private static final double STILL = 1.0E-12;
    /** The furthest a turn can push a body in a tick, in blocks: more than this and the server's speed check could see it. */
    public static final double MOST_PUSH = 0.25;
    /** How many pushes a turn can take to get clear. */
    private static final int PUSHES = 4;
    /** Two pushes this far apart, as the cosine of the angle, are from opposite sides: the body is wedged, and no more pushes will help. */
    private static final double OPPOSITE = -0.5;
    private static final Vector3dc NOT_MOVED = new Vector3d();
    private static final Quaterniondc NO_TURN = new Quaterniond();
    private static final Vector3dc WORLD_X = new Vector3d(1, 0, 0);
    private static final Vector3dc WORLD_Y = new Vector3d(0, 1, 0);
    private static final Vector3dc WORLD_Z = new Vector3d(0, 0, 1);

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

        /** The smallest box along the world's axes that holds this one. */
        private AABB bounds() {
            Vector3d[] axes = axes();
            double x = reach(axes, WORLD_X);
            double y = reach(axes, WORLD_Y);
            double z = reach(axes, WORLD_Z);
            return new AABB(middle.x() - x, middle.y() - y, middle.z() - z, middle.x() + x, middle.y() + y, middle.z() + z);
        }
    }

    /** One of a body's boxes this tick: the blocks it can reach, and how deep in one of them it started. */
    private record Moving(Box box, List<Box> near, double deepest) {}

    /** The first place a move meets a block: how far through the step, as a fraction, and the axis the move can no longer go along. */
    private record Contact(double time, Vector3d axis) {}

    /** The axes between two turns. A body's boxes share one turn and blocks share none, so this is rarely worked out again. */
    private record Axes(Quaterniondc ourTurn, Quaterniondc theirTurn, Vector3d[] ours, Vector3d[] theirs, List<Vector3d> all) {
        static Axes between(Box moving, Box fixed, @Nullable Axes last) {
            if (last != null && last.ourTurn == moving.turn() && last.theirTurn == fixed.turn()) {
                return last;
            }
            Vector3d[] ours = moving.axes();
            Vector3d[] theirs = fixed.axes();
            return new Axes(moving.turn(), fixed.turn(), ours, theirs, candidates(ours, theirs));
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

    /** A box's eight corners in the world. The bits of each index pick its side on the box's own axes, as {@code DebugShapes.box} wants. */
    public static List<Vec3> corners(Box box) {
        List<Vec3> corners = new ArrayList<>();
        for (int i = 0; i < 8; i++) {
            Vector3d local = new Vector3d((i & 1) == 0 ? -box.half().x() : box.half().x(), (i & 2) == 0 ? -box.half().y() : box.half().y(),
                    (i & 4) == 0 ? -box.half().z() : box.half().z());
            box.turn().transform(local).add(box.middle());
            corners.add(new Vec3(local.x, local.y, local.z));
        }
        return corners;
    }

    /**
     * The shortest move that takes {@code moving} out of {@code fixed}, or nothing when they do not overlap. Two boxes are apart when
     * some axis has a gap between them, and only 15 axes need looking at: the three of each box, and the crosses of those.
     */
    public static Optional<Vector3d> push(Box moving, Box fixed) {
        Vector3d out = new Vector3d();
        double depth = depth(moving, NOT_MOVED, fixed, Axes.between(moving, fixed, null), out);
        return depth > 0.0 ? Optional.of(out.mul(depth)) : Optional.empty();
    }

    /** How deep {@code moving}, shifted {@code by}, is in {@code fixed}, with the way out in {@code out}, or 0 if they only touch or are apart. */
    private static double depth(Box moving, Vector3dc by, Box fixed, Axes axes, Vector3d out) {
        double apartX = fixed.middle().x() - moving.middle().x() - by.x();
        double apartY = fixed.middle().y() - moving.middle().y() - by.y();
        double apartZ = fixed.middle().z() - moving.middle().z() - by.z();
        double least = Double.MAX_VALUE;
        for (Vector3d axis : axes.all()) {
            double along = axis.dot(apartX, apartY, apartZ);
            double overlap = moving.reach(axes.ours(), axis) + fixed.reach(axes.theirs(), axis) - Math.abs(along);
            if (overlap <= TOUCHING) {
                return 0.0;
            }
            if (overlap < least) {
                least = overlap;
                out.set(axis).mul(along > 0.0 ? -1.0 : 1.0);
            }
        }
        return least;
    }

    /**
     * How much of {@code velocity} a body can have this tick. The move runs until a box meets a block, loses the part that goes into
     * that block, and carries on with the rest, so the body slides along what it hits. It is never longer than {@code velocity}.
     *
     * <p>A box that starts inside something can go no deeper into anything than it started, however it moves: it can back out, or go
     * along a tunnel narrower than itself, where the next wall block ahead is no deeper than the one beside it. So a body is not
     * trapped by a chassis fitted in a tunnel, and a hair of overlap left by rounding opens no way through a wall.
     *
     * @param body     the body's boxes where it is now
     * @param blocks   all that can stop it, out to the length of {@code velocity} from {@code body}
     * @param velocity what it wants to move this tick, in blocks
     */
    public static Vec3 resolve(List<Box> body, List<Box> blocks, Vec3 velocity) {
        double length = velocity.length();
        if (length <= STILL) {
            return velocity;
        }
        List<Moving> boxes = moving(body, body, blocks, length);
        Vector3d left = new Vector3d(velocity.x, velocity.y, velocity.z);
        Vector3d moved = new Vector3d();
        int slides = 0;
        while (slides < SLIDES && left.length() > STILL) {
            Vector3d step = new Vector3d(left);
            if (step.length() > SUB_STEP) {
                step.normalize(SUB_STEP);
            }
            Contact contact = first(boxes, moved, step);
            if (contact == null) {
                moved.add(step);
                left.sub(step);
            } else {
                moved.fma(contact.time(), step);
                left.fma(-contact.time(), step);
                left.fma(-left.dot(contact.axis()), contact.axis());
                slides++;
            }
        }
        return slides == 0 ? velocity : new Vec3(moved.x, moved.y, moved.z);
    }

    /**
     * Each box of {@code to} with the blocks within {@code reach} of it, and how deep the same box of {@code from} is: {@code from} is
     * where the tick starts, and a move and a turn can both go no deeper than that.
     */
    private static List<Moving> moving(List<Box> from, List<Box> to, List<Box> blocks, double reach) {
        List<AABB> bounds = blocks.stream().map(Box::bounds).toList();
        List<Moving> boxes = new ArrayList<>();
        Vector3d out = new Vector3d();
        Axes axes = null;
        for (int i = 0; i < to.size(); i++) {
            Box start = from.get(i);
            AABB started = start.bounds();
            AABB around = to.get(i).bounds().inflate(reach);
            List<Box> near = new ArrayList<>();
            double deepest = 0.0;
            for (int j = 0; j < blocks.size(); j++) {
                Box block = blocks.get(j);
                if (around.intersects(bounds.get(j))) {
                    near.add(block);
                }
                if (started.intersects(bounds.get(j))) {
                    axes = Axes.between(start, block, axes);
                    deepest = Math.max(deepest, depth(start, NOT_MOVED, block, axes, out));
                }
            }
            if (!near.isEmpty()) {
                boxes.add(new Moving(to.get(i), near, deepest));
            }
        }
        return boxes;
    }

    @Nullable
    private static Contact first(List<Moving> boxes, Vector3dc moved, Vector3dc step) {
        Contact first = null;
        Axes axes = null;
        for (Moving box : boxes) {
            for (Box block : box.near()) {
                axes = Axes.between(box.box(), block, axes);
                Contact contact = sweep(box.box(), moved, step, block, box.deepest(), axes);
                if (contact != null && (first == null || contact.time() < first.time())) {
                    first = contact;
                }
            }
        }
        return first;
    }

    /**
     * When a box, already {@code moved} from where it stands, first goes more than {@code deepest} into {@code fixed} on its way through
     * {@code step}, or null if it does not. They are that deep only while every axis is that deep, so the contact is when the last axis
     * closes, and that axis is the one the move loses. An axis that only touches counts as having a gap of nothing, so if it is not closing
     * it keeps them apart, or a body sliding along a wall would catch on each block, and if it is closing the contact is now.
     */
    @Nullable
    private static Contact sweep(Box moving, Vector3dc moved, Vector3dc step, Box fixed, double deepest, Axes axes) {
        double apartX = fixed.middle().x() - moving.middle().x() - moved.x();
        double apartY = fixed.middle().y() - moving.middle().y() - moved.y();
        double apartZ = fixed.middle().z() - moving.middle().z() - moved.z();
        double closes = Double.NEGATIVE_INFINITY;
        double opens = Double.POSITIVE_INFINITY;
        Vector3d last = null;
        for (Vector3d axis : axes.all()) {
            double along = axis.dot(apartX, apartY, apartZ);
            double speed = axis.dot(step);
            double room = moving.reach(axes.ours(), axis) + fixed.reach(axes.theirs(), axis) - deepest;
            double gap = Math.abs(along) - room;
            boolean moves = Math.abs(speed) > STILL;
            if (gap >= -TOUCHING) {
                if (!moves || along * speed <= 0.0) {
                    return null;
                }
                double time = Math.max(gap, 0.0) / Math.abs(speed);
                if (time > closes) {
                    closes = time;
                    last = axis;
                }
            }
            if (moves) {
                opens = Math.min(opens, (along + Math.signum(speed) * room) / speed);
            }
            if (closes >= opens || closes >= 1.0) {
                return null;
            }
        }
        return last == null ? null : new Contact(closes, last);
    }

    /**
     * What it takes for a body to turn: {@link Vec3#ZERO} if nothing is in the way, the push that clears it, or nothing if it can't
     * turn that far. As with {@link #resolve}, a box can go no deeper into anything than it was, so a body already inside something
     * can turn out of it.
     *
     * <p>The pushes are taken one at a time, deepest first, each looking at where the last one left the body. Adding them up at once
     * would count one wall once for every block of it. Two pushes that undo each other mean it is wedged.
     *
     * @param now    the body's boxes where they are
     * @param next   the same boxes, in the same order, at the next heading
     * @param blocks all that can stop it, out to {@link #MOST_PUSH} from {@code next}
     */
    public static Optional<Vec3> turnPush(List<Box> now, List<Box> next, List<Box> blocks) {
        List<Moving> boxes = moving(now, next, blocks, MOST_PUSH);
        Vector3d pushed = new Vector3d();
        for (int i = 0; i <= PUSHES; i++) {
            Vector3d push = deepestPush(boxes, pushed);
            if (push == null) {
                return Optional.of(i == 0 ? Vec3.ZERO : new Vec3(pushed.x, pushed.y, pushed.z));
            }
            if (pushed.dot(push) < OPPOSITE * pushed.length() * push.length()) {
                return Optional.empty();
            }
            pushed.add(push);
            if (pushed.length() > MOST_PUSH) {
                return Optional.empty();
            }
        }
        return Optional.empty();
    }

    /** The longest way any box, shifted {@code by}, has to go to be no deeper in a block than it started, or null if none has to. */
    @Nullable
    private static Vector3d deepestPush(List<Moving> boxes, Vector3dc by) {
        Vector3d deepest = null;
        double most = TOUCHING;
        Vector3d out = new Vector3d();
        Axes axes = null;
        for (Moving box : boxes) {
            for (Box block : box.near()) {
                axes = Axes.between(box.box(), block, axes);
                double extra = depth(box.box(), by, block, axes, out) - box.deepest();
                if (extra > most) {
                    most = extra;
                    deepest = new Vector3d(out).mul(extra);
                }
            }
        }
        return deepest;
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
