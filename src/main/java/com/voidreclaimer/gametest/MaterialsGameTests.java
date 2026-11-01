package com.voidreclaimer.gametest;

import com.voidreclaimer.Config;
import com.voidreclaimer.VoidReclaimer;
import com.voidreclaimer.dust.DustData;
import com.voidreclaimer.dust.DustKinds;
import com.voidreclaimer.machine.ConfigCondition;
import com.voidreclaimer.registry.ModAttachments;
import com.voidreclaimer.registry.ModFluids;
import com.voidreclaimer.registry.ModMaterials;
import com.voidreclaimer.replicator.Milestones;
import com.voidreclaimer.replicator.Replicator;
import com.voidreclaimer.replicator.ReplicatorLogic;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.neoforged.neoforge.common.conditions.ICondition;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/** Phase 7 part 1: the nebula materials, their fluids, the Create recipe chains, the Replicator raw items and the toggles. */
@GameTestHolder(VoidReclaimer.MOD_ID)
@PrefixGameTestTemplate(false)
public final class MaterialsGameTests {
    private MaterialsGameTests() {}

    /** Every Create-side recipe the chains add: path under {@code recipe/}, and the recipe type it must load as. */
    private static final Map<String, String> CHAIN_RECIPES = new LinkedHashMap<>();

    static {
        String mixing = "create:mixing";
        String pressing = "create:pressing";
        CHAIN_RECIPES.put("create/ice/crushing_raw_ice_shards", "create:crushing");
        CHAIN_RECIPES.put("create/ice/water_from_cryo_ice", mixing);
        CHAIN_RECIPES.put("create/ice/purified_water", mixing);
        CHAIN_RECIPES.put("create/ice/nebulite_propellant", mixing);
        CHAIN_RECIPES.put("create/ice/coolant_frame_plate", pressing);
        CHAIN_RECIPES.put("create/ice/coolant_fluid", mixing);
        CHAIN_RECIPES.put("create/carbon/milling_carbon_flakes", "create:milling");
        CHAIN_RECIPES.put("create/carbon/carbon_paste", mixing);
        CHAIN_RECIPES.put("create/carbon/carbon_composite_plate", pressing);
        CHAIN_RECIPES.put("create/carbon/silicon_carbide_slurry", mixing);
        CHAIN_RECIPES.put("create/carbon/thermal_shielding_tile", pressing);
        CHAIN_RECIPES.put("create/carbon/silicon_carbide_coating", "create:compacting");
        CHAIN_RECIPES.put("create/carbon/high_purity_graphite", pressing);
        CHAIN_RECIPES.put("create/silicon/pure_silicon_dust", "create:splashing");
        CHAIN_RECIPES.put("create/silicon/liquid_silicon", mixing);
        CHAIN_RECIPES.put("create/silicon/doped_silicon_ingot", mixing);
        CHAIN_RECIPES.put("create/silicon/high_efficiency_solar_cell", "create:deploying");
        CHAIN_RECIPES.put("create/silicon/advanced_sensor_unit", "create:deploying");
        CHAIN_RECIPES.put("create/metallic/refined_metal_nodule", "minecraft:blasting");
        CHAIN_RECIPES.put("create/metallic/micro_alloy_sheet", pressing);
        CHAIN_RECIPES.put("create/sail/high_output_solar_sail", "create:sequenced_assembly");
        CHAIN_RECIPES.put("create/terraform/bio_nutrient_slurry", mixing);
    }

