package com.voidreclaimer.client.hud;

import com.voidreclaimer.api.dust.DustKind;
import com.voidreclaimer.dust.DustKinds;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.RegistryAccess;
import net.minecraft.resources.ResourceLocation;

/** Draws a dust kind's icon: its sprite when the kind has one, else a square in its colour. Drawn through {@link GuiGraphics} only. */
public final class DustIcons {
    public static final int SIZE = 9;

    private DustIcons() {}

    public static void draw(GuiGraphics graphics, RegistryAccess access, ResourceLocation kindId, int x, int y, float alpha) {
        DustKind kind = DustKinds.get(access, kindId);
        if (kind != null && kind.icon().isPresent()) {
            graphics.setColor(1f, 1f, 1f, alpha);
            graphics.blitSprite(kind.icon().get(), x, y, SIZE, SIZE);
            graphics.setColor(1f, 1f, 1f, 1f);
        } else {
            int color = (Math.round(alpha * 255) << 24) | (DustKinds.color(access, kindId) & 0xFFFFFF);
            graphics.fill(x + 1, y + 1, x + SIZE - 1, y + SIZE - 1, color);
        }
    }
}
