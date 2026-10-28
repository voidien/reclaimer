package com.voidreclaimer.datagen;

import com.voidreclaimer.VoidReclaimer;
import net.minecraft.data.DataGenerator;
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
    }
}
