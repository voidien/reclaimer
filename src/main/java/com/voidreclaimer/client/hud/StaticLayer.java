package com.voidreclaimer.client.hud;

import com.voidreclaimer.link.SignalMath;
import com.voidreclaimer.registry.ModAttachments;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.util.RandomSource;

/** Static across the view as a link nears the edge of its signal: more dashes the weaker it is, changing every tick. A lost signal also counts down. */
public final class StaticLayer {
    private static final int MOST_DASHES = 400;
    /** A storm at full strength gives this much static, enough to read through. */
    private static final float STORM_STATIC = 0.4f;
    private static final int LONGEST_DASH = 24;
    private static final int SEED_STEP = 31;
    private static final int HALF_SECOND = 10;
    private static final int WARNING = 0xFFFF5555;

    private StaticLayer() {}

    public static void render(GuiGraphics graphics, DeltaTracker delta) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null) {
            return;
        }
        double level = Math.max(SignalMath.staticLevel(minecraft.player.getData(ModAttachments.LINK_SIGNAL)),
                minecraft.level == null ? 0f : WeatherScramble.of(minecraft.level) * STORM_STATIC);
        if (level <= 0) {
            return;
        }
        int width = graphics.guiWidth();
        int height = graphics.guiHeight();
        int left = minecraft.player.getData(ModAttachments.LINK_LOSING);
        RandomSource random = RandomSource.create(minecraft.player.tickCount * (long) SEED_STEP);
        int alpha = (int) (0x20 + 0x60 * level);
        for (int i = 0; i < (int) (MOST_DASHES * level); i++) {
            int x = random.nextInt(width);
            int y = random.nextInt(height);
            int grey = 0x80 + random.nextInt(0x80);
            graphics.fill(x, y, x + 1 + random.nextInt(LONGEST_DASH), y + 1, alpha << 24 | grey << 16 | grey << 8 | grey);
        }
        // Flashes, on and off each half second, so it reads over the static.
        if (left > 0 && minecraft.player.tickCount / HALF_SECOND % 2 == 0) {
            Component text = Component.translatable("hud.void_reclaimer.link.losing", left);
            graphics.drawCenteredString(minecraft.font, text, width / 2, height / 3, WARNING);
        }
    }
}
