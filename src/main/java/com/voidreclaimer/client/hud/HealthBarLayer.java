package com.voidreclaimer.client.hud;

import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.entity.player.Player;

/**
 * Replaces the vanilla hearts: one bar left of the hotbar, longer and taller for bigger bodies ({@link HealthBarMath}),
 * with notches every 100 health and the numbers above it. Drawn with {@link GuiGraphics#fill} only, so HUD batching mods
 * keep working.
 */
public final class HealthBarLayer {
    private static final int OUTLINE = 0xFF101418;
    private static final int BACK = 0xFF2A1A1C;
    private static final int FILL = 0xFFD64545;
    private static final int FILL_LOW = 0xFFFF8A8A;
    private static final int NOTCH = 0x66000000;
    private static final int TEXT = 0xFFFFFFFF;
    private static final int TEXT_GAP = 10;

    private HealthBarLayer() {}

    public static void render(GuiGraphics graphics, DeltaTracker deltaTracker) {
        Minecraft mc = Minecraft.getInstance();
        // The replaced vanilla layer was wrapped in these conditions; replacing it drops them, so check here.
        if (mc.options.hideGui || mc.gameMode == null || !mc.gameMode.canHurtPlayer()) {
            return;
        }
        if (!(mc.getCameraEntity() instanceof Player player)) {
            return;
        }

        Gui gui = mc.gui;
        float max = Math.max(1f, player.getMaxHealth());
        float health = Math.min(player.getHealth(), max);
        int width = HealthBarMath.width(max);
        int height = HealthBarMath.height(max);
        int x = graphics.guiWidth() / 2 - 91;
        // Bottom edge where the hearts' bottom was (they were 9 tall), the numbers above the bar.
        int y = graphics.guiHeight() - gui.leftHeight + 9 - height;
        boolean low = health <= max * 0.25f && (gui.getGuiTicks() / 5) % 2 == 0;

        graphics.drawString(mc.font, Math.round(health) + " / " + Math.round(max), x, y - TEXT_GAP, TEXT, true);
        graphics.fill(x - 1, y - 1, x + width + 1, y + height + 1, OUTLINE);
        graphics.fill(x, y, x + width, y + height, BACK);
        int filled = HealthBarMath.fill(health, max, width);
        if (filled > 0) {
            graphics.fill(x, y, x + filled, y + height, low ? FILL_LOW : FILL);
        }
        for (int notch : HealthBarMath.notches(max, width)) {
            graphics.fill(x + notch, y, x + notch + 1, y + height, NOTCH);
        }
        gui.leftHeight += height + 2;
    }
}
