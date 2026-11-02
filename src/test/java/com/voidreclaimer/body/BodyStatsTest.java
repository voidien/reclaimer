package com.voidreclaimer.body;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import com.voidreclaimer.api.body.Chassis;
import com.voidreclaimer.api.body.ModuleKind;
import com.voidreclaimer.api.body.ModuleStats;
import com.voidreclaimer.api.body.SectionStats;
import com.voidreclaimer.api.body.SizeClass;
import com.voidreclaimer.api.body.Socket;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;

class BodyStatsTest {
    private static final SizeClass DRONE = BuiltinBodies.SIZE_CLASSES.get(BuiltinBodies.DRONE_SIZE);
    /** The starter Drone's sections, as in ModItems: 0.5 mass in all, like the Phase 5a frame. */
    private static final List<SectionStats> DRONE_SECTIONS = List.of(
            section(0.2f, 1.6f, 12f, 1f, 1, Map.of(ModuleKind.CORE, 1, ModuleKind.STORAGE, 1)),
            section(0.05f, 0f, 0f, 0f, 0, Map.of(ModuleKind.SENSOR, 1)),
            section(0.05f, 0f, 0f, 0f, 0, Map.of(ModuleKind.THRUSTER, 1)),
            section(0.05f, 0f, 0f, 0f, 0, Map.of(ModuleKind.THRUSTER, 1)),
            section(0.05f, 0f, 0f, 0f, 0, Map.of(ModuleKind.THRUSTER, 1)),
            section(0.05f, 0f, 0f, 0f, 0, Map.of(ModuleKind.THRUSTER, 1)),
            section(0.05f, 0f, 0f, 0f, 0, Map.of(ModuleKind.TOOL, 2)));
    private static final ModuleStats CORE_MK1 = ModuleStats.core(1, 0.2f, new ModuleStats.Core(100f, 1.0f));
    private static final ModuleStats THRUSTER_MK1 = ModuleStats.thruster(1, 0.3f, Optional.empty(), new ModuleStats.Thruster(0.03f, 0.04f, 10f));
    private static final ModuleStats STORAGE_MK1 = ModuleStats.storage(1, 0.2f, new ModuleStats.Storage(1, 100f));

    private static SectionStats section(float mass, float budget, float health, float intake, int rows, Map<ModuleKind, Integer> mounts) {
        return new SectionStats(ResourceLocation.fromNamespaceAndPath("test", "part"), 1, mass, budget, health, 0f, intake, rows,
                0f, 0f, 0f, mounts, Optional.empty());
    }

    private static BodyStats drone(List<ModuleStats> modules) {
        return BodyStats.compute(BuiltinBodies.DRONE_SIZE, DRONE, DRONE_SECTIONS, modules);
    }

    @Test
    void starterBodyKeepsThePhaseTwoHandling() {
        BodyStats stats = drone(List.of(CORE_MK1, THRUSTER_MK1));
        assertEquals(1.0, stats.mass(), 1e-6);
        assertEquals(0.03, stats.flight().acceleration(), 1e-6);
        assertEquals(0.04, stats.flight().brakeDeceleration(), 1e-6);
        assertEquals(0.5, stats.flight().maxSpeed(), 1e-6, "10 m/s");
        assertEquals(1.0, stats.energyUse(), 1e-6);
        assertEquals(100f, stats.energyCapacity());
        assertEquals(18, stats.inventorySlots(), "hotbar + one row");
        assertEquals(1, stats.coreTier());
        assertEquals(12f, stats.maxHealth());
        assertEquals(0.4, stats.scale());
        assertFalse(stats.overloaded());
    }

    @Test
    void overloadIsASoftPenalty() {
        // Two thrusters and extras: 1.0 + 0.3 + 0.6 = 1.9 of 1.6.
        BodyStats heavy = drone(List.of(CORE_MK1, THRUSTER_MK1, THRUSTER_MK1, ModuleStats.simple(ModuleKind.TOOL, 1, 0.6f)));
        assertTrue(heavy.overloaded());
        double overload = BodyStats.overload(1.9f, 1.6f);
        assertEquals(0.1875, overload, 1e-6);
        assertEquals(0.06 / (1 + 2 * overload) / 1.9, heavy.flight().acceleration(), 1e-6);
        assertEquals(0.5 / (1 + overload), heavy.flight().maxSpeed(), 1e-6);
        assertEquals(1 + 2 * overload, heavy.energyUse(), 1e-6);
        assertTrue(heavy.flight().acceleration() > 0);
    }

