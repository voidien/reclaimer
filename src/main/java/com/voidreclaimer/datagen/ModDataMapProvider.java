package com.voidreclaimer.datagen;

import com.voidreclaimer.api.ship.ThrusterFuel;
import com.voidreclaimer.registry.ModDataMaps;
import com.voidreclaimer.registry.ModFluids;
import java.util.concurrent.CompletableFuture;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.material.Fluid;
import net.neoforged.neoforge.common.data.DataMapProvider;

/** The built-in data map entries. */
public final class ModDataMapProvider extends DataMapProvider {
    public ModDataMapProvider(PackOutput output, CompletableFuture<HolderLookup.Provider> registries) {
        super(output, registries);
    }

    private static void tfmg(Builder<ThrusterFuel, Fluid> fuels, String id, ThrusterFuel fuel) {
        fuels.add(ResourceKey.create(Registries.FLUID, ResourceLocation.fromNamespaceAndPath("tfmg", id)), fuel, false);
    }

    @Override
    protected void gather() {
        var fuels = builder(ModDataMaps.THRUSTER_FUEL)
                .add(BuiltInRegistries.FLUID.wrapAsHolder(ModFluids.NEBULITE_PROPELLANT.source().get()), new ThrusterFuel(1f, 2f), false);
        // The Factory Must Grow (a required mod): weaker and thirstier than our propellant, which stays the reward for its chain.
        tfmg(fuels, "kerosene", new ThrusterFuel(0.8f, 3f));
        tfmg(fuels, "lpg", new ThrusterFuel(0.7f, 3f));
        tfmg(fuels, "gasoline", new ThrusterFuel(0.6f, 3f));
        tfmg(fuels, "diesel", new ThrusterFuel(0.5f, 3f));
    }
}
