package com.voidreclaimer.launch;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class LaunchAnimationTest {
    private static final float EPS = 1e-5f;

    @Test
    void theGunLeansWithTheWindUpAndRestsWhenIdle() {
        assertEquals(0f, LaunchAnimation.gunPitch(0f, -1f), EPS, "idle, never fired");
        assertEquals(LaunchAnimation.WINDUP_DEGREES, LaunchAnimation.gunPitch(1f, -1f), EPS, "fully wound");
        float last = 0f;
        for (float p = 0.1f; p <= 1f; p += 0.1f) {
            float pitch = LaunchAnimation.gunPitch(p, -1f);
            assertTrue(pitch <= last, "the lean grows with the wind-up: " + p);
            last = pitch;
        }
        assertEquals(LaunchAnimation.gunPitch(1f, -1f), LaunchAnimation.gunPitch(3f, -1f), EPS, "progress is clamped");
    }

    @Test
    void aLaunchKicksTheGunAndTheKickFadesOut() {
        assertEquals(LaunchAnimation.RECOIL_DEGREES, LaunchAnimation.gunPitch(0f, 0f), EPS, "full kick at the launch tick");
        float half = LaunchAnimation.gunPitch(0f, LaunchAnimation.RECOIL_TICKS / 2f);
        assertTrue(half > 0f && half < LaunchAnimation.RECOIL_DEGREES, "half way, the kick is fading: " + half);
        assertEquals(0f, LaunchAnimation.gunPitch(0f, LaunchAnimation.RECOIL_TICKS), EPS, "gone after RECOIL_TICKS");
        assertEquals(0f, LaunchAnimation.gunPitch(0f, 1000f), EPS, "long after");
    }

    @Test
    void theDishesTurnAndTheCranesSway() {
        assertEquals(0f, LaunchAnimation.dish(0f, LaunchAnimation.DISH_A_DEGREES_PER_TICK), EPS);
        assertEquals(60f, LaunchAnimation.dish(100f, LaunchAnimation.DISH_A_DEGREES_PER_TICK), EPS);
        float b = LaunchAnimation.dish(100f, LaunchAnimation.DISH_B_DEGREES_PER_TICK);
        assertTrue(b >= 0f && b < 360f, "headings stay within a turn: " + b);
        assertEquals(360f - 45f, b, EPS, "dish B turns the other way");
        assertEquals(0f, LaunchAnimation.crane(0f, 0f), EPS);
        assertEquals(LaunchAnimation.CRANE_SWAY_DEGREES, LaunchAnimation.crane(LaunchAnimation.CRANE_PERIOD_TICKS / 4f, 0f), 1e-3f);
        LaunchAnimation.Pose pose = LaunchAnimation.at(0.5f, -1f, LaunchAnimation.CRANE_PERIOD_TICKS / 4f);
        assertEquals(-pose.crane(), pose.crane2(), 1e-3f, "the second crane is half a period behind");
    }
}
