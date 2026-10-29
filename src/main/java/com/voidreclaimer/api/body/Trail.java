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
 * Thruster exhaust style. Entries live in the synced datapack registry {@code void_reclaimer:trail}; the name
 * defaults to {@code body.<ns>.trail.<path>}.
 *
 * @param color     particle RGB; without one the body's accent tint is used
 * @param particles false for "no trail"
 * @param order     position in the Body tab's cycle (ties sort by id)
 * @param requires  milestone the player must have reached to pick it
 */
public record Trail(Optional<Integer> color, boolean particles, int order, Optional<Component> name,
                    Optional<ResourceKey<Milestone>> requires) implements Cosmetic {
    public static final Codec<Trail> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.INT.optionalFieldOf("color").forGetter(Trail::color),
            Codec.BOOL.optionalFieldOf("particles", true).forGetter(Trail::particles),
            Codec.INT.optionalFieldOf("order", 100).forGetter(Trail::order),
            ComponentSerialization.CODEC.optionalFieldOf("name").forGetter(Trail::name),
            ModCodecs.key(VoidReclaimerRegistries.MILESTONE).optionalFieldOf("requires").forGetter(Trail::requires)
    ).apply(instance, Trail::new));

    /** Particle color for a body with the given accent tint. */
    public int color(int accent) {
        return color.orElse(accent);
    }

    @Override
    public Component displayName(ResourceLocation id) {
        return name.orElseGet(() -> Component.translatable("body." + id.getNamespace() + ".trail." + id.getPath()));
    }
}
