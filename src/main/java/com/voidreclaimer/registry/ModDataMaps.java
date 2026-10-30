package com.voidreclaimer.registry;

import com.voidreclaimer.VoidReclaimer;
import com.voidreclaimer.api.ship.ThrusterFuel;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.material.Fluid;
import net.neoforged.neoforge.registries.datamaps.DataMapType;
import net.neoforged.neoforge.registries.datamaps.RegisterDataMapTypesEvent;

/** The mod's data maps: per-entry values on vanilla registries, in {@code data/<ns>/data_maps/}. */
public final class ModDataMaps {
    /** What each fluid is worth in a Chemical Thruster. Server side only: the thruster runs on the server. */
    public static final DataMapType<Fluid, ThrusterFuel> THRUSTER_FUEL =
            DataMapType.builder(VoidReclaimer.id("thruster_fuel"), Registries.FLUID, ThrusterFuel.CODEC).build();

    private ModDataMaps() {}

    public static void register(RegisterDataMapTypesEvent event) {
        event.register(THRUSTER_FUEL);
    }
}
