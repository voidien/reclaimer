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
        replicator(output, "dust_sensor", ModItems.DUST_SENSOR.get(), 1, DustCost.of(10, 15, 0, 5), 20,
                ReplicatorCategories.MODULES, Milestones.AWAKENING);
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
    }

    /** A multi-output recipe, titled {@code recipe.void_reclaimer.<name>}. */
    private static void kit(RecipeOutput output, String name, List<ItemStack> results, DustCost cost, int seconds,
                            ResourceKey<ReplicatorCategory> category, ResourceKey<Milestone> milestone) {
        ReplicatorRecipe recipe = new ReplicatorRecipe(results, cost, seconds * 20, category, milestone,
                Optional.of(Component.translatable("recipe.void_reclaimer." + name)));
        output.accept(VoidReclaimer.id("replicator/" + name), recipe, null);
    }

    /** Four raw items for eight dust of their kind; off with the {@code replicatorRawItems} toggle. */
    private static void rawItems(RecipeOutput output, String name, ItemLike result, DustCost cost, ResourceKey<ReplicatorCategory> category) {
        ReplicatorRecipe recipe = new ReplicatorRecipe(new ItemStack(result, 4), cost, 10 * 20, category, Milestones.FIRST_DUST);
        output.accept(VoidReclaimer.id("replicator/" + name), recipe, null, new ConfigCondition("replicatorRawItems"));
    }

    private static void replicator(RecipeOutput output, String name, ItemLike result, int count, DustCost cost, int seconds,
                                   ResourceKey<ReplicatorCategory> category, ResourceKey<Milestone> milestone) {
        ReplicatorRecipe recipe = new ReplicatorRecipe(new ItemStack(result, count), cost, seconds * 20, category, milestone);
        output.accept(VoidReclaimer.id("replicator/" + name), recipe, null);
    }
}
