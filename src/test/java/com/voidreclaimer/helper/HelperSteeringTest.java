package com.voidreclaimer.helper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.Test;

class HelperSteeringTest {
    @Test
    void aStepMovesTheSpeedTowardsTheTargetAndStopsOnIt() {
        Vec3 from = new Vec3(0, 0, 0), to = new Vec3(10, 0, 0);
        Vec3 step = HelperSteering.step(from, to, 0.35);
        assertEquals(0.35, step.x, 1e-9);
        assertEquals(0.0, step.y, 1e-9);
        Vec3 diagonal = HelperSteering.step(from, new Vec3(3, 4, 0), 1.0);
        assertEquals(1.0, diagonal.length(), 1e-9, "a step is exactly the speed long");
        assertEquals(to, HelperSteering.step(new Vec3(9.8, 0, 0), to, 0.35), "the last step lands on the target");
        assertEquals(to, HelperSteering.step(to, to, 0.35), "on the target it stays");
    }

    @Test
    void arrivalIsWithinReachAndYawFacesTheTarget() {
        Vec3 target = new Vec3(5, 5, 5);
        assertTrue(HelperSteering.arrived(new Vec3(5, 5, 6.4), target, 1.5));
        assertFalse(HelperSteering.arrived(new Vec3(5, 5, 7), target, 1.5));
        assertEquals(0f, HelperSteering.yaw(Vec3.ZERO, new Vec3(0, 0, 1), 42f), 1e-4, "south is yaw 0");
        assertEquals(-90f, HelperSteering.yaw(Vec3.ZERO, new Vec3(1, 0, 0), 42f), 1e-4, "east is yaw -90");
        assertEquals(42f, HelperSteering.yaw(Vec3.ZERO, new Vec3(0, 3, 0), 42f), 1e-4, "straight up keeps the yaw");
    }
}
