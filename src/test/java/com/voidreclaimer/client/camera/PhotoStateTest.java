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

    @Test
    void anOrbitPutsTheCameraAtItsDistanceLookingAtTheFocus() {
        PhotoState state = new PhotoState();
        Vec3 focus = new Vec3(100.0, 64.0, -20.0);
        state.orbit(focus, 30f, 20f, 12.0);
        assertEquals(12.0, state.position().distanceTo(focus), 1e-6);
        Vec3 toFocus = focus.subtract(state.position()).normalize();
        Vec3 look = PhotoState.direction(state.yaw(), state.pitch());
        assertEquals(1.0, toFocus.dot(look), 1e-6, "the view points at the focus");
        assertTrue(state.position().y > focus.y, "a positive pitch looks down, so the camera is above");
    }

    @Test
    void placingKeepsAnglesInRangeAndSetFovItsLimits() {
        PhotoState state = new PhotoState();
        state.place(new Vec3(1, 2, 3), 370f, 120f);
        assertEquals(10f, state.yaw(), 1e-4);
        assertEquals(90f, state.pitch(), 1e-4);
        assertEquals(new Vec3(1, 2, 3), state.position());
        state.setFov(500f);
        assertEquals(PhotoState.MAX_FOV, state.fov(), 1e-4);
    }

    @Test
    void aSheetHasFourNamedViewsAndStartsAtTheSubjectsFront() {
        assertEquals(4, PhotoSheet.VIEWS.size());
        assertEquals(4, PhotoSheet.VIEWS.stream().map(PhotoSheet.View::suffix).distinct().count());
        assertEquals("front", PhotoSheet.VIEWS.get(0).suffix());
        // A subject facing south (yaw 0) is seen from the south, looking north.
        Vec3 look = PhotoState.direction(PhotoSheet.frontYaw(0f), 0f);
        assertEquals(-1.0, look.z, 1e-6);
    }
}
