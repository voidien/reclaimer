package com.voidreclaimer.registry;

import java.util.Comparator;
import java.util.List;
import java.util.function.ToIntFunction;
import javax.annotation.Nullable;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.resources.ResourceKey;

/** Reads a datapack registry whose entries carry a sort order, the same way on client and server. */
public final class OrderedRegistry {
    private OrderedRegistry() {}

    public record Entry<T>(ResourceKey<T> key, T value) {}

    /** All entries by {@code order}, then id. Empty when the registry doesn't exist (yet). */
    public static <T> List<Entry<T>> sorted(RegistryAccess access, ResourceKey<? extends Registry<T>> registry, ToIntFunction<T> order) {
        return access.registry(registry)
                .map(r -> r.entrySet().stream()
                        .map(e -> new Entry<>(e.getKey(), e.getValue()))
                        .sorted(Comparator.comparingInt((Entry<T> e) -> order.applyAsInt(e.value()))
                                .thenComparing(e -> e.key().location()))
                        .toList())
                .orElse(List.of());
    }

    @Nullable
    public static <T> T get(RegistryAccess access, ResourceKey<? extends Registry<T>> registry, ResourceKey<T> key) {
        return access.registry(registry).map(r -> r.get(key)).orElse(null);
    }
}
