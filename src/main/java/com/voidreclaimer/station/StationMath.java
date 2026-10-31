package com.voidreclaimer.station;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Rotation;
import org.joml.Quaterniondc;
import org.joml.Vector3d;

/**
 * Pure helpers for setting a ship down as blocks: a ship can only be placed back into the world turned by whole quarter
 * turns about the vertical, so its orientation is snapped to the nearest one and must be level enough to begin with.
 */
public final class StationMath {
    /** How far from upright (degrees) a ship may lean and still be set down. */
    public static final double LEVEL_TOLERANCE_DEGREES = 10.0;

    private StationMath() {}

    /**
     * The nearest number of quarter turns about Y (0 to 3) that {@code orientation} (ship frame to world) amounts to,
     * in the sense of {@code Vec3.yRot}: the ship's own +Z turned by {@code quarterTurns * 90} degrees points where its
     * +Z points in the world.
     */
    public static int quarterTurns(Quaterniondc orientation) {
        Vector3d forward = orientation.transform(new Vector3d(0, 0, 1));
        double yaw = Math.atan2(forward.x, forward.z);
        int turns = (int) Math.round(yaw / (Math.PI / 2.0));
        return Math.floorMod(turns, 4);
    }

    /**
     * How far, in degrees (0 to 45), {@code orientation}'s yaw is from the nearest quarter turn: a docking visitor must
     * be within tolerance of one for its blocks to join the station's grid.
     */
    public static double yawResidualDegrees(Quaterniondc orientation) {
        Vector3d forward = orientation.transform(new Vector3d(0, 0, 1));
        double yaw = Math.toDegrees(Math.atan2(forward.x, forward.z));
        double nearest = Math.round(yaw / 90.0) * 90.0;
        return Math.abs(yaw - nearest);
    }

    /** Whether the ship's up is within {@code toleranceDegrees} of straight up. */
    public static boolean isLevel(Quaterniondc orientation, double toleranceDegrees) {
        Vector3d up = orientation.transform(new Vector3d(0, 1, 0));
        return up.y >= Math.cos(Math.toRadians(toleranceDegrees));
    }

    /** A block offset turned by {@code quarterTurns} quarter turns about Y, as {@code Vec3.yRot} would turn it. */
    public static BlockPos rotateY(BlockPos offset, int quarterTurns) {
        int x = offset.getX(), z = offset.getZ();
        return switch (Math.floorMod(quarterTurns, 4)) {
            case 1 -> new BlockPos(z, offset.getY(), -x);
            case 2 -> new BlockPos(-x, offset.getY(), -z);
            case 3 -> new BlockPos(-z, offset.getY(), x);
            default -> offset;
        };
    }

    /** The block-state rotation that goes with {@code quarterTurns} in Sable's assembly transform ({@code 4 - ordinal}). */
    public static Rotation blockRotation(int quarterTurns) {
        return Rotation.values()[Math.floorMod(4 - Math.floorMod(quarterTurns, 4), 4)];
    }
}
