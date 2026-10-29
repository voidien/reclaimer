package com.voidreclaimer.datagen;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.voidreclaimer.VoidReclaimer;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import net.minecraft.data.CachedOutput;
import net.minecraft.data.DataProvider;
import net.minecraft.data.PackOutput;

/**
 * The Create recipes of the nebula chains, written as plain JSON (Create's recipe types are data-driven). Each recipe carries
 * a {@code void_reclaimer:config} condition for its chain's toggle. Chains and machines are as in the design doc; the amounts
 * (fluid in millibuckets, times, ratios) are placeholders because the doc doesn't give them, and are marked as such in
 * {@code PROGRESS.md}.
 */
public final class ModCreateRecipeProvider implements DataProvider {
    private static final String ICE = "iceChain";
    private static final String CARBON = "carbonChain";
    private static final String SILICON = "siliconChain";
    private static final String METALLIC = "metallicChain";
    private static final String SAIL = "solarSail";

    private static final int BUCKET_QUARTER = 250;

    private final PackOutput output;
    private final List<CompletableFuture<?>> pending = new ArrayList<>();
    private CachedOutput cache;

    public ModCreateRecipeProvider(PackOutput output) {
        this.output = output;
    }

    @Override
    public CompletableFuture<?> run(CachedOutput cache) {
        this.cache = cache;
        pending.clear();
        ice();
        carbon();
        silicon();
        metallic();
        solarSail();
        return CompletableFuture.allOf(pending.toArray(CompletableFuture[]::new));
    }

    @Override
    public String getName() {
        return "Void Reclaimer Create recipes";
    }

    // ---- chains -------------------------------------------------------------------------------------------

    private void ice() {
        // Raw Ice Shards -> Crushing Wheels -> Crushed Cryo-Ice
        save("ice/crushing_raw_ice_shards", ICE, processing("create:crushing", 100, null,
                List.of(item("raw_ice_shards")), List.of(result("crushed_cryo_ice"))));
        // Crushed Cryo-Ice -> Heated Basin -> Water
        save("ice/water_from_cryo_ice", ICE, processing("create:mixing", 0, "heated",
                List.of(item("crushed_cryo_ice")), List.of(fluidResult("minecraft:water", BUCKET_QUARTER))));
        // Water -> Superheated Basin (distilling) -> Purified Water
        save("ice/purified_water", ICE, processing("create:mixing", 0, "superheated",
                List.of(fluid("minecraft:water", BUCKET_QUARTER)), List.of(fluidResult("void_reclaimer:purified_water", BUCKET_QUARTER))));
        // Oxygen Gas + Hydrogen Gas -> Superheated Basin (Blaze Burner) -> Nebulite Propellant
        save("ice/nebulite_propellant", ICE, processing("create:mixing", 0, "superheated",
                List.of(fluid("void_reclaimer:oxygen_gas", BUCKET_QUARTER), fluid("void_reclaimer:hydrogen_gas", 2 * BUCKET_QUARTER)),
                List.of(fluidResult("void_reclaimer:nebulite_propellant", BUCKET_QUARTER))));
        // Packed Ice Block -> Mechanical Press -> Coolant Frame Plate (the vanilla Packed Ice block)
        save("ice/coolant_frame_plate", ICE, processing("create:pressing", 0, null,
                List.of(vanillaItem("minecraft:packed_ice")), List.of(result("coolant_frame_plate"))));
        // Purified Water + Crushed Cryo-Ice -> Basin -> Coolant Fluid
        save("ice/coolant_fluid", ICE, processing("create:mixing", 0, null,
                List.of(fluid("void_reclaimer:purified_water", BUCKET_QUARTER), item("crushed_cryo_ice")),
                List.of(fluidResult("void_reclaimer:coolant_fluid", BUCKET_QUARTER))));
    }

