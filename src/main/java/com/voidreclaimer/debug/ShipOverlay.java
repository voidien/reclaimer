package com.voidreclaimer.debug;

import com.voidreclaimer.ship.Ships;
import com.voidreclaimer.ship.ThrusterLike;
import dev.ryanhcode.sable.api.sublevel.SubLevelContainer;
import dev.ryanhcode.sable.sublevel.ServerSubLevel;
import dev.ryanhcode.sable.sublevel.SubLevel;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.phys.Vec3;

/**
 * Ships as photo mode's overlay shows them, read from Sable on the server: each ship's box, centre of mass and velocity, and an arrow
 * per thruster: its available force in grey, what it pushes with now in orange. Those are what decide how a ship behaves, and none
 * can be judged from a picture.
 */
public final class ShipOverlay {
    public static final int BOX = 0xFF50E070;
    public static final int MASS = 0xFFFF40FF;
    public static final int VELOCITY = 0xFF40C0FF;
    public static final int FORCE_NOW = 0xFFFF9020;
    public static final int FORCE_MAX = 0xFF808080;
    /** The longest thruster arrow, in blocks; the strongest thruster on the ship draws this long. */
    private static final double ARROW = 3.0;

    private ShipOverlay() {}

    /** Adds every ship within {@code range} blocks of {@code around}. */
    public static void add(ServerLevel level, Vec3 around, double range, DebugShapes shapes, List<String> facts) {
        SubLevelContainer container = SubLevelContainer.getContainer(level);
        if (container == null) {
            return;
        }
        for (SubLevel subLevel : container.getAllSubLevels()) {
            if (subLevel instanceof ServerSubLevel ship && !ship.isRemoved()
                    && toVec(ship.logicalPose().position()).distanceTo(around) <= range) {
                add(level, ship, shapes, facts);
            }
        }
    }

    private static Vec3 toVec(org.joml.Vector3dc v) {
        return new Vec3(v.x(), v.y(), v.z());
    }

    private static void add(ServerLevel level, ServerSubLevel ship, DebugShapes shapes, List<String> facts) {
        int[] min = {Integer.MAX_VALUE, Integer.MAX_VALUE, Integer.MAX_VALUE};
        int[] max = {Integer.MIN_VALUE, Integer.MIN_VALUE, Integer.MIN_VALUE};
        int blocks = 0;
        // Not Ships.blocksOf: despite its name that lists only a ship's block entities.
        for (BlockPos pos : com.voidreclaimer.station.StationAssembly.blocksOf(level, ship)) {
            blocks++;
            min[0] = Math.min(min[0], pos.getX());
            min[1] = Math.min(min[1], pos.getY());
            min[2] = Math.min(min[2], pos.getZ());
            max[0] = Math.max(max[0], pos.getX() + 1);
            max[1] = Math.max(max[1], pos.getY() + 1);
            max[2] = Math.max(max[2], pos.getZ() + 1);
        }
        if (blocks == 0) {
            return;
        }
        List<Vec3> corners = new ArrayList<>();
        for (int corner = 0; corner < 8; corner++) {
            corners.add(ship.logicalPose().transformPosition(new Vec3((corner & 1) == 0 ? min[0] : max[0],
                    (corner & 2) == 0 ? min[1] : max[1], (corner & 4) == 0 ? min[2] : max[2])));
        }
        shapes.box(corners, BOX);
        String name = ship.getName() == null || ship.getName().isEmpty() ? "ship" : ship.getName();
        Vec3 velocity = toVec(ship.latestLinearVelocity);
        var mass = ship.getMassTracker();
        Vec3 centre = corners.get(7);
        double kilograms = 0.0;
        if (mass != null && !mass.isInvalid()) {
            centre = ship.logicalPose().transformPosition(toVec(mass.getCenterOfMass()));
            kilograms = mass.getMass();
            shapes.line(centre.add(-0.5, 0, 0), centre.add(0.5, 0, 0), MASS);
            shapes.line(centre.add(0, -0.5, 0), centre.add(0, 0.5, 0), MASS);
            shapes.line(centre.add(0, 0, -0.5), centre.add(0, 0, 0.5), MASS);
        }
        shapes.arrow(centre, velocity, VELOCITY);
        List<ThrusterLike> thrusters = Ships.thrustersOf(level, ship);
        double strongest = thrusters.stream().mapToDouble(ThrusterLike::maxForce).max().orElse(0.0);
        double total = 0.0;
        for (ThrusterLike thruster : thrusters) {
            total += thruster.force();
            Vec3 at = ship.logicalPose().transformPosition(Vec3.atCenterOf(thruster.getBlockPos()));
            Direction push = thruster.pushDirection();
            Vec3 local = Vec3.atCenterOf(thruster.getBlockPos()).add(push.getStepX(), push.getStepY(), push.getStepZ());
            Vec3 way = ship.logicalPose().transformPosition(local).subtract(at);
            if (strongest > 0.0) {
                shapes.arrow(at, way.scale(ARROW * thruster.maxForce() / strongest), FORCE_MAX);
                shapes.arrow(at, way.scale(ARROW * thruster.force() / strongest), FORCE_NOW);
            }
        }
        String line = String.format(Locale.ROOT, "%s: %d blocks, mass %.0f, %.1f blocks/s, %d thrusters pushing %.0f", name, blocks, kilograms,
                velocity.length(), thrusters.size(), total);
        shapes.text(centre.add(0, 1, 0), line, BOX);
        facts.add("Ship " + line);
    }
}