    @GameTest(template = "empty")
    public static void everyChainRecipeLoadsAsItsCreateType(GameTestHelper helper) {
        var manager = helper.getLevel().getServer().getRecipeManager();
        for (Map.Entry<String, String> expected : CHAIN_RECIPES.entrySet()) {
            ResourceLocation id = VoidReclaimer.id(expected.getKey());
            RecipeHolder<?> holder = manager.byKey(id).orElse(null);
            helper.assertTrue(holder != null, "recipe " + id + " did not load (a bad format is dropped silently)");
            ResourceLocation type = BuiltInRegistries.RECIPE_TYPE.getKey(holder.value().getType());
            helper.assertTrue(type != null && type.toString().equals(expected.getValue()),
                    id + " loaded as " + type + ", expected " + expected.getValue());
        }
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void itemResultsAreTheDocsItems(GameTestHelper helper) {
        var server = helper.getLevel().getServer();
        var access = server.registryAccess();
        Map<String, net.minecraft.world.item.Item> results = new LinkedHashMap<>();
        results.put("create/ice/crushing_raw_ice_shards", ModMaterials.CRUSHED_CRYO_ICE.get());
        results.put("create/ice/coolant_frame_plate", ModMaterials.COOLANT_FRAME_PLATE.get());
        results.put("create/carbon/milling_carbon_flakes", ModMaterials.FINE_CARBON_POWDER.get());
        results.put("create/carbon/carbon_composite_plate", ModMaterials.CARBON_COMPOSITE_PLATE.get());
        results.put("create/carbon/thermal_shielding_tile", ModMaterials.THERMAL_SHIELDING_TILE.get());
        results.put("create/carbon/silicon_carbide_coating", ModMaterials.SILICON_CARBIDE_COATING.get());
        results.put("create/silicon/pure_silicon_dust", ModMaterials.PURE_SILICON_DUST.get());
        results.put("create/silicon/advanced_sensor_unit", ModMaterials.ADVANCED_SENSOR_UNIT.get());
        results.put("create/metallic/refined_metal_nodule", ModMaterials.REFINED_METAL_NODULE.get());
        results.put("create/metallic/micro_alloy_sheet", ModMaterials.MICRO_ALLOY_SHEET.get());
        results.put("create/sail/high_output_solar_sail", ModMaterials.HIGH_OUTPUT_SOLAR_SAIL.get());
        helper.assertTrue(server.getRecipeManager().byKey(VoidReclaimer.id("create/carbon/silicon_carbide_coating")).orElseThrow().value()
                .getResultItem(access).getCount() == 2, "the coating recipe should make 2");
        results.forEach((path, item) -> {
            RecipeHolder<?> holder = server.getRecipeManager().byKey(VoidReclaimer.id(path)).orElseThrow();
            helper.assertTrue(holder.value().getResultItem(access).is(item), path + " makes " + holder.value().getResultItem(access) + ", not " + item);
        });
        helper.succeed();
    }

    /** One Sequenced Assembly recipe as the owner decided it: base item, result, loops and the type of each step. */
    private record Assembly(String path, net.minecraft.world.item.Item base, net.minecraft.world.item.Item result,
                            net.minecraft.world.item.Item transitional, int loops, List<String> steps) {}

    private static final String DEPLOY = "create:deploying";
    private static final String SPOUT = "create:filling";
    private static final String PRESS = "create:pressing";

    @GameTest(template = "empty")
    public static void frameAndMachineryAreTheOwnersSequencedAssemblies(GameTestHelper helper) {
        var server = helper.getLevel().getServer();
        var access = server.registryAccess();
        List<Assembly> expected = List.of(
                new Assembly("create/frame/zero_g_precision_frame", ModMaterials.MICRO_ALLOY_SHEET.get(), ModMaterials.ZERO_G_PRECISION_FRAME.get(),
                        ModMaterials.INCOMPLETE_FRAME.get(), 2, List.of(DEPLOY, DEPLOY, PRESS)),
                new Assembly("create/frame/dyson_swarm_satellite_payload", ModMaterials.ZERO_G_PRECISION_FRAME.get(),
                        ModMaterials.DYSON_SWARM_SATELLITE_PAYLOAD.get(), ModMaterials.INCOMPLETE_SWARM_PAYLOAD.get(), 2, List.of(DEPLOY, DEPLOY, SPOUT, PRESS)),
                new Assembly("create/frame/dyson_shell_truss_module", ModMaterials.ZERO_G_PRECISION_FRAME.get(),
                        ModMaterials.DYSON_SHELL_TRUSS_MODULE.get(), ModMaterials.INCOMPLETE_TRUSS_MODULE.get(), 3, List.of(DEPLOY, DEPLOY, SPOUT, PRESS)),
                new Assembly("create/frame/rcs_thruster", ModMaterials.ZERO_G_PRECISION_FRAME.get(), ModMaterials.RCS_THRUSTER.get(),
                        ModMaterials.INCOMPLETE_RCS_THRUSTER.get(), 1, List.of(DEPLOY, SPOUT, DEPLOY, PRESS)),
                new Assembly("create/frame/reaction_wheel_gyroscope", ModMaterials.ZERO_G_PRECISION_FRAME.get(),
                        ModMaterials.REACTION_WHEEL_GYROSCOPE.get(), ModMaterials.INCOMPLETE_GYROSCOPE.get(), 1, List.of(SPOUT, DEPLOY, DEPLOY, PRESS)),
                new Assembly("create/frame/atmospheric_catalyst_core", ModMaterials.ADVANCED_SENSOR_UNIT.get(), ModMaterials.ATMOSPHERIC_CATALYST_CORE.get(),
                        ModMaterials.INCOMPLETE_CATALYST_CORE.get(), 1, List.of(DEPLOY, SPOUT, PRESS)));
        for (Assembly want : expected) {
            RecipeHolder<?> holder = server.getRecipeManager().byKey(VoidReclaimer.id(want.path())).orElse(null);
            helper.assertTrue(holder != null, want.path() + " did not load");
            helper.assertTrue(holder.value() instanceof com.simibubi.create.content.processing.sequenced.SequencedAssemblyRecipe,
                    want.path() + " is a " + holder.value().getClass().getSimpleName());
            var recipe = (com.simibubi.create.content.processing.sequenced.SequencedAssemblyRecipe) holder.value();
            helper.assertTrue(recipe.getIngredient().test(new net.minecraft.world.item.ItemStack(want.base())), want.path() + " has the wrong base");
            helper.assertTrue(recipe.getResultItem(access).is(want.result()), want.path() + " makes " + recipe.getResultItem(access));
            helper.assertTrue(recipe.getTransitionalItem().is(want.transitional()), want.path() + " transitional " + recipe.getTransitionalItem());
            helper.assertTrue(recipe.getLoops() == want.loops(), want.path() + " loops " + recipe.getLoops() + ", expected " + want.loops());
            List<String> steps = recipe.getSequence().stream()
                    .map(step -> String.valueOf(BuiltInRegistries.RECIPE_TYPE.getKey(step.getRecipe().getType()))).toList();
            helper.assertTrue(steps.equals(want.steps()), want.path() + " steps " + steps + ", expected " + want.steps());
        }
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void fluidsAndBucketsAreRegistered(GameTestHelper helper) {
        helper.assertTrue(ModFluids.ALL.size() == 7, "expected the seven fluids, got " + ModFluids.ALL.size());
        for (ModFluids.Entry fluid : ModFluids.ALL) {
            ResourceLocation id = VoidReclaimer.id(fluid.id());
            helper.assertTrue(BuiltInRegistries.FLUID.containsKey(id), "no source fluid " + id);
            helper.assertTrue(BuiltInRegistries.FLUID.containsKey(VoidReclaimer.id(fluid.id() + "_flowing")), "no flowing fluid " + id);
            helper.assertTrue(fluid.bucket().get().content == fluid.source().get(), id + " bucket holds another fluid");
            helper.assertTrue(fluid.source().get().getFluidType() == fluid.type().get(), id + " fluid has the wrong type");
        }
        helper.assertTrue(ModFluids.OXYGEN_GAS.type().get().getDensity() < 0, "gases should float");
        helper.succeed();
    }

    /** The basin takes Purified Water, Fine Carbon Powder and Ashen Slag, unheated, and gives the slurry. */
    @GameTest(template = "empty")
    public static void theBasinMixesBioNutrientSlurry(GameTestHelper helper) {
        RecipeHolder<?> holder = helper.getLevel().getServer().getRecipeManager()
                .byKey(VoidReclaimer.id("create/terraform/bio_nutrient_slurry")).orElseThrow();
        var recipe = (com.simibubi.create.content.processing.basin.BasinRecipe) holder.value();
        helper.assertTrue(recipe.getRequiredHeat() == com.simibubi.create.content.processing.recipe.HeatCondition.NONE, "heat " + recipe.getRequiredHeat());
        var water = recipe.getFluidIngredients();
        helper.assertTrue(water.size() == 1 && water.get(0).test(new net.neoforged.neoforge.fluids.FluidStack(ModFluids.PURIFIED_WATER.source().get(), 250)),
                "fluid input " + water);
        var items = recipe.getIngredients();
        helper.assertTrue(items.size() == 2, "item inputs " + items.size());
        helper.assertTrue(items.get(0).test(new net.minecraft.world.item.ItemStack(ModMaterials.FINE_CARBON_POWDER.get())), "no carbon powder");
        helper.assertTrue(items.get(1).test(new net.minecraft.world.item.ItemStack(com.voidreclaimer.registry.ModItems.ASHEN_SLAG.get())), "no slag");
        var out = recipe.getFluidResults();
        helper.assertTrue(out.size() == 1 && out.get(0).is(ModFluids.BIO_NUTRIENT_SLURRY.source().get()) && out.get(0).getAmount() == 250, "result " + out);
        helper.succeed();
    }

    /** The recipe file itself carries the toggle: the condition is what keeps it out when the toggle is off. */
    @GameTest(template = "empty")
    public static void theSlurryRecipeIsGatedByItsToggle(GameTestHelper helper) throws java.io.IOException {
        try (var in = VoidReclaimer.class.getResourceAsStream("/data/void_reclaimer/recipe/create/terraform/bio_nutrient_slurry.json")) {
            helper.assertTrue(in != null, "no recipe file");
            var json = com.google.gson.JsonParser.parseString(new String(in.readAllBytes(), java.nio.charset.StandardCharsets.UTF_8)).getAsJsonObject();
            var condition = json.getAsJsonArray("neoforge:conditions").get(0).getAsJsonObject();
            helper.assertTrue("void_reclaimer:config".equals(condition.get("type").getAsString()) && "bioSlurry".equals(condition.get("key").getAsString()),
                    "condition " + condition);
        }
        helper.assertTrue(Config.TOGGLES.containsKey("bioSlurry"), "the recipe names a toggle that does not exist");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void togglesGateTheirRecipes(GameTestHelper helper) {
        helper.assertTrue(Config.TOGGLES.keySet().containsAll(java.util.List.of(
                "replicatorRawItems", "iceChain", "carbonChain", "siliconChain", "metallicChain", "solarSail",
                "frame", "dysonPayloads", "rcsThruster", "reactionWheel", "catalystCore", "bioSlurry")), "toggles: " + Config.TOGGLES.keySet());
        var iceChain = Config.TOGGLES.get("iceChain");
        try {
            ICondition condition = new ConfigCondition("iceChain");
            helper.assertTrue(condition.test(net.neoforged.neoforge.common.conditions.ICondition.IContext.EMPTY), "on by default");
            iceChain.set(false);
            helper.assertFalse(condition.test(net.neoforged.neoforge.common.conditions.ICondition.IContext.EMPTY), "toggle ignored");
            helper.assertFalse(Config.isEnabled("iceChain"), "isEnabled ignores the toggle");
        } finally {
            iceChain.set(true);
        }
        helper.assertTrue(Config.isEnabled("not_a_toggle"), "unknown toggles count as on");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void replicatorMakesRawItemsFromDust(GameTestHelper helper) {
        ServerPlayer player = TestPlayers.survivalInDeepSpace(helper, 85_000.5, 80, 0.5);
        try {
            ResourceLocation recipe = VoidReclaimer.id("replicator/raw_ice_shards");
            helper.assertTrue(helper.getLevel().getServer().getRecipeManager().byKey(recipe).isPresent(), "no raw ice recipe");
            Replicator.store(player, Replicator.data(player).withUnlocked(Milestones.AWAKENING).withUnlocked(Milestones.FIRST_DUST));
            player.setData(ModAttachments.DUST, DustData.of(0, 0, 0, 0));
            helper.assertTrue(Replicator.queue(player, recipe) == ReplicatorLogic.QueueResult.NOT_ENOUGH_DUST, "queued without dust");
            player.setData(ModAttachments.DUST, DustData.of(0, 0, 8, 0));
            helper.assertTrue(Replicator.queue(player, recipe) == ReplicatorLogic.QueueResult.QUEUED, "not queued with 8 ice dust");
            helper.assertTrue(player.getData(ModAttachments.DUST).get(DustKinds.ICE) < 1e-3f, "dust not taken");
            helper.succeed();
        } finally {
            TestPlayers.remove(player);
        }
    }
}
