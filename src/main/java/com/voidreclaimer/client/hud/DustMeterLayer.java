package com.voidreclaimer.client.hud;

import com.voidreclaimer.dust.DustData;
import com.voidreclaimer.dust.DustField;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.resources.ResourceLocation;
import com.voidreclaimer.dust.DustKinds;
import com.voidreclaimer.registry.ModAttachments;
import com.voidreclaimer.space.ModDimensions;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;

/**
 * Dust meter in the top-left corner: for each dust kind its icon, a bar and the amount. The names appear beside the bars while a
 * screen is open. It dims after a while without any change (see {@link DustMeterFade}).
 */
public final class DustMeterLayer {
    private static final int X = 4;
    private static final int Y = 4;
    private static final int ROW = 11;
    private static final int BAR_WIDTH = 50;

    private static final Map<ResourceLocation, Integer> SHOWN = new HashMap<>();
    private static long lastChange;

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
        long now = player.level().getGameTime();
        boolean changed = false;
        for (DustKinds.Entry entry : DustKinds.all(player.registryAccess())) {
            int shown = Mth.floor(dust.get(entry.id()));
            Integer before = SHOWN.put(entry.id(), shown);
            changed |= before != null && before != shown;
        }
        if (changed || lastChange == 0 || lastChange > now) {
            lastChange = now;
        }
        DustField field = DustField.of(player.level());
        boolean inCloud = field != null && field.sample(player.getBoundingBox().getCenter()).inCloud();
        float alpha = DustMeterFade.alpha(now - lastChange, inCloud || mc.screen != null);
        int alphaBits = Math.round(alpha * 255) << 24;

        float capacity = DustData.capacity(player);
        int y = Y;
        for (DustKinds.Entry entry : DustKinds.all(player.registryAccess())) {
            float amount = dust.get(entry.id());
            int color = alphaBits | (entry.kind().color() & 0xFFFFFF);
            DustIcons.draw(graphics, player.registryAccess(), entry.id(), X, y, alpha);
            int barX = X + DustIcons.SIZE + 3;
            graphics.fill(barX, y + 2, barX + BAR_WIDTH, y + 6, ((int) (alpha * 0x90) << 24) | 0x202028);
            int filled = Mth.clamp(Math.round(amount / capacity * BAR_WIDTH), 0, BAR_WIDTH);
            if (filled > 0) {
                graphics.fill(barX, y + 2, barX + filled, y + 6, color);
            }
            int textX = barX + BAR_WIDTH + 4;
            String number = Integer.toString(Mth.floor(amount));
            graphics.drawString(mc.font, number, textX, y, alphaBits | 0xDDDDDD);
            if (mc.screen != null) {
                graphics.drawString(mc.font, entry.kind().displayName(entry.id()), textX + mc.font.width(number) + 5, y, alphaBits | 0x99AABB);
            }
            y += ROW;
        }
    }
}
