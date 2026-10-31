package com.voidreclaimer.client.screen;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.voidreclaimer.api.body.ModuleKind;
import com.voidreclaimer.body.BodyData;
import com.voidreclaimer.client.render.body.BoneProbe;
import java.util.List;
import org.joml.Vector3f;
import org.junit.jupiter.api.Test;

class AssemblyMarkersTest {
    private static BoneProbe.Point socket(String name, float x, float z) {
        return new BoneProbe.Point(name, BoneProbe.Kind.SOCKET, true, new Vector3f(x, 0, z), name, null);
    }

    private static BoneProbe.Point mount(String socket, float x, float z) {
        return new BoneProbe.Point(socket + "__m", BoneProbe.Kind.MOUNT, true, new Vector3f(x, 0, z), socket,
                new BodyData.Mount(socket, ModuleKind.TOOL, 0));
    }

    private static List<String> names(List<BoneProbe.Point> points) {
        return points.stream().map(BoneProbe.Point::name).toList();
    }

    @Test
    void socketsShowFirstAndASelectedSocketAddsItsMounts() {
        List<BoneProbe.Point> all = List.of(socket("arm", 0, 10), mount("arm", 1, 10), socket("leg", 2, 10), mount("leg", 3, 10));
        assertEquals(List.of("arm", "leg"), names(AssemblyMarkers.visible(all, null, s -> true, 0f)));
        assertEquals(List.of("arm", "arm__m", "leg"), names(AssemblyMarkers.visible(all, "arm", s -> true, 0f)));
    }

    @Test
    void aSocketWithNoPointOfItsOwnShowsItsMountsStraightAway() {
        // The Drone's rig has mounts but no socket bones: without this it would show nothing.
        List<BoneProbe.Point> all = List.of(mount("rotor", 0, 10), mount("claw", 1, 10));
        assertEquals(List.of("rotor__m", "claw__m"), names(AssemblyMarkers.visible(all, null, s -> true, 0f)));
    }

    @Test
    void theFarSideIsHiddenUntilTheModelTurns() {
        List<BoneProbe.Point> all = List.of(socket("front", 0, 20), socket("back", 0, -20));
        assertEquals(List.of("front"), names(AssemblyMarkers.visible(all, null, s -> true, 0f)));
    }

    @Test
    void theAreaFilterThinsMarkers() {
        List<BoneProbe.Point> all = List.of(socket("arm", 0, 10), socket("leg", 1, 10));
        assertEquals(List.of("leg"), names(AssemblyMarkers.visible(all, null, s -> s.equals("leg"), 0f)));
    }

    @Test
    void aClickTakesTheClosestMarkerWithinReachOnly() {
        List<BoneProbe.Point> shown = List.of(socket("a", 10, 0), socket("b", 20, 0));
        assertEquals("a", AssemblyMarkers.pick(shown, 12, 0, 6).orElseThrow().name());
        assertEquals("b", AssemblyMarkers.pick(shown, 18, 0, 6).orElseThrow().name());
        assertTrue(AssemblyMarkers.pick(shown, 40, 0, 6).isEmpty(), "too far from both: a drag, not a pick");
    }
}
