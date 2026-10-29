package com.voidreclaimer.compat.jei;

import com.voidreclaimer.VoidReclaimer;
import com.voidreclaimer.dust.DustKinds;
import com.voidreclaimer.replicator.Milestones;
import java.util.List;
import net.minecraft.world.item.ItemStack;
import java.util.Map;
import net.minecraft.resources.ResourceLocation;
import com.voidreclaimer.registry.ModItems;
import com.voidreclaimer.registry.ModRecipeTypes;
import com.voidreclaimer.replicator.ReplicatorRecipe;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.category.IRecipeCategory;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.item.crafting.RecipeHolder;

/** JEI category listing Replicator recipes with their dust cost per kind, build time and unlocking milestone. */
final class ReplicatorJeiCategory implements IRecipeCategory<RecipeHolder<ReplicatorRecipe>> {
    static final RecipeType<RecipeHolder<ReplicatorRecipe>> TYPE = RecipeType.createFromDeferredVanilla(ModRecipeTypes.REPLICATOR).get();
    private static final int WIDTH = 160;
    /** Three text lines plus one row for the extra parts of a kit. */
    private static final int HEIGHT = 66;
    private static final int KIT_ROW_Y = 48;
    private static final int KIT_COLUMNS = 8;

    private final IDrawable icon;

    ReplicatorJeiCategory(IGuiHelper guiHelper) {
        this.icon = guiHelper.createDrawableItemLike(ModItems.DUST_SENSOR.get());
    }

    @Override
    public RecipeType<RecipeHolder<ReplicatorRecipe>> getRecipeType() {
        return TYPE;
    }

    @Override
    public Component getTitle() {
        return Component.translatable("replicator.void_reclaimer.title");
    }

    @Override
    public IDrawable getIcon() {
        return icon;
    }

    @Override
    public int getWidth() {
        return WIDTH;
    }

    @Override
    public int getHeight() {
        return HEIGHT;
    }

    @Override
    public void setRecipe(IRecipeLayoutBuilder builder, RecipeHolder<ReplicatorRecipe> holder, IFocusGroup focuses) {
        // The first result sits top right; a kit's other parts go in rows below the text.
        List<ItemStack> results = holder.value().results();
        builder.addSlot(RecipeIngredientRole.OUTPUT, WIDTH - 20, 4).addItemStack(results.get(0));
        for (int i = 1; i < results.size(); i++) {
            int index = i - 1;
            builder.addSlot(RecipeIngredientRole.OUTPUT, 1 + (index % KIT_COLUMNS) * 18, KIT_ROW_Y + (index / KIT_COLUMNS) * 18)
                    .addItemStack(results.get(i));
        }
    }

    @Override
    public void draw(RecipeHolder<ReplicatorRecipe> holder, IRecipeSlotsView slots, GuiGraphics graphics, double mouseX, double mouseY) {
        ReplicatorRecipe recipe = holder.value();
        Font font = Minecraft.getInstance().font;
        int x = 0;
        var access = Minecraft.getInstance().level.registryAccess();
        for (ResourceLocation kind : DustKinds.ordered(access, recipe.cost().amounts().keySet())) {
            float amount = recipe.cost().get(kind);
            if (amount > 0f) {
                graphics.fill(x, 5, x + 6, 11, 0xFF000000 | DustKinds.color(access, kind));
                graphics.drawString(font, Integer.toString(Mth.ceil(amount)), x + 8, 4, 0xFF404040, false);
                x += 30;
            }
        }
        graphics.drawString(font, Component.translatable("jei.void_reclaimer.time", Mth.ceil(recipe.ticks() / 20f)), 0, 20, 0xFF404040, false);
        graphics.drawString(font, Component.translatable("jei.void_reclaimer.unlock", Milestones.title(Minecraft.getInstance().level.registryAccess(), recipe.milestone())), 0, 34, 0xFF606060, false);
    }

}
