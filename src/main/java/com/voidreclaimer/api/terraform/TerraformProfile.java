package com.voidreclaimer.api.terraform;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.List;
import java.util.Optional;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

/**
 * What a Terraforming Spire pulse turns ruined ground into. Entries live in the datapack registry
 * {@code void_reclaimer:terraform_profile} (server-side only, not synced); the pulse uses
 * {@code void_reclaimer:restored}, which a datapack may replace.
 *
 * @param swaps the block rewrites, in the order they are tried (the first that matches wins)
 * @param biome the biome every column of the area is rewritten to
 */
public record TerraformProfile(List<Swap> swaps, ResourceKey<Biome> biome) {
    public static final Codec<TerraformProfile> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Swap.CODEC.listOf().fieldOf("swaps").forGetter(TerraformProfile::swaps),
            ResourceKey.codec(Registries.BIOME).fieldOf("biome").forGetter(TerraformProfile::biome)
    ).apply(instance, TerraformProfile::new));

    /**
     * One rewrite: every {@code from} block becomes {@code to}, or {@code surface} when nothing solid covers it (air above).
     */
    public record Swap(Block from, BlockState to, Optional<BlockState> surface) {
        public static final Codec<Swap> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                BuiltInRegistries.BLOCK.byNameCodec().fieldOf("from").forGetter(Swap::from),
                BlockState.CODEC.fieldOf("to").forGetter(Swap::to),
                BlockState.CODEC.optionalFieldOf("surface").forGetter(Swap::surface)
        ).apply(instance, Swap::new));
    }

    /** What {@code state} becomes, if any swap matches; {@code exposed} is true when air lies directly above it. */
    public Optional<BlockState> replacement(BlockState state, boolean exposed) {
        for (Swap swap : swaps) {
            if (state.is(swap.from())) {
                return Optional.of(exposed ? swap.surface().orElse(swap.to()) : swap.to());
            }
        }
        return Optional.empty();
    }
}
