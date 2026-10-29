package com.voidreclaimer.registry;

import com.mojang.serialization.Codec;
import com.voidreclaimer.VoidReclaimer;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;

/** Codecs shared by saves and data files. */
public final class ModCodecs {
    /**
     * A resource id where a bare name means our namespace: {@code "metallic"} reads as {@code void_reclaimer:metallic}.
     * Saves and recipes from before the registries wrote bare names.
     */
    public static final Codec<ResourceLocation> ID = Codec.STRING.comapFlatMap(
            s -> ResourceLocation.read(s.indexOf(':') >= 0 ? s : VoidReclaimer.MOD_ID + ":" + s),
            ResourceLocation::toString);

    private ModCodecs() {}

    /** A registry entry key read with {@link #ID}. */
    public static <T> Codec<ResourceKey<T>> key(ResourceKey<? extends Registry<T>> registry) {
        return ID.xmap(id -> ResourceKey.create(registry, id), ResourceKey::location);
    }
}
