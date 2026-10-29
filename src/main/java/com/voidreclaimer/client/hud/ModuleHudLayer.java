package com.voidreclaimer.client.hud;

import com.voidreclaimer.body.Modules;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/** The active tool module, above the flight readout; dimmed while the main hand is busy (the module needs it free). */
public final class ModuleHudLayer {
    private ModuleHudLayer() {}

    public static void render(GuiGraphics graphics, DeltaTracker deltaTracker) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.options.hideGui || !(mc.getCameraEntity() instanceof Player player)) {
            return;
        }
        ItemStack tool = Modules.activeTool(player);
        if (tool.isEmpty()) {
            return;
        }
        int x = graphics.guiWidth() / 2 + 96;
        int y = graphics.guiHeight() - 50;
        boolean ready = player.getMainHandItem().isEmpty();
        graphics.renderItem(tool, x, y);
        graphics.drawString(mc.font, tool.getHoverName(), x + 18, y + 4, ready ? 0x9FE8FF : 0x6F8A99);
        if (!ready) {
            graphics.fill(x, y, x + 16, y + 16, 0x90000000);
        }
    }
}
