package com.voidreclaimer.compat.jei;

import com.voidreclaimer.VoidReclaimer;
import com.voidreclaimer.registry.ModRecipeTypes;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import com.voidreclaimer.client.screen.ReplicatorScreen;
import java.util.Optional;
import mezz.jei.api.gui.builder.IClickableIngredientFactory;
import mezz.jei.api.gui.handlers.IGuiProperties;
import mezz.jei.api.gui.handlers.IScreenHandler;
import mezz.jei.api.registration.IGuiHandlerRegistration;
import mezz.jei.api.registration.IRecipeCategoryRegistration;
import mezz.jei.api.runtime.IClickableIngredient;
import net.minecraft.client.gui.screens.Screen;
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

    /**
     * The Replicator screen is not a container screen, so JEI is told its bounds (to keep its lists beside the panel) and which
     * item is under the mouse, which makes JEI's recipe and uses keys work on results, queued jobs and output items.
     */
    @Override
    public void registerGuiHandlers(IGuiHandlerRegistration registration) {
        registration.addGuiScreenHandler(ReplicatorScreen.class, new IScreenHandler<ReplicatorScreen>() {
            @Override
            public IGuiProperties apply(ReplicatorScreen screen) {
                return new IGuiProperties() {
                    @Override
                    public Class<? extends Screen> screenClass() {
                        return ReplicatorScreen.class;
                    }

                    @Override
                    public int guiLeft() {
                        return screen.guiLeft();
                    }

                    @Override
                    public int guiTop() {
                        return screen.guiTop();
                    }

                    @Override
                    public int guiXSize() {
                        return screen.guiWidth();
                    }

                    @Override
                    public int guiYSize() {
                        return screen.guiHeight();
                    }

                    @Override
                    public int screenWidth() {
                        return screen.width;
                    }

                    @Override
                    public int screenHeight() {
                        return screen.height;
                    }
                };
            }

            @Override
            public Optional<? extends IClickableIngredient<?>> getClickableIngredientUnderMouse(IClickableIngredientFactory factory,
                                                                                               ReplicatorScreen screen, double mouseX, double mouseY) {
                ReplicatorScreen.ItemHit hit = screen.itemAt(mouseX, mouseY);
                if (hit == null) {
                    return Optional.empty();
                }
                return factory.createBuilder(hit.stack()).buildWithArea(hit.x(), hit.y(), hit.width(), hit.height());
            }
        });
    }
}