    private void carbon() {
        // Carbon Flakes -> Millstone -> Fine Carbon Powder
        save("carbon/milling_carbon_flakes", CARBON, processing("create:milling", 100, null,
                List.of(item("carbon_flakes")), List.of(result("fine_carbon_powder"))));
        // Fine Carbon Powder + Water -> Basin -> Carbon Paste
        save("carbon/carbon_paste", CARBON, processing("create:mixing", 0, null,
                List.of(item("fine_carbon_powder"), fluid("minecraft:water", BUCKET_QUARTER / 2)), List.of(result("carbon_paste"))));
        // Carbon Paste -> Mechanical Press -> Carbon Composite Plate
        save("carbon/carbon_composite_plate", CARBON, processing("create:pressing", 0, null,
                List.of(item("carbon_paste")), List.of(result("carbon_composite_plate"))));
        // Fine Carbon Powder + Liquid Silicon -> Basin -> Silicon-Carbide Slurry
        save("carbon/silicon_carbide_slurry", CARBON, processing("create:mixing", 0, null,
                List.of(item("fine_carbon_powder"), fluid("void_reclaimer:liquid_silicon", BUCKET_QUARTER)), List.of(result("silicon_carbide_slurry"))));
        // Silicon-Carbide Slurry -> Mechanical Press -> Thermal Shielding Tile
        save("carbon/thermal_shielding_tile", CARBON, processing("create:pressing", 0, null,
                List.of(item("silicon_carbide_slurry")), List.of(result("thermal_shielding_tile"))));
        // Fine Carbon Powder -> Mechanical Press (the doc says high pressure; Create has one pressure) -> High-Purity Graphite
        save("carbon/high_purity_graphite", CARBON, processing("create:pressing", 0, null,
                List.of(item("fine_carbon_powder")), List.of(result("high_purity_graphite"))));
    }

    private void silicon() {
        // Silicon Grit -> Encased Fan washing -> Pure Silicon Dust
        save("silicon/pure_silicon_dust", SILICON, processing("create:splashing", 0, null,
                List.of(item("silicon_grit")), List.of(result("pure_silicon_dust"))));
        // Pure Silicon Dust -> Heated Basin -> Liquid Silicon
        save("silicon/liquid_silicon", SILICON, processing("create:mixing", 0, "heated",
                List.of(item("pure_silicon_dust")), List.of(fluidResult("void_reclaimer:liquid_silicon", BUCKET_QUARTER))));
        // Liquid Silicon + Metallic Grain -> Heated Basin -> Doped Silicon Ingot
        save("silicon/doped_silicon_ingot", SILICON, processing("create:mixing", 0, "heated",
                List.of(fluid("void_reclaimer:liquid_silicon", BUCKET_QUARTER), item("metallic_grains")), List.of(result("doped_silicon_ingot"))));
        // Quartz Wafer + Carbon Thread -> Deployer -> High-Efficiency Solar Cell
        save("silicon/high_efficiency_solar_cell", SILICON, processing("create:deploying", 0, null,
                List.of(item("quartz_wafer"), item("carbon_thread")), List.of(result("high_efficiency_solar_cell"))));
        // Doped Silicon Ingot + Electron Tube -> Deployer -> Advanced Sensor Unit (Create's Electron Tube)
        save("silicon/advanced_sensor_unit", SILICON, processing("create:deploying", 0, null,
                List.of(item("doped_silicon_ingot"), vanillaItem("create:electron_tube")), List.of(result("advanced_sensor_unit"))));
    }

    private void metallic() {
        // Metallic Grains -> Encased Fan blasting -> Refined Metal Nodule (the fan uses vanilla blasting recipes)
        JsonObject blasting = new JsonObject();
        blasting.addProperty("type", "minecraft:blasting");
        blasting.addProperty("category", "misc");
        blasting.add("ingredient", item("metallic_grains"));
        blasting.add("result", result("refined_metal_nodule"));
        blasting.addProperty("experience", 0.1);
        blasting.addProperty("cookingtime", 100);
        save("metallic/refined_metal_nodule", METALLIC, blasting);
        // Refined Metal Nodule -> Mechanical Press -> Micro-Alloy Sheet
        save("metallic/micro_alloy_sheet", METALLIC, processing("create:pressing", 0, null,
                List.of(item("refined_metal_nodule")), List.of(result("micro_alloy_sheet"))));
    }

