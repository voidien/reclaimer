package com.voidreclaimer.api.milestone;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Optional;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.resources.ResourceLocation;

/**
 * A progression milestone. Reaching one unlocks the Replicator recipes that name it and adds a first-person log entry
 * from the AI. Entries live in the synced datapack registry {@code void_reclaimer:milestone}.
 *
 * <p>Texts default to translation keys from the id ({@code milestone.<ns>.<path>}, {@code .hint},
 * {@code log.<ns>.<path>.title} and {@code .text}); a datapack without a resource pack can give them inline.
 *
 * @param criterion when it is reached
 * @param order     position in the AI log and in checking order (ties sort by id)
 */
public record Milestone(MilestoneCriterion criterion, int order, Optional<Component> title, Optional<Component> hint,
                        Optional<Component> logTitle, Optional<Component> logText) {
    public static final Codec<Milestone> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            MilestoneCriterion.CODEC.fieldOf("criterion").forGetter(Milestone::criterion),
            Codec.INT.optionalFieldOf("order", 100).forGetter(Milestone::order),
            ComponentSerialization.CODEC.optionalFieldOf("title").forGetter(Milestone::title),
            ComponentSerialization.CODEC.optionalFieldOf("hint").forGetter(Milestone::hint),
            ComponentSerialization.CODEC.optionalFieldOf("log_title").forGetter(Milestone::logTitle),
            ComponentSerialization.CODEC.optionalFieldOf("log_text").forGetter(Milestone::logText)
    ).apply(instance, Milestone::new));

    public Milestone(MilestoneCriterion criterion, int order) {
        this(criterion, order, Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty());
    }

    public boolean isReached(MilestoneContext context) {
        return criterion.isReached(context);
    }

    /** Short name, e.g. for the "locked" hint. */
    public Component title(ResourceLocation id) {
        return title.orElseGet(() -> Component.translatable(key("milestone", id, "")));
    }

    /** What to do to reach it, shown on locked recipes. */
    public Component hint(ResourceLocation id) {
        return hint.orElseGet(() -> Component.translatable(key("milestone", id, ".hint")));
    }

    /** The AI's log entry written when the milestone is reached. */
    public Component logTitle(ResourceLocation id) {
        return logTitle.orElseGet(() -> Component.translatable(key("log", id, ".title")));
    }

    public Component logText(ResourceLocation id) {
        return logText.orElseGet(() -> Component.translatable(key("log", id, ".text")));
    }

    private static String key(String prefix, ResourceLocation id, String suffix) {
        return prefix + "." + id.getNamespace() + "." + id.getPath() + suffix;
    }
}
