package com.voidreclaimer.client.hud;

import com.voidreclaimer.dust.DustData;
import com.voidreclaimer.dust.DustKinds;
import com.voidreclaimer.registry.ModAttachments;
import com.voidreclaimer.space.ModDimensions;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;

/** Basic dust meter: one compact bar per dust kind in the top-left corner. */
public final class DustMeterLayer {
    private static final int X = 4;
    private static final int Y = 4;
    private static final int ROW = 10;
    private static final int BAR_WIDTH = 50;

    private DustMeterLayer() {}

    public static void render(GuiGraphics graphics, DeltaTracker deltaTracker) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.options.hideGui || mc.getDebugOverlay().showDebugScreen() || !(mc.getCameraEntity() instanceof Player player)) {
            return;
        }
        DustData dust = player.getData(ModAttachments.DUST);
        if (!ModDimensions.isDeepSpace(player.level()) && dust.equals(DustData.EMPTY)) {
            return;
        }
        float capacity = DustData.capacity(player);
        int y = Y;
        for (DustKinds.Entry entry : DustKinds.all(player.registryAccess())) {
            float amount = dust.get(entry.id());
            int color = 0xFF000000 | entry.kind().color();
            graphics.fill(X, y + 1, X + 6, y + 7, color);
            int barX = X + 9;
            graphics.fill(barX, y + 2, barX + BAR_WIDTH, y + 6, 0x90202028);
            int filled = Mth.clamp(Math.round(amount / capacity * BAR_WIDTH), 0, BAR_WIDTH);
            if (filled > 0) {
                graphics.fill(barX, y + 2, barX + filled, y + 6, color);
            }
            graphics.drawString(mc.font, Integer.toString(Mth.floor(amount)), barX + BAR_WIDTH + 4, y, 0xDDDDDD);
            y += ROW;
        }
    }
}
