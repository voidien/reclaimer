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
        basicItem(ModItems.BODY_BLUEPRINT.get());
        for (var shell : java.util.List.of(ModItems.DRONE_SHELL, ModItems.MECH_SHELL, ModItems.SHIP_SHELL, ModItems.SHUTTLE_SHELL)) {
            withExistingParent(shell.getId().getPath(), "item/generated").texture("layer0", modLoc("item/body_blueprint"));
        }
        basicItem(ModItems.PORTAL_IGNITER.get());
        basicItem(ModItems.PLANETARY_GRID.get());
        withExistingParent("test_starship", mcLoc("item/template_spawn_egg"));
        basicItem(ModItems.HELPER_DRONE.get());
        for (var material : com.voidreclaimer.registry.ModMaterials.ALL) {
            basicItem(material.item().get());
        }
        for (var incomplete : com.voidreclaimer.registry.ModMaterials.INCOMPLETE) {
            basicItem(incomplete.item().get());
        }
        for (var fluid : com.voidreclaimer.registry.ModFluids.ALL) {
            basicItem(fluid.bucket().get());
        }
        for (var item : ModItems.ITEMS.getEntries()) {
            if (item.get() instanceof SectionItem || item.get() instanceof ModuleItem && item != ModItems.DUST_SENSOR
                    && item != ModItems.SALVAGE_TOOL && item != ModItems.NANITE_SWARM) {
                basicItem(item.get());
            }
        }
    }
}
