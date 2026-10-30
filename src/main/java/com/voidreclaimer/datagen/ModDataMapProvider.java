package com.voidreclaimer.datagen;

import com.voidreclaimer.api.ship.ThrusterFuel;
import com.voidreclaimer.registry.ModDataMaps;
import com.voidreclaimer.registry.ModFluids;
import java.util.concurrent.CompletableFuture;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.data.PackOutput;
import net.neoforged.neoforge.common.data.DataMapProvider;

/** The built-in data map entries. */
public final class ModDataMapProvider extends DataMapProvider {
    public ModDataMapProvider(PackOutput output, CompletableFuture<HolderLookup.Provider> registries) {
        super(output, registries);
    }

    @Override
    protected void gather() {
        builder(ModDataMaps.THRUSTER_FUEL)
                .add(BuiltInRegistries.FLUID.wrapAsHolder(ModFluids.NEBULITE_PROPELLANT.source().get()), new ThrusterFuel(1f, 2f), false);
    }
}
