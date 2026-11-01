package com.voidreclaimer.datagen;

import com.voidreclaimer.VoidReclaimer;
import com.voidreclaimer.registry.ModFluids;
import com.voidreclaimer.registry.ModTags;
import java.util.concurrent.CompletableFuture;
import javax.annotation.Nullable;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.data.tags.FluidTagsProvider;
import net.neoforged.neoforge.common.data.ExistingFileHelper;

public final class ModFluidTagsProvider extends FluidTagsProvider {
    public ModFluidTagsProvider(PackOutput output, CompletableFuture<HolderLookup.Provider> registries, @Nullable ExistingFileHelper helper) {
        super(output, registries, VoidReclaimer.MOD_ID, helper);
    }

    @Override
    protected void addTags(HolderLookup.Provider provider) {
        tag(ModTags.SPIRE_WATER).add(ModFluids.PURIFIED_WATER.source().get());
        tag(ModTags.SPIRE_SLURRY).add(ModFluids.BIO_NUTRIENT_SLURRY.source().get());
    }
}