    @Test
    void storageAddsRowsAndDustUpToTheMaximum() {
        BodyStats one = drone(List.of(CORE_MK1, THRUSTER_MK1, STORAGE_MK1));
        assertEquals(27, one.inventorySlots());
        assertEquals(100f, one.dustCapacity());
        BodyStats lots = drone(List.of(CORE_MK1, STORAGE_MK1, STORAGE_MK1, STORAGE_MK1, STORAGE_MK1));
        assertEquals(36, lots.inventorySlots(), "never more than the vanilla inventory");
    }

    @Test
    void missingSectionsTakeTheirStatsWithThem() {
        BodyStats hullOnly = BodyStats.compute(BuiltinBodies.DRONE_SIZE, DRONE, List.of(DRONE_SECTIONS.get(0)), List.of(CORE_MK1));
        assertEquals(0.4, hullOnly.mass(), 1e-6);
        assertEquals(0.0, hullOnly.flight().acceleration(), 1e-9, "no thrusters, no thrust");
        BodyStats empty = BodyStats.compute(BuiltinBodies.DRONE_SIZE, DRONE, List.of(), List.of());
        assertEquals(100f, empty.energyCapacity(), "fallback without a core");
        assertEquals(BodyStats.MIN_HEALTH, empty.maxHealth(), "never zero health");
        assertEquals(9, empty.inventorySlots(), "hotbar only");
    }

    @Test
    void builtinLayoutsMatchTheDesign() {
        Chassis drone = BuiltinBodies.CHASSIS.get(BuiltinBodies.DRONE);
        Chassis mech = BuiltinBodies.CHASSIS.get(BuiltinBodies.MECH);
        Chassis ship = BuiltinBodies.CHASSIS.get(BuiltinBodies.SHIP);
        assertEquals(7, drone.sockets().size());
        assertEquals(16, mech.sockets().size(), "15 + the optional back socket");
        assertEquals(31, ship.sockets().size());
        for (Chassis chassis : List.of(drone, mech, ship)) {
            Set<String> ids = chassis.sockets().stream().map(Socket::id).collect(Collectors.toSet());
            assertEquals(chassis.sockets().size(), ids.size(), "socket ids unique");
            assertTrue(chassis.socket(chassis.coreSocket()).required(), "core socket required");
            assertEquals(1, chassis.sockets().stream().filter(Socket::required).count(), "only the core is required");
            // Round trip through the codec, which also validates parents.
            Chassis copy = Chassis.CODEC.parse(JsonOps.INSTANCE, Chassis.CODEC.encodeStart(JsonOps.INSTANCE, chassis).getOrThrow()).getOrThrow();
            assertEquals(chassis, copy);
        }
        // Cross-fit only through shared socket types: Ship hardpoints take a Mech hand, the Mech back a Drone rotor arm.
        assertTrue(ship.socket("hardpoint_1").accepts(BuiltinBodies.HAND));
        assertTrue(mech.socket("back").accepts(BuiltinBodies.ROTOR_ARM));
        assertFalse(mech.socket("left_hand").accepts(BuiltinBodies.ROTOR_ARM));
    }

    @Test
    void chassisCodecRejectsBrokenLayouts() {
        String noCore = "{\"size_class\":\"drone\",\"core_socket\":\"hull\",\"sockets\":[{\"id\":\"arm\",\"accepts\":[\"a:b\"]}]}";
        assertTrue(Chassis.CODEC.parse(JsonOps.INSTANCE, JsonParser.parseString(noCore)).isError());
        String badParent = "{\"size_class\":\"drone\",\"core_socket\":\"hull\",\"sockets\":[{\"id\":\"hull\",\"accepts\":[\"a:b\"]},"
                + "{\"id\":\"arm\",\"accepts\":[\"a:c\"],\"parent\":\"nowhere\"}]}";
        assertTrue(Chassis.CODEC.parse(JsonOps.INSTANCE, JsonParser.parseString(badParent)).isError());
    }

