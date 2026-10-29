package com.voidreclaimer.datagen;

import com.voidreclaimer.VoidReclaimer;
import com.voidreclaimer.registry.ModItems;
import com.voidreclaimer.replicator.DustCost;
import com.voidreclaimer.api.milestone.Milestone;
import com.voidreclaimer.api.replicator.ReplicatorCategory;
import com.voidreclaimer.replicator.ReplicatorCategories;
import com.voidreclaimer.replicator.Milestones;
import net.minecraft.resources.ResourceKey;
import com.voidreclaimer.replicator.ReplicatorRecipe;
import java.util.concurrent.CompletableFuture;
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
        replicator(output, "tether_anchor", ModItems.TETHER_ANCHOR.get(), 1, DustCost.of(15, 5, 0, 0), 20,
                ReplicatorCategories.BLOCKS, Milestones.FIRST_STRUCTURE);
        replicator(output, "backup_core", ModItems.BACKUP_CORE.get(), 1, DustCost.of(30, 20, 10, 10), 60,
                ReplicatorCategories.BLOCKS, Milestones.DIVERSIFIED);

        // Body parts. The drone frame, Mk I core and Mk I thruster are also craftable as spares.
        ResourceKey<ReplicatorCategory> parts = ReplicatorCategories.PARTS;
        replicator(output, "thruster_mk1", ModItems.THRUSTER_MK1.get(), 1, DustCost.of(10, 5, 0, 0), 30, parts, Milestones.AWAKENING);
        replicator(output, "drone_frame", ModItems.DRONE_FRAME.get(), 1, DustCost.of(20, 10, 0, 0), 60, parts, Milestones.FIRST_DUST);
        replicator(output, "core_mk1", ModItems.CORE_MK1.get(), 1, DustCost.of(15, 10, 0, 5), 45, parts, Milestones.FIRST_DUST);
        replicator(output, "storage_mk1", ModItems.STORAGE_MK1.get(), 1, DustCost.of(8, 8, 0, 0), 30, parts, Milestones.FIRST_DUST);
        replicator(output, "brass_trim", ModItems.BRASS_TRIM.get(), 1, DustCost.of(5, 0, 0, 0), 10, parts, Milestones.FIRST_STRUCTURE);
        replicator(output, "cogwheel_crown", ModItems.COGWHEEL_CROWN.get(), 1, DustCost.of(8, 0, 0, 4), 15, parts, Milestones.DIVERSIFIED);
        replicator(output, "core_mk2", ModItems.CORE_MK2.get(), 1, DustCost.of(40, 30, 10, 20), 90, parts, Milestones.EXPANSION);
        replicator(output, "thruster_mk2", ModItems.THRUSTER_MK2.get(), 1, DustCost.of(30, 10, 0, 10), 60, parts, Milestones.EXPANSION);
        replicator(output, "storage_mk2", ModItems.STORAGE_MK2.get(), 1, DustCost.of(20, 20, 10, 0), 60, parts, Milestones.EXPANSION);
        replicator(output, "mech_frame", ModItems.MECH_FRAME.get(), 1, DustCost.of(80, 60, 20, 40), 180, parts, Milestones.EXPANSION);
    }

    private static void replicator(RecipeOutput output, String name, ItemLike result, int count, DustCost cost, int seconds,
                                   ResourceKey<ReplicatorCategory> category, ResourceKey<Milestone> milestone) {
        ReplicatorRecipe recipe = new ReplicatorRecipe(new ItemStack(result, count), cost, seconds * 20, category, milestone);
        output.accept(VoidReclaimer.id("replicator/" + name), recipe, null);
    }
}
