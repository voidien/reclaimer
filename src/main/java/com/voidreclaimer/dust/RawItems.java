package com.voidreclaimer.dust;

import com.voidreclaimer.VoidReclaimer;
import java.util.Map;
import javax.annotation.Nullable;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;

/** The raw item each built-in dust kind condenses into: what machines in a cloud collect. */
public final class RawItems {
    private static final Map<ResourceLocation, ResourceLocation> BY_KIND = Map.of(
            DustKinds.METALLIC, VoidReclaimer.id("metallic_grains"),
            DustKinds.SILICATE, VoidReclaimer.id("silicon_grit"),
            DustKinds.ICE, VoidReclaimer.id("raw_ice_shards"),
            DustKinds.CARBON, VoidReclaimer.id("carbon_flakes"));

    private RawItems() {}

    /** The raw item for dust of {@code kind}, or null for a kind with none (datapack kinds). */
    @Nullable
    public static Item forKind(ResourceLocation kind) {
        ResourceLocation id = BY_KIND.get(kind);
        Item item = id == null ? Items.AIR : BuiltInRegistries.ITEM.get(id);
        return item == Items.AIR ? null : item;
    }
}
