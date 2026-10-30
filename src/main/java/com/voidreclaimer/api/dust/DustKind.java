package com.voidreclaimer.api.dust;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.voidreclaimer.registry.ModCodecs;
import java.util.Optional;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.resources.ResourceLocation;

/**
 * What a cosmic dust cloud is made of. Entries live in the synced datapack registry
 * {@code void_reclaimer:dust_kind} ({@code data/<namespace>/void_reclaimer/dust_kind/<name>.json}).
 *
 * @param color       RGB used for HUD meters, labels and the cloud scanner mesh
 * @param cloudWeight relative chance that a generated cloud is of this kind; 0 keeps it out of generated clouds
 * @param name        display name; without one the translation key {@code dust.<namespace>.<path>} is used
 * @param order       sort position in the HUD and in cloud generation (ties sort by id); the built-in kinds use 0-3
 * @param icon        GUI sprite (9x9, under {@code textures/gui/sprites/}) drawn beside amounts in the HUD and the Replicator;
 *                    without one a square in {@code color} is drawn
 */
public record DustKind(int color, double cloudWeight, Optional<Component> name, int order, Optional<ResourceLocation> icon) {
    public static final int DEFAULT_ORDER = 100;

    public static final Codec<DustKind> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.INT.fieldOf("color").forGetter(DustKind::color),
            Codec.doubleRange(0.0, Double.MAX_VALUE).optionalFieldOf("cloud_weight", 1.0).forGetter(DustKind::cloudWeight),
            ComponentSerialization.CODEC.optionalFieldOf("name").forGetter(DustKind::name),
            Codec.INT.optionalFieldOf("order", DEFAULT_ORDER).forGetter(DustKind::order),
            ModCodecs.ID.optionalFieldOf("icon").forGetter(DustKind::icon)
    ).apply(instance, DustKind::new));

    public DustKind(int color, double cloudWeight, int order) {
        this(color, cloudWeight, Optional.empty(), order, Optional.empty());
    }

    public DustKind(int color, double cloudWeight, int order, ResourceLocation icon) {
        this(color, cloudWeight, Optional.empty(), order, Optional.of(icon));
    }

    /** The display name of the kind registered under {@code id}. */
    public Component displayName(ResourceLocation id) {
        return name.orElseGet(() -> Component.translatable("dust." + id.getNamespace() + "." + id.getPath()));
    }
}
