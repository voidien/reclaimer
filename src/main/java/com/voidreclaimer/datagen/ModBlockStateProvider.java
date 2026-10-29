package com.voidreclaimer.datagen;

import com.voidreclaimer.VoidReclaimer;
import com.voidreclaimer.registry.ModBlocks;
import net.minecraft.data.PackOutput;
import net.neoforged.neoforge.client.model.generators.BlockStateProvider;
import net.neoforged.neoforge.common.data.ExistingFileHelper;

public final class ModBlockStateProvider extends BlockStateProvider {
    public ModBlockStateProvider(PackOutput output, ExistingFileHelper helper) {
        super(output, VoidReclaimer.MOD_ID, helper);
    }

    @Override
    protected void registerStatesAndModels() {
        simpleBlockWithItem(ModBlocks.SCAFFOLD.get(), cubeAll(ModBlocks.SCAFFOLD.get()));
        simpleBlockWithItem(ModBlocks.TETHER_ANCHOR.get(), cubeAll(ModBlocks.TETHER_ANCHOR.get()));
        simpleBlockWithItem(ModBlocks.BACKUP_CORE.get(), cubeAll(ModBlocks.BACKUP_CORE.get()));
        simpleBlockWithItem(ModBlocks.ASTEROID_SILICATE.get(), cubeAll(ModBlocks.ASTEROID_SILICATE.get()));
        simpleBlockWithItem(ModBlocks.ASTEROID_ICE.get(), cubeAll(ModBlocks.ASTEROID_ICE.get()));
        simpleBlockWithItem(ModBlocks.ASTEROID_CARBON.get(), cubeAll(ModBlocks.ASTEROID_CARBON.get()));
        simpleBlockWithItem(ModBlocks.ASTEROID_METALLIC.get(), cubeAll(ModBlocks.ASTEROID_METALLIC.get()));
    }
}
