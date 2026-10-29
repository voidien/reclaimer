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
                "nanite_swarm", "core_mk2", "storage_mk1", "storage_mk2");
        java.util.Set<String> seen = new java.util.HashSet<>();
        for (ResourceKey<Chassis> key : java.util.List.of(BuiltinBodies.DRONE, BuiltinBodies.MECH, BuiltinBodies.SHIP)) {
            BodyData preset = Bodies.preset(key, true);
            helper.assertTrue(BodyRules.normalize(preset, BuiltinBodies.CHASSIS.get(key)).returned().isEmpty(),
                    key.location() + " full preset has parts that don't fit");
            preset.modules().values().forEach(m -> seen.add(net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(m.getItem()).getPath()));
        }
        helper.assertTrue(seen.containsAll(expected), "missing modules on the full presets: " + expected.stream().filter(e -> !seen.contains(e)).toList());
        helper.succeed();
    }
}
