package com.voidreclaimer.datagen;

import com.voidreclaimer.VoidReclaimer;
import com.voidreclaimer.registry.ModItems;
import com.voidreclaimer.registry.ModTags;
import java.util.concurrent.CompletableFuture;
import javax.annotation.Nullable;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.data.tags.ItemTagsProvider;
import net.minecraft.world.level.block.Block;
import net.neoforged.neoforge.common.data.ExistingFileHelper;

public final class ModItemTagsProvider extends ItemTagsProvider {
    public ModItemTagsProvider(PackOutput output, CompletableFuture<HolderLookup.Provider> registries,
                               CompletableFuture<TagLookup<Block>> blockTags, @Nullable ExistingFileHelper helper) {
        super(output, registries, blockTags, VoidReclaimer.MOD_ID, helper);
    }

    @Override
    protected void addTags(HolderLookup.Provider provider) {
        tag(ModTags.DUST_SENSOR_MODULES).add(ModItems.DUST_SENSOR.get());
        tag(ModTags.DYSON_SATELLITE_PAYLOADS).add(com.voidreclaimer.registry.ModMaterials.DYSON_SWARM_SATELLITE_PAYLOAD.get());
        tag(ModTags.DYSON_TRUSS_PAYLOADS).add(com.voidreclaimer.registry.ModMaterials.DYSON_SHELL_TRUSS_MODULE.get());
        tag(ModTags.SPIRE_CATALYSTS).add(com.voidreclaimer.registry.ModMaterials.ATMOSPHERIC_CATALYST_CORE.get());
    }
}
