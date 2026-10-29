package com.voidreclaimer.replicator;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.voidreclaimer.api.VoidReclaimerRegistries;
import com.voidreclaimer.api.milestone.Milestone;
import com.voidreclaimer.api.replicator.ReplicatorCategory;
import com.voidreclaimer.registry.ModCodecs;
import com.voidreclaimer.registry.ModRecipeSerializers;
import net.minecraft.resources.ResourceKey;
import com.voidreclaimer.registry.ModRecipeTypes;
import net.minecraft.core.HolderLookup;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;

/**
 * A Replicator recipe: turns dust from the AI's reserve into an item over time.
 *
 * @param result    the item produced
 * @param cost      dust paid when the job is queued (refunded if cancelled)
 * @param ticks     build time
 * @param category  Replicator screen tab (an entry of {@code void_reclaimer:replicator_category})
 * @param milestone milestone that unlocks the recipe
 */
public record ReplicatorRecipe(ItemStack result, DustCost cost, int ticks, ResourceKey<ReplicatorCategory> category, ResourceKey<Milestone> milestone)
        implements Recipe<ReplicatorInput> {

    public static final MapCodec<ReplicatorRecipe> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            ItemStack.STRICT_CODEC.fieldOf("result").forGetter(ReplicatorRecipe::result),
            DustCost.CODEC.fieldOf("cost").forGetter(ReplicatorRecipe::cost),
            ExtraCodecs.POSITIVE_INT.fieldOf("ticks").forGetter(ReplicatorRecipe::ticks),
            ModCodecs.key(VoidReclaimerRegistries.REPLICATOR_CATEGORY).fieldOf("category").forGetter(ReplicatorRecipe::category),
            ModCodecs.key(VoidReclaimerRegistries.MILESTONE).fieldOf("milestone").forGetter(ReplicatorRecipe::milestone)
    ).apply(instance, ReplicatorRecipe::new));

    public static final StreamCodec<RegistryFriendlyByteBuf, ReplicatorRecipe> STREAM_CODEC = StreamCodec.composite(
            ItemStack.STREAM_CODEC, ReplicatorRecipe::result,
            DustCost.STREAM_CODEC, ReplicatorRecipe::cost,
            ByteBufCodecs.VAR_INT, ReplicatorRecipe::ticks,
            ResourceKey.streamCodec(VoidReclaimerRegistries.REPLICATOR_CATEGORY), ReplicatorRecipe::category,
            ResourceKey.streamCodec(VoidReclaimerRegistries.MILESTONE), ReplicatorRecipe::milestone,
            ReplicatorRecipe::new);

    /** Affordable with the given reserve (unlocking is checked separately, per player). */
    @Override
    public boolean matches(ReplicatorInput input, Level level) {
        return cost.canAfford(input.dust());
    }

    @Override
    public ItemStack assemble(ReplicatorInput input, HolderLookup.Provider registries) {
        return result.copy();
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return true;
    }

    @Override
    public ItemStack getResultItem(HolderLookup.Provider registries) {
        return result;
    }

    /** Keeps the vanilla recipe book out of it (it has no category for this type). */
    @Override
    public boolean isSpecial() {
        return true;
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return ModRecipeSerializers.REPLICATOR.get();
    }

    @Override
    public RecipeType<?> getType() {
        return ModRecipeTypes.REPLICATOR.get();
    }

    public static final class Serializer implements RecipeSerializer<ReplicatorRecipe> {
        @Override
        public MapCodec<ReplicatorRecipe> codec() {
            return CODEC;
        }

        @Override
        public StreamCodec<RegistryFriendlyByteBuf, ReplicatorRecipe> streamCodec() {
            return STREAM_CODEC;
        }
    }
}
