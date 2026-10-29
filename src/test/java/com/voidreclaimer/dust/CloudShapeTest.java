package com.voidreclaimer.dust;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.Test;

class CloudShapeTest {
    private static final DustField FIELD = new DustField(1234L);

    @Test
    void edgeSitsAtTheOutermostEdgeDensity() {
        DustCloud cloud = DustFieldTest.anyCloud();
        CloudShape shape = CloudShape.compute(FIELD, cloud);
        int lumpy = 0;
        for (int lat = 0; lat <= CloudShape.LAT_BANDS; lat++) {
            for (int lon = 0; lon < CloudShape.LON_SEGMENTS; lon++) {
                double r = shape.radius(lat, lon);
                assertTrue(r > 0 && r <= cloud.radius(), "radius out of range: " + r);
                Vec3 dir = CloudShape.direction(lat, lon);
                // Just inside the edge there is dust; everywhere further out, sampled finely, there is none.
                assertTrue(FIELD.density(cloud, cloud.center().add(dir.scale(r))) >= CloudShape.EDGE_DENSITY - 1e-9);
                for (double out = r + cloud.radius() / 64; out < cloud.radius(); out += cloud.radius() / 64) {
                    assertTrue(FIELD.density(cloud, cloud.center().add(dir.scale(out))) < CloudShape.EDGE_DENSITY,
                            "dust found beyond the drawn edge at " + out + " (edge " + r + ")");
                }
                if (Math.abs(r - shape.radius(lat, lon + 1)) > cloud.radius() * 0.01) {
                    lumpy++;
                }
            }
        }
        assertTrue(lumpy > 10, "outline should follow the noisy edge, not a perfect sphere");
    }

    @Test
    void deterministicAndClosedAroundTheSeam() {
        DustCloud cloud = DustFieldTest.anyCloud();
        CloudShape a = CloudShape.compute(FIELD, cloud);
        CloudShape b = CloudShape.compute(new DustField(1234L), cloud);
        for (int lat = 0; lat <= CloudShape.LAT_BANDS; lat++) {
            for (int lon = 0; lon < CloudShape.LON_SEGMENTS; lon++) {
                assertEquals(a.radius(lat, lon), b.radius(lat, lon));
            }
            assertEquals(a.vertex(lat, 0), a.vertex(lat, CloudShape.LON_SEGMENTS), "ring must close");
        }
        // Poles: every longitude points the same way.
        assertEquals(0.0, CloudShape.direction(0, 5).subtract(CloudShape.direction(0, 17)).length(), 1e-9);
    }
}
