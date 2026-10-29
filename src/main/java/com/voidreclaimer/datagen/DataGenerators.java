package com.voidreclaimer.datagen;

import com.voidreclaimer.VoidReclaimer;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.DataGenerator;
import net.minecraft.data.loot.LootTableProvider;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.data.PackOutput;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.data.event.GatherDataEvent;

@EventBusSubscriber(modid = VoidReclaimer.MOD_ID)
public final class DataGenerators {
    private DataGenerators() {}

    @SubscribeEvent
    static void onGatherData(GatherDataEvent event) {
        DataGenerator generator = event.getGenerator();
        PackOutput output = generator.getPackOutput();
        generator.addProvider(event.includeClient(), new ModLangProvider(output));
        generator.addProvider(event.includeClient(), new ModSoundDefinitionsProvider(output, event.getExistingFileHelper()));
        generator.addProvider(event.includeClient(), new ModItemModelProvider(output, event.getExistingFileHelper()));
        generator.addProvider(event.includeClient(), new ModBlockStateProvider(output, event.getExistingFileHelper()));

        ModDatapackProvider datapack = generator.addProvider(event.includeServer(),
                new ModDatapackProvider(output, event.getLookupProvider()));
        CompletableFuture<HolderLookup.Provider> registries = datapack.getRegistryProvider();
        generator.addProvider(event.includeServer(), new ModRecipeProvider(output, registries));
        generator.addProvider(event.includeServer(), new ModBlockTagsProvider(output, registries, event.getExistingFileHelper()));
        generator.addProvider(event.includeServer(), new LootTableProvider(output, Set.of(),
                List.of(new LootTableProvider.SubProviderEntry(ModBlockLootProvider::new, LootContextParamSets.BLOCK)), registries));
    }
}
