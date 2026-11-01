package com.voidreclaimer.datagen;

import com.voidreclaimer.VoidReclaimer;
import com.voidreclaimer.registry.ModItems;
import com.voidreclaimer.replicator.DustCost;
import com.voidreclaimer.api.milestone.Milestone;
import com.voidreclaimer.api.replicator.ReplicatorCategory;
import com.voidreclaimer.machine.ConfigCondition;
import com.voidreclaimer.registry.ModMaterials;
import com.voidreclaimer.replicator.ReplicatorCategories;
import com.voidreclaimer.replicator.Milestones;
import net.minecraft.resources.ResourceKey;
import com.voidreclaimer.replicator.ReplicatorRecipe;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import net.minecraft.network.chat.Component;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.data.recipes.RecipeOutput;
import net.minecraft.data.recipes.RecipeProvider;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ItemLike;

/** Replicator recipes. Costs are metallic / silicate / ice / carbon dust; times are in seconds. */
public final class ModRecipeProvider extends RecipeProvider {
    public ModRecipeProvider(PackOutput output, CompletableFuture<HolderLookup.Provider> registries) {
        super(output, registries);
    }

    @Override
    protected void buildRecipes(RecipeOutput output) {
        aeronautics(output);
        replicator(output, "dust_sensor", ModItems.DUST_SENSOR.get(), 1, DustCost.of(10, 15, 0, 5), 20,
                ReplicatorCategories.MODULES, Milestones.AWAKENING);
        replicator(output, "cloud_collector", ModItems.CLOUD_COLLECTOR.get(), 1, DustCost.of(30, 25, 15, 20), 60,
                ReplicatorCategories.MODULES, Milestones.DIVERSIFIED);
        replicator(output, "portal_frame", ModItems.PORTAL_FRAME.get(), 8, DustCost.of(40, 32, 24, 16), 20,
                ReplicatorCategories.BLOCKS, Milestones.STARSHIP);
        replicator(output, "terraforming_spire", ModItems.TERRAFORMING_SPIRE.get(), 1, DustCost.of(60, 50, 40, 50), 60,
                ReplicatorCategories.BLOCKS, Milestones.STARSHIP);
        replicator(output, "chemical_thruster", ModItems.CHEMICAL_THRUSTER.get(), 1, DustCost.of(60, 40, 30, 40), 40,
                ReplicatorCategories.BLOCKS, Milestones.STARSHIP);
        replicator(output, "ion_thruster", ModItems.ION_THRUSTER.get(), 1, DustCost.of(50, 50, 40, 30), 40,
                ReplicatorCategories.BLOCKS, Milestones.STARSHIP);
        replicator(output, "helm", ModItems.HELM.get(), 1, DustCost.of(40, 30, 20, 50), 60,
                ReplicatorCategories.BLOCKS, Milestones.STARSHIP);
        replicator(output, "station_core", ModItems.STATION_CORE.get(), 1, DustCost.of(80, 60, 40, 100), 120,
                ReplicatorCategories.BLOCKS, Milestones.STARSHIP);
        replicator(output, "docking_clamp", ModItems.DOCKING_CLAMP.get(), 1, DustCost.of(40, 30, 10, 60), 60,
                ReplicatorCategories.BLOCKS, Milestones.STARSHIP);
        machineRecipe(output, "holo_projector", ModItems.HOLO_PROJECTOR.get(), DustCost.of(30, 40, 10, 20), "holoProjector");
        replicator(output, "portal_igniter", ModItems.PORTAL_IGNITER.get(), 1, DustCost.of(12, 10, 8, 12), 15,
                ReplicatorCategories.TOOLS, Milestones.STARSHIP);
        replicator(output, "planetary_grid", ModItems.PLANETARY_GRID.get(), 1, DustCost.of(10, 15, 5, 10), 10,
                ReplicatorCategories.TOOLS, Milestones.EARTHFALL);
        replicator(output, "scaffold", ModItems.SCAFFOLD.get(), 8, DustCost.of(0, 8, 0, 0), 5,
                ReplicatorCategories.BLOCKS, Milestones.AWAKENING);
        replicator(output, "salvage_tool", ModItems.SALVAGE_TOOL.get(), 1, DustCost.of(15, 5, 0, 0), 30,
                ReplicatorCategories.TOOLS, Milestones.FIRST_DUST);
        replicator(output, "nanite_swarm", ModItems.NANITE_SWARM.get(), 1, DustCost.of(20, 10, 0, 10), 40,
                ReplicatorCategories.TOOLS, Milestones.FIRST_DUST);
        replicator(output, "mechanical_drill", ModItems.MECHANICAL_DRILL.get(), 1, DustCost.of(25, 15, 0, 10), 40,
                ReplicatorCategories.TOOLS, Milestones.FIRST_DUST);
        replicator(output, "matter_harvester", ModItems.MATTER_HARVESTER.get(), 1, DustCost.of(40, 25, 10, 20), 60,
                ReplicatorCategories.TOOLS, Milestones.FIRST_DUST);
        replicator(output, "tether_anchor", ModItems.TETHER_ANCHOR.get(), 1, DustCost.of(15, 5, 0, 0), 20,
                ReplicatorCategories.BLOCKS, Milestones.FIRST_STRUCTURE);
        replicator(output, "backup_core", ModItems.BACKUP_CORE.get(), 1, DustCost.of(30, 20, 10, 10), 60,
                ReplicatorCategories.BLOCKS, Milestones.DIVERSIFIED);

        // Raw nebula items from dust. Amounts and times are placeholders; the design doc doesn't price them.
        ResourceKey<ReplicatorCategory> materials = ReplicatorCategories.MATERIALS;
        rawItems(output, "raw_ice_shards", ModMaterials.RAW_ICE_SHARDS.get(), DustCost.of(0, 0, 8, 0), materials);
        rawItems(output, "carbon_flakes", ModMaterials.CARBON_FLAKES.get(), DustCost.of(0, 0, 0, 8), materials);
        rawItems(output, "silicon_grit", ModMaterials.SILICON_GRIT.get(), DustCost.of(0, 8, 0, 0), materials);
        rawItems(output, "metallic_grains", ModMaterials.METALLIC_GRAINS.get(), DustCost.of(8, 0, 0, 0), materials);

        // Machines. Prices are placeholders; the design doc gives no recipe for them.
        machineRecipe(output, "electrolyzer", ModItems.ELECTROLYZER.get(), DustCost.of(30, 20, 10, 10), "electrolyzer");
        machineRecipe(output, "cloud_siphon", ModItems.CLOUD_SIPHON.get(), DustCost.of(35, 25, 20, 25), "cloudSiphon");
        machineRecipe(output, "spinner", ModItems.SPINNER.get(), DustCost.of(25, 15, 0, 15), "spinner");
        // Helper Drone (ferry); placeholder price.
        replicator(output, "helper_drone", ModItems.HELPER_DRONE.get(), 1, DustCost.of(60, 40, 10, 40), 90, ReplicatorCategories.TOOLS, Milestones.EXPANSION);
        // The Launch Complex's controller; placeholder price. The old three Mass Driver blocks have no recipe any more.
        machineRecipe(output, "launch_controller", ModItems.LAUNCH_CONTROLLER.get(), DustCost.of(90, 65, 20, 70), "massDriver");
        // Encased Fan freezing: a Water Bucket carried through powder snow becomes Packed Ice (the bucket comes back empty).
        output.accept(VoidReclaimer.id("freezing/water_bucket"), new com.voidreclaimer.machine.FreezingRecipe(
                net.minecraft.world.item.crafting.Ingredient.of(net.minecraft.world.item.Items.WATER_BUCKET),
                List.of(new ItemStack(net.minecraft.world.item.Items.PACKED_ICE), new ItemStack(net.minecraft.world.item.Items.BUCKET))),
                null, new ConfigCondition("fanFreezing"));

        // Modules. The Mk I core and thruster are also craftable as spares.
        ResourceKey<ReplicatorCategory> parts = ReplicatorCategories.PARTS;
        replicator(output, "thruster_mk1", ModItems.THRUSTER_MK1.get(), 1, DustCost.of(10, 5, 0, 0), 30, parts, Milestones.AWAKENING);
        replicator(output, "core_mk1", ModItems.CORE_MK1.get(), 1, DustCost.of(15, 10, 0, 5), 45, parts, Milestones.FIRST_DUST);
        replicator(output, "storage_mk1", ModItems.STORAGE_MK1.get(), 1, DustCost.of(8, 8, 0, 0), 30, parts, Milestones.FIRST_DUST);
        replicator(output, "brass_trim", ModItems.BRASS_TRIM.get(), 1, DustCost.of(5, 0, 0, 0), 10, parts, Milestones.FIRST_STRUCTURE);
        replicator(output, "cogwheel_crown", ModItems.COGWHEEL_CROWN.get(), 1, DustCost.of(8, 0, 0, 4), 15, parts, Milestones.DIVERSIFIED);
        replicator(output, "core_mk2", ModItems.CORE_MK2.get(), 1, DustCost.of(40, 30, 10, 20), 90, parts, Milestones.EXPANSION);
        replicator(output, "thruster_mk2", ModItems.THRUSTER_MK2.get(), 1, DustCost.of(30, 10, 0, 10), 60, parts, Milestones.EXPANSION);
        replicator(output, "storage_mk2", ModItems.STORAGE_MK2.get(), 1, DustCost.of(20, 20, 10, 0), 60, parts, Milestones.EXPANSION);

        // Drone sections: the hull costs what the Drone frame did; limbs are cheap spares.
        replicator(output, "drone_hull", ModItems.DRONE_HULL.get(), 1, DustCost.of(20, 10, 0, 0), 60, parts, Milestones.FIRST_DUST);
        replicator(output, "rotor_arm", ModItems.ROTOR_ARM.get(), 1, DustCost.of(5, 2, 0, 0), 15, parts, Milestones.AWAKENING);
        replicator(output, "sensor_dome", ModItems.SENSOR_DOME.get(), 1, DustCost.of(4, 4, 0, 0), 15, parts, Milestones.FIRST_DUST);
        replicator(output, "tool_claw", ModItems.TOOL_CLAW.get(), 1, DustCost.of(5, 3, 0, 0), 15, parts, Milestones.FIRST_DUST);
        replicator(output, "body_blueprint", ModItems.BODY_BLUEPRINT.get(), 1, DustCost.of(2, 3, 0, 0), 10, ReplicatorCategories.TOOLS,
                Milestones.FIRST_DUST);
        replicator(output, "rotor_arm_mk2", ModItems.ROTOR_ARM_MK2.get(), 1, DustCost.of(15, 5, 5, 5), 40, parts, Milestones.EXPANSION);

        // Mech sections: a whole Mech costs about what the Mech frame did.
        replicator(output, "mech_torso", ModItems.MECH_TORSO.get(), 1, DustCost.of(40, 30, 10, 20), 120, parts, Milestones.EXPANSION);
        replicator(output, "mech_head", ModItems.MECH_HEAD.get(), 1, DustCost.of(8, 6, 2, 4), 30, parts, Milestones.EXPANSION);
        replicator(output, "mech_pelvis", ModItems.MECH_PELVIS.get(), 1, DustCost.of(8, 6, 2, 4), 30, parts, Milestones.EXPANSION);
        replicator(output, "mech_upper_arm", ModItems.MECH_UPPER_ARM.get(), 1, DustCost.of(4, 3, 1, 2), 20, parts, Milestones.EXPANSION);
        replicator(output, "mech_forearm", ModItems.MECH_FOREARM.get(), 1, DustCost.of(4, 3, 1, 2), 20, parts, Milestones.EXPANSION);
        replicator(output, "mech_hand", ModItems.MECH_HAND.get(), 1, DustCost.of(3, 2, 1, 1), 15, parts, Milestones.EXPANSION);
        replicator(output, "mech_thigh", ModItems.MECH_THIGH.get(), 1, DustCost.of(4, 3, 1, 2), 20, parts, Milestones.EXPANSION);
        replicator(output, "mech_shin", ModItems.MECH_SHIN.get(), 1, DustCost.of(4, 3, 1, 2), 20, parts, Milestones.EXPANSION);
        replicator(output, "mech_foot", ModItems.MECH_FOOT.get(), 1, DustCost.of(3, 2, 1, 1), 15, parts, Milestones.EXPANSION);
        replicator(output, "mech_forearm_mk2", ModItems.MECH_FOREARM_MK2.get(), 1, DustCost.of(12, 8, 4, 6), 60, parts, Milestones.EXPANSION);
        replicator(output, "mech_shin_mk2", ModItems.MECH_SHIN_MK2.get(), 1, DustCost.of(12, 8, 4, 6), 60, parts, Milestones.EXPANSION);

        // Kits: several sections in one job, a little cheaper than crafting them singly. Sections stack to 1.
        kit(output, "drone_kit", List.of(new ItemStack(ModItems.DRONE_HULL.get()), new ItemStack(ModItems.SENSOR_DOME.get()),
                new ItemStack(ModItems.TOOL_CLAW.get()), new ItemStack(ModItems.ROTOR_ARM.get()),
                new ItemStack(ModItems.ROTOR_ARM.get()), new ItemStack(ModItems.ROTOR_ARM.get()), new ItemStack(ModItems.ROTOR_ARM.get())),
                DustCost.of(40, 20, 0, 0), 80, parts, Milestones.FIRST_DUST);
        kit(output, "mech_core_kit", List.of(new ItemStack(ModItems.MECH_TORSO.get()), new ItemStack(ModItems.MECH_PELVIS.get()),
                new ItemStack(ModItems.MECH_HEAD.get())), DustCost.of(48, 36, 12, 24), 150, parts, Milestones.EXPANSION);

        // Ship sections (tier 3), unlocked by the Starship milestone. Placeholder prices: a whole Ship costs several times a Mech.
        ResourceKey<Milestone> starship = Milestones.STARSHIP;
        replicator(output, "ship_keel", ModItems.SHIP_KEEL.get(), 1, DustCost.of(120, 90, 60, 80), 240, parts, starship);
        replicator(output, "ship_bow", ModItems.SHIP_BOW.get(), 1, DustCost.of(24, 18, 12, 16), 60, parts, starship);
        replicator(output, "ship_bridge", ModItems.SHIP_BRIDGE.get(), 1, DustCost.of(30, 22, 14, 20), 70, parts, starship);
        replicator(output, "ship_hull_segment", ModItems.SHIP_HULL_SEGMENT.get(), 1, DustCost.of(20, 16, 10, 14), 50, parts, starship);
        replicator(output, "ship_hull_segment_mk2", ModItems.SHIP_HULL_SEGMENT_MK2.get(), 1, DustCost.of(50, 40, 25, 35), 120, parts, starship);
        replicator(output, "ship_wing", ModItems.SHIP_WING.get(), 1, DustCost.of(18, 14, 8, 12), 45, parts, starship);
        replicator(output, "ship_engine_nacelle", ModItems.SHIP_ENGINE_NACELLE.get(), 1, DustCost.of(30, 20, 10, 24), 70, parts, starship);
        replicator(output, "ship_engine_nacelle_mk2", ModItems.SHIP_ENGINE_NACELLE_MK2.get(), 1, DustCost.of(70, 50, 30, 60), 140, parts, starship);
        replicator(output, "ship_hardpoint", ModItems.SHIP_HARDPOINT.get(), 1, DustCost.of(12, 8, 6, 10), 40, parts, starship);
        replicator(output, "ship_cargo_bay", ModItems.SHIP_CARGO_BAY.get(), 1, DustCost.of(24, 20, 10, 14), 60, parts, starship);
        replicator(output, "ship_sensor_mast", ModItems.SHIP_SENSOR_MAST.get(), 1, DustCost.of(14, 10, 8, 12), 40, parts, starship);
        replicator(output, "ship_rcs_cluster", ModItems.SHIP_RCS_CLUSTER.get(), 1, DustCost.of(14, 10, 8, 12), 40, parts, starship);
        replicator(output, "ship_dorsal_spine", ModItems.SHIP_DORSAL_SPINE.get(), 1, DustCost.of(28, 22, 12, 18), 60, parts, starship);
        replicator(output, "ship_ventral_fin", ModItems.SHIP_VENTRAL_FIN.get(), 1, DustCost.of(16, 12, 8, 10), 45, parts, starship);
        // Core Mk III opens the Ship's tier 3 sections, Mk IV the Titan's tier 4.
        replicator(output, "core_mk3", ModItems.CORE_MK3.get(), 1, DustCost.of(120, 90, 50, 70), 180, parts, starship);
        replicator(output, "core_mk4", ModItems.CORE_MK4.get(), 1, DustCost.of(400, 300, 200, 250), 360, parts, TITAN_UNLOCK);

        // Shuttle sections (a second Ship layout), a little cheaper than the Ship's. Placeholder prices.
        replicator(output, "shuttle_hull", ModItems.SHUTTLE_HULL.get(), 1, DustCost.of(100, 75, 50, 65), 240, parts, starship);
        replicator(output, "shuttle_nose", ModItems.SHUTTLE_NOSE.get(), 1, DustCost.of(20, 15, 10, 14), 60, parts, starship);
        replicator(output, "shuttle_windshield", ModItems.SHUTTLE_WINDSHIELD.get(), 1, DustCost.of(24, 18, 12, 16), 60, parts, starship);
        replicator(output, "shuttle_dome", ModItems.SHUTTLE_DOME.get(), 1, DustCost.of(20, 15, 10, 14), 50, parts, starship);
        replicator(output, "shuttle_ridge", ModItems.SHUTTLE_RIDGE.get(), 1, DustCost.of(16, 12, 8, 11), 45, parts, starship);
        replicator(output, "shuttle_aft", ModItems.SHUTTLE_AFT.get(), 1, DustCost.of(28, 22, 14, 20), 70, parts, starship);
        replicator(output, "shuttle_strut", ModItems.SHUTTLE_STRUT.get(), 1, DustCost.of(10, 8, 5, 7), 30, parts, starship);
        replicator(output, "shuttle_nacelle", ModItems.SHUTTLE_NACELLE.get(), 1, DustCost.of(30, 22, 14, 20), 70, parts, starship);
        kit(output, "shuttle_core_kit", List.of(new ItemStack(ModItems.SHUTTLE_HULL.get()), new ItemStack(ModItems.SHUTTLE_WINDSHIELD.get()),
                new ItemStack(ModItems.SHUTTLE_NOSE.get())), DustCost.of(130, 98, 64, 85), 270, parts, starship);

        // Titan sections: about ten times the Ship's, since the Titan is over 60 blocks long. Placeholder prices.
        replicator(output, "titan_saucer", ModItems.TITAN_SAUCER.get(), 1, DustCost.of(1200, 900, 600, 800), 900, parts, TITAN_UNLOCK);
        replicator(output, "titan_neck", ModItems.TITAN_NECK.get(), 1, DustCost.of(280, 200, 140, 180), 300, parts, TITAN_UNLOCK);
        replicator(output, "titan_bridge", ModItems.TITAN_BRIDGE.get(), 1, DustCost.of(360, 260, 180, 240), 360, parts, TITAN_UNLOCK);
        replicator(output, "titan_sensor_array", ModItems.TITAN_SENSOR_ARRAY.get(), 1, DustCost.of(200, 150, 100, 140), 240, parts, TITAN_UNLOCK);
        replicator(output, "titan_deflector", ModItems.TITAN_DEFLECTOR.get(), 1, DustCost.of(260, 190, 130, 180), 300, parts, TITAN_UNLOCK);
        replicator(output, "titan_bay_door", ModItems.TITAN_BAY_DOOR.get(), 1, DustCost.of(340, 250, 170, 230), 360, parts, TITAN_UNLOCK);
        replicator(output, "titan_pylon", ModItems.TITAN_PYLON.get(), 1, DustCost.of(220, 160, 110, 150), 240, parts, TITAN_UNLOCK);
        replicator(output, "titan_nacelle", ModItems.TITAN_NACELLE.get(), 1, DustCost.of(300, 220, 150, 200), 300, parts, TITAN_UNLOCK);
        kit(output, "titan_core_kit", List.of(new ItemStack(ModItems.TITAN_SAUCER.get()), new ItemStack(ModItems.TITAN_NECK.get()),
                new ItemStack(ModItems.TITAN_BRIDGE.get())), DustCost.of(1650, 1220, 830, 1100), 1500, parts, TITAN_UNLOCK);
        kit(output, "ship_core_kit", List.of(new ItemStack(ModItems.SHIP_KEEL.get()), new ItemStack(ModItems.SHIP_BRIDGE.get()),
                new ItemStack(ModItems.SHIP_BOW.get())), DustCost.of(150, 114, 74, 102), 300, parts, starship);
    }

