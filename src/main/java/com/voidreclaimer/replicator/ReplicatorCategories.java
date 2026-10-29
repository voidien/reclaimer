package com.voidreclaimer.replicator;

import com.voidreclaimer.VoidReclaimer;
import com.voidreclaimer.api.VoidReclaimerRegistries;
import com.voidreclaimer.api.replicator.ReplicatorCategory;
import com.voidreclaimer.registry.OrderedRegistry;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;

/** The built-in Replicator tabs and lookups in the {@code void_reclaimer:replicator_category} registry. */
public final class ReplicatorCategories {
    public static final ResourceKey<ReplicatorCategory> TOOLS = key("tools");
    public static final ResourceKey<ReplicatorCategory> MODULES = key("modules");
    public static final ResourceKey<ReplicatorCategory> PARTS = key("parts");
    public static final ResourceKey<ReplicatorCategory> BLOCKS = key("blocks");

    public static final Map<ResourceKey<ReplicatorCategory>, ReplicatorCategory> BUILTIN = builtin();

    private ReplicatorCategories() {}

    /** Registered tabs in order. */
    public static List<OrderedRegistry.Entry<ReplicatorCategory>> all(RegistryAccess access) {
        return OrderedRegistry.sorted(access, VoidReclaimerRegistries.REPLICATOR_CATEGORY, ReplicatorCategory::order);
    }

    public static Component title(OrderedRegistry.Entry<ReplicatorCategory> entry) {
        return entry.value().title(entry.key().location());
    }

    private static ResourceKey<ReplicatorCategory> key(String name) {
        return ResourceKey.create(VoidReclaimerRegistries.REPLICATOR_CATEGORY, VoidReclaimer.id(name));
    }

    private static Map<ResourceKey<ReplicatorCategory>, ReplicatorCategory> builtin() {
        Map<ResourceKey<ReplicatorCategory>, ReplicatorCategory> map = new LinkedHashMap<>();
        map.put(TOOLS, new ReplicatorCategory(0));
        map.put(MODULES, new ReplicatorCategory(1));
        map.put(PARTS, new ReplicatorCategory(2));
        map.put(BLOCKS, new ReplicatorCategory(3));
        return Map.copyOf(map);
    }
}
