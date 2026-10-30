package com.voidreclaimer.client.render.body;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.voidreclaimer.VoidReclaimer;
import com.voidreclaimer.api.VoidReclaimerRegistries;
import com.voidreclaimer.api.body.ModuleKind;
import com.voidreclaimer.body.BodyData;
import com.voidreclaimer.body.BuiltinBodies;
import net.minecraft.resources.ResourceKey;
import org.junit.jupiter.api.Test;

class BodyLookTest {
    @Test
    void builtinChassisUseTheirOwnRig() {
        assertEquals("drone", BodyLook.rig(BuiltinBodies.DRONE));
        assertEquals("mech", BodyLook.rig(BuiltinBodies.MECH));
        assertEquals("ship", BodyLook.rig(BuiltinBodies.SHIP));
    }

    @Test
    void unknownOrMissingChassisFallBackToTheDroneRig() {
        assertEquals("drone", BodyLook.rig(null));
        assertEquals("drone", BodyLook.rig(ResourceKey.create(VoidReclaimerRegistries.CHASSIS, VoidReclaimer.id("tripod"))));
    }

    @Test
    void onlyTheMechStaysUpright() {
        assertTrue(BodyLook.pitches("drone"));
        assertTrue(BodyLook.pitches("ship"));
        assertFalse(BodyLook.pitches("mech"));
    }

    @Test
    void mountBonesFollowTheRigNaming() {
        BodyData.Mount core = new BodyData.Mount("torso", ModuleKind.CORE, 0);
        BodyData.Mount sensor = new BodyData.Mount("head", ModuleKind.SENSOR, 1);
        assertEquals("mount_core_1", BodyLook.rigMountBone("torso", core));
        assertEquals("head__mount_sensor_2", BodyLook.rigMountBone("torso", sensor));
        assertEquals("mount_sensor_2", BodyLook.sectionMountBone(sensor));
    }

    @Test
    void capBonesAreRecognised() {
        assertTrue(BodyLook.isCap("cap_head"));
        assertFalse(BodyLook.isCap("head"));
        assertFalse(BodyLook.isCap("mount_core_1"));
    }

    @Test
    void speedingUpIsThrustSlowingDownIsBrakingSteadyIsIdle() {
        assertEquals(BodyLook.Pose.THRUST, BodyLook.pose(0.10, 0.05, false));
        assertEquals(BodyLook.Pose.BRAKE, BodyLook.pose(0.05, 0.10, false));
        assertEquals(BodyLook.Pose.IDLE, BodyLook.pose(0.10, 0.10, false));
        assertEquals(BodyLook.Pose.IDLE, BodyLook.pose(0.0, 0.0, false));
    }

    @Test
    void aSteadyCruiseUnderThrustKeepsTheThrustPose() {
        assertEquals(BodyLook.Pose.THRUST, BodyLook.pose(0.2, 0.2, false, 0.2, 0.0, true));
        assertEquals(BodyLook.Pose.IDLE, BodyLook.pose(0.2, 0.2, false, 0.2, 0.0, false));
        assertEquals(BodyLook.Pose.IDLE, BodyLook.pose(0.0, 0.0, false, 0.0, 0.0, true));
    }

    @Test
    void stoppingNeedsSomethingToStopBeforeItCountsAsBraking() {
        assertEquals(BodyLook.Pose.IDLE, BodyLook.pose(0.0, 0.005, false));
    }

    @Test
    void lowPowerWinsOverMotion() {
        assertEquals(BodyLook.Pose.LOW_POWER, BodyLook.pose(0.2, 0.1, true));
        assertEquals(BodyLook.Pose.LOW_POWER, BodyLook.pose(0.0, 0.0, true));
    }

    @Test
    void animationNamesMatchTheArtFiles() {
        assertEquals("animation.void_reclaimer.mech.low_power", BodyLook.animationName("mech", BodyLook.Pose.LOW_POWER.animation()));
        assertEquals("animation.void_reclaimer.ship.thrust", BodyLook.animationName("ship", BodyLook.Pose.THRUST.animation()));
    }

    @Test
    void casingIsPaintedOverTheTopLeftSwatchOnly() {
        int width = 32;
        int[] base = new int[width * 32];
        java.util.Arrays.fill(base, 0xFF112233);
        int[] swatch = new int[16 * 16];
        java.util.Arrays.fill(swatch, 0xFFAABBCC);
        int[] out = BodyLook.paintCasing(base, width, swatch, 16);
        assertEquals(0xFFAABBCC, out[0]);
        assertEquals(0xFFAABBCC, out[15 * width + 15]);
        assertEquals(0xFF112233, out[16]);
        assertEquals(0xFF112233, out[16 * width]);
        assertNotSame(base, out);
        assertEquals(0xFF112233, base[0], "the input must not change");
    }

