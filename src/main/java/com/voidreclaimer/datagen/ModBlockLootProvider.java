package com.voidreclaimer.datagen;

import com.voidreclaimer.registry.ModBlocks;
import java.util.Set;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.loot.BlockLootSubProvider;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.level.block.Block;

/** Block drops when broken without the Salvage Tool (which refunds dust instead). */
public final class ModBlockLootProvider extends BlockLootSubProvider {
    public ModBlockLootProvider(HolderLookup.Provider registries) {
        super(Set.of(), FeatureFlags.REGISTRY.allFlags(), registries);
    }

    @Override
    protected void generate() {
        dropSelf(ModBlocks.SCAFFOLD.get());
        dropSelf(ModBlocks.ASHEN_SLAG.get());
        dropSelf(ModBlocks.PORTAL_FRAME.get());
        dropSelf(ModBlocks.CHEMICAL_THRUSTER.get());
        dropSelf(ModBlocks.ION_THRUSTER.get());
        dropSelf(ModBlocks.TERRAFORMING_SPIRE.get());
        dropSelf(ModBlocks.TETHER_ANCHOR.get());
        dropSelf(ModBlocks.BACKUP_CORE.get());
        dropSelf(ModBlocks.ELECTROLYZER.get());
        dropSelf(ModBlocks.SPINNER.get());
        dropSelf(ModBlocks.CLOUD_SIPHON.get());
        dropSelf(ModBlocks.KINETIC_DRIVE_BASE.get());
        dropSelf(ModBlocks.PAYLOAD_CHAMBER.get());
        dropSelf(ModBlocks.MAGNETIC_BARREL_NOZZLE.get());
        dropSelf(ModBlocks.QUANTUM_RECEIVER.get());
        // What the Matter Harvester takes out whole; the Mechanical Drill grinds the block to dust instead.
        dropSelf(ModBlocks.ASTEROID_SILICATE.get());
        dropSelf(ModBlocks.ASTEROID_ICE.get());
        dropSelf(ModBlocks.ASTEROID_CARBON.get());
        dropSelf(ModBlocks.ASTEROID_METALLIC.get());
    }

    @Override
    protected Iterable<Block> getKnownBlocks() {
        return ModBlocks.BLOCKS.getEntries().stream().map(holder -> (Block) holder.get()).toList();
    }
}
