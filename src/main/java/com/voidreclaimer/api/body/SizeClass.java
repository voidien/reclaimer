package com.voidreclaimer.api.body;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Optional;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.resources.ResourceLocation;

/**
 * A body size class (Drone, Mech, Ship). Entries live in the synced datapack registry {@code void_reclaimer:size_class};
 * the name defaults to {@code body.<ns>.size.<path>}.
 *
 * @param scale value of the vanilla {@code generic.scale} attribute for bodies of this class
 * @param rank  ordering for "at least this size" checks (modules with a minimum size); built-ins 0, 1, 2
 */
public record SizeClass(double scale, int rank, Optional<Component> name) {
    public static final Codec<SizeClass> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.doubleRange(0.0625, 16.0).fieldOf("scale").forGetter(SizeClass::scale),
            Codec.INT.fieldOf("rank").forGetter(SizeClass::rank),
            ComponentSerialization.CODEC.optionalFieldOf("name").forGetter(SizeClass::name)
    ).apply(instance, SizeClass::new));

    public SizeClass(double scale, int rank) {
        this(scale, rank, Optional.empty());
    }

    public Component displayName(ResourceLocation id) {
        return name.orElseGet(() -> Component.translatable("body." + id.getNamespace() + ".size." + id.getPath()));
    }
}
