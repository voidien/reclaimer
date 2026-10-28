package com.voidreclaimer.compat.jei;

import com.voidreclaimer.VoidReclaimer;
import com.voidreclaimer.registry.ModRecipeTypes;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.registration.IRecipeCategoryRegistration;
import mezz.jei.api.registration.IRecipeRegistration;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;

/** JEI integration; only loaded when JEI is installed (JEI discovers it by annotation). */
@JeiPlugin
public final class VoidReclaimerJeiPlugin implements IModPlugin {
    @Override
    public ResourceLocation getPluginUid() {
        return VoidReclaimer.id("jei_plugin");
    }

    @Override
    public void registerCategories(IRecipeCategoryRegistration registration) {
        registration.addRecipeCategories(new ReplicatorJeiCategory(registration.getJeiHelpers().getGuiHelper()));
    }

    @Override
    public void registerRecipes(IRecipeRegistration registration) {
        if (Minecraft.getInstance().level != null) {
            registration.addRecipes(ReplicatorJeiCategory.TYPE,
                    Minecraft.getInstance().level.getRecipeManager().getAllRecipesFor(ModRecipeTypes.REPLICATOR.get()));
        }
    }
}
