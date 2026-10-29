package com.voidreclaimer.registry;

import com.voidreclaimer.VoidReclaimer;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;

/** Item tags other mods and datapacks can add to. Block tags live in {@link ModBlocks}. */
public final class ModTags {
    /** Installed in a sensor slot, these items work as a Dust Sensor (cloud readout, pulse, hum, scanner). */
    public static final TagKey<Item> DUST_SENSOR_MODULES = ItemTags.create(VoidReclaimer.id("modules/dust_sensor"));

    private ModTags() {}
}