    @Test
    void aLargerSwatchIsSampledToFit() {
        int width = 16;
        int[] base = new int[16 * 16];
        int[] swatch = new int[32 * 32];
        for (int y = 0; y < 32; y++) {
            for (int x = 0; x < 32; x++) {
                swatch[y * 32 + x] = x < 16 ? 0xFF000001 : 0xFF000002;
            }
        }
        int[] out = BodyLook.paintCasing(base, width, swatch, 32);
        assertEquals(0xFF000001, out[0]);
        assertEquals(0xFF000002, out[15], "the right half of the swatch lands on the right half of the area");
    }

    @Test
    void anUnusableSwatchLeavesTheTextureAlone() {
        int[] base = {1, 2, 3, 4};
        assertArrayEquals(base, BodyLook.paintCasing(base, 2, new int[0], 0));
        assertArrayEquals(base, BodyLook.paintCasing(base, 2, new int[4], 2), "a texture smaller than the swatch area is left alone");
    }

    @Test
    void forwardAndLateralFollowTheFacing() {
        // Yaw 0 faces +Z (south); the body's right hand is -X (west).
        assertEquals(1.0, BodyLook.forward(0, 1, 0f), 1e-9);
        assertEquals(-1.0, BodyLook.forward(0, -1, 0f), 1e-9);
        assertEquals(1.0, BodyLook.lateral(-1, 0, 0f), 1e-9);
        assertEquals(-1.0, BodyLook.lateral(1, 0, 0f), 1e-9);
        // Facing west (yaw 90): ahead is -X, right is +Z... checked against the same rotation.
        assertEquals(1.0, BodyLook.forward(-1, 0, 90f), 1e-9);
        assertEquals(1.0, BodyLook.lateral(0, -1, 90f), 1e-9);
    }

    @Test
    void sidewaysMotionIsAStrafeBackwardMotionIsFlyingBack() {
        assertEquals(BodyLook.Pose.STRAFE_RIGHT, BodyLook.pose(0.1, 0.1, false, 0.0, 0.1));
        assertEquals(BodyLook.Pose.STRAFE_LEFT, BodyLook.pose(0.1, 0.1, false, 0.0, -0.1));
        assertEquals(BodyLook.Pose.BACK, BodyLook.pose(0.1, 0.1, false, -0.1, 0.0));
        assertEquals(BodyLook.Pose.THRUST, BodyLook.pose(0.1, 0.05, false, 0.1, 0.0));
    }

    @Test
    void aSlightDriftSidewaysIsNotAStrafe() {
        assertEquals(BodyLook.Pose.IDLE, BodyLook.pose(0.1, 0.1, false, 0.09, 0.04));
        assertEquals(BodyLook.Pose.IDLE, BodyLook.pose(0.005, 0.005, false, 0.0, 0.005), "too slow to count");
    }

    @Test
    void lowPowerBeatsStrafing() {
        assertEquals(BodyLook.Pose.LOW_POWER, BodyLook.pose(0.1, 0.1, true, 0.0, 0.1));
    }

    @Test
    void missingPosesFallBackToThrustThenIdle() {
        assertEquals(java.util.List.of("strafe_left", "thrust", "idle"), BodyLook.fallbacks(BodyLook.Pose.STRAFE_LEFT));
        assertEquals(java.util.List.of("back", "thrust", "idle"), BodyLook.fallbacks(BodyLook.Pose.BACK));
        assertEquals(java.util.List.of("brake", "idle"), BodyLook.fallbacks(BodyLook.Pose.BRAKE));
        assertEquals(java.util.List.of("low_power", "idle"), BodyLook.fallbacks(BodyLook.Pose.LOW_POWER));
        assertEquals(java.util.List.of("idle"), BodyLook.fallbacks(BodyLook.Pose.IDLE));
    }

    @Test
    void bigBodiesGetTheCameraFurtherBackSmallOnesKeepTheirs() {
        // Vanilla multiplies the distance by the body scale after this: 4 * factor * scale is what the camera really gets.
        assertEquals(4f, BodyRenderEvents.cameraDistance(4f, 0.4f));
        assertEquals(4f, BodyRenderEvents.cameraDistance(4f, 1f));
        assertEquals(4f, BodyRenderEvents.cameraDistance(4f, 1.2f));
        assertEquals(4f * 3.3f, BodyRenderEvents.cameraDistance(4f, 3f), 1e-4f, "a ship: about 40 blocks");
        assertEquals(4f * 5f, BodyRenderEvents.cameraDistance(4f, 8f), 1e-4f, "capped for the Titan: about 160 blocks");
    }
}
