package com.voidreclaimer.asteroid;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.voidreclaimer.dust.DustCloud;
import com.voidreclaimer.dust.DustKinds;
import java.util.List;
import java.util.Optional;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.Test;

class AsteroidFieldTest {
    private static final Vec3 SPAWN = new Vec3(0.5, 64, 0.5);
    private static final double SAFE = 300;
    private static final int RANGE = 12;

    private static AsteroidField field(long seed) {
        return new AsteroidField(seed, SPAWN, SAFE);
    }

    @FunctionalInterface
    private interface CellVisitor {
        void visit(int cx, int cy, int cz, List<AsteroidBody> bodies);
    }

    private static void forCells(AsteroidField field, CellVisitor visitor) {
        for (int cx = -RANGE; cx <= RANGE; cx++) {
            for (int cy = -2; cy <= 1; cy++) {
                for (int cz = -RANGE; cz <= RANGE; cz++) {
                    visitor.visit(cx, cy, cz, field.bodiesIn(cx, cy, cz));
                }
            }
        }
    }

    @Test
    void sameSeedGivesTheSameField() {
        AsteroidField a = field(42L);
        AsteroidField b = field(42L);
        forCells(a, (cx, cy, cz, bodies) -> assertEquals(b.bodiesIn(cx, cy, cz), bodies));
        // A different seed puts bodies elsewhere.
        AsteroidField other = field(43L);
        boolean[] differs = {false};
        forCells(a, (cx, cy, cz, bodies) -> differs[0] |= !other.bodiesIn(cx, cy, cz).equals(bodies));
        assertTrue(differs[0]);
    }

    @Test
    void noBodyLeavesItsCell() {
        for (long seed : new long[]{1L, 2L, 3L}) {
            forCells(field(seed), (cx, cy, cz, bodies) -> {
                for (AsteroidBody body : bodies) {
                    double e = body.maxExtent();
                    Vec3 c = body.center();
                    assertTrue(c.x - e >= cx * 1024.0 && c.x + e <= (cx + 1) * 1024.0, "x of " + body);
                    assertTrue(c.y - e >= cy * 1024.0 && c.y + e <= (cy + 1) * 1024.0, "y of " + body);
                    assertTrue(c.z - e >= cz * 1024.0 && c.z + e <= (cz + 1) * 1024.0, "z of " + body);
                }
            });
        }
    }

    @Test
    void spawnStaysClear() {
        for (long seed : new long[]{1L, 2L, 3L, 4L}) {
            forCells(field(seed), (cx, cy, cz, bodies) -> {
                for (AsteroidBody body : bodies) {
                    assertTrue(body.center().distanceTo(SPAWN) >= body.maxExtent() + SAFE, "body near spawn: " + body);
                }
            });
        }
    }

    @Test
    void sparseWithRareGiantsOfHundredsOfBlocks() {
        int[] cells = {0};
        int[] filled = {0};
        int[] giants = {0};
        double[] widest = {0};
        int[] small = {0};
        AsteroidField field = field(7L);
        forCells(field, (cx, cy, cz, bodies) -> {
            cells[0]++;
            if (!bodies.isEmpty()) {
                filled[0]++;
            }
            for (AsteroidBody body : bodies) {
                if (body.size() == AsteroidBody.Size.GIANT) {
                    giants[0]++;
                    widest[0] = Math.max(widest[0], 2 * Math.max(body.rx(), Math.max(body.ry(), body.rz())));
                } else if (body.size() == AsteroidBody.Size.SMALL) {
                    small[0]++;
                }
            }
        });
        double fraction = filled[0] / (double) cells[0];
        assertTrue(fraction > 0.2 && fraction < 0.4, "filled cells " + fraction);
        assertTrue(giants[0] >= 1 && giants[0] < filled[0] * 0.1, "giants " + giants[0] + " of " + filled[0]);
        assertTrue(widest[0] >= 200 && widest[0] <= 300.0001, "widest giant " + widest[0]);
        assertTrue(small[0] > giants[0] * 10, "small rocks should dominate");
    }

    @Test
    void clustersAreMadeOfTheKindOfTheirNearestCloud() {
        AsteroidField field = field(11L);
        int[] counted = {0};
        int[] matching = {0};
        forCells(field, (cx, cy, cz, bodies) -> {
            if (bodies.isEmpty()) {
                return;
            }
            Vec3 middle = new Vec3((cx + 0.5) * 1024, (cy + 0.5) * 1024, (cz + 0.5) * 1024);
            Optional<DustCloud> cloud = field.dust().nearest(middle, AsteroidField.CLOUD_RANGE, false);
            if (cloud.isPresent() && List.of(DustKinds.SILICATE, DustKinds.ICE, DustKinds.CARBON, DustKinds.METALLIC).contains(cloud.get().kind())) {
                counted[0]++;
                if (bodies.get(0).kind().equals(cloud.get().kind())) {
                    matching[0]++;
                }
            }
        });
        assertTrue(counted[0] > 100, "too few clusters near clouds to judge: " + counted[0]);
        assertTrue(matching[0] / (double) counted[0] > 0.8, "kind follows the cloud only " + matching[0] + "/" + counted[0]);
    }

    @Test
    void everyKindAppearsWhereNoCloudDecides() {
        AsteroidField field = field(5L);
        java.util.Set<ResourceLocation> seen = new java.util.HashSet<>();
        RandomSource random = RandomSource.create(1);
        for (int i = 0; i < 400; i++) {
            seen.add(field.kindFor(new Vec3(1_000_000 + i * 5000.0, 0, 0), random));
        }
        assertEquals(4, seen.size(), "kinds: " + seen);
    }

    @Test
    void bodyShapeIsALumpyEllipsoid() {
        AsteroidBody body = new AsteroidBody(new Vec3(100, 50, -20), 20, 15, 18, DustKinds.ICE, 99L, AsteroidBody.Size.LARGE);
        assertTrue(body.contains(100, 50, -20), "center");
        assertFalse(body.contains(100 + body.maxExtent() + 1, 50, -20), "far outside");
        assertFalse(body.contains(100, 50 + 15 * AsteroidBody.OUTER + 0.5, -20), "just beyond the outer limit");
        // The volume is roughly an ellipsoid's, within the noise.
        int inside = 0;
        int samples = 40_000;
        RandomSource random = RandomSource.create(3);
        double e = body.maxExtent();
        for (int i = 0; i < samples; i++) {
            if (body.contains(100 + (random.nextDouble() * 2 - 1) * e, 50 + (random.nextDouble() * 2 - 1) * e, -20 + (random.nextDouble() * 2 - 1) * e)) {
                inside++;
            }
        }
        double volume = inside / (double) samples * 8 * e * e * e;
        double ellipsoid = 4.0 / 3.0 * Math.PI * 20 * 15 * 18;
        assertTrue(volume > ellipsoid * 0.65 && volume < ellipsoid * 1.45, "volume " + volume + " vs " + ellipsoid);
    }

    @Test
    void noiseStaysInRange() {
        RandomSource random = RandomSource.create(8);
        for (int i = 0; i < 5000; i++) {
            double n = AsteroidBody.noise(5L, random.nextDouble() * 100 - 50, random.nextDouble() * 100 - 50, random.nextDouble() * 100 - 50);
            assertTrue(n >= -1.0001 && n <= 1.0001, "noise " + n);
        }
    }
}
