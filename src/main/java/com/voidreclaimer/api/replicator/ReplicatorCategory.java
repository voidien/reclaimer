package com.voidreclaimer.api.replicator;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Optional;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.resources.ResourceLocation;

/**
 * A tab of the Replicator screen. Recipes name their category by id. Entries live in the synced datapack registry
 * {@code void_reclaimer:replicator_category}; the title defaults to {@code replicator.<ns>.tab.<path>}.
 *
 * @param order tab position (ties sort by id)
 */
public record ReplicatorCategory(int order, Optional<Component> title) {
    public static final Codec<ReplicatorCategory> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.INT.optionalFieldOf("order", 100).forGetter(ReplicatorCategory::order),
            ComponentSerialization.CODEC.optionalFieldOf("title").forGetter(ReplicatorCategory::title)
    ).apply(instance, ReplicatorCategory::new));

    public ReplicatorCategory(int order) {
        this(order, Optional.empty());
    }

    public Component title(ResourceLocation id) {
        return title.orElseGet(() -> Component.translatable("replicator." + id.getNamespace() + ".tab." + id.getPath()));
    }
}
