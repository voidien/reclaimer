package com.voidreclaimer.earth;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.ryanhcode.sable.companion.math.BoundingBox3i;
import dev.ryanhcode.sable.companion.math.Pose3d;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaterniond;
import org.joml.Vector3d;
import org.junit.jupiter.api.Test;

class ShipGateTest {
    private static final double EPS = 1e-6;
    /** A hull 7 blocks along X, 2 up, 3 along Z, in plot coordinates. */
    private static final BoundingBox3i HULL = new BoundingBox3i(0, 0, 0, 6, 1, 2);

    private static Pose3d pose(Quaterniond orientation) {
        return new Pose3d(new Vector3d(), orientation, new Vector3d(), new Vector3d(1, 1, 1));
    }

    @Test
    void theTurnBetweenPortalsBringsAShipOutTheFarGatesWay() {
        assertEquals(0, ShipGate.turnsBetween(Direction.SOUTH, Direction.SOUTH));
        assertEquals(1, ShipGate.turnsBetween(Direction.SOUTH, Direction.EAST));
        assertEquals(3, ShipGate.turnsBetween(Direction.EAST, Direction.SOUTH), "a quarter turn back");
        assertEquals(2, ShipGate.turnsBetween(Direction.NORTH, Direction.SOUTH));
        // The turn's quaternion and the block turn agree: south turned once is east.
        Vector3d east = ShipGate.turn(1).transform(new Vector3d(0, 0, 1));
        assertEquals(1.0, east.x, EPS);
        assertEquals(0.0, east.z, EPS);
    }

    @Test
    void worldExtentsFollowThePose() {
        Vec3 straight = ShipGate.worldExtents(HULL, pose(new Quaterniond()));
        assertEquals(7, straight.x, EPS);
        assertEquals(2, straight.y, EPS);
        assertEquals(3, straight.z, EPS);
        Vec3 turned = ShipGate.worldExtents(HULL, pose(new Quaterniond().rotateY(Math.PI / 2)));
        assertEquals(3, turned.x, EPS, "a quarter turn swaps the footprint");
        assertEquals(7, turned.z, EPS);
        Vec3 leaning = ShipGate.worldExtents(HULL, pose(new Quaterniond().rotateZ(Math.PI / 4)));
        assertTrue(leaning.y > 2 && leaning.y < 7 && leaning.x < 7, "a lean trades width for height: " + leaning);
    }

    @Test
    void aShipFitsAGateWiderAndTallerThanItself() {
        assertTrue(ShipGate.fits(new Vec3(7, 2, 3), Direction.Axis.X, 12, 6));
        assertTrue(ShipGate.fits(new Vec3(12.4, 6.4, 3), Direction.Axis.X, 12, 6), "half a block of slack");
        assertFalse(ShipGate.fits(new Vec3(13, 2, 3), Direction.Axis.X, 12, 6), "too wide");
        assertFalse(ShipGate.fits(new Vec3(7, 7, 3), Direction.Axis.X, 12, 6), "too tall");
        assertFalse(ShipGate.fits(new Vec3(3, 2, 13), Direction.Axis.Z, 12, 6), "too wide across a gate along Z");
        assertTrue(ShipGate.fits(new Vec3(30, 2, 7), Direction.Axis.Z, 12, 6), "depth along the way through does not matter");
    }
}
