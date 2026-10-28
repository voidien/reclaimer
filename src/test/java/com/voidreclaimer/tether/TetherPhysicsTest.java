package com.voidreclaimer.tether;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.Test;

class TetherPhysicsTest {
    private static final double LENGTH = 16;

    @Test
    void leashIgnoresMotionInsideItsLength() {
        Vec3 velocity = new Vec3(0.3, 0, 0);
        assertEquals(velocity, TetherPhysics.leash(velocity, new Vec3(10, 0, 0), LENGTH));
    }

    @Test
    void leashStopsOutwardDriftAndPullsBack() {
        Vec3 next = TetherPhysics.leash(new Vec3(0.4, 0, 0.2), new Vec3(18, 0, 0), LENGTH);
        assertTrue(next.x < 0, "moving back toward the anchor, got " + next);
        assertEquals(0.2, next.z, 1.0E-9, "sideways motion is kept");
        // Drifting with no thrust from just past the limit never escapes.
        Vec3 pos = new Vec3(LENGTH + 0.5, 0, 0);
        Vec3 velocity = new Vec3(0.5, 0, 0);
        for (int i = 0; i < 200; i++) {
            velocity = TetherPhysics.leash(velocity, pos, LENGTH);
            pos = pos.add(velocity);
            assertTrue(pos.length() < LENGTH + 1.0, "escaped the leash at tick " + i);
        }
        // Inward motion past the limit is not slowed.
        Vec3 inward = TetherPhysics.leash(new Vec3(-0.3, 0, 0), new Vec3(17, 0, 0), LENGTH);
        assertTrue(inward.x <= -0.3);
    }

    @Test
    void recallFliesToTheAnchorAndStops() {
        Vec3 pos = new Vec3(200, 30, -50);
        Vec3 velocity = new Vec3(0, 0.5, 0);
        double speed = 1.0;
        double maxSeen = 0;
        int ticks = 0;
        while (pos.length() > 2.5 && ticks < 2000) {
            velocity = TetherPhysics.recall(velocity, pos.scale(-1), speed, 0.1);
            maxSeen = Math.max(maxSeen, velocity.length());
            pos = pos.add(velocity);
            ticks++;
        }
        assertTrue(pos.length() <= 2.5, "never arrived: " + pos);
        assertTrue(maxSeen <= speed + 1.0E-9, "faster than recall speed: " + maxSeen);
        assertTrue(velocity.length() < 0.5, "still flying fast on arrival: " + velocity.length());
        assertTrue(ticks < 400, "took too long: " + ticks);
    }

    @Test
    void longRangeRecallTeleports() {
        assertFalse(TetherPhysics.usesTeleport(256, 256));
        assertTrue(TetherPhysics.usesTeleport(256.1, 256));
    }
}
