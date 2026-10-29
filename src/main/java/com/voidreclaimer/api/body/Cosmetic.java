package com.voidreclaimer.api.body;

import com.voidreclaimer.api.milestone.Milestone;
import java.util.Optional;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;

/** A pickable body look ({@link Casing}, {@link Trail}): ordered, named, optionally locked behind a milestone. */
public interface Cosmetic {
    int order();

    Optional<ResourceKey<Milestone>> requires();

    Component displayName(ResourceLocation id);
}
