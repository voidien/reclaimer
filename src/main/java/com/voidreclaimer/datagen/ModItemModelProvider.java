package com.voidreclaimer.datagen;

import com.voidreclaimer.VoidReclaimer;
import com.voidreclaimer.body.part.ModuleItem;
import com.voidreclaimer.body.part.SectionItem;
import com.voidreclaimer.registry.ModItems;
import net.minecraft.data.PackOutput;
import net.neoforged.neoforge.client.model.generators.ItemModelProvider;
import net.neoforged.neoforge.common.data.ExistingFileHelper;

public final class ModItemModelProvider extends ItemModelProvider {
    public ModItemModelProvider(PackOutput output, ExistingFileHelper helper) {
        super(output, VoidReclaimer.MOD_ID, helper);
    }

    @Override
    protected void registerModels() {
        basicItem(ModItems.DUST_SENSOR.get());
        basicItem(ModItems.SALVAGE_TOOL.get());
        basicItem(ModItems.NANITE_SWARM.get());
        for (var item : ModItems.ITEMS.getEntries()) {
            if (item.get() instanceof SectionItem || item.get() instanceof ModuleItem && item != ModItems.DUST_SENSOR
                    && item != ModItems.SALVAGE_TOOL && item != ModItems.NANITE_SWARM) {
                basicItem(item.get());
            }
        }
    }
}
