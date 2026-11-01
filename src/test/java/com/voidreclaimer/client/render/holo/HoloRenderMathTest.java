package com.voidreclaimer.client.render.holo;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.voidreclaimer.display.HoloDisplay;
import java.util.List;
import net.minecraft.core.Direction;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.junit.jupiter.api.Test;

/** Layout, turns and look of a hologram, without a client. */
class HoloRenderMathTest {
    private static final float EPSILON = 1e-5f;

    private static void close(Vector3f expected, Vector3f actual) {
        assertTrue(expected.distance(actual) < EPSILON, "expected " + expected + ", got " + actual);
    }

    @Test
    void thePanelFitsTheLongestLine() {
        HoloDisplay display = HoloDisplay.EMPTY.withTexts(List.of("ab", "abcd"));
        HoloTextLayout layout = HoloTextLayout.of(display, text -> text.length() * 6);
        assertEquals(24 + 2 * HoloTextLayout.PADDING, layout.width());
        assertEquals(2 * HoloTextLayout.LINE_HEIGHT + 2 * HoloTextLayout.PADDING, layout.height());
    }

    @Test
    void aBlankLineKeepsItsPlace() {
        HoloDisplay display = HoloDisplay.EMPTY.withTexts(List.of("top", "", "bottom"));
        List<HoloTextLayout.Row> rows = HoloTextLayout.of(display, String::length).rows();
        assertEquals(2, rows.size(), "blank lines draw nothing");
        assertEquals(HoloTextLayout.PADDING, rows.get(0).y());
        assertEquals(HoloTextLayout.PADDING + 2 * HoloTextLayout.LINE_HEIGHT, rows.get(1).y());
    }

    @Test
    void aLineUsesItsOwnColourOrTheDisplays() {
        HoloDisplay display = HoloDisplay.EMPTY.withColor(0x112233).withLine(0, "own", 0xFF0000).withLine(1, "shared", HoloDisplay.NO_COLOR);
        List<HoloTextLayout.Row> rows = HoloTextLayout.of(display, String::length).rows();
        assertEquals(0xFF0000, rows.get(0).color());
        assertEquals(0x112233, rows.get(1).color());
    }

    @Test
    void aBillboardFacesTheCameraInTheWorld() {
        Quaternionf camera = new Quaternionf().rotationXYZ(0.4f, 1.1f, 0f);
        Quaternionf local = HoloTransform.billboard(new Quaternionf(), camera);
        close(camera.transform(new Vector3f(0, 0, 1)), local.transform(new Vector3f(0, 0, 1)));
    }

    /** The whole point of the parent turn: on a turned ship the picture must end up where it does in the world. */
    @Test
    void aBillboardFacesTheCameraOnATurnedShip() {
        Quaternionf ship = new Quaternionf().rotationXYZ(0.9f, -0.3f, 0.5f);
        Quaternionf camera = new Quaternionf().rotationXYZ(0.2f, 2.0f, 0f);
        Quaternionf local = HoloTransform.billboard(ship, camera);
        Quaternionf world = new Quaternionf(ship).mul(local);
        close(camera.transform(new Vector3f(0, 0, 1)), world.transform(new Vector3f(0, 0, 1)));
        close(camera.transform(new Vector3f(0, 1, 0)), world.transform(new Vector3f(0, 1, 0)));
    }

    @Test
    void aFixedPictureLooksTheWayTheBlockFaces() {
        for (Direction facing : Direction.Plane.HORIZONTAL) {
            Vector3f front = HoloTransform.fixed(facing).transform(new Vector3f(0, 0, 1));
            close(new Vector3f(facing.getStepX(), 0, facing.getStepZ()), front);
        }
    }

    @Test
    void aPictureGrowsWithItsScale() {
        assertEquals(1f, HoloTransform.size(64, 1f), EPSILON);
        assertEquals(2f, HoloTransform.size(64, 2f), EPSILON);
    }

    @Test
    void theFlickerStaysInItsBand() {
        for (float time = 0; time < 2000; time += 0.37f) {
            float value = HoloStyle.flicker(time, 123456789L);
            assertTrue(value >= 0.85f && value <= 1f, "flicker " + value);
        }
    }

    @Test
    void argbPutsTheAlphaOnTop() {
        assertEquals(0xFF123456, HoloStyle.argb(0x123456, 1f));
        assertEquals(0x00123456, HoloStyle.argb(0x123456, 0f));
    }

    @Test
    void theStyleTexturesExist() {
        for (var texture : List.of(HoloStyle.PANEL, HoloStyle.GLOW)) {
            assertNotNull(getClass().getClassLoader().getResource("assets/" + texture.getNamespace() + "/" + texture.getPath()), texture.toString());
        }
    }
}
