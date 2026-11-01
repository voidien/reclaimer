package com.voidreclaimer.registry;

import com.mojang.serialization.Codec;
import com.voidreclaimer.VoidReclaimer;
import com.voidreclaimer.api.VoidReclaimerRegistries;
import com.voidreclaimer.api.body.ModuleStats;
import com.voidreclaimer.api.body.SectionStats;
import com.voidreclaimer.api.body.ToolModuleType;
import net.minecraft.resources.ResourceKey;
import java.util.function.Supplier;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.codec.ByteBufCodecs;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModDataComponents {
    public static final DeferredRegister.DataComponents COMPONENTS =
            DeferredRegister.createDataComponents(Registries.DATA_COMPONENT_TYPE, VoidReclaimer.MOD_ID);

    /** The sectors of ruined Earth a Planetary Grid last read, one bit each (see {@code earth.SectorMap}). */
    public static final Supplier<DataComponentType<Integer>> PLANET_SECTORS = COMPONENTS.registerComponentType("planet_sectors",
            builder -> builder.persistent(Codec.INT).networkSynchronized(ByteBufCodecs.VAR_INT));

    /** Nanite Swarm build distance in blocks. */
    public static final Supplier<DataComponentType<Integer>> NANITE_RANGE = COMPONENTS.registerComponentType("nanite_range",
            builder -> builder.persistent(Codec.INT).networkSynchronized(ByteBufCodecs.VAR_INT));

    /** Body section stats; a default component on section items. */
    public static final Supplier<DataComponentType<SectionStats>> SECTION = COMPONENTS.registerComponentType("section",
            builder -> builder.persistent(SectionStats.CODEC).networkSynchronized(SectionStats.STREAM_CODEC));

    /** A body section's condition, 1 (new) to 0 (worn out); absent means new. See {@code body.Wear}. */
    public static final Supplier<DataComponentType<Float>> CONDITION = COMPONENTS.registerComponentType("condition",
            builder -> builder.persistent(Codec.floatRange(0f, 1f)).networkSynchronized(ByteBufCodecs.FLOAT));

    /** A saved body design on a Body Blueprint. */
    public static final Supplier<DataComponentType<com.voidreclaimer.body.Blueprint>> BLUEPRINT = COMPONENTS.registerComponentType(
            "blueprint", builder -> builder.persistent(com.voidreclaimer.body.Blueprint.CODEC)
                    .networkSynchronized(com.voidreclaimer.body.Blueprint.STREAM_CODEC));

    /** Body module stats; a default component on module items. */
    /** A Helper Drone item's source and destination. */
    public static final Supplier<DataComponentType<com.voidreclaimer.helper.HelperLinks>> HELPER_LINKS = COMPONENTS.registerComponentType("helper_links",
            builder -> builder.persistent(com.voidreclaimer.helper.HelperLinks.CODEC).networkSynchronized(com.voidreclaimer.helper.HelperLinks.STREAM_CODEC));
    /** A Helper Drone item's battery as a share of full, 0 to 1. Absent means full. */
    public static final Supplier<DataComponentType<Float>> HELPER_BATTERY = COMPONENTS.registerComponentType("helper_battery",
            builder -> builder.persistent(com.mojang.serialization.Codec.floatRange(0f, 1f)).networkSynchronized(net.minecraft.network.codec.ByteBufCodecs.FLOAT));
    public static final Supplier<DataComponentType<ModuleStats>> MODULE = COMPONENTS.registerComponentType("module",
            builder -> builder.persistent(ModuleStats.CODEC).networkSynchronized(ModuleStats.STREAM_CODEC));

    /**
     * What a tool module does: the id of an entry of {@code void_reclaimer:tool_module_type}. A key rather than the
     * entry itself, so items can name types registered after them.
     */
    public static final Supplier<DataComponentType<ResourceKey<ToolModuleType>>> TOOL_MODULE = COMPONENTS.registerComponentType(
            "tool_module", builder -> builder.persistent(ModCodecs.key(VoidReclaimerRegistries.TOOL_MODULE_TYPE))
                    .networkSynchronized(ResourceKey.streamCodec(VoidReclaimerRegistries.TOOL_MODULE_TYPE)));

    private ModDataComponents() {}
}
