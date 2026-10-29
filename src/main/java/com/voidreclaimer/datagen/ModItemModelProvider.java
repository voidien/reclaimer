package com.voidreclaimer.datagen;

import com.voidreclaimer.VoidReclaimer;
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
        for (var part : java.util.List.of(ModItems.DRONE_FRAME, ModItems.MECH_FRAME, ModItems.SHIP_FRAME, ModItems.CORE_MK1,
                ModItems.CORE_MK2, ModItems.THRUSTER_MK1, ModItems.THRUSTER_MK2, ModItems.STORAGE_MK1, ModItems.STORAGE_MK2,
                ModItems.BRASS_TRIM, ModItems.COGWHEEL_CROWN)) {
            basicItem(part.get());
        }
    }
}
