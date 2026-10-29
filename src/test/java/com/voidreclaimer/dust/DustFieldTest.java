package com.voidreclaimer.dust;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.JsonObject;
import com.mojang.serialization.JsonOps;
import com.voidreclaimer.api.dust.DustKind;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.Test;

class DustFieldTest {
    private static final DustField FIELD = new DustField(1234L);

    @Test
    void generationIsDeterministicPerSeed() {
        DustField again = new DustField(1234L);
        for (int x = -3; x <= 3; x++) {
            for (int z = -3; z <= 3; z++) {
                assertEquals(FIELD.cloudInCell(x, 0, z), again.cloudInCell(x, 0, z));
            }
        }
        // A different seed gives a different layout.
        DustField other = new DustField(99L);
        boolean anyDifferent = false;
        for (int x = -3; x <= 3 && !anyDifferent; x++) {
            anyDifferent = !FIELD.cloudInCell(x, 0, 0).equals(other.cloudInCell(x, 0, 0));
        }
        assertTrue(anyDifferent);
    }

    @Test
    void cloudsAreLargeRareAndInsideTheirCell() {
        int cells = 0;
        int clouds = 0;
        for (int x = -10; x < 10; x++) {
            for (int y = -2; y < 2; y++) {
                for (int z = -10; z < 10; z++) {
                    cells++;
                    Optional<DustCloud> cloud = FIELD.cloudInCell(x, y, z);
                    if (cloud.isPresent()) {
                        clouds++;
                        DustCloud c = cloud.get();
                        assertTrue(c.radius() >= DustField.MIN_RADIUS && c.radius() <= DustField.MAX_RADIUS);
                        assertEquals(x, Math.floorDiv((int) Math.floor(c.center().x), DustField.CELL));
                        assertEquals(z, Math.floorDiv((int) Math.floor(c.center().z), DustField.CELL));
                    }
                }
            }
        }
        double rate = clouds / (double) cells;
        assertTrue(Math.abs(rate - DustField.CLOUD_CHANCE) < 0.05, "cloud rate " + rate);
    }

    @Test
    void densityIsZeroOutsideAndHighestTowardTheCore() {
        DustCloud cloud = anyCloud();
        Vec3 c = cloud.center();
        assertEquals(0.0, FIELD.density(cloud, c.add(cloud.radius() + 1, 0, 0)));
        assertEquals(0.0, FIELD.density(cloud, c.add(0, 0, cloud.radius())));
        double core = FIELD.density(cloud, c);
        double mid = FIELD.density(cloud, c.add(cloud.radius() * 0.5, 0, 0));
        double rim = FIELD.density(cloud, c.add(cloud.radius() * 0.95, 0, 0));
        assertTrue(core > 0.15, "core density " + core);
        assertTrue(core > rim, "core " + core + " should exceed rim " + rim);
        assertTrue(mid > rim, "mid " + mid + " should exceed rim " + rim);
        assertTrue(core <= 1.0);
    }

    @Test
    void sampleFindsTheCloudAndNothingFarAway() {
        DustCloud cloud = anyCloud();
        DustField.Sample inside = FIELD.sample(cloud.center());
        assertTrue(inside.inCloud());
        assertEquals(cloud, inside.cloud());
        assertFalse(FIELD.sample(farFromClouds()).inCloud());
    }

    @Test
    void nearestFindsTheClosestSurfaceAndCanSkipTheContainingCloud() {
        DustCloud cloud = anyCloud();
        Vec3 outside = cloud.center().add(cloud.radius() + 50, 0, 0);
        Optional<DustCloud> nearest = FIELD.nearest(outside, 60, false);
        assertTrue(nearest.isPresent());
        assertTrue(nearest.get().surfaceDistance(outside) <= 60);
        assertTrue(FIELD.nearest(outside, 10, false).map(c -> c.surfaceDistance(outside) <= 10).orElse(true));

        Optional<DustCloud> fromInside = FIELD.nearest(cloud.center(), 5000, true);
        fromInside.ifPresent(other -> assertNotEquals(cloud, other));
    }

