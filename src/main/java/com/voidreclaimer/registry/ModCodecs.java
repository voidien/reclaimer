package com.voidreclaimer.registry;

import com.mojang.datafixers.util.Pair;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;
import com.voidreclaimer.VoidReclaimer;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import org.slf4j.Logger;

/** Codecs shared by saves and data files. */
public final class ModCodecs {
    private static final Logger LOGGER = LogUtils.getLogger();

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

    /**
     * A list where an entry that cannot be read costs that entry alone, and says so: the rest of it may be someone's only copy of a body
     * or their items. Plain {@code listOf} loses the whole list to one bad entry, such as an item from a mod that has gone.
     */
    public static <T> Codec<List<T>> eachOnItsOwn(Codec<T> entry, String what) {
        Codec<List<T>> whole = entry.listOf();
        return new Codec<>() {
            @Override
            public <O> DataResult<Pair<List<T>, O>> decode(DynamicOps<O> ops, O input) {
                return ops.getStream(input).map(items -> {
                    List<T> kept = new ArrayList<>();
                    items.forEach(item -> entry.parse(ops, item)
                            .resultOrPartial(error -> LOGGER.error("{} cannot be read and is dropped: {}", what, error))
                            .ifPresent(kept::add));
                    return Pair.of(List.copyOf(kept), ops.empty());
                });
            }

            @Override
            public <O> DataResult<O> encode(List<T> input, DynamicOps<O> ops, O prefix) {
                return whole.encode(input, ops, prefix);
            }
        };
    }
}
