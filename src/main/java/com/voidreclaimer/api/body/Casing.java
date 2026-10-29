package com.voidreclaimer.api.body;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.voidreclaimer.api.VoidReclaimerRegistries;
import com.voidreclaimer.api.milestone.Milestone;
import com.voidreclaimer.registry.ModCodecs;
import java.util.Optional;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;

/**
 * A body casing material, after Create's casings. Entries live in the synced datapack registry
 * {@code void_reclaimer:casing}; the name defaults to {@code body.<ns>.casing.<path>}.
 *
 * @param order    position in the Body tab's cycle (ties sort by id)
 * @param texture  texture the body renderer uses (Phase 5b), e.g. {@code create:block/brass_casing}
 * @param requires milestone the player must have reached to pick it
 */
public record Casing(int order, Optional<Component> name, Optional<ResourceLocation> texture,
                     Optional<ResourceKey<Milestone>> requires) implements Cosmetic {
    public static final Codec<Casing> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.INT.optionalFieldOf("order", 100).forGetter(Casing::order),
            ComponentSerialization.CODEC.optionalFieldOf("name").forGetter(Casing::name),
            ResourceLocation.CODEC.optionalFieldOf("texture").forGetter(Casing::texture),
            ModCodecs.key(VoidReclaimerRegistries.MILESTONE).optionalFieldOf("requires").forGetter(Casing::requires)
    ).apply(instance, Casing::new));

    @Override
    public Component displayName(ResourceLocation id) {
        return name.orElseGet(() -> Component.translatable("body." + id.getNamespace() + ".casing." + id.getPath()));
    }
}
