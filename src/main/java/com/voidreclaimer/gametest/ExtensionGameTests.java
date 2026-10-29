package com.voidreclaimer.gametest;

import com.voidreclaimer.VoidReclaimer;
import net.minecraft.world.entity.ai.attributes.Attributes;
import java.util.Optional;
import java.util.Map;
import com.voidreclaimer.registry.ModItems;
import com.voidreclaimer.registry.ModDataComponents;
import com.voidreclaimer.body.Modules;
import com.voidreclaimer.body.BodyRules;
import com.voidreclaimer.body.BodyData;
import com.voidreclaimer.api.body.SectionStats;
import com.voidreclaimer.api.body.ModuleStats;
import com.voidreclaimer.api.VoidReclaimerRegistries;
import com.voidreclaimer.api.body.Casing;
import com.voidreclaimer.api.milestone.Milestone;
import com.voidreclaimer.api.replicator.ReplicatorCategory;
import com.voidreclaimer.body.Bodies;
import com.voidreclaimer.body.Cosmetics;
import com.voidreclaimer.replicator.ReplicatorCategories;
import com.voidreclaimer.api.body.ModuleKind;
import com.voidreclaimer.dust.DustCloud;
import com.voidreclaimer.dust.DustSensor;
import net.minecraft.world.item.ItemStack;
import com.voidreclaimer.dust.DustData;
import com.voidreclaimer.dust.DustKinds;
import com.voidreclaimer.registry.ModAttachments;
import com.voidreclaimer.replicator.Replicator;
import com.voidreclaimer.replicator.ReplicatorLogic;
import com.voidreclaimer.replicator.Milestones;
import net.minecraft.world.item.Items;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * Extension points, exercised with the {@link GameTestPack} (namespace {@value GameTestPack#NAMESPACE}) the way a
 * datapack or addon uses them.
 */
@GameTestHolder(VoidReclaimer.MOD_ID)
@PrefixGameTestTemplate(false)
public final class ExtensionGameTests {
    private static final ResourceLocation PLASMA = ResourceLocation.fromNamespaceAndPath(GameTestPack.NAMESPACE, "plasma");
    private static final ResourceKey<Milestone> PLASMA_HOARDER = ResourceKey.create(VoidReclaimerRegistries.MILESTONE,
            ResourceLocation.fromNamespaceAndPath(GameTestPack.NAMESPACE, "plasma_hoarder"));
    private static final ResourceLocation PLASMA_GLASS = ResourceLocation.fromNamespaceAndPath(GameTestPack.NAMESPACE, "replicator/plasma_glass");

    private static final ResourceKey<ReplicatorCategory> EXPERIMENTAL = ResourceKey.create(VoidReclaimerRegistries.REPLICATOR_CATEGORY,
            ResourceLocation.fromNamespaceAndPath(GameTestPack.NAMESPACE, "experimental"));
    private static final ResourceKey<Casing> OBSIDIAN = ResourceKey.create(VoidReclaimerRegistries.CASING,
            ResourceLocation.fromNamespaceAndPath(GameTestPack.NAMESPACE, "obsidian"));

    private ExtensionGameTests() {}

    /** A datapack casing that requires a milestone can only be picked once it is reached; unknown ids are refused. */
    @GameTest(template = "empty")
    public static void datapackCasingIsLockedBehindItsMilestone(GameTestHelper helper) {
        ServerPlayer player = TestPlayers.survivalInDeepSpace(helper, 61_000.5, 80, 0.5);
        try {
            Cosmetics start = Bodies.data(player).cosmetics();
            Bodies.setCosmetics(player, start.withCasing(OBSIDIAN));
            helper.assertTrue(Bodies.data(player).cosmetics().equals(start), "locked casing applied");
            Bodies.setCosmetics(player, start.withCasing(ResourceKey.create(VoidReclaimerRegistries.CASING,
                    ResourceLocation.fromNamespaceAndPath(GameTestPack.NAMESPACE, "missing"))));
            helper.assertTrue(Bodies.data(player).cosmetics().equals(start), "unregistered casing applied");

            Replicator.store(player, Replicator.data(player).withUnlocked(PLASMA_HOARDER));
            Bodies.setCosmetics(player, start.withCasing(OBSIDIAN));
            helper.assertTrue(Bodies.data(player).cosmetics().casing().equals(OBSIDIAN), "unlocked casing refused");
            helper.assertTrue(Cosmetics.next(player.registryAccess(), VoidReclaimerRegistries.CASING, Casing::order,
                    Cosmetics.COPPER, Replicator.data(player)::isUnlocked).equals(OBSIDIAN), "cycle skips the datapack casing");
            helper.succeed();
        } finally {
            TestPlayers.remove(player);
        }
    }

    /** A datapack dust kind is registered after the built-in ones, keeps its name, and stays out of clouds at weight 0. */
    @GameTest(template = "empty")
    public static void datapackDustKindIsRegistered(GameTestHelper helper) {
        var kinds = DustKinds.all(helper.getLevel().registryAccess());
        helper.assertTrue(kinds.size() == 5, "expected 4 built-in kinds + plasma, got " + kinds);
        helper.assertTrue(kinds.get(0).id().equals(DustKinds.METALLIC), "built-in order changed: " + kinds);
        helper.assertTrue(kinds.get(4).id().equals(PLASMA), "plasma not last: " + kinds);
        helper.assertTrue(DustKinds.name(helper.getLevel().registryAccess(), PLASMA).getString().equals("Plasma"), "inline name ignored");
        helper.assertTrue(DustKinds.pick(kinds, 0.999).equals(DustKinds.CARBON), "a weight-0 kind took part in cloud generation");
        helper.succeed();
    }

    /** A datapack milestone with a built-in criterion type unlocks a datapack recipe that costs the datapack kind. */
    @GameTest(template = "empty")
    public static void datapackMilestoneUnlocksDatapackRecipe(GameTestHelper helper) {
        ServerPlayer player = TestPlayers.survivalInDeepSpace(helper, 60_000.5, 80, 0.5);
        try {
            Replicator.checkMilestones(player);
            helper.assertFalse(Replicator.data(player).isUnlocked(PLASMA_HOARDER), "unlocked without plasma");
            helper.assertTrue(Replicator.queue(player, PLASMA_GLASS) == ReplicatorLogic.QueueResult.LOCKED, "recipe not locked");

            player.setData(ModAttachments.DUST, DustData.EMPTY.add(PLASMA, 5f, 200f));
            Replicator.checkMilestones(player);
            helper.assertTrue(Replicator.data(player).isUnlocked(PLASMA_HOARDER), "milestone not reached at 5 plasma");
            helper.assertTrue(Replicator.queue(player, PLASMA_GLASS) == ReplicatorLogic.QueueResult.QUEUED, "recipe not queued");
            helper.assertTrue(Math.abs(player.getData(ModAttachments.DUST).get(PLASMA) - 3f) < 1e-4, "plasma not paid");
            helper.assertTrue(Replicator.recipe(player, PLASMA_GLASS).category().equals(EXPERIMENTAL), "recipe category not read");
            helper.assertTrue(ReplicatorCategories.all(player.registryAccess()).getLast().key().equals(EXPERIMENTAL),
                    "datapack tab not after the built-in ones");
            helper.succeed();
        } finally {
            TestPlayers.remove(player);
        }
    }

    /** DustGainEvent can replace the amount or cancel the gain. */
    @GameTest(template = "empty")
    public static void dustGainEventChangesOrCancelsGain(GameTestHelper helper) {
        DustCloud cloud = DustGameTests.findCloud(DustGameTests.field(helper));
        ServerPlayer fixed = TestPlayers.survivalInDeepSpace(helper, cloud.center().x, cloud.center().y, cloud.center().z);
        ServerPlayer cancelled = TestPlayers.survivalInDeepSpace(helper, cloud.center().x, cloud.center().y, cloud.center().z);
        try {
            fixed.addTag(TestEventListeners.FIXED_DUST);
            cancelled.addTag(TestEventListeners.NO_DUST);
            for (int i = 0; i < 10; i++) {
                fixed.doTick();
                cancelled.doTick();
            }
            float got = fixed.getData(ModAttachments.DUST).get(cloud.kind());
            helper.assertTrue(Math.abs(got - 10f) < 1e-3, "expected exactly 10 dust, got " + got);
            helper.assertTrue(cancelled.getData(ModAttachments.DUST).total() == 0f, "cancelled gain still added dust");
            helper.succeed();
        } finally {
            TestPlayers.remove(fixed);
            TestPlayers.remove(cancelled);
        }
    }

    /** MilestoneReachedEvent fires once per milestone reached in play. */
    @GameTest(template = "empty")
    public static void milestoneReachedEventFiresOnce(GameTestHelper helper) {
        ServerPlayer player = TestPlayers.survivalInDeepSpace(helper, 62_000.5, 80, 0.5);
        try {
            Replicator.checkMilestones(player);
            Replicator.checkMilestones(player);
            var reached = TestEventListeners.REACHED.getOrDefault(player.getUUID(), java.util.List.of());
            helper.assertTrue(reached.equals(java.util.List.of(Milestones.AWAKENING)), "events: " + reached);
            helper.succeed();
        } finally {
            TestPlayers.remove(player);
        }
    }

    /** ReplicatorJobFinishedEvent can add outputs. */
    @GameTest(template = "empty", timeoutTicks = 200)
    public static void jobFinishedEventAddsOutputs(GameTestHelper helper) {
        ServerPlayer player = TestPlayers.survivalInDeepSpace(helper, 63_000.5, 80, 0.5);
        player.addTag(TestEventListeners.BONUS_OUTPUT);
        Replicator.store(player, Replicator.data(player).withUnlocked(PLASMA_HOARDER));
        player.setData(ModAttachments.DUST, DustData.EMPTY.add(PLASMA, 10f, 200f));
        helper.assertTrue(Replicator.queue(player, PLASMA_GLASS) == ReplicatorLogic.QueueResult.QUEUED, "not queued");
        helper.onEachTick(player::doTick);
        helper.runAfterDelay(60, () -> {
            try {
                helper.assertTrue(player.getInventory().countItem(Items.GLASS_PANE) == 4, "recipe output missing");
                helper.assertTrue(player.getInventory().countItem(Items.DIAMOND) == 1, "event output missing");
                helper.succeed();
            } finally {
                TestPlayers.remove(player);
            }
        });
    }

    /** Any item in #void_reclaimer:modules/dust_sensor works as a sensor module (the test pack tags the spyglass). */
    @GameTest(template = "empty")
    public static void taggedItemWorksAsDustSensor(GameTestHelper helper) {
        ServerPlayer player = TestPlayers.survivalInDeepSpace(helper, 64_000.5, 80, 0.5);
        try {
            helper.assertFalse(DustSensor.has(player), "sensor without a module");
            TestPlayers.installModule(player, ModuleKind.SENSOR, 0, new ItemStack(Items.STICK));
            helper.assertFalse(DustSensor.has(player), "untagged item counted as a sensor");
            TestPlayers.installModule(player, ModuleKind.SENSOR, 0, new ItemStack(Items.SPYGLASS));
            helper.assertTrue(DustSensor.has(player), "tagged item not counted as a sensor");
            helper.succeed();
        } finally {
            TestPlayers.remove(player);
        }
    }

    /** A datapack chassis (with a datapack size class) takes over when its core section is installed. */
    @GameTest(template = "empty")
    public static void datapackChassisSetsTheLayout(GameTestHelper helper) {
        ServerPlayer player = TestPlayers.survivalInDeepSpace(helper, 65_000.5, 80, 0.5);
        try {
            ItemStack core = new ItemStack(Items.IRON_BLOCK);
            core.set(ModDataComponents.SECTION.get(), new SectionStats(ResourceLocation.fromNamespaceAndPath(GameTestPack.NAMESPACE, "tripod_body"),
                    1, 0.3f, 2f, 6f, 0f, 1f, 1, 0f, 0f, 0f, Map.of(ModuleKind.CORE, 1),
                    Optional.of(ResourceKey.create(VoidReclaimerRegistries.CHASSIS, ResourceLocation.fromNamespaceAndPath(GameTestPack.NAMESPACE, "tripod")))));
            player.getInventory().items.set(0, core);
            helper.assertTrue(Bodies.installSection(player, "hull", 0) == BodyRules.Check.OK, "tripod core refused");
            helper.assertTrue(Bodies.data(player).coreSocket().equals("body"), "layout not switched");
            helper.assertTrue(Math.abs(player.getAttributeValue(Attributes.SCALE) - 0.25) < 1e-6, "datapack size class not applied");
            helper.assertTrue(Bodies.view(player).missing().size() == 3, "tripod legs");
            int arm = player.getInventory().findSlotMatchingItem(new ItemStack(ModItems.ROTOR_ARM.get()));
            helper.assertTrue(Bodies.installSection(player, "leg_1", arm) == BodyRules.Check.OK, "rotor arm refused as a leg");
            int claw = player.getInventory().findSlotMatchingItem(new ItemStack(ModItems.TOOL_CLAW.get()));
            helper.assertTrue(Bodies.installSection(player, "leg_2", claw) == BodyRules.Check.WRONG_SOCKET, "claw accepted as a leg");
            helper.succeed();
        } finally {
            TestPlayers.remove(player);
        }
    }

    /** An addon tool module type runs on use; ModuleUseEvent can stop it. */
    @GameTest(template = "empty")
    public static void addonToolModuleTypeAndModuleUseEvent(GameTestHelper helper) {
        ServerPlayer player = TestPlayers.survivalInDeepSpace(helper, 66_000.5, 80, 0.5);
        try {
            ItemStack marker = new ItemStack(Items.STICK);
            marker.set(ModDataComponents.MODULE.get(), ModuleStats.simple(ModuleKind.TOOL, 1, 0f));
            marker.set(ModDataComponents.TOOL_MODULE.get(), GameTestPack.MARKER);
            TestPlayers.installModule(player, ModuleKind.TOOL, 0, marker);
            Modules.use(player);
            helper.assertTrue(GameTestPack.MARKER_USES.getOrDefault(player.getUUID(), 0) == 1, "addon tool type not used");
            player.addTag(TestEventListeners.NO_MODULES);
            Modules.use(player);
            helper.assertTrue(GameTestPack.MARKER_USES.getOrDefault(player.getUUID(), 0) == 1, "cancelled use still ran");
            helper.succeed();
        } finally {
            TestPlayers.remove(player);
        }
    }

    /** BodyStatsEvent changes computed stats; BodyChangedEvent fires once per change. */
    @GameTest(template = "empty")
    public static void bodyStatsAndChangedEvents(GameTestHelper helper) {
        ServerPlayer player = TestPlayers.survivalInDeepSpace(helper, 67_000.5, 80, 0.5);
        try {
            player.addTag(TestEventListeners.TOUGH);
            int before = TestEventListeners.BODY_CHANGES.getOrDefault(player.getUUID(), 0);
            player.getInventory().items.set(3, new ItemStack(ModItems.STORAGE_MK1.get()));
            helper.assertTrue(Bodies.installModule(player, new BodyData.Mount("hull", ModuleKind.STORAGE, 0), 3) == BodyRules.Check.OK,
                    "install failed");
            helper.assertTrue(TestEventListeners.BODY_CHANGES.getOrDefault(player.getUUID(), 0) == before + 1, "BodyChangedEvent count");
            helper.assertTrue(player.getMaxHealth() == 22f, "BodyStatsEvent ignored: " + player.getMaxHealth());
            helper.succeed();
        } finally {
            TestPlayers.remove(player);
        }
    }
}