    /** The milestone that opens the Titan's recipes. Placeholder: the owner picks between the Starship and a Dyson tier later. */
    private static final ResourceKey<Milestone> TITAN_UNLOCK = Milestones.DYSON_TIER_3;

    /** A multi-output recipe, titled {@code recipe.void_reclaimer.<name>}. */
    private static void kit(RecipeOutput output, String name, List<ItemStack> results, DustCost cost, int seconds,
                            ResourceKey<ReplicatorCategory> category, ResourceKey<Milestone> milestone) {
        ReplicatorRecipe recipe = new ReplicatorRecipe(results, cost, seconds * 20, category, milestone,
                Optional.of(Component.translatable("recipe.void_reclaimer." + name)));
        output.accept(VoidReclaimer.id("replicator/" + name), recipe, null);
    }

    private static void machineRecipe(RecipeOutput output, String name, ItemLike result, DustCost cost, String toggle) {
        ReplicatorRecipe recipe = new ReplicatorRecipe(new ItemStack(result), cost, 60 * 20, ReplicatorCategories.BLOCKS, Milestones.DIVERSIFIED);
        output.accept(VoidReclaimer.id("replicator/" + name), recipe, null, new ConfigCondition(toggle));
    }

    /** Four raw items for eight dust of their kind; off with the {@code replicatorRawItems} toggle. */
    private static void rawItems(RecipeOutput output, String name, ItemLike result, DustCost cost, ResourceKey<ReplicatorCategory> category) {
        ReplicatorRecipe recipe = new ReplicatorRecipe(new ItemStack(result, 4), cost, 10 * 20, category, Milestones.FIRST_DUST);
        output.accept(VoidReclaimer.id("replicator/" + name), recipe, null, new ConfigCondition("replicatorRawItems"));
    }

