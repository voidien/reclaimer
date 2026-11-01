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
    private static final String FRAME = "frame";
    private static final String PAYLOADS = "dysonPayloads";
    private static final String RCS = "rcsThruster";
    private static final String GYRO = "reactionWheel";
    private static final String CATALYST = "catalystCore";
    private static final String RECEIVER = "quantumReceiver";
    private static final String SLURRY = "bioSlurry";

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
        air();
        solarSail();
        frameAndMachinery();
        quantumReceiver();
        terraforming();
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
        // Silicon-Carbide Slurry + Carbon Composite Plate -> Basin, then Press (Create's basin compacting) -> 2 Silicon-Carbide Coating
        JsonObject coating = processing("create:compacting", 0, null,
                List.of(item("silicon_carbide_slurry"), item("carbon_composite_plate")), List.of(result("silicon_carbide_coating")));
        coating.getAsJsonArray("results").get(0).getAsJsonObject().addProperty("count", 2);
        save("carbon/silicon_carbide_coating", CARBON, coating);
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
        assembly("sail/high_output_solar_sail", SAIL, item("micro_alloy_sheet"), "high_output_solar_sail", 1,
                deploy(item("high_efficiency_solar_cell")), deploy(item("carbon_composite_plate")),
                spout("minecraft:water"), press());
    }

    /**
     * The Zero-G Precision Frame and what is built on it. All the owner's decisions (2026-10-29); spout amounts are placeholders.
     * Create has one press pressure, so the truss module's "max pressure" is an ordinary press.
     */
    private void frameAndMachinery() {
        // Micro-Alloy Sheet base: Deploy High-Purity Graphite (bearings), Deploy Micro-Alloy Sheet, Press; twice round.
        assembly("frame/zero_g_precision_frame", FRAME, item("micro_alloy_sheet"), "zero_g_precision_frame", 2,
                deploy(item("high_purity_graphite")), deploy(item("micro_alloy_sheet")), press());
        // Megastructure payloads, from a Frame.
        assembly("frame/dyson_swarm_satellite_payload", PAYLOADS, item("zero_g_precision_frame"), "dyson_swarm_satellite_payload", 2,
                deploy(item("carbon_composite_plate")), deploy(item("high_efficiency_solar_cell")),
                spout("void_reclaimer:liquid_silicon"), press());
        assembly("frame/dyson_shell_truss_module", PAYLOADS, item("zero_g_precision_frame"), "dyson_shell_truss_module", 3,
                deploy(item("thermal_shielding_tile")), deploy(item("micro_alloy_sheet")),
                spout("void_reclaimer:coolant_fluid"), press());
        // Machinery, from a Frame (Precision Mechanism is Create's).
        assembly("frame/rcs_thruster", RCS, item("zero_g_precision_frame"), "rcs_thruster", 1,
                deploy(item("carbon_composite_plate")), spout("void_reclaimer:nebulite_propellant"),
                deploy(vanillaItem("create:precision_mechanism")), press());
        assembly("frame/reaction_wheel_gyroscope", GYRO, item("zero_g_precision_frame"), "reaction_wheel_gyroscope", 1,
                spout("void_reclaimer:liquid_silicon"), deploy(item("high_purity_graphite")),
                deploy(vanillaItem("create:precision_mechanism")), press());
        // Atmospheric Catalyst Core: Advanced Sensor Unit base, Deploy Quartz Wafer, Spout Nebulite Propellant, Press.
        assembly("frame/atmospheric_catalyst_core", CATALYST, item("advanced_sensor_unit"), "atmospheric_catalyst_core", 1,
                deploy(item("quartz_wafer")), spout("void_reclaimer:nebulite_propellant"), press());
    }

    /** Sub-Light Quantum Receiver: Micro-Alloy Sheet base, Deploy Solar Cell, Deploy Composite Plate, Deploy Precision Mechanism, Spout Coolant. */
    private void quantumReceiver() {
        assembly("receiver/quantum_receiver", RECEIVER, item("micro_alloy_sheet"), "quantum_receiver", 1,
                deploy(item("high_efficiency_solar_cell")), deploy(item("carbon_composite_plate")),
                deploy(vanillaItem("create:precision_mechanism")), spout("void_reclaimer:coolant_fluid"));
    }

    /** Purified Water + Fine Carbon Powder + Ashen Slag (ruined Earth's own minerals) -> Basin -> Bio-Nutrient Slurry. */
    private void terraforming() {
        save("terraform/bio_nutrient_slurry", SLURRY, processing("create:mixing", 0, null,
                List.of(fluid("void_reclaimer:purified_water", BUCKET_QUARTER), item("fine_carbon_powder"), item("ashen_slag")),
                List.of(fluidResult("void_reclaimer:bio_nutrient_slurry", BUCKET_QUARTER))));
    }

    // ---- Sequenced Assembly -------------------------------------------------------------------------------

    /** One step of a Sequenced Assembly recipe: what is done to the transitional item, and with what. */
    private record Step(String type, JsonObject added) {}

    private static Step deploy(JsonObject ingredient) {
        return new Step("create:deploying", ingredient);
    }

    private static Step spout(String fluid) {
        return new Step("create:filling", fluid(fluid, BUCKET_QUARTER));
    }

    private static Step press() {
        return new Step("create:pressing", null);
    }

    /**
     * A Sequenced Assembly recipe: {@code base} goes through {@code steps}, {@code loops} times, and ends as {@code result}. The
     * transitional item is {@code incomplete_<result>}.
     */
    private void assembly(String path, String toggle, JsonObject base, String result, int loops, Step... steps) {
        String transitional = "incomplete_" + result;
        JsonObject recipe = new JsonObject();
        recipe.addProperty("type", "create:sequenced_assembly");
        recipe.add("ingredient", base);
        recipe.addProperty("loops", loops);
        JsonArray results = new JsonArray();
        results.add(result(result));
        recipe.add("results", results);
        JsonObject transitionalItem = new JsonObject();
        transitionalItem.addProperty("id", "void_reclaimer:" + transitional);
        recipe.add("transitional_item", transitionalItem);
        JsonArray sequence = new JsonArray();
        for (Step step : steps) {
            JsonObject json = new JsonObject();
            json.addProperty("type", step.type());
            JsonArray ingredients = new JsonArray();
            ingredients.add(item(transitional));
            if (step.added() != null) {
                ingredients.add(step.added());
            }
            json.add("ingredients", ingredients);
            JsonArray out = new JsonArray();
            out.add(result(transitional));
            json.add("results", out);
            sequence.add(json);
        }
        recipe.add("sequence", sequence);
        save(path, toggle, recipe);
    }

    // ---- JSON helpers -------------------------------------------------------------------------------------

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

    /**
     * A few Create routes to Aeronautics parts from our nebula materials (the owner: "not too many"); the Replicator
     * makes everything, these are the cheaper machine routes. Each sits under the toggle of the chain it uses.
     */
    private void air() {
        // Carbon Composite Plate -> Mechanical Press -> Andesite Propeller
        save("air/andesite_propeller", CARBON, processing("create:pressing", 0, null,
                List.of(item("carbon_composite_plate")), List.of(resultOf("aeronautics:andesite_propeller"))));
        // Micro-Alloy Sheet -> Mechanical Press -> Steam Vent
        save("air/steam_vent", METALLIC, processing("create:pressing", 0, null,
                List.of(item("micro_alloy_sheet")), List.of(resultOf("aeronautics:steam_vent"))));
        // Pure Silicon Dust + Fine Carbon Powder -> Heated Basin -> Levitite Blend (a fluid; Aeronautics crystallises it)
        save("air/levitite_blend", SILICON, processing("create:mixing", 0, "heated",
                List.of(item("pure_silicon_dust"), item("fine_carbon_powder")), List.of(fluidResult("aeronautics:levitite_blend", BUCKET_QUARTER))));
    }

    private static JsonObject resultOf(String id) {
        JsonObject result = new JsonObject();
        result.addProperty("id", id);
        return result;
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
