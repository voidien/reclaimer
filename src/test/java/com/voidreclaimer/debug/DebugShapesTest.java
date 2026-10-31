package com.voidreclaimer.debug;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.Test;

class DebugShapesTest {
    @Test
    void aBoxHasTwelveEdgesEachOneBlockLong() {
        List<Vec3> corners = new ArrayList<>();
        for (int i = 0; i < 8; i++) {
            corners.add(new Vec3(i & 1, (i >> 1) & 1, (i >> 2) & 1));
        }
        DebugShapes shapes = new DebugShapes();
        shapes.box(corners, 1);
        assertEquals(12, shapes.lines().size());
        for (DebugShapes.Line line : shapes.lines()) {
            assertEquals(1.0, line.from().distanceTo(line.to()), 1e-9, "an edge joins neighbouring corners: " + line);
        }
    }

    @Test
    void anArrowEndsWhereItsVectorSaysAndHasAHead() {
        DebugShapes shapes = new DebugShapes();
        Vec3 from = new Vec3(1, 2, 3);
        shapes.arrow(from, new Vec3(0, 0, 4), 1);
        assertEquals(3, shapes.lines().size(), "a shaft and two head strokes");
        assertEquals(new Vec3(1, 2, 7), shapes.lines().get(0).to());
    }

    @Test
    void anArrowStraightUpStillHasAHead() {
        DebugShapes shapes = new DebugShapes();
        shapes.arrow(Vec3.ZERO, new Vec3(0, 2, 0), 1);
        for (DebugShapes.Line line : shapes.lines()) {
            assertTrue(Double.isFinite(line.to().x) && Double.isFinite(line.to().z), "no division by a zero side vector: " + line);
        }
    }

    @Test
    void aZeroVectorDrawsNothing() {
        DebugShapes shapes = new DebugShapes();
        shapes.arrow(Vec3.ZERO, Vec3.ZERO, 1);
        assertTrue(shapes.lines().isEmpty(), "a resting ship has no velocity arrow");
    }
}
