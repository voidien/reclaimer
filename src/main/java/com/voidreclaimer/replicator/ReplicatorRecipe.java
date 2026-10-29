package com.voidreclaimer.replicator;

import com.mojang.serialization.DataResult;
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
import java.util.List;
import java.util.Optional;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;

/**
 * A Replicator recipe: turns dust from the AI's reserve into items over time.
 *
 * <p>JSON takes {@code "result": stack} for one output or {@code "results": [stacks]} for several (kits); files from
 * before multi-output recipes still load.
 *
 * @param results   items produced (at least one); the first is the one shown
 * @param cost      dust paid when the job is queued (refunded if cancelled)
 * @param ticks     build time
 * @param category  Replicator screen tab (an entry of {@code void_reclaimer:replicator_category})
 * @param milestone milestone that unlocks the recipe
 * @param title     name shown instead of the first result's (kits)
 */
public record ReplicatorRecipe(List<ItemStack> results, DustCost cost, int ticks, ResourceKey<ReplicatorCategory> category,
                               ResourceKey<Milestone> milestone, Optional<Component> title) implements Recipe<ReplicatorInput> {

    public static final MapCodec<ReplicatorRecipe> CODEC = RecordCodecBuilder.<ReplicatorRecipe>mapCodec(instance -> instance.group(
            ItemStack.STRICT_CODEC.optionalFieldOf("result").forGetter(r -> r.results.size() == 1 ? Optional.of(r.results.get(0)) : Optional.empty()),
            ItemStack.STRICT_CODEC.listOf().optionalFieldOf("results").forGetter(r -> r.results.size() > 1 ? Optional.of(r.results) : Optional.empty()),
            DustCost.CODEC.fieldOf("cost").forGetter(ReplicatorRecipe::cost),
            ExtraCodecs.POSITIVE_INT.fieldOf("ticks").forGetter(ReplicatorRecipe::ticks),
            ModCodecs.key(VoidReclaimerRegistries.REPLICATOR_CATEGORY).fieldOf("category").forGetter(ReplicatorRecipe::category),
            ModCodecs.key(VoidReclaimerRegistries.MILESTONE).fieldOf("milestone").forGetter(ReplicatorRecipe::milestone),
            ComponentSerialization.CODEC.optionalFieldOf("title").forGetter(ReplicatorRecipe::title)
    ).apply(instance, (single, list, cost, ticks, category, milestone, title) -> new ReplicatorRecipe(
            list.orElseGet(() -> single.map(List::of).orElse(List.of())), cost, ticks, category, milestone, title)))
            .validate(recipe -> recipe.results.isEmpty() ? DataResult.error(() -> "a Replicator recipe needs a result") : DataResult.success(recipe));

    public static final StreamCodec<RegistryFriendlyByteBuf, ReplicatorRecipe> STREAM_CODEC = StreamCodec.composite(
            ItemStack.STREAM_CODEC.apply(ByteBufCodecs.list()), ReplicatorRecipe::results,
            DustCost.STREAM_CODEC, ReplicatorRecipe::cost,
            ByteBufCodecs.VAR_INT, ReplicatorRecipe::ticks,
            ResourceKey.streamCodec(VoidReclaimerRegistries.REPLICATOR_CATEGORY), ReplicatorRecipe::category,
            ResourceKey.streamCodec(VoidReclaimerRegistries.MILESTONE), ReplicatorRecipe::milestone,
            ByteBufCodecs.optional(ComponentSerialization.TRUSTED_STREAM_CODEC), ReplicatorRecipe::title,
            ReplicatorRecipe::new);

    public ReplicatorRecipe {
        results = results.stream().map(ItemStack::copy).toList();
    }

    public ReplicatorRecipe(ItemStack result, DustCost cost, int ticks, ResourceKey<ReplicatorCategory> category, ResourceKey<Milestone> milestone) {
        this(List.of(result), cost, ticks, category, milestone, Optional.empty());
    }

    /** The first result (the one shown). */
    public ItemStack result() {
        return results.get(0);
    }

    /** How many of {@code item} one job makes. */
    public int count(Item item) {
        int count = 0;
        for (ItemStack stack : results) {
            if (stack.is(item)) {
                count += stack.getCount();
            }
        }
        return count;
    }

    /** Items one job makes, in total. */
    public int totalCount() {
        int count = 0;
        for (ItemStack stack : results) {
            count += stack.getCount();
        }
        return count;
    }

    /** The title, or the first result's name. */
    public Component displayName() {
        return title.orElseGet(() -> result().getHoverName());
    }

    /** Affordable with the given reserve (unlocking is checked separately, per player). */
    @Override
    public boolean matches(ReplicatorInput input, Level level) {
        return cost.canAfford(input.dust());
    }

    @Override
    public ItemStack assemble(ReplicatorInput input, HolderLookup.Provider registries) {
        return result().copy();
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return true;
    }

    @Override
    public ItemStack getResultItem(HolderLookup.Provider registries) {
        return result();
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
