package com.voidreclaimer.body;

import static org.junit.jupiter.api.Assertions.assertEquals;

import dev.ryanhcode.sable.companion.math.Pose3d;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaterniond;
import org.joml.Vector3d;
import org.junit.jupiter.api.Test;

class BoxCollisionTest {
    @Test
    void aShipsBlockTurnsWithTheShip() {
        Pose3d pose = new Pose3d();
        pose.position().set(100, 50, 200);
        pose.orientation().set(new Quaterniond().rotationY(Math.toRadians(45)).rotateX(Math.toRadians(30)));
        AABB block = new AABB(1000, 2, 3, 1002, 3, 7);
        BodyBoxes.Box box = BoxCollision.onShip(pose, block);
        var corners = BodyBoxes.corners(box);
        int index = 0;
        for (double z : new double[] {block.minZ, block.maxZ}) {
            for (double y : new double[] {block.minY, block.maxY}) {
                for (double x : new double[] {block.minX, block.maxX}) {
                    Vec3 expected = pose.transformPosition(new Vec3(x, y, z));
                    Vec3 actual = corners.get(index++);
                    assertEquals(0.0, expected.distanceTo(actual), 1e-6, "corner " + (index - 1) + " is at " + actual + ", not " + expected);
                }
            }
        }
    }
}
