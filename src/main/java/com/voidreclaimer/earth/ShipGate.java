package com.voidreclaimer.earth;

import com.voidreclaimer.station.StationMath;
import dev.ryanhcode.sable.companion.math.BoundingBox3ic;
import dev.ryanhcode.sable.companion.math.Pose3dc;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaterniond;
import org.joml.Vector3d;

/** Pure maths for a ship at an Atmospheric Portal: does it fit the gate, and how does it turn to come out the far one. */
public final class ShipGate {
    /** A ship may be this much wider or taller than the gate's opening and still pass (blocks are measured whole). */
    static final double SLACK = 0.5;

    private ShipGate() {}

    /** How many quarter turns about Y take south (+Z) to {@code direction}: south 0, east 1, north 2, west 3. */
    public static int quarterTurnsOf(Direction direction) {
        return switch (direction) {
            case EAST -> 1;
            case NORTH -> 2;
            case WEST -> 3;
            default -> 0;
        };
    }

    /** The quarter turns that take the way a ship leaves one portal ({@code from}) to the way it comes out of the other ({@code to}). */
    public static int turnsBetween(Direction from, Direction to) {
        return Math.floorMod(quarterTurnsOf(to) - quarterTurnsOf(from), 4);
    }

    /** A rotation of {@code quarterTurns} about Y, matching {@link StationMath#rotateY}. */
    public static Quaterniond turn(int quarterTurns) {
        return new Quaterniond().rotateY(Math.floorMod(quarterTurns, 4) * (Math.PI / 2.0));
    }

    /** The ship's extent across the gate (along {@code across}) and up, in blocks, from its block bounds turned by its pose. */
    public static Vec3 worldExtents(BoundingBox3ic blocks, Pose3dc pose) {
        double minX = Double.MAX_VALUE, minY = Double.MAX_VALUE, minZ = Double.MAX_VALUE;
        double maxX = -Double.MAX_VALUE, maxY = -Double.MAX_VALUE, maxZ = -Double.MAX_VALUE;
        // Block bounds are inclusive block coordinates: the far faces are one block further.
        double[] xs = {blocks.minX(), blocks.maxX() + 1.0}, ys = {blocks.minY(), blocks.maxY() + 1.0}, zs = {blocks.minZ(), blocks.maxZ() + 1.0};
        for (double x : xs) {
            for (double y : ys) {
                for (double z : zs) {
                    Vector3d corner = pose.transformPosition(new Vector3d(x, y, z));
                    minX = Math.min(minX, corner.x); maxX = Math.max(maxX, corner.x);
                    minY = Math.min(minY, corner.y); maxY = Math.max(maxY, corner.y);
                    minZ = Math.min(minZ, corner.z); maxZ = Math.max(maxZ, corner.z);
                }
            }
        }
        return new Vec3(maxX - minX, maxY - minY, maxZ - minZ);
    }

    /** Whether a ship of world extents {@code extents} passes a gate {@code width} across along {@code across} and {@code height} tall. */
    public static boolean fits(Vec3 extents, Direction.Axis across, int width, int height) {
        double wide = across == Direction.Axis.X ? extents.x : extents.z;
        return wide <= width + SLACK && extents.y <= height + SLACK;
    }
}
