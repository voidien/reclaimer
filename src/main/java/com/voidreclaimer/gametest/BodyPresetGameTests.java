package com.voidreclaimer.gametest;

import com.voidreclaimer.VoidReclaimer;
import com.voidreclaimer.api.body.Chassis;
import com.voidreclaimer.body.Bodies;
import com.voidreclaimer.body.BodyData;
import com.voidreclaimer.body.BodyRules;
import com.voidreclaimer.body.BuiltinBodies;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/** The body presets behind {@code /void_reclaimer body preset}: complete, valid bodies of each built-in chassis. */
@GameTestHolder(VoidReclaimer.MOD_ID)
@PrefixGameTestTemplate(false)
public final class BodyPresetGameTests {
    private BodyPresetGameTests() {}

    private static void check(GameTestHelper helper, ResourceKey<Chassis> key, double x, boolean full) {
        ServerPlayer player = TestPlayers.survivalInDeepSpace(helper, x, 80, 0.5);
        try {
            Chassis chassis = BuiltinBodies.CHASSIS.get(key);
            BodyData preset = Bodies.preset(key, full);
            helper.assertTrue(BodyRules.normalize(preset, chassis).returned().isEmpty(), key.location() + " preset has parts that don't fit");
            Bodies.store(player, preset);
            helper.assertTrue(Bodies.view(player).missing().isEmpty(), key.location() + " preset leaves sockets empty");
            helper.assertTrue(Bodies.data(player).pending().isEmpty(), "parts were handed back");
            helper.assertTrue(Bodies.stats(player).size().equals(chassis.sizeClass()), "wrong size class " + Bodies.stats(player).size());
            helper.assertTrue(!Bodies.data(player).modules().isEmpty(), "no modules installed");
            helper.succeed();
        } finally {
            TestPlayers.remove(player);
        }
    }

    @GameTest(template = "empty", timeoutTicks = 60)
    public static void dronePresetIsComplete(GameTestHelper helper) {
        check(helper, BuiltinBodies.DRONE, 89_000.5, false);
    }

    @GameTest(template = "empty", timeoutTicks = 60)
    public static void mechPresetIsComplete(GameTestHelper helper) {
        check(helper, BuiltinBodies.MECH, 89_020.5, false);
    }

    @GameTest(template = "empty", timeoutTicks = 60)
    public static void shipPresetIsComplete(GameTestHelper helper) {
        check(helper, BuiltinBodies.SHIP, 89_040.5, false);
    }

    @GameTest(template = "empty", timeoutTicks = 60)
    public static void shuttlePresetIsComplete(GameTestHelper helper) {
        check(helper, BuiltinBodies.SHUTTLE, 89_120.5, false);
    }

    /** The Shuttle is a second layout of the Ship size class: same scale, own chassis. */
    @GameTest(template = "empty", timeoutTicks = 60)
    public static void shuttleSharesTheShipSizeClassButHasItsOwnLayout(GameTestHelper helper) {
        helper.assertTrue(BuiltinBodies.CHASSIS.get(BuiltinBodies.SHUTTLE).sizeClass().equals(BuiltinBodies.SHIP_SIZE), "not Ship class");
        helper.assertTrue(!BuiltinBodies.CHASSIS.get(BuiltinBodies.SHUTTLE).sockets().equals(BuiltinBodies.CHASSIS.get(BuiltinBodies.SHIP).sockets()), "same sockets as the Ship");
        helper.assertTrue(BuiltinBodies.SIZE_CLASSES.get(BuiltinBodies.SHIP_SIZE).scale() == 3.0, "the Ship size class changed");
        helper.succeed();
    }

    @GameTest(template = "empty", timeoutTicks = 60)
    public static void titanPresetIsComplete(GameTestHelper helper) {
        check(helper, BuiltinBodies.TITAN, 89_080.5, false);
    }

    /** The Titan is a new size class over 50 blocks long: the rig is 8.4 blocks at scale 1. */
    @GameTest(template = "empty", timeoutTicks = 60)
    public static void titanIsTheBiggestSizeClass(GameTestHelper helper) {
        double titan = BuiltinBodies.SIZE_CLASSES.get(BuiltinBodies.TITAN_SIZE).scale();
        helper.assertTrue(titan * 8.4 > 50.0, "the Titan would be only " + titan * 8.4 + " blocks long");
        for (var other : java.util.List.of(BuiltinBodies.DRONE_SIZE, BuiltinBodies.MECH_SIZE, BuiltinBodies.SHIP_SIZE)) {
            helper.assertTrue(BuiltinBodies.SIZE_CLASSES.get(other).scale() < titan, other.location() + " is not smaller");
            helper.assertTrue(BuiltinBodies.SIZE_CLASSES.get(other).rank() < BuiltinBodies.SIZE_CLASSES.get(BuiltinBodies.TITAN_SIZE).rank(), "rank");
        }
        helper.succeed();
    }

