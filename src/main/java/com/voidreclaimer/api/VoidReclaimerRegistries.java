package com.voidreclaimer.api;

import com.voidreclaimer.VoidReclaimer;
import com.voidreclaimer.api.dust.DustKind;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;

/**
 * Keys of every Void Reclaimer registry. Datapack registries are loaded from
 * {@code data/<namespace>/void_reclaimer/<registry>/<name>.json}; code registries are filled with
 * {@code DeferredRegister.create(key, modId)}.
 */
public final class VoidReclaimerRegistries {
    /** Synced datapack registry: what dust clouds are made of. */
    public static final ResourceKey<Registry<DustKind>> DUST_KIND = key("dust_kind");

    private VoidReclaimerRegistries() {}

    private static <T> ResourceKey<Registry<T>> key(String name) {
        return ResourceKey.createRegistryKey(VoidReclaimer.id(name));
    }
}
