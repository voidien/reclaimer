package com.voidreclaimer.registry;

import com.voidreclaimer.VoidReclaimer;
import net.minecraft.tags.FluidTags;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.material.Fluid;

/** Item and fluid tags other mods and datapacks can add to. Block tags live in {@link ModBlocks}. */
public final class ModTags {
    /** Installed in a sensor slot, these items work as a Dust Sensor (cloud readout, pulse, hum, scanner). */
    public static final TagKey<Item> DUST_SENSOR_MODULES = ItemTags.create(VoidReclaimer.id("modules/dust_sensor"));

    /** Payloads the Kinetic Mass Driver launches as one Dyson Swarm Satellite each. */
    public static final TagKey<Item> DYSON_SATELLITE_PAYLOADS = ItemTags.create(VoidReclaimer.id("dyson/satellite_payloads"));
    /** Payloads the Kinetic Mass Driver launches as one Dyson Shell Truss Module each. */
    public static final TagKey<Item> DYSON_TRUSS_PAYLOADS = ItemTags.create(VoidReclaimer.id("dyson/truss_payloads"));

    /** What a Spire tower burns, one or more per pulse. */
    public static final TagKey<Item> SPIRE_CATALYSTS = ItemTags.create(VoidReclaimer.id("spire/catalysts"));
    /** Fluids a Spire tower takes as its water. */
    public static final TagKey<Fluid> SPIRE_WATER = FluidTags.create(VoidReclaimer.id("spire/water"));
    /** Fluids a Spire tower takes as its slurry. */
    public static final TagKey<Fluid> SPIRE_SLURRY = FluidTags.create(VoidReclaimer.id("spire/slurry"));

    private ModTags() {}
}
