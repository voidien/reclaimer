package com.voidreclaimer.registry;

import com.voidreclaimer.VoidReclaimer;
import com.voidreclaimer.replicator.ReplicatorRecipe;
import java.util.function.Supplier;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.crafting.RecipeType;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModRecipeTypes {
    public static final DeferredRegister<RecipeType<?>> RECIPE_TYPES = DeferredRegister.create(Registries.RECIPE_TYPE, VoidReclaimer.MOD_ID);

    public static final Supplier<RecipeType<ReplicatorRecipe>> REPLICATOR = RECIPE_TYPES.register("replicator",
            () -> RecipeType.simple(VoidReclaimer.id("replicator")));

    public static final Supplier<RecipeType<com.voidreclaimer.machine.FreezingRecipe>> FREEZING = RECIPE_TYPES.register("freezing",
            () -> RecipeType.simple(VoidReclaimer.id("freezing")));

    private ModRecipeTypes() {}
}