    @Test
    void moduleStatsCodecDefaults() {
        ModuleStats parsed = ModuleStats.CODEC.parse(JsonOps.INSTANCE, JsonParser.parseString(
                "{\"kind\":\"thruster\",\"tier\":2,\"mass\":0.6,\"min_size\":\"mech\",\"thruster\":{\"thrust\":0.07,\"top_speed\":16}}"))
                .getOrThrow();
        assertEquals(ModuleKind.THRUSTER, parsed.kind());
        assertEquals(BuiltinBodies.MECH_SIZE, parsed.minSize().orElseThrow(), "bare size names are ours");
        assertEquals(0f, parsed.thruster().orElseThrow().brake(), "missing fields default");
        ModuleStats simple = ModuleStats.CODEC.parse(JsonOps.INSTANCE, JsonParser.parseString("{\"kind\":\"cosmetic\"}")).getOrThrow();
        assertEquals(1, simple.tier());
        assertTrue(simple.minSize().isEmpty());
        assertEquals(Cosmetics.DEFAULT, Cosmetics.CODEC.parse(JsonOps.INSTANCE, JsonParser.parseString("{}")).getOrThrow());
    }

    @Test
    void oldCosmeticsWithBareNamesStillLoad() {
        com.google.gson.JsonObject json = new com.google.gson.JsonObject();
        json.addProperty("casing", "brass");
        json.addProperty("trail", "ember");
        Cosmetics cosmetics = Cosmetics.CODEC.parse(JsonOps.INSTANCE, json).getOrThrow();
        assertEquals(Cosmetics.BRASS, cosmetics.casing());
        assertEquals(Cosmetics.EMBER, cosmetics.trail());
        assertEquals(Cosmetics.DEFAULT_ACCENT, cosmetics.accent());
    }

    @Test
    void aSizeClassSetsHowFastItsBodiesGoUpToItsLimit() {
        double mk1 = drone(List.of(CORE_MK1, THRUSTER_MK1)).flight().maxSpeed();
        SizeClass triple = new SizeClass(1.0, 0, 3.0);
        assertEquals(mk1 * 3.0, BodyStats.compute(BuiltinBodies.DRONE_SIZE, triple, DRONE_SECTIONS, List.of(CORE_MK1, THRUSTER_MK1)).flight().maxSpeed(), 1e-4);
        SizeClass limited = new SizeClass(1.0, 0, Optional.empty(), 100.0, 25.0);
        assertEquals(25.0 / 20.0, BodyStats.compute(BuiltinBodies.DRONE_SIZE, limited, DRONE_SECTIONS, List.of(CORE_MK1, THRUSTER_MK1)).flight().maxSpeed(), 1e-4, "the limit holds");
    }

    @Test
    void theSamePodsHoldMoreOnASizeClassThatSaysSo() {
        ModuleStats pod = ModuleStats.storage(1, 0.2f, new ModuleStats.Storage(1, 100f));
        float plain = BodyStats.compute(BuiltinBodies.DRONE_SIZE, new SizeClass(1.0, 0), DRONE_SECTIONS, List.of(CORE_MK1, pod)).dustCapacity();
        assertTrue(plain > 0f, "the pod holds nothing, so this proves nothing");
        float four = BodyStats.compute(BuiltinBodies.DRONE_SIZE, new SizeClass(1.0, 0, 1.0, 4.0), DRONE_SECTIONS, List.of(CORE_MK1, pod)).dustCapacity();
        assertEquals(plain * 4f, four, 1e-3f);
    }

    @Test
    void theBuiltInClassesGiveTheOwnersSpeedsOnAMarkOneThruster() {
        Map<String, Double> wanted = Map.of("drone", 10.0, "mech", 14.0, "titan", 20.0, "ship", 30.0, "shuttle", 40.0);
        BuiltinBodies.SIZE_CLASSES.forEach((key, size) ->
                assertEquals(wanted.get(key.location().getPath()), Math.min(10.0 * size.speedFactor(), size.maxSpeed()), 1e-6, key.location().toString()));
    }
}
