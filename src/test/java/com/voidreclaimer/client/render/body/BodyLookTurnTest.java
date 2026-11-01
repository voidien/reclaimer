package com.voidreclaimer.client.render.body;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.joml.Vector3f;
import org.junit.jupiter.api.Test;

class BodyLookTurnTest {
    /** Where the nose of a rig ends up: built facing +Z it needs a rig yaw of 180, built facing -Z it needs none. */
    private static Vector3f nose(float yaw, float pitch, float rigYaw) {
        Vector3f built = new Vector3f(0f, 0f, rigYaw == 180f ? 1f : -1f);
        return BodyLook.turn(yaw, pitch, rigYaw).transform(built);
    }

    private static void assertNose(float x, float y, float z, Vector3f nose) {
        assertEquals(x, nose.x, 1e-3f, "x");
        assertEquals(y, nose.y, 1e-3f, "y");
        assertEquals(z, nose.z, 1e-3f, "z");
    }

    @Test
    void aBoneHiddenAloneKeepsItsChildren() {
        software.bernie.geckolib.cache.object.GeoBone bone = new software.bernie.geckolib.cache.object.GeoBone(null, "root", false, null, false, null);
        BodyLook.hideAlone(bone, true);
        assertEquals(true, bone.isHidden(), "the bone's own cubes stay");
        assertEquals(false, bone.isHidingChildren(), "a Mech with no pelvis would lose its chest");
        BodyLook.hideAlone(bone, false);
        assertEquals(false, bone.isHidden());
    }

    @Test
    void levelBodiesFaceWhereTheyLookWhicheverWayTheRigWasBuilt() {
        for (float rigYaw : new float[] {0f, 180f}) {
            assertNose(0f, 0f, 1f, nose(0f, 0f, rigYaw));
            assertNose(-1f, 0f, 0f, nose(90f, 0f, rigYaw));
        }
    }

    @Test
    void lookingDownPointsTheNoseDownOnEveryRig() {
        float down = (float) Math.sin(Math.toRadians(50f));
        float along = (float) Math.cos(Math.toRadians(50f));
        for (float rigYaw : new float[] {0f, 180f}) {
            assertNose(0f, -down, along, nose(0f, 50f, rigYaw));
            assertNose(-along, -down, 0f, nose(90f, 50f, rigYaw));
            assertNose(0f, down, along, nose(0f, -50f, rigYaw));
        }
    }
}
