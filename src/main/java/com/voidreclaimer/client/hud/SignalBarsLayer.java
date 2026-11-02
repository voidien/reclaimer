package com.voidreclaimer.client.hud;

import com.voidreclaimer.link.SignalMath;
import com.voidreclaimer.registry.ModAttachments;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;

/** The link's strength while the mind is in a shell: bars in the top right corner, the way a phone shows it. */
public final class SignalBarsLayer {
    private static final int MARGIN = 6;
    private static final int BAR_WIDTH = 3;
    private static final int BAR_GAP = 1;
    private static final int BAR_STEP = 3;
    private static final int LIT = 0xFF9FE8FF;
    private static final int WEAK = 0xFFFF5555;
    private static final int UNLIT = 0x60FFFFFF;

    private SignalBarsLayer() {}

    public static void render(GuiGraphics graphics, DeltaTracker delta) {
        Minecraft minecraft = Minecraft.getInstance();
        // Negative with no link: the main body's dust storage is what says the mind is away.
        if (minecraft.player == null || minecraft.options.hideGui || minecraft.player.getData(ModAttachments.HOME_DUST_CAPACITY) < 0f) {
            return;
        }
        int lit = SignalMath.bars(minecraft.player.getData(ModAttachments.LINK_SIGNAL));
        int colour = lit <= 1 ? WEAK : LIT;
        int bottom = MARGIN + SignalMath.BARS * BAR_STEP;
        int left = graphics.guiWidth() - MARGIN - SignalMath.BARS * (BAR_WIDTH + BAR_GAP);
        for (int bar = 0; bar < SignalMath.BARS; bar++) {
            int x = left + bar * (BAR_WIDTH + BAR_GAP);
            graphics.fill(x, bottom - (bar + 1) * BAR_STEP, x + BAR_WIDTH, bottom, bar < lit ? colour : UNLIT);
        }
    }
}
