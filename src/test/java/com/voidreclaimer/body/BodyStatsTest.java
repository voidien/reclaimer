package com.voidreclaimer.body;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class BodyStatsTest {
    private static final PartStats DRONE = PartStats.frame(1, 0.5f, new PartStats.Frame(SizeClass.DRONE, 1.6f, 12f, 0f, 1.0f, 1,
            Map.of(PartKind.THRUSTER, 2, PartKind.STORAGE, 1)));
    private static final PartStats CORE_MK1 = PartStats.core(1, 0.2f, new PartStats.Core(100f, 1.0f));
    private static final PartStats THRUSTER_MK1 = PartStats.thruster(1, 0.3f, SizeClass.DRONE, new PartStats.Thruster(0.03f, 0.04f, 10f));
    private static final PartStats STORAGE_MK1 = PartStats.storage(1, 0.2f, new PartStats.Storage(1, 100f));

    @Test
    void starterBodyKeepsThePhaseTwoHandling() {
        BodyStats stats = BodyStats.compute(DRONE, List.of(CORE_MK1, THRUSTER_MK1));
        assertEquals(1.0, stats.mass(), 1e-6);
        assertEquals(0.03, stats.flight().acceleration(), 1e-6);
        assertEquals(0.04, stats.flight().brakeDeceleration(), 1e-6);
        assertEquals(0.5, stats.flight().maxSpeed(), 1e-6, "10 m/s");
        assertEquals(1.0, stats.energyUse(), 1e-6);
        assertEquals(100f, stats.energyCapacity());
        assertEquals(18, stats.inventorySlots(), "hotbar + one row");
        assertEquals(1, stats.coreTier());
        assertEquals(SizeClass.DRONE, stats.size());
        assertFalse(stats.overloaded());
    }

    @Test
    void overloadIsASoftPenalty() {
        // Two thrusters and extras: 1.0 + 0.3 + 0.6 = 1.9 of 1.6.
        BodyStats heavy = BodyStats.compute(DRONE, List.of(CORE_MK1, THRUSTER_MK1, THRUSTER_MK1,
                PartStats.simple(PartKind.TOOL, 1, 0.6f)));
        assertTrue(heavy.overloaded());
        double overload = BodyStats.overload(1.9f, 1.6f);
        assertEquals(0.1875, overload, 1e-6);
        assertEquals(0.06 / (1 + 2 * overload) / 1.9, heavy.flight().acceleration(), 1e-6);
        assertEquals(0.5 / (1 + overload), heavy.flight().maxSpeed(), 1e-6);
        assertEquals(1 + 2 * overload, heavy.energyUse(), 1e-6);
        // Still flies: more thrust, but each extra gram costs.
        assertTrue(heavy.flight().acceleration() > 0);
    }

    @Test
    void storageAddsRowsAndDustUpToTheMaximum() {
        BodyStats one = BodyStats.compute(DRONE, List.of(CORE_MK1, THRUSTER_MK1, STORAGE_MK1));
        assertEquals(27, one.inventorySlots());
        assertEquals(100f, one.dustCapacity());
        BodyStats lots = BodyStats.compute(DRONE, List.of(CORE_MK1, STORAGE_MK1, STORAGE_MK1, STORAGE_MK1, STORAGE_MK1));
        assertEquals(36, lots.inventorySlots(), "never more than the vanilla inventory");
    }

    @Test
    void missingPartsFallBackSafely() {
        BodyStats empty = BodyStats.compute(null, List.of());
        assertEquals(0.0, empty.flight().acceleration(), 1e-9, "no thrusters, no thrust");
        assertEquals(100f, empty.energyCapacity());
        assertEquals(18, empty.inventorySlots());
    }

    @Test
    void partStatsCodecRoundTripsAndDefaults() {
        PartStats parsed = PartStats.CODEC.parse(JsonOps.INSTANCE, JsonParser.parseString(
                "{\"kind\":\"thruster\",\"tier\":2,\"mass\":0.6,\"min_size\":\"mech\",\"thruster\":{\"thrust\":0.07,\"top_speed\":16}}"))
                .getOrThrow();
        assertEquals(PartKind.THRUSTER, parsed.kind());
        assertEquals(SizeClass.MECH, parsed.minSize());
        assertEquals(0f, parsed.thruster().orElseThrow().brake(), "missing fields default");
        PartStats simple = PartStats.CODEC.parse(JsonOps.INSTANCE, JsonParser.parseString("{\"kind\":\"cosmetic\"}")).getOrThrow();
        assertEquals(1, simple.tier());
        assertEquals(SizeClass.DRONE, simple.minSize());
        assertEquals(Cosmetics.DEFAULT, Cosmetics.CODEC.parse(JsonOps.INSTANCE, JsonParser.parseString("{}")).getOrThrow());
    }

    @Test
    void sizeClassesOrderAndScale() {
        assertTrue(SizeClass.SHIP.atLeast(SizeClass.MECH));
        assertFalse(SizeClass.DRONE.atLeast(SizeClass.MECH));
        assertEquals(0.4, SizeClass.DRONE.scale());
        assertEquals(1.2, SizeClass.MECH.scale());
        assertEquals(3.0, SizeClass.SHIP.scale());
    }
}