    /** A section can't be above the core's tier: Ship sections (3) need a Core Mk III, Titan sections (4) a Core Mk IV. */
    @GameTest(template = "empty", timeoutTicks = 60)
    public static void shipAndTitanSectionsNeedTheirCores(GameTestHelper helper) {
        ServerPlayer player = TestPlayers.survivalInDeepSpace(helper, 89_100.5, 80, 0.5);
        try {
            check(helper, player, BuiltinBodies.SHIP, "wing_port", new net.minecraft.world.item.ItemStack(com.voidreclaimer.registry.ModItems.SHIP_WING.get()),
                    com.voidreclaimer.registry.ModItems.CORE_MK2.get(), com.voidreclaimer.registry.ModItems.CORE_MK3.get());
            check(helper, player, BuiltinBodies.SHUTTLE, "nose", new net.minecraft.world.item.ItemStack(com.voidreclaimer.registry.ModItems.SHUTTLE_NOSE.get()),
                    com.voidreclaimer.registry.ModItems.CORE_MK2.get(), com.voidreclaimer.registry.ModItems.CORE_MK3.get());
            check(helper, player, BuiltinBodies.TITAN, "bridge", new net.minecraft.world.item.ItemStack(com.voidreclaimer.registry.ModItems.TITAN_BRIDGE.get()),
                    com.voidreclaimer.registry.ModItems.CORE_MK3.get(), com.voidreclaimer.registry.ModItems.CORE_MK4.get());
            helper.succeed();
        } finally {
            TestPlayers.remove(player);
        }
    }

    private static void check(GameTestHelper helper, ServerPlayer player, ResourceKey<Chassis> chassis, String socket,
                              net.minecraft.world.item.ItemStack section, net.minecraft.world.item.Item tooLow, net.minecraft.world.item.Item enough) {
        for (var item : java.util.List.of(tooLow, enough)) {
            BodyData base = Bodies.preset(chassis);
            BodyData.Mount core = base.modules().keySet().stream().filter(m -> m.kind() == com.voidreclaimer.api.body.ModuleKind.CORE).findFirst().orElseThrow();
            Bodies.store(player, base.withModule(core, new net.minecraft.world.item.ItemStack(item)));
            BodyRules.Check result = BodyRules.canInstallSection(Bodies.view(player), socket, section);
            boolean expectOk = item == enough;
            helper.assertTrue((result == BodyRules.Check.OK) == expectOk,
                    chassis.location() + " " + socket + " with " + item + ": " + result);
        }
    }

    /** The Mech's jetpack is a rotor arm on its back, with a thruster. */
    @GameTest(template = "empty", timeoutTicks = 60)
    public static void mechPresetHasAJetpack(GameTestHelper helper) {
        BodyData preset = Bodies.preset(BuiltinBodies.MECH);
        helper.assertFalse(preset.section("back").isEmpty(), "no jetpack on the back");
        helper.assertTrue(preset.modules().keySet().stream().anyMatch(m -> m.socket().equals("back")), "no thruster on the jetpack");
        helper.succeed();
    }

    /** Taking a section off empties its socket and leaves the other limbs alone. */
    @GameTest(template = "empty", timeoutTicks = 60)
    public static void removingASectionLeavesTheOtherLimbs(GameTestHelper helper) {
        ServerPlayer player = TestPlayers.survivalInDeepSpace(helper, 89_060.5, 80, 0.5);
        try {
            Bodies.store(player, Bodies.preset(BuiltinBodies.MECH));
            helper.assertTrue(Bodies.removeSection(player, "left_forearm") == BodyRules.Check.OK, "forearm not removed");
            BodyData body = Bodies.data(player);
            helper.assertTrue(body.section("left_forearm").isEmpty(), "forearm still there");
            helper.assertFalse(body.section("left_upper_arm").isEmpty(), "the upper arm went too");
            helper.assertFalse(body.section("right_forearm").isEmpty(), "the other forearm went too");
            helper.succeed();
        } finally {
            TestPlayers.remove(player);
        }
    }

    /** The full presets put every kind of module on the body, and all of them fit the chassis. */
    @GameTest(template = "empty", timeoutTicks = 60)
    public static void fullPresetsCarryEveryModuleAndAllFit(GameTestHelper helper) {
        java.util.Set<String> expected = java.util.Set.of("dust_sensor", "mechanical_drill", "matter_harvester", "salvage_tool",
                "nanite_swarm", "core_mk2", "core_mk3", "core_mk4", "storage_mk1", "storage_mk2");
        java.util.Set<String> seen = new java.util.HashSet<>();
        for (ResourceKey<Chassis> key : java.util.List.of(BuiltinBodies.DRONE, BuiltinBodies.MECH, BuiltinBodies.SHIP, BuiltinBodies.SHUTTLE, BuiltinBodies.TITAN)) {
            BodyData preset = Bodies.preset(key, true);
            helper.assertTrue(BodyRules.normalize(preset, BuiltinBodies.CHASSIS.get(key)).returned().isEmpty(),
                    key.location() + " full preset has parts that don't fit");
            preset.modules().values().forEach(m -> seen.add(net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(m.getItem()).getPath()));
        }
        helper.assertTrue(seen.containsAll(expected), "missing modules on the full presets: " + expected.stream().filter(e -> !seen.contains(e)).toList());
        helper.succeed();
    }
}
