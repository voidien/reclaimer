package com.voidreclaimer.replicator;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.voidreclaimer.api.VoidReclaimerRegistries;
import com.voidreclaimer.api.milestone.Milestone;
import com.voidreclaimer.registry.ModCodecs;
import com.voidreclaimer.registry.ModRecipeSerializers;
import net.minecraft.resources.ResourceKey;
import com.voidreclaimer.registry.ModRecipeTypes;
import io.netty.buffer.ByteBuf;
import java.util.function.IntFunction;
import net.minecraft.core.HolderLookup;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.ByIdMap;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.util.StringRepresentable;
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
 * @param category  Replicator screen tab
 * @param milestone milestone that unlocks the recipe
 */
public record ReplicatorRecipe(ItemStack result, DustCost cost, int ticks, Category category, ResourceKey<Milestone> milestone)
        implements Recipe<ReplicatorInput> {

    public enum Category implements StringRepresentable {
        TOOLS("tools"), MODULES("modules"), PARTS("parts"), BLOCKS("blocks");

        public static final Codec<Category> CODEC = StringRepresentable.fromEnum(Category::values);
        private static final IntFunction<Category> BY_ID = ByIdMap.continuous(Enum::ordinal, values(), ByIdMap.OutOfBoundsStrategy.ZERO);
        public static final StreamCodec<ByteBuf, Category> STREAM_CODEC = ByteBufCodecs.idMapper(BY_ID, Enum::ordinal);

        private final String name;

        Category(String name) {
            this.name = name;
        }

        @Override
        public String getSerializedName() {
            return name;
        }

        public Component title() {
            return Component.translatable("replicator.void_reclaimer.tab." + name);
        }
    }

    public static final MapCodec<ReplicatorRecipe> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            ItemStack.STRICT_CODEC.fieldOf("result").forGetter(ReplicatorRecipe::result),
            DustCost.CODEC.fieldOf("cost").forGetter(ReplicatorRecipe::cost),
            ExtraCodecs.POSITIVE_INT.fieldOf("ticks").forGetter(ReplicatorRecipe::ticks),
            Category.CODEC.fieldOf("category").forGetter(ReplicatorRecipe::category),
            ModCodecs.key(VoidReclaimerRegistries.MILESTONE).fieldOf("milestone").forGetter(ReplicatorRecipe::milestone)
    ).apply(instance, ReplicatorRecipe::new));

    public static final StreamCodec<RegistryFriendlyByteBuf, ReplicatorRecipe> STREAM_CODEC = StreamCodec.composite(
            ItemStack.STREAM_CODEC, ReplicatorRecipe::result,
            DustCost.STREAM_CODEC, ReplicatorRecipe::cost,
            ByteBufCodecs.VAR_INT, ReplicatorRecipe::ticks,
            Category.STREAM_CODEC, ReplicatorRecipe::category,
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
