package com.voidreclaimer.datagen;

import com.voidreclaimer.VoidReclaimer;
import com.voidreclaimer.registry.ModBlocks;
import java.util.concurrent.CompletableFuture;
import javax.annotation.Nullable;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.neoforged.neoforge.common.data.BlockTagsProvider;
import net.neoforged.neoforge.common.data.ExistingFileHelper;

public final class ModBlockTagsProvider extends BlockTagsProvider {
    public ModBlockTagsProvider(PackOutput output, CompletableFuture<HolderLookup.Provider> registries, @Nullable ExistingFileHelper helper) {
        super(output, registries, VoidReclaimer.MOD_ID, helper);
    }

    @Override
    protected void addTags(HolderLookup.Provider provider) {
        tag(net.minecraft.tags.BlockTags.MINEABLE_WITH_PICKAXE).add(ModBlocks.HOLO_PROJECTOR.get(), ModBlocks.HELM.get(), ModBlocks.STATION_CORE.get(), ModBlocks.DOCKING_CLAMP.get(), ModBlocks.RCS_THRUSTER.get(), ModBlocks.ION_THRUSTER.get(), ModBlocks.CHEMICAL_THRUSTER.get(), ModBlocks.PORTAL_FRAME.get(), ModBlocks.TERRAFORMING_SPIRE.get(), ModBlocks.ELECTROLYZER.get(), ModBlocks.SPINNER.get(), ModBlocks.CLOUD_SIPHON.get(),
                ModBlocks.KINETIC_DRIVE_BASE.get(), ModBlocks.PAYLOAD_CHAMBER.get(), ModBlocks.MAGNETIC_BARREL_NOZZLE.get(), ModBlocks.QUANTUM_RECEIVER.get(),
                ModBlocks.LAUNCH_CONTROLLER.get(), ModBlocks.LAUNCH_PAD.get());
        // Create's wrench picks these up whole.
        tag(net.minecraft.tags.BlockTags.create(net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("create", "wrench_pickup")))
                .add(ModBlocks.CHEMICAL_THRUSTER.get(), ModBlocks.ELECTROLYZER.get(), ModBlocks.SPINNER.get(), ModBlocks.CLOUD_SIPHON.get(),
                        ModBlocks.KINETIC_DRIVE_BASE.get(), ModBlocks.PAYLOAD_CHAMBER.get(), ModBlocks.MAGNETIC_BARREL_NOZZLE.get(), ModBlocks.QUANTUM_RECEIVER.get(),
                        ModBlocks.LAUNCH_CONTROLLER.get());
        // The launch complex's ring: our frame blocks, and Create's sturdier casings when Create is there.
        tag(ModBlocks.LAUNCH_FRAME).add(ModBlocks.SCAFFOLD.get(), ModBlocks.PORTAL_FRAME.get())
                .addOptional(net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("create", "industrial_iron_block"))
                .addOptional(net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("create", "andesite_casing"));
        tag(ModBlocks.ASTEROID_BLOCKS).add(ModBlocks.ASTEROID_SILICATE.get(), ModBlocks.ASTEROID_ICE.get(),
                ModBlocks.ASTEROID_CARBON.get(), ModBlocks.ASTEROID_METALLIC.get());
        tag(ModBlocks.SALVAGEABLE).add(ModBlocks.SCAFFOLD.get(), ModBlocks.TETHER_ANCHOR.get(), ModBlocks.BACKUP_CORE.get(),
                ModBlocks.PORTAL_FRAME.get(), ModBlocks.TERRAFORMING_SPIRE.get());
    }
}