    @Test
    void echoPointsRoughlyAtTheCloudAndWeakensWithDistance() {
        DustCloud cloud = anyCloud();
        RandomSource random = RandomSource.create(7);
        Vec3 near = cloud.center().add(cloud.radius() + 100, 0, 0);
        Vec3 far = cloud.center().add(cloud.radius() + 1800, 0, 0);
        for (int i = 0; i < 50; i++) {
            SensorPulse.Echo echo = SensorPulse.computeEcho(FIELD, near, 2048, random);
            if (echo.strength() == SensorPulse.Strength.NONE) {
                continue;
            }
            Vec3 exact = FIELD.nearest(near, 2048, true).orElseThrow().center().subtract(near).normalize();
            double angle = Math.toDegrees(Math.acos(Math.min(1.0, echo.direction().dot(exact))));
            assertTrue(angle <= 30.0 + 1e-6, "echo error " + angle);
            assertEquals(1.0, echo.direction().length(), 1e-6);
        }
        assertEquals(SensorPulse.Strength.STRONG, SensorPulse.Strength.forDistance(100, 2048));
        assertEquals(SensorPulse.Strength.MODERATE, SensorPulse.Strength.forDistance(1000, 2048));
        assertEquals(SensorPulse.Strength.FAINT, SensorPulse.Strength.forDistance(1800, 2048));
        assertTrue(SensorPulse.delayFor(1800) > SensorPulse.delayFor(100));
        assertTrue(SensorPulse.computeEcho(FIELD, far, 2048, random).delayTicks() >= SensorPulse.delayFor(0));
    }

    @Test
    void blurNeverExceedsTheMaximumError() {
        RandomSource random = RandomSource.create(3);
        Vec3 dir = new Vec3(0.3, -0.5, 0.8).normalize();
        double max = Math.toRadians(20);
        for (int i = 0; i < 500; i++) {
            Vec3 blurred = SensorPulse.blur(dir, max, random);
            assertTrue(Math.acos(Math.min(1.0, blurred.dot(dir))) <= max + 1e-5);
        }
    }

    @Test
    void dustGainScalesWithDensityAndSpeed() {
        float still = DustTicker.gainPerTick(1.0, 0.0, 0.1, 0.1);
        float flying = DustTicker.gainPerTick(1.0, 0.5, 0.1, 0.1);
        float edge = DustTicker.gainPerTick(0.2, 0.5, 0.1, 0.1);
        assertEquals(0.005f, still, 1e-6f);
        assertEquals(0.055f, flying, 1e-6f);
        assertEquals(0.011f, edge, 1e-6f);
        assertEquals(0f, DustTicker.gainPerTick(0.0, 0.5, 0.1, 0.1));
    }

    @Test
    void dustDataRespectsCapacityPerKind() {
        DustData data = DustData.EMPTY.add(DustKinds.ICE, 150f, 200f).add(DustKinds.ICE, 100f, 200f);
        assertEquals(200f, data.get(DustKinds.ICE));
        assertEquals(0f, data.get(DustKinds.METALLIC));
        assertEquals(5f, data.add(DustKinds.CARBON, 5f, 200f).get(DustKinds.CARBON));
        assertEquals(1, data.kindsHeld());
    }

    @Test
    void builtinKindsKeepThePhase3CloudRanges() {
        // Cumulative weights 0.30 / 0.65 / 0.85 / 1.0 in metallic, silicate, ice, carbon order: existing worlds keep their clouds.
        List<DustKinds.Entry> kinds = DustKinds.builtinEntries();
        assertEquals(DustKinds.METALLIC, DustKinds.pick(kinds, 0.0));
        assertEquals(DustKinds.METALLIC, DustKinds.pick(kinds, 0.29));
        assertEquals(DustKinds.SILICATE, DustKinds.pick(kinds, 0.31));
        assertEquals(DustKinds.ICE, DustKinds.pick(kinds, 0.66));
        assertEquals(DustKinds.CARBON, DustKinds.pick(kinds, 0.999));
    }

