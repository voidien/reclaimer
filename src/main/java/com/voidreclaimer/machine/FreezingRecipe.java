package com.voidreclaimer.machine;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.voidreclaimer.registry.ModRecipeSerializers;
import com.voidreclaimer.registry.ModRecipeTypes;
import java.util.List;
import net.minecraft.core.HolderLookup;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.level.Level;

/**
 * An Encased Fan freezing recipe: an item carried through powder snow by the fan becomes the results. Create has no
 * freezing, so this is our own recipe type; {@link FreezingFanType} runs it.
 *
 * @param ingredient the item that freezes (one at a time; a stack gives a stack of results)
 * @param results    what it becomes, including anything left over, such as the empty bucket
 */
public record FreezingRecipe(Ingredient ingredient, List<ItemStack> results) implements Recipe<SingleRecipeInput> {
    public static final MapCodec<FreezingRecipe> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            Ingredient.CODEC_NONEMPTY.fieldOf("ingredient").forGetter(FreezingRecipe::ingredient),
            ItemStack.STRICT_CODEC.listOf().fieldOf("results").forGetter(FreezingRecipe::results)
    ).apply(instance, FreezingRecipe::new));

    public static final StreamCodec<RegistryFriendlyByteBuf, FreezingRecipe> STREAM_CODEC = StreamCodec.composite(
            Ingredient.CONTENTS_STREAM_CODEC, FreezingRecipe::ingredient,
            ItemStack.STREAM_CODEC.apply(ByteBufCodecs.list()), FreezingRecipe::results,
            FreezingRecipe::new);

    public FreezingRecipe {
        results = results.stream().map(ItemStack::copy).toList();
    }

    @Override
    public boolean matches(SingleRecipeInput input, Level level) {
        return ingredient.test(input.item());
    }

    @Override
    public ItemStack assemble(SingleRecipeInput input, HolderLookup.Provider registries) {
        return results.isEmpty() ? ItemStack.EMPTY : results.get(0).copy();
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return true;
    }

    @Override
    public ItemStack getResultItem(HolderLookup.Provider registries) {
        return results.isEmpty() ? ItemStack.EMPTY : results.get(0).copy();
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return ModRecipeSerializers.FREEZING.get();
    }

    @Override
    public RecipeType<?> getType() {
        return ModRecipeTypes.FREEZING.get();
    }

    public static final class Serializer implements RecipeSerializer<FreezingRecipe> {
        @Override
        public MapCodec<FreezingRecipe> codec() {
            return CODEC;
        }

        @Override
        public StreamCodec<RegistryFriendlyByteBuf, FreezingRecipe> streamCodec() {
            return STREAM_CODEC;
        }
    }
}
