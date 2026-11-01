package com.voidreclaimer.client.hud;

import com.voidreclaimer.link.SignalMath;
import com.voidreclaimer.registry.ModAttachments;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.util.RandomSource;

/** Static across the view as a link nears the edge of its signal: more dashes the weaker it is, changing every tick. */
public final class StaticLayer {
    private static final int MOST_DASHES = 400;
    private static final int LONGEST_DASH = 24;
    private static final int SEED_STEP = 31;

    private StaticLayer() {}

    public static void render(GuiGraphics graphics, DeltaTracker delta) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null) {
            return;
        }
        double level = SignalMath.staticLevel(minecraft.player.getData(ModAttachments.LINK_SIGNAL));
        if (level <= 0) {
            return;
        }
        int width = graphics.guiWidth();
        int height = graphics.guiHeight();
        RandomSource random = RandomSource.create(minecraft.player.tickCount * (long) SEED_STEP);
        int alpha = (int) (0x20 + 0x60 * level);
        for (int i = 0; i < (int) (MOST_DASHES * level); i++) {
            int x = random.nextInt(width);
            int y = random.nextInt(height);
            int grey = 0x80 + random.nextInt(0x80);
            graphics.fill(x, y, x + 1 + random.nextInt(LONGEST_DASH), y + 1, alpha << 24 | grey << 16 | grey << 8 | grey);
        }
    }
}