    @Test
    void addedKindsJoinTheWeightsAndZeroWeightKindsStayOut() {
        ResourceLocation plasma = ResourceLocation.fromNamespaceAndPath("addon", "plasma");
        List<DustKinds.Entry> kinds = new ArrayList<>(DustKinds.builtinEntries());
        kinds.add(new DustKinds.Entry(plasma, new DustKind(0xFF00FF, 1.0, DustKind.DEFAULT_ORDER)));
        kinds.add(new DustKinds.Entry(ResourceLocation.fromNamespaceAndPath("addon", "rare"), new DustKind(0xFFFFFF, 0.0, DustKind.DEFAULT_ORDER)));
        kinds.sort(DustKinds.ORDER);
        assertEquals(plasma, DustKinds.pick(kinds, 0.99));
        assertEquals(DustKinds.METALLIC, DustKinds.pick(kinds, 0.0));
        assertNull(DustKinds.pick(List.of(new DustKinds.Entry(plasma, new DustKind(0, 0.0, DustKind.DEFAULT_ORDER))), 0.5));
    }

    @Test
    void oldSavesWithBareKindNamesStillLoad() {
        JsonObject old = new JsonObject();
        old.addProperty("metallic", 12.5f);
        old.addProperty("silicate", 0f);
        old.addProperty("ice", 3f);
        old.addProperty("carbon", 0f);
        DustData data = DustData.CODEC.parse(JsonOps.INSTANCE, old).getOrThrow();
        assertEquals(12.5f, data.get(DustKinds.METALLIC));
        assertEquals(3f, data.get(DustKinds.ICE));
        assertEquals(2, data.kindsHeld(), "empty kinds are not stored");
        DustData roundTrip = DustData.CODEC.parse(JsonOps.INSTANCE, DustData.CODEC.encodeStart(JsonOps.INSTANCE, data).getOrThrow()).getOrThrow();
        assertEquals(data, roundTrip);
    }

    @Test
    void unknownKindsAreKeptNotDropped() {
        JsonObject json = new JsonObject();
        json.addProperty("addon:plasma", 7f);
        DustData data = DustData.CODEC.parse(JsonOps.INSTANCE, json).getOrThrow();
        assertEquals(7f, data.get(ResourceLocation.fromNamespaceAndPath("addon", "plasma")));
    }

    @Test
    void starterNebulaHasEveryKindNearSpawn() {
        Vec3 spawn = new Vec3(0.5, 64, 0.5);
        Vec3 center = StarterNebula.center(1234L, spawn);
        assertEquals(StarterNebula.DISTANCE, center.distanceTo(spawn), 1.0E-6);
        assertEquals(center, StarterNebula.center(1234L, spawn), "same seed, same place");
        assertNotEquals(center, StarterNebula.center(99L, spawn), "direction depends on the seed");

        DustField withNebula = new DustField(1234L, center, DustKinds.builtinEntries());
        for (ResourceLocation kind : DustKinds.BUILTIN.keySet()) {
            DustCloud pocket = StarterNebula.pockets(center).stream().filter(c -> c.kind().equals(kind)).findFirst().orElseThrow();
            DustField.Sample sample = withNebula.sample(pocket.center());
            assertTrue(sample.inCloud() && sample.density() > 0.3, kind + " pocket too thin: " + sample.density());
            // Reachable: every pocket's edge is well within the sensor hum range of spawn.
            assertTrue(pocket.surfaceDistance(spawn) < 400, kind + " pocket too far: " + pocket.surfaceDistance(spawn));
        }
        assertTrue(withNebula.nearest(spawn, 512, false).isPresent());
        // Without the rule the same seed has no nebula there.
        assertFalse(FIELD.cloudsAround(center, 0).stream().anyMatch(c -> c.radius() == StarterNebula.POCKET_RADIUS));
    }

    static DustCloud anyCloud() {
        for (int x = 0; x < 50; x++) {
            Optional<DustCloud> cloud = FIELD.cloudInCell(x, 0, 0);
            if (cloud.isPresent()) {
                return cloud.get();
            }
        }
        throw new AssertionError("no cloud in 50 cells");
    }

    static Vec3 farFromClouds() {
        for (int x = 0; x < 200; x++) {
            Vec3 pos = new Vec3(x * 97.0, 0, x * 31.0);
            List<DustCloud> around = FIELD.cloudsAround(pos, 0);
            if (around.stream().noneMatch(c -> c.contains(pos))) {
                return pos;
            }
        }
        throw new AssertionError("no empty point found");
    }
}
