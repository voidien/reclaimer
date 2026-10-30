package com.voidreclaimer.client.camera;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.Test;

class PhotoStateTest {
    private static PhotoState at(Vec3 position, float yaw, float pitch) {
        PhotoState state = new PhotoState();
        state.start(position, yaw, pitch, 70f);
        return state;
    }

    @Test
    void forwardFollowsTheViewDirection() {
        PhotoState south = at(Vec3.ZERO, 0f, 0f); // yaw 0 looks along +Z
        south.move(1, 0, 0, 2.0, Vec3.ZERO);
        assertEquals(2.0, south.position().z, 1e-6);
        assertEquals(0.0, south.position().x, 1e-6);
        PhotoState west = at(Vec3.ZERO, 90f, 0f); // yaw 90 looks along -X
        west.move(1, 0, 0, 2.0, Vec3.ZERO);
        assertEquals(-2.0, west.position().x, 1e-6);
        PhotoState up = at(Vec3.ZERO, 0f, -90f); // looking straight up
        up.move(1, 0, 0, 1.0, Vec3.ZERO);
        assertEquals(1.0, up.position().y, 1e-6);
    }

    @Test
    void strafeIsToTheRightOfTheView() {
        PhotoState state = at(Vec3.ZERO, 0f, 0f); // facing +Z: right is -X
        state.move(0, 1, 0, 1.0, Vec3.ZERO);
        assertEquals(-1.0, state.position().x, 1e-6);
        assertEquals(0.0, state.position().z, 1e-6);
        state.move(0, 0, 1, 1.0, Vec3.ZERO);
        assertEquals(1.0, state.position().y, 1e-6);
    }

    @Test
    void theCameraStaysWithinRangeOfTheBody() {
        PhotoState state = at(Vec3.ZERO, 0f, 0f);
        for (int i = 0; i < 2000; i++) {
            state.move(1, 0, 0, 10.0, Vec3.ZERO);
        }
        assertEquals(PhotoState.MAX_RANGE, state.position().length(), 1e-6);
    }

    @Test
    void lookRollAndFovAreClamped() {
        PhotoState state = at(Vec3.ZERO, 0f, 0f);
        state.look(0, 1_000_000);
        assertEquals(90f, state.pitch());
        state.look(0, -2_000_000);
        assertEquals(-90f, state.pitch());
        state.rollBy(500f);
        assertEquals(180f, state.roll());
        state.resetRoll();
        assertEquals(0f, state.roll());
        state.zoom(1000);
        assertEquals(PhotoState.MIN_FOV, state.fov(), 1e-4f);
        state.zoom(-1000);
        assertEquals(PhotoState.MAX_FOV, state.fov(), 1e-4f);
        assertTrue(state.fov() <= PhotoState.MAX_FOV);
    }
}
