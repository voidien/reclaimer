package com.voidreclaimer.registry;

import com.mojang.serialization.Codec;
import com.voidreclaimer.VoidReclaimer;
import com.voidreclaimer.body.PartStats;
import java.util.function.Supplier;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.codec.ByteBufCodecs;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModDataComponents {
    public static final DeferredRegister.DataComponents COMPONENTS =
            DeferredRegister.createDataComponents(Registries.DATA_COMPONENT_TYPE, VoidReclaimer.MOD_ID);

    /** Nanite Swarm build distance in blocks. */
    public static final Supplier<DataComponentType<Integer>> NANITE_RANGE = COMPONENTS.registerComponentType("nanite_range",
            builder -> builder.persistent(Codec.INT).networkSynchronized(ByteBufCodecs.VAR_INT));

    /** Body part stats; set as a default component on part items. */
    public static final Supplier<DataComponentType<PartStats>> PART_STATS = COMPONENTS.registerComponentType("part_stats",
            builder -> builder.persistent(PartStats.CODEC).networkSynchronized(PartStats.STREAM_CODEC));

    private ModDataComponents() {}
}
