package com.voidreclaimer.gametest;

import com.voidreclaimer.VoidReclaimer;
import com.voidreclaimer.api.VoidReclaimerRegistries;
import com.voidreclaimer.api.body.Casing;
import com.voidreclaimer.body.Blueprint;
import com.voidreclaimer.body.Blueprints;
import com.voidreclaimer.body.Bodies;
import com.voidreclaimer.body.Cosmetics;
import com.voidreclaimer.body.Wear;
import com.voidreclaimer.dust.DustData;
import com.voidreclaimer.dust.DustKinds;
import com.voidreclaimer.flight.FlightStats;
import com.voidreclaimer.registry.ModAttachments;
import com.voidreclaimer.registry.ModDataComponents;
import com.voidreclaimer.registry.ModItems;
import com.voidreclaimer.replicator.Milestones;
import com.voidreclaimer.replicator.Replicator;
import java.util.Map;
import java.util.Optional;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/** Per-section paint, wear and repair, and Body Blueprints. */
@GameTestHolder(VoidReclaimer.MOD_ID)
@PrefixGameTestTemplate(false)
public final class SectionCareGameTests {
    private static final String ARM = "rotor_front_left";

    private SectionCareGameTests() {}

    @GameTest(template = "empty")
    public static void sectionPaintOverridesAreValidated(GameTestHelper helper) {
        ServerPlayer player = TestPlayers.survivalInDeepSpace(helper, 70_000.5, 80, 0.5);
        try {
            Cosmetics start = Bodies.data(player).cosmetics();
            Bodies.setCosmetics(player, start.withPaint(ARM, new Cosmetics.SectionPaint(Optional.of(Cosmetics.BRASS), Optional.of(0xFF0000))));
            Cosmetics painted = Bodies.data(player).cosmetics();
            helper.assertTrue(painted.casing(ARM).equals(Cosmetics.BRASS), "section casing not applied");
            helper.assertTrue(painted.accent(ARM) == 0xFF0000, "section accent not applied");
            helper.assertTrue(painted.casing("hull").equals(start.casing()), "other sections changed");

            ResourceKey<Casing> obsidian = ResourceKey.create(VoidReclaimerRegistries.CASING,
                    ResourceLocation.fromNamespaceAndPath(GameTestPack.NAMESPACE, "obsidian"));
            Bodies.setCosmetics(player, painted.withPaint("hull", new Cosmetics.SectionPaint(Optional.of(obsidian), Optional.empty())));
            helper.assertTrue(Bodies.data(player).cosmetics().equals(painted), "locked casing applied to a section");
            helper.succeed();
        } finally {
            TestPlayers.remove(player);
        }
    }

    @GameTest(template = "empty")
    public static void thrustWearsThrusterSectionsAndHandlingDrops(GameTestHelper helper) {
        ServerPlayer player = TestPlayers.survivalInDeepSpace(helper, 71_000.5, 80, 0.5);
        try {
            for (int i = 0; i < 1200 * 5; i++) { // five minutes of thrust
                Wear.onThrustTick(player);
            }
            float arm = Wear.condition(Bodies.data(player).section(ARM));
            helper.assertTrue(arm < 0.95f && arm > 0.9f, "arm condition after 5 min: " + arm);
            helper.assertTrue(Wear.condition(Bodies.data(player).section("hull")) == 1f, "hull wore without a thruster");

            Bodies.store(player, Bodies.data(player).withSection(ARM, Wear.withCondition(Bodies.data(player).section(ARM), 0f)));
            double acceleration = FlightStats.of(player).acceleration();
            helper.assertTrue(acceleration == 0.0, "a worn-out arm should give nothing: " + acceleration);

            Bodies.store(player, Bodies.data(player).withSection(ARM, Wear.withCondition(Bodies.data(player).section(ARM), Wear.FLOOR_FROM_USE + 0.005f)));
            for (int i = 0; i < 1200 * 5; i++) {
                Wear.onThrustTick(player);
            }
            helper.assertTrue(Wear.condition(Bodies.data(player).section(ARM)) >= Wear.FLOOR_FROM_USE - 1e-6f, "thrust wore a section right out");
            double nearlyOut = FlightStats.of(player).acceleration();
            helper.assertTrue(nearlyOut > 0.03 * 0.74, "a nearly worn-out arm should lose about 25%: " + nearlyOut);

            player.setGameMode(GameType.CREATIVE);
            Bodies.store(player, Bodies.data(player).withSection(ARM, Wear.withCondition(Bodies.data(player).section(ARM), 1f)));
            for (int i = 0; i < 1200; i++) {
                Wear.onThrustTick(player);
            }
            helper.assertTrue(Wear.condition(Bodies.data(player).section(ARM)) == 1f, "creative body wore");
            helper.succeed();
        } finally {
            TestPlayers.remove(player);
        }
    }