    /** The sixteen dye colours, as Aeronautics names its envelopes. */
    private static final List<String> COLOURS = List.of("white", "orange", "magenta", "light_blue", "yellow", "lime", "pink", "gray",
            "light_gray", "cyan", "purple", "blue", "brown", "green", "red", "black");

    /**
     * Every Create Aeronautics block, unlocked by first setting foot on ruined Earth (the air-flight parts are of no use in
     * space). Costs are placeholders: lifting parts lean on ice and metal, propellers on carbon, levitite on silicon.
     */
    private static void aeronautics(RecipeOutput output) {
        aero(output, "wooden_propeller", 1, DustCost.of(0, 20, 0, 10), 20);
        aero(output, "andesite_propeller", 1, DustCost.of(0, 30, 0, 20), 30);
        aero(output, "smart_propeller", 1, DustCost.of(10, 40, 20, 30), 45);
        aero(output, "propeller_bearing", 1, DustCost.of(10, 20, 10, 50), 45);
        aero(output, "gyroscopic_propeller_bearing", 1, DustCost.of(20, 30, 40, 80), 90);
        aero(output, "steam_vent", 1, DustCost.of(40, 10, 0, 30), 40);
        aero(output, "adjustable_burner", 1, DustCost.of(20, 30, 10, 40), 45);
        // Levitite Blend is a fluid (its Create mixing route is in ModCreateRecipeProvider); the crystal and its refinement are blocks.
        aero(output, "levitite", 1, DustCost.of(20, 10, 60, 20), 60);
        aero(output, "pearlescent_levitite", 1, DustCost.of(30, 20, 90, 30), 90);
        aero(output, "mounted_potato_cannon", 1, DustCost.of(10, 20, 20, 40), 60);
        aero(output, "aviators_goggles", 1, DustCost.of(5, 10, 5, 20), 20);
        aero(output, "end_stone_powder", 4, DustCost.of(0, 20, 0, 0), 10);
        for (String colour : COLOURS) {
            aero(output, colour + "_envelope", 4, DustCost.of(20, 10, 0, 10), 20);
            aero(output, colour + "_envelope_encased_shaft", 1, DustCost.of(10, 5, 0, 15), 15);
        }
    }

    private static void aero(RecipeOutput output, String id, int count, DustCost cost, int seconds) {
        net.minecraft.resources.ResourceLocation key = net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("aeronautics", id);
        net.minecraft.world.item.Item item = net.minecraft.core.registries.BuiltInRegistries.ITEM.getOptional(key)
                .orElseThrow(() -> new IllegalStateException("Create Aeronautics item missing in datagen: " + key));
        replicator(output, "aeronautics_" + id, item, count, cost, seconds, ReplicatorCategories.BLOCKS, Milestones.EARTHFALL);
    }

    private static void replicator(RecipeOutput output, String name, ItemLike result, int count, DustCost cost, int seconds,
                                   ResourceKey<ReplicatorCategory> category, ResourceKey<Milestone> milestone) {
        ReplicatorRecipe recipe = new ReplicatorRecipe(new ItemStack(result, count), cost, seconds * 20, category, milestone);
        output.accept(VoidReclaimer.id("replicator/" + name), recipe, null);
    }
}
