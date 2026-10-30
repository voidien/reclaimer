package com.voidreclaimer.client.hud;

import com.voidreclaimer.VoidReclaimer;
import com.voidreclaimer.energy.EnergyData;
import com.voidreclaimer.registry.ModAttachments;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

/**
 * Replaces the vanilla food bar: ten energy cells in the same slot, right of the hotbar. Empty cells flash red while
 * in low power mode. Drawn only through {@link GuiGraphics#blitSprite} so HUD batching mods keep working.
 *
 * <p>Each sprite file is a strip of ten identical battery cells (90x9, as the art was delivered), so one cell is the
 * strip's leftmost 9x9: drawing the whole sprite into a 9x9 box squeezed all ten cells into one (a comb).
 */
public final class EnergyBarLayer {
    private static final ResourceLocation EMPTY = VoidReclaimer.id("hud/energy_empty");
    private static final ResourceLocation EMPTY_WARNING = VoidReclaimer.id("hud/energy_empty_warning");
    private static final ResourceLocation HALF = VoidReclaimer.id("hud/energy_half");
    private static final ResourceLocation FULL = VoidReclaimer.id("hud/energy_full");
    private static final int CELLS = 10;
    private static final int SIZE = 9;
    /** Size of each sprite file: {@link #CELLS} cells side by side. */
    private static final int STRIP_WIDTH = 90;

    private EnergyBarLayer() {}

    public static void render(GuiGraphics graphics, DeltaTracker deltaTracker) {
        Minecraft mc = Minecraft.getInstance();
        // The replaced vanilla layer was wrapped in these conditions; replacing it drops them, so check here.
        if (mc.options.hideGui || mc.gameMode == null || !mc.gameMode.canHurtPlayer()) {
            return;
        }
        if (!(mc.getCameraEntity() instanceof Player player)) {
            return;
        }
        // Like vanilla food, yield the slot to a ridden mob's health bar.
        if (player.getVehicle() instanceof LivingEntity vehicle && vehicle.showVehicleHealth()) {
            return;
        }

        Gui gui = mc.gui;
        EnergyData data = player.getData(ModAttachments.ENERGY);
        int halves = data.halfIcons();
        boolean flash = data.lowPower() && (gui.getGuiTicks() / 10) % 2 == 0;
        int right = graphics.guiWidth() / 2 + 91;
        int top = graphics.guiHeight() - gui.rightHeight;

        for (int i = 0; i < CELLS; i++) {
            int x = right - i * 8 - SIZE;
            cell(graphics, flash ? EMPTY_WARNING : EMPTY, x, top);
            int filled = halves - i * 2;
            if (filled >= 2) {
                cell(graphics, FULL, x, top);
            } else if (filled == 1) {
                cell(graphics, HALF, x, top);
            }
        }
        gui.rightHeight += 10;
    }

    /** One cell: the leftmost 9x9 of the 90x9 strip. */
    private static void cell(GuiGraphics graphics, ResourceLocation sprite, int x, int y) {
        graphics.blitSprite(sprite, STRIP_WIDTH, SIZE, 0, 0, x, y, SIZE, SIZE);
    }
}