    /** Micro-Alloy Sheet base: Deploy Solar Cell, Deploy Carbon Composite Plate, Spout Water, Press. */
    private void solarSail() {
        JsonObject recipe = new JsonObject();
        recipe.addProperty("type", "create:sequenced_assembly");
        recipe.add("ingredient", item("micro_alloy_sheet"));
        recipe.addProperty("loops", 1);
        JsonArray results = new JsonArray();
        results.add(result("high_output_solar_sail"));
        recipe.add("results", results);
        JsonObject transitional = new JsonObject();
        transitional.addProperty("id", "void_reclaimer:incomplete_high_output_solar_sail");
        recipe.add("transitional_item", transitional);
        JsonArray sequence = new JsonArray();
        sequence.add(step("create:deploying", item("high_efficiency_solar_cell")));
        sequence.add(step("create:deploying", item("carbon_composite_plate")));
        sequence.add(step("create:filling", fluid("minecraft:water", BUCKET_QUARTER)));
        sequence.add(step("create:pressing", null));
        recipe.add("sequence", sequence);
        save("sail/high_output_solar_sail", SAIL, recipe);
    }

    // ---- JSON helpers -------------------------------------------------------------------------------------

    private static JsonObject step(String type, JsonObject added) {
        JsonObject step = new JsonObject();
        step.addProperty("type", type);
        JsonArray ingredients = new JsonArray();
        ingredients.add(item("incomplete_high_output_solar_sail"));
        if (added != null) {
            ingredients.add(added);
        }
        step.add("ingredients", ingredients);
        JsonArray results = new JsonArray();
        results.add(result("incomplete_high_output_solar_sail"));
        step.add("results", results);
        return step;
    }

    private static JsonObject processing(String type, int time, String heat, List<JsonObject> ingredients, List<JsonObject> results) {
        JsonObject recipe = new JsonObject();
        recipe.addProperty("type", type);
        if (heat != null) {
            recipe.addProperty("heat_requirement", heat);
        }
        JsonArray in = new JsonArray();
        ingredients.forEach(in::add);
        recipe.add("ingredients", in);
        if (time > 0) {
            recipe.addProperty("processing_time", time);
        }
        JsonArray out = new JsonArray();
        results.forEach(out::add);
        recipe.add("results", out);
        return recipe;
    }

    private static JsonObject item(String name) {
        return vanillaItem("void_reclaimer:" + name);
    }

    private static JsonObject vanillaItem(String id) {
        JsonObject ingredient = new JsonObject();
        ingredient.addProperty("item", id);
        return ingredient;
    }

    private static JsonObject fluid(String id, int amount) {
        JsonObject ingredient = new JsonObject();
        ingredient.addProperty("type", "neoforge:single");
        ingredient.addProperty("amount", amount);
        ingredient.addProperty("fluid", id);
        return ingredient;
    }

    private static JsonObject result(String name) {
        JsonObject result = new JsonObject();
        result.addProperty("id", "void_reclaimer:" + name);
        return result;
    }

    private static JsonObject fluidResult(String id, int amount) {
        JsonObject result = new JsonObject();
        result.addProperty("amount", amount);
        result.addProperty("id", id);
        return result;
    }

    private void save(String name, String toggle, JsonObject recipe) {
        JsonObject condition = new JsonObject();
        condition.addProperty("type", "void_reclaimer:config");
        condition.addProperty("key", toggle);
        JsonArray conditions = new JsonArray();
        conditions.add(condition);
        JsonObject withConditions = new JsonObject();
        withConditions.add("neoforge:conditions", conditions);
        recipe.entrySet().forEach(entry -> withConditions.add(entry.getKey(), entry.getValue()));
        Path path = output.getOutputFolder(PackOutput.Target.DATA_PACK).resolve(VoidReclaimer.MOD_ID + "/recipe/create/" + name + ".json");
        pending.add(DataProvider.saveStable(cache, (JsonElement) withConditions, path));
    }
}
