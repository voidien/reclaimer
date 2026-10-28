package com.voidreclaimer.registry;

import com.voidreclaimer.VoidReclaimer;
import com.voidreclaimer.replicator.ReplicatorRecipe;
import java.util.function.Supplier;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModRecipeSerializers {
    public static final DeferredRegister<RecipeSerializer<?>> RECIPE_SERIALIZERS =
            DeferredRegister.create(Registries.RECIPE_SERIALIZER, VoidReclaimer.MOD_ID);

    public static final Supplier<RecipeSerializer<ReplicatorRecipe>> REPLICATOR = RECIPE_SERIALIZERS.register("replicator",
            ReplicatorRecipe.Serializer::new);

    private ModRecipeSerializers() {}
}