    @GameTest(template = "empty")
    public static void repairCostsDustAndRestoresCondition(GameTestHelper helper) {
        ServerPlayer player = TestPlayers.survivalInDeepSpace(helper, 72_000.5, 80, 0.5);
        try {
            Bodies.store(player, Bodies.data(player).withSection(ARM, Wear.withCondition(Bodies.data(player).section(ARM), 0.5f)));
            helper.assertFalse(Bodies.repair(player, ARM), "repaired without dust");
            player.setData(ModAttachments.DUST, DustData.of(10, 10, 0, 0));
            helper.assertTrue(Bodies.repair(player, ARM), "repair refused");
            helper.assertTrue(Wear.condition(Bodies.data(player).section(ARM)) == 1f, "not repaired");
            // Rotor arm recipe: metallic 5, silicate 2; half worn at 20% -> 0.5 and 0.2.
            DustData dust = player.getData(ModAttachments.DUST);
            helper.assertTrue(Math.abs(dust.get(DustKinds.METALLIC) - 9.5f) < 1e-3 && Math.abs(dust.get(DustKinds.SILICATE) - 9.8f) < 1e-3,
                    "repair cost: " + dust);
            helper.assertFalse(Bodies.repair(player, ARM), "repaired a new section");
            helper.succeed();
        } finally {
            TestPlayers.remove(player);
        }
    }

    @GameTest(template = "empty", timeoutTicks = 100)
    public static void blueprintSavesBuildsAndQueuesMissingParts(GameTestHelper helper) {
        ServerPlayer player = TestPlayers.survivalInDeepSpace(helper, 73_000.5, 80, 0.5);
        try {
            ItemStack blueprint = new ItemStack(ModItems.BODY_BLUEPRINT.get());
            Blueprints.save(player, blueprint);
            Blueprint design = blueprint.get(ModDataComponents.BLUEPRINT.get());
            helper.assertTrue(design != null && design.sections().size() == 7 && design.modules().size() == 2, "saved design: " + design);

            // Take the arm (and its thruster) off: applying puts both back from the inventory.
            Bodies.removeSection(player, ARM);
            Map<Item, Integer> missing = Blueprints.apply(player, design);
            helper.assertTrue(missing.isEmpty(), "missing after rebuilding from the inventory: " + missing);
            helper.assertTrue(Bodies.view(player).missing().isEmpty(), "arm not back");
            helper.assertTrue(Bodies.data(player).modules().size() == 2, "thruster not back");

            // Lose them: applying reports both missing, and queueing makes Replicator jobs for them.
            Bodies.removeSection(player, ARM);
            player.getInventory().clearContent();
            missing = Blueprints.apply(player, design);
            helper.assertTrue(missing.get(ModItems.ROTOR_ARM.get()) == 1 && missing.get(ModItems.THRUSTER_MK1.get()) == 1, "missing: " + missing);
            Replicator.store(player, Replicator.data(player).withUnlocked(Milestones.AWAKENING));
            player.setData(ModAttachments.DUST, DustData.of(50, 50, 0, 0));
            int queued = Blueprints.queueMissing(player, design);
            helper.assertTrue(queued == 2 && Replicator.data(player).jobs().size() == 2, "queued " + queued);
            helper.succeed();
        } finally {
            TestPlayers.remove(player);
        }
    }
}
