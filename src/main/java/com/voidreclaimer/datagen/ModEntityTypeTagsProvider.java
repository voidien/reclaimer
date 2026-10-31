package com.voidreclaimer.datagen;

import com.voidreclaimer.VoidReclaimer;
import com.voidreclaimer.registry.ModEntities;
import dev.ryanhcode.sable.index.SableTags;
import java.util.concurrent.CompletableFuture;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.data.tags.EntityTypeTagsProvider;
import net.neoforged.neoforge.common.data.ExistingFileHelper;

/** Entity type tags: the Helm seat rides ships, so Sable must keep it in the ship's plot. */
public final class ModEntityTypeTagsProvider extends EntityTypeTagsProvider {
    public ModEntityTypeTagsProvider(PackOutput output, CompletableFuture<HolderLookup.Provider> registries, ExistingFileHelper helper) {
        super(output, registries, VoidReclaimer.MOD_ID, helper);
    }

    @Override
    protected void addTags(HolderLookup.Provider provider) {
        // Like Create's seat: stays in the sub-level (plot) and rides with the ship, and goes with the ship when it is removed.
        tag(SableTags.RETAIN_IN_SUB_LEVEL).add(ModEntities.HELM_SEAT.get());
        tag(SableTags.DESTROY_WITH_SUB_LEVEL).add(ModEntities.HELM_SEAT.get());
    }
}
