package com.voidreclaimer.datagen;

import com.voidreclaimer.VoidReclaimer;
import com.voidreclaimer.registry.ModItems;
import com.voidreclaimer.replicator.DustCost;
import com.voidreclaimer.replicator.Milestone;
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
        replicator(output, "dust_sensor", ModItems.DUST_SENSOR.get(), 1, new DustCost(10, 15, 0, 5), 20,
                ReplicatorRecipe.Category.MODULES, Milestone.AWAKENING);
        replicator(output, "scaffold", ModItems.SCAFFOLD.get(), 8, new DustCost(0, 8, 0, 0), 5,
                ReplicatorRecipe.Category.BLOCKS, Milestone.AWAKENING);
        replicator(output, "salvage_tool", ModItems.SALVAGE_TOOL.get(), 1, new DustCost(15, 5, 0, 0), 30,
                ReplicatorRecipe.Category.TOOLS, Milestone.FIRST_DUST);
        replicator(output, "nanite_swarm", ModItems.NANITE_SWARM.get(), 1, new DustCost(20, 10, 0, 10), 40,
                ReplicatorRecipe.Category.TOOLS, Milestone.FIRST_DUST);
        replicator(output, "tether_anchor", ModItems.TETHER_ANCHOR.get(), 1, new DustCost(15, 5, 0, 0), 20,
                ReplicatorRecipe.Category.BLOCKS, Milestone.FIRST_STRUCTURE);
        replicator(output, "backup_core", ModItems.BACKUP_CORE.get(), 1, new DustCost(30, 20, 10, 10), 60,
                ReplicatorRecipe.Category.BLOCKS, Milestone.DIVERSIFIED);
    }

    private static void replicator(RecipeOutput output, String name, ItemLike result, int count, DustCost cost, int seconds,
                                   ReplicatorRecipe.Category category, Milestone milestone) {
        ReplicatorRecipe recipe = new ReplicatorRecipe(new ItemStack(result, count), cost, seconds * 20, category, milestone);
        output.accept(VoidReclaimer.id("replicator/" + name), recipe, null);
    }
}
